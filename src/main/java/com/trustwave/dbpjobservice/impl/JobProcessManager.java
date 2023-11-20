package com.trustwave.dbpjobservice.impl;



import static com.trustwave.dbpjobservice.interfaces.DetailedInfoScope.AllTasks;
import static com.trustwave.dbpjobservice.interfaces.DetailedInfoScope.None;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.transaction.annotation.Transactional;





















import com.googlecode.sarasvati.Engine;
import com.googlecode.sarasvati.GraphProcess;
import com.googlecode.sarasvati.NodeToken;
import com.googlecode.sarasvati.env.Env;
import com.googlecode.sarasvati.event.ExecutionEventType;
import com.trustwave.dbpjobservice.actions.ValidationDescriptor;
import com.trustwave.dbpjobservice.backend.JobProcessDao;
import com.trustwave.dbpjobservice.backend.JobProcessRecord;
import com.trustwave.dbpjobservice.backend.JobRecord;
import com.trustwave.dbpjobservice.backend.JobTemplateRecord;
import com.trustwave.dbpjobservice.backend.ProcessTaskCountsRecord;
import com.trustwave.dbpjobservice.impl.exceptions.WorkflowException;
import com.trustwave.dbpjobservice.interfaces.DetailedInfoScope;
import com.trustwave.dbpjobservice.interfaces.Event;
import com.trustwave.dbpjobservice.interfaces.ExternalParameterValue;
import com.trustwave.dbpjobservice.interfaces.Job;
import com.trustwave.dbpjobservice.interfaces.JobProcessInfo;
import com.trustwave.dbpjobservice.parameters.EnvironmentManager;
import com.trustwave.dbpjobservice.parameters.ParameterDescriptor;
import com.trustwave.dbpjobservice.parameters.ParameterManager;
import com.trustwave.dbpjobservice.parameters.TypedEnvironment;
import com.trustwave.dbpjobservice.workflow.ActionManager;
import com.trustwave.dbpjobservice.workflow.ArcEvaluator;
import com.trustwave.dbpjobservice.workflow.EngineFactory;
import com.trustwave.dbpjobservice.workflow.EventManager;
import com.trustwave.dbpjobservice.workflow.WorkflowExecutor;
import com.trustwave.dbpjobservice.workflow.WorkflowManager;
import com.trustwave.dbpjobservice.workflow.WorkflowTask;
import com.trustwave.dbpjobservice.workflow.WorkflowUtils;
import com.trustwave.dbpjobservice.workflow.api.action.IJobAction;
import com.trustwave.dbpjobservice.workflow.api.util.TimeLogger;
import com.trustwave.dbpjobservice.xml.XmlExitCondition;

public class JobProcessManager 
{
	private static Logger logger = LogManager.getLogger( JobProcessManager.class );
	private static JobProcessManager instance;

	/** <p>Get instance of this class (singleton).</p>
	 *  <p>WARNING: returned instance does NOT provide implicit transactions
	 *  in method annotated with @Transactional. Though instance is created
	 *  with Spring (via init() method below), it's an instance of this class,
	 *  not an instance of transactional proxy!  If transactionality is required,
	 *  use Spring-populated property, as JobServiceImpl does.
	 *  </p> 
	 */
	public static JobProcessManager getInstance() {
		return instance;
	}
	
	// this method is used only for testing
	public static JobProcessManager setInstance( JobProcessManager inst )
	{
		JobProcessManager inst0 = instance;
		instance = inst;
		return inst0;
	}
	
	public void init() {
		instance = this;
	}

	private WorkflowManager workflowManager;
	private JobManager         jobManager;
	private WorkflowExecutor workflowExecutor;
	private JobProcessDao processDao;
	private JobStatusManager   jobStatusManager;
	private JobStatusRetriever jobStatusRetriever;
	private EventManager eventManager;
	private ActionManager actionManager;
	private EnvironmentManager environmentManager;
	private EngineFactory engineFactory;
	private Configuration      config;
	private Timer              timer = null;

	
	public ActionManager getActionManager() 
	{
		return actionManager;
	}
	
	public void setActionManager(ActionManager actionManager) 
	{
		this.actionManager = actionManager;
	}

	@Transactional
	public JobProcessRecord createJobProcess( long jobId )
	{
		return createJobProcess( jobId, new ArrayList<ExternalParameterValue>() );
	}
	
	@Transactional
	public JobProcessRecord createPublicJobProcess( long jobId,
			                                        List<ExternalParameterValue> parameters)
	{
		checkPublicJob( jobId );
		JobProcessRecord jpr = createJobProcess( jobId, parameters );
		return jpr;
	}
	
	void checkPublicJob( long jobId )
	{
		Job job = null;
		try {
			job = getJobManager().findJob( jobId );
			if (job.getId() == getJobManager().getTestCredentialsJob( job.getOrgId() ).getId() ) {
				return;
			}
		} 
		catch (Exception e) {
		}
		throw new RuntimeException( Messages.getString("job.create.instance.notPublic", (job != null? job.getName(): ""+jobId)) );
	}
	
	@Transactional
	public JobProcessRecord createJobProcess(
			long jobId, List<ExternalParameterValue> additionalParameters )
	{
		Job job = getJobManager().findJob( jobId );
		if (additionalParameters != null) {
			job.mergeParameters( additionalParameters );
		}
		JobRecord jr = getJobManager().findJobRecord( jobId );
		JobTemplateRecord jtr = jr.getJobTemplate();
		ParameterManager pm = getJobManager().getParameterManager( jtr );
		
		Map<String, String> parametersMap =	new HashMap<String, String>();
		for (ExternalParameterValue p: job.getParameters()) {
			ParameterDescriptor pd = pm.getFreeParameterNotNull( p.getName() );
			String value = pd.externalParameterValueToString( p );
			parametersMap.put( p.getName(), value );
		}
		
		GraphProcess process = engineFactory.createProcess( jtr.getGraph() );
		long id = engineFactory.getProcessId( process );
		JobProcessRecord jpr = new JobProcessRecord( id, jr );
		pm.populateProcessEnv( process, parametersMap );
		List<ValidationDescriptor> validationList =
				pm.prepareValidationList( job.getParameters() );
		populateImplicitParameters( process, jpr, validationList );
		getProcessDao().saveJobProcess( jpr );
		logger.info( "Created process " + jpr + ", org=" + job.getOrgId()
				   + "; parameters=" + job.getParameters() );
		return jpr;
	}
	
	private void populateImplicitParameters( GraphProcess process,
			                                 JobProcessRecord jpr,
			                                 List<ValidationDescriptor> validationList )
	{
		TypedEnvironment tenv = environmentManager.getProcessEnvironment( process );
		tenv.setAttribute( "jobName",       jpr.getJob().getName() );
		tenv.setAttribute( "jobTemplate",   process.getGraph().getName() );
		tenv.setAttribute( "processId",     "" + jpr.getId() );
		tenv.setAttribute( "jobInstanceId", "" + jpr.getId() );
		tenv.setAttribute( "jobId",         "" + jpr.getJob().getJobId() );
		tenv.setAttribute( "jobOrgId",      "" + jpr.getJob().getOrgId() );
		tenv.setAttribute( "validationList", validationList );
	}
	
	private List<JobProcessInfo> getProcessListInfo(
					List<JobProcessRecord> jprList, 
					Map<Long, ProcessTaskCountsRecord> taskCountsMap )
	{
		List<JobProcessInfo> piList = new ArrayList<JobProcessInfo>();
		JobStatusRetriever retriever = getJobStatusRetriever();
		TimeLogger tlog = new TimeLogger( logger, "proclist", 10 );
		
		for (JobProcessRecord jpr: jprList) {
			if (!jobManager.isUtilityJob( jpr.getJob() )) {
				ProcessTaskCountsRecord taskCounts =
						taskCountsMap.get( jpr.getId() );
				JobProcessInfo pi =	retriever.getProcessInfo( jpr, None, taskCounts );
				piList.add( pi );
			}
		}
		
		tlog.log( "proclist, size=", piList.size() );
		return piList; 
	}
	
	@Transactional
	public List<JobProcessInfo> getActiveProcesses()
	{
		return getProcessListInfo( getProcessDao().getRunningProcesses(),
				                   getProcessDao().getActiveTaskCounts() );
	}
	
	@Transactional
	public List<JobProcessInfo> getAllProcesses()
	{
		return getProcessListInfo( getProcessDao().getAllProcesses(),
                                   getProcessDao().getActiveTaskCounts() );
	}
	
	@Transactional
	public List<JobProcessInfo> getProcessesForJob( long jobId )
	{
		return getProcessListInfo( getProcessDao().getProcessesForJob( jobId ),
                                   getProcessDao().getActiveTaskCounts() );
	}
	
	@Transactional
	public List<JobProcessInfo> getCompletedJobInstances( 
			Calendar atOrAfter, Calendar before ) 
	{
		Date from = (atOrAfter != null? atOrAfter.getTime(): new Date(0));
		Date to   = (before != null? before.getTime(): new Date());
		return getProcessListInfo( getProcessDao().getCompletedProcesses( from, to ),
			                       new HashMap<Long,ProcessTaskCountsRecord>() );
	}
	
	public JobProcessInfo getProcessesInfo( long processId )
	{
		return getProcessesInfoWithRetry( processId, AllTasks );
	}
	
	@Transactional
	public JobProcessInfo getProcessesInfo( long processId, DetailedInfoScope detailsScope )
	{
		JobProcessRecord jpr = getProcessDao().getProcess( processId );
		return getJobStatusRetriever().getProcessInfo( jpr, detailsScope, null );
	}
	
	public JobProcessInfo getProcessesInfoWithRetry( long processId, DetailedInfoScope detailsScope )
	{
		RuntimeException ex = null;
		int attempt = 0;
		
		while (true) {
			try {
				JobProcessInfo pi = getProcessesInfo( processId, detailsScope );
				if (attempt > 0) {
					logger.info( "Retry after deadlock was successfull, attempt=" + attempt );
				}
				return pi;
			}
			catch (RuntimeException e) {
				ex = e;
			}
			
			if (!containsException( ex, LockAcquisitionException.class )) {
				throw ex;
			}
			if (++attempt >= 3) {
				break;
			}
			logger.info( "Deadlock on getProcessInfo(), retrying, attempt=" + attempt );
			try { Thread.sleep( 100 ); } catch (InterruptedException e1) {}
		}
		
		logger.error( ex.getMessage() +  "- too many retry attempts, giving up", ex );
		throw ex;
	}
	
	private boolean containsException( Throwable e, Class<? extends Throwable> targetClass )
	{
		for (int i = 0;  i < 6 && e != null; i++) {
			if (targetClass.isAssignableFrom( e.getClass() )) { 
				return true;
			}
			e = e.getCause();
		}
		return false;
	}
	 
	
	@Transactional
	public JobProcessInfo getPublicProcessesInfo( long processId, DetailedInfoScope detailsScope )
	{
		JobProcessRecord jpr = getProcessRecordById( processId );
		checkPublicJob( jpr.getJob().getJobId() );
		return getJobStatusRetriever().getProcessInfo( jpr, detailsScope, null );
	}
	
	public JobProcessRecord getProcessRecordById( long processId )
	{
		JobProcessRecord jpr = getProcessDao().getProcess( processId );
		if (jpr == null) {
			throw new RuntimeException(Messages.getString("instance.notFound", processId));
		}
		return jpr;
	}
	
	/**
	 * Resume previously paused process
	 * @param processId
	 */
	@Transactional
	public void resumePausedProcess( final long processId )
	{
		final JobProcessRecord jpr = getProcessRecordById( processId );
		if (jpr.isPaused()) {
			logger.info( "Resuming paused process " + jpr );
			jpr.setPaused( false );
		}
		if (!jpr.isActive()) {
			activateInactiveProcess( jpr );
		}
		
		getWorkflowExecutor().submit(
				new WorkflowTask( "resumeProcessActions(" + jpr + ")", processId, null ) {
					public void run() {
						getActionManager().resumeProcessActions(processId);
					}
				});
	}
	
	/**
	 * <p>Start or resume inactive process.</p> 
	 * <p>Normally this method called after createJobProcess()</p>
	 * @param processId
	 */
	@Transactional
	public void resumeInactiveProcess( long processId )
	{
		JobProcessRecord jpr = getProcessRecordById( processId );
		activateInactiveProcess( jpr );
	}

	/** 
	 * Invoked by job service on startup
	 */
	@Transactional
	public void resumeAllInactiveProcesses()
	{
		logger.info( "Resuming all inactive processes ..." );
		List<JobProcessRecord> procList =
			getProcessDao().getRunningProcesses();
		for (JobProcessRecord jpr: procList) {
			if (!jpr.isActive() && !jpr.isPaused()) {
				activateInactiveProcess( jpr );
			}
		}
	}
	
	public void optionallyResumeJobInstancesOnStartup()
	{
		if (config.isResumeJobInstancesOnStartup()) {
			resumeAllInactiveProcesses();
		}
		else {
			logger.info( "Auto-resuming job instances on startup is disabled" );
		}
	}
	
	/** 
	 * Delete a process
	 */
	@Transactional
	public void deleteCompletedProcessInfo(long processId)
	{
		JobProcessRecord jpr = getProcessRecordById(processId);
		if (!jpr.isFinished()) {
			throw new RuntimeException(Messages.getString("delete.instance.running"));
		}
		getProcessDao().deleteProcess( jpr );
		engineFactory.deleteProcess( processId );
		logger.info( "Deleted process " + jpr );
	}

	/**
	 * <p>Start or resume inactive process.<p/>
	 * <p>Inactive processes are those that are not executed by engine.
	 * Process becomes inactive:
	 * <ul><li>after initial process creation</li>
	 *     <li>after process was paused</p>  
	 *     <li>when job service is restarted</li>
	 * </ul></p>
	 * <p>Process should have 'active' flag set to <code>false</code> to be resumed.
	 * When process is resumed, it's 'active' flag is set to <code>true</code>.</p>
	 * 
	 * @param jpr  Job process record
	 */
	private void activateInactiveProcess( final JobProcessRecord jpr )
	{
		if (jpr.isPaused()) {
			logger.warn( "Process " + jpr + " is paused, not activating" );
			return;
		}
		if (jpr.isActive()) {
			logger.warn( "Process " + jpr + " is already active, not activating" );
			return;
		}
		jpr.setActive( true );
		getWorkflowExecutor().submit(
			new WorkflowTask( "resumeInactiveProcess(" + jpr + ")", jpr.getId(), null ) {
				public void run() {
					logger.info( "Resuming inactive process " + jpr );
					getWorkflowManager().resumeProcess( jpr.getId(), getEngine() );
				}
			});
	}
	
	/**
	 * Populate action parameters from token content
	 * @param action
	 * @param token
	 */
	public void populateAction( IJobAction action, NodeToken token )
	{
		ParameterManager pm = getParameterManager( token );
		pm.populateActionParameters( action, token );
	}
	
	public Map<String,Object> getActionParameters( IJobAction action, NodeToken token )
	{
		ParameterManager pm = getParameterManager( token );
		return pm.getActionParameters( action, token ); 
	}
	
	/** 
	 * Save all action output parameters in the associated token
	 * Normally called asynchronously, so we need transaction 
	 * @param action
	 * @param actionComplete TODO
	 */
	public void saveActionOutput( final IJobAction action, final boolean actionComplete )
	{
		if (config.isSaveOutputInWorkflowThread()) {
			getWorkflowExecutor().submit(
					new WorkflowTask( "saveActionOutput(" + action + ")",
					          		  action.getProcessId(), action ) {
						public void run() {
							doSaveActionOutput( action, actionComplete );
						}
					});
		}
		else {
			saveActionOutputTransactional( action, actionComplete );
		}
	}
	
	@Transactional
	public void saveActionOutputTransactional( final IJobAction action, final boolean actionComplete )
	{
		doSaveActionOutput( action, actionComplete );
	}
	
	private void doSaveActionOutput( IJobAction action, boolean actionComplete )
	{
		NodeToken token =
			getWorkflowManager().getTokenById( action.getTokenId(), action.getProcessId() );
		if (token == null) {
			throw new WorkflowException( Messages.getString("workflow.action.token.notFound", action.getTokenId(), action) );
		}
		ParameterManager pm = getParameterManager( token );
		try {
			pm.saveActionOutput( action, token, actionComplete );
		} 
		catch (Exception e) {
			logger.error( "Cannot save action output: " + e.getMessage(), e );
			action.setCondition( XmlExitCondition.WORKFLOW_ERROR, e.getMessage() );
			pm.saveStateOnly( action, token );
		}
		// report changes in status parameters to status manager
		getJobStatusManager().onSaveActionOutput( action, token, pm.getEnvironment(token), actionComplete );
	}
	
	/** 
	 * Save all action output parameters in the associated token
	 * @param action
	 */
	public void completeAction( final IJobAction action )
	{
		getWorkflowExecutor().submit(
			new WorkflowTask( "completeAction(" + action + ")",
					          action.getProcessId(), action ) 
			{
				public void run() {
					doCompleteAction( action, getEngine() );
				
			}
		});
	}
	
	private void doCompleteAction( IJobAction action, Engine engine )
	{
		NodeToken token =
				getWorkflowManager().getTokenById( action.getTokenId(), action.getProcessId() );
		if (token == null) {
			throw new WorkflowException( Messages.getString("workflow.action.token.notFound", action.getTokenId(), action) );
		}
		
		ParameterManager pm = getParameterManager( token );
		
		String tsName = pm.getTokenSetName( action );
		Map<String,List<?>> tokenSetMemberEnv = null;
		if (tsName != null) {
			try {
				tokenSetMemberEnv = pm.createTokenSetMemberEnv( action, token );
			} 
			catch (Exception e) {
				logger.error( "Cannot create token set environment in "  + action + ": " + e.getMessage(), e );
				action.setCondition( XmlExitCondition.GENERAL_FAILURE,
						             "Cannot create token set environment: " + e.getMessage() );
				tsName = null;
			}
		}
		
		if (tsName != null) {
			if (tokenSetMemberEnv.size() == 0) {
				logger.debug( "Empty token set from "  + action + ", will not create token set!" );
				action.setCondition( XmlExitCondition.EMPTY_TOKENSET, "Empty token set" );
				tsName = null;
			}
		}
		
		ArcEvaluator arcEvaluator =
				new ArcEvaluator( action, pm.getEnvironment( token ) );
		String arcName = arcEvaluator.evaluateArc();
		
		if (tsName != null) {
			try {
				if (logger.isDebugEnabled()) {
					logger.debug( "Token set split node '"
								+ token.getNode().getName()
								+ "', creating tokenset environment '"
								+ tsName + "'" );
				}
				Env tokenSetEnv = environmentManager
									.createTokenSetEnvironment( token, tsName );
				getWorkflowManager().completeWithTokenSet(
						token, arcName, tsName, 
						tokenSetEnv, tokenSetMemberEnv, engine );
			} 
			catch (Exception e) {
				logger.error( "Cannot create token set '" + tsName
 						    + "' in " + action + ": " + e, e );
				environmentManager.closeTokenSetEnvironment( token );
				tsName = null;
				// set error condition and try to complete without token set
				action.setCondition( XmlExitCondition.WORKFLOW_ERROR, e.toString() );
				arcName = arcEvaluator.evaluateArc();
			}
		}
		if (tsName == null) {
			if (logger.isDebugEnabled()) {
				logger.debug( "Completing token "  + token + ", arcName=" + arcName );
			}
			getWorkflowManager().completeToken( token, arcName, engine );
		}
		
		if (!token.isComplete()) {
		   logger.debug( "force completing token id: " + token.getId()  );
		   token.markComplete();
		}
		
		getJobStatusManager().onActionCompleted( action, token, arcName, tsName != null );
		
		Long processId = engineFactory.getProcessId( token.getProcess() );
		JobProcessRecord jpr = getProcessRecordById( processId );
		if (!jpr.isPaused()) {
			getWorkflowExecutor().pushProcess( jpr.getId() );
		}
	}
	
	public void closeTokensetEnvOnJoin( NodeToken token ) 
	{
		// if this is tokenset merge node, close tokenset environment:
		if (WorkflowUtils.isTokenSetMergeNode( token.getNode() )) {
			if (logger.isDebugEnabled()) {
				logger.debug( "Token set join node '" + token.getNode().getName()
							+ "', closing tokenset environment '"
							+ environmentManager.getTokenSetName( token )
							+ "'" );
			}
			environmentManager.closeTokenSetEnvironment( token );
		}
	}
	
	
	public void processEvents( List<Event> events )
	{
		for (Event e: events) {
			getEventManager().onEvent( e );
		}
	}
	
	public void onProcessCompleted( JobProcessRecord jpr, ExecutionEventType eventType )
	{
		environmentManager.removeProcessEnvironments( jpr.getId() );
		
		long deleteAfterMilliseconds =
				jobManager.getDeleteCompletedInstancesAftetrMilliseconds( jpr.getJob() );
		if (deleteAfterMilliseconds >= 0) {
			final long pid = jpr.getId();
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
					deleteCompletedProcessInfo( pid );
				}
			};
			getTimer().schedule( task, deleteAfterMilliseconds );
			logger.debug( "Scheduled deletion of " + jpr
					    + " in " + deleteAfterMilliseconds + " ms." );
		}
	}
	
	private synchronized Timer getTimer()
	{
		if (timer == null) {
			timer = new Timer( true );
		}
		return timer;
	}
	
	// for unit tests only
	void setTimer( Timer timer )
	{
		this.timer = timer;
	}
	
	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------
	
	public ParameterManager getParameterManager( NodeToken token )
	{
		return getJobManager().getParameterManager( token.getProcess().getGraph() );
	}

	@Transactional
	public void cancelJobInstance(final long processId) 
	{
		final JobProcessRecord jpr = getProcessRecordById( processId );
		getWorkflowExecutor().submit(
				new WorkflowTask( "cancelProcess(" + jpr + ")", processId, null ) {
					public void run() {
						logger.info( "Canceling process " + jpr );
						jpr.setPaused(false);
						getWorkflowManager().startCancelProcess(
								jpr.getId(), getEngine() );
						getActionManager().beginCancelingProcessActions(processId);
					}
				});
	}
	
	@Transactional
	public void finalizeCancel(final long processId) 
	{
		final JobProcessRecord jpr = getProcessRecordById( processId );
		getWorkflowExecutor().submit(
				new WorkflowTask( "finalizeCancelProcess(" + jpr + ")", processId, null ) {
					public void run() {
						getWorkflowManager().finalizeCancelProcess(processId, getEngine());			
					};
			});
	}
	
	@Transactional
	public List<String> getEnvironmentVariableNames( long processId )
	{
		GraphProcess process = engineFactory.findProcess( processId );
		ParameterManager pm =
				getJobManager().getParameterManager( process.getGraph() );
		return new ArrayList<String>( pm.getEnvironmentVariables().keySet() );
	}
	
	@Transactional
	public ExternalParameterValue getEnvironmentVariable( long processId, String variableName)
	{
		GraphProcess process = engineFactory.findProcess( processId );
		ParameterManager pm =
				getJobManager().getParameterManager( process.getGraph() );
		ParameterDescriptor pd = pm.getEnvironmentVariable( variableName );
		ExternalParameterValue pv = null;
		if (pd != null) {
			Object value = getFullProcessEnvironment( process ).getAttribute( variableName );
			pv = pd.getExternalizer().objectToExternalValue( value );
		}
		return pv;
	}

	@Transactional
	public List<ExternalParameterValue> getProcessParameters( long processId )
	{
		List<ExternalParameterValue> params = new ArrayList<ExternalParameterValue>();
		GraphProcess process = engineFactory.findProcess( processId );
		ParameterManager pm =
				getJobManager().getParameterManager( process.getGraph() );
		TypedEnvironment env = environmentManager.getProcessEnvironment( process );
		
		for (ParameterDescriptor pd: pm.getOrderedFreeParameters()) {
			Object value = env.getAttribute( pd.getInputName() );
			ExternalParameterValue extValue = 
					pd.getExternalizer().objectToExternalValue( value );
			params.add( extValue );
		}
		return params;
	}

	
	private TypedEnvironment getFullProcessEnvironment( GraphProcess process )
	{
		// TODO: add global environment when it is supported
		return environmentManager.getProcessEnvironment( process );
	}
	
	@Transactional
	public void pauseProcess(final long processId) 
	{
		final JobProcessRecord jpr = getProcessRecordById( processId );
		getWorkflowExecutor().submit(
				new WorkflowTask( "pauseProcess(" + jpr + ")", processId, null ) {
					public void run() {
						logger.info( "Pausing process " + jpr );
						jpr.setPaused(true);
						jpr.setActive( false );
						getProcessDao().saveJobProcess(jpr);
						getActionManager().pauseProcessActions(processId);
					}
				});
		
	}

	public WorkflowManager getWorkflowManager() {
		return workflowManager;
	}
	public void setWorkflowManager(WorkflowManager workflowManager) {
		this.workflowManager = workflowManager;
	}
	public JobManager getJobManager() {
		return jobManager;
	}
	public void setJobManager(JobManager jobManager) {
		this.jobManager = jobManager;
	}
	public JobProcessDao getProcessDao() {
		return processDao;
	}
	public void setProcessDao(JobProcessDao processDao) {
		this.processDao = processDao;
	}
	public WorkflowExecutor getWorkflowExecutor() {
		return workflowExecutor;
	}
	public void setWorkflowExecutor(WorkflowExecutor workflowExecutor) {
		this.workflowExecutor = workflowExecutor;
	}
	public JobStatusManager getJobStatusManager() {
		return jobStatusManager;
	}
	public void setJobStatusManager(JobStatusManager jobStatusManager) {
		this.jobStatusManager = jobStatusManager;
	}
	public JobStatusRetriever getJobStatusRetriever() {
		return jobStatusRetriever;
	}
	public void setJobStatusRetriever(JobStatusRetriever jobStatusRetriever) {
		this.jobStatusRetriever = jobStatusRetriever;
	}
	public EventManager getEventManager() {
		return eventManager;
	}
	public void setEventManager(EventManager eventManager) {
		this.eventManager = eventManager;
	}
	public Configuration getConfig() {
		return config;
	}
	public void setConfig(Configuration config) {
		this.config = config;
	}
	public EnvironmentManager getEnvironmentManager() {
		return environmentManager;
	}
	public void setEnvironmentManager(EnvironmentManager environmentManager) {
		this.environmentManager = environmentManager;
	}
	public EngineFactory getEngineFactory()	{
		return engineFactory;
	}
	public void setEngineFactory( EngineFactory engineFactory )	{
		this.engineFactory = engineFactory;
	}
}

package com.trustwave.dbpjobservice.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.googlecode.sarasvati.Arc;
import com.googlecode.sarasvati.Graph;
import com.googlecode.sarasvati.Node;
import com.trustwave.dbpjobservice.backend.JobDao;
import com.trustwave.dbpjobservice.backend.JobParameterRecord;
import com.trustwave.dbpjobservice.backend.JobProcessRecord;
import com.trustwave.dbpjobservice.backend.JobRecord;
import com.trustwave.dbpjobservice.backend.JobTemplateRecord;
import com.trustwave.dbpjobservice.interfaces.ExternalParameterValue;
import com.trustwave.dbpjobservice.interfaces.Job;
import com.trustwave.dbpjobservice.interfaces.JobTemplate;
import com.trustwave.dbpjobservice.interfaces.KeyValue;
import com.trustwave.dbpjobservice.interfaces.Task;
import com.trustwave.dbpjobservice.interfaces.TaskSummary;
import com.trustwave.dbpjobservice.parameters.EnvironmentManager;
import com.trustwave.dbpjobservice.parameters.ParameterDescriptor;
import com.trustwave.dbpjobservice.parameters.ParameterManager;
import com.trustwave.dbpjobservice.parameters.PossibleParameterValuesAdapter;
import com.trustwave.dbpjobservice.workflow.ArcEvaluator;
import com.trustwave.dbpjobservice.workflow.EngineFactory;
import com.trustwave.dbpjobservice.workflow.WorkflowManager;
import com.trustwave.dbpjobservice.workflow.WorkflowUtils;
import com.trustwave.dbpjobservice.workflow.api.action.JobContext;
import com.trustwave.dbpjobservice.workflow.api.action.JobContextImpl;
import com.trustwave.dbpjobservice.workflow.api.action.JobServiceContextImpl;
import com.trustwave.dbpjobservice.xml.XmlExtendedDescription;
import com.trustwave.dbpjobservice.xml.XmlParameters;

public class JobManager 
{
	private static Logger logger = LogManager.getLogger( JobManager.class );
	
	private WorkflowManager   workflowManager;
	private JobDao jobDao;
	private TaskChainFactory taskChainFactory;
	private Configuration     config;
	private EnvironmentManager environmentManager;
	private EngineFactory engineFactory;
	private boolean           addValidationToWorkflow = true;
	
	private static final String PROPERTY_FILE_NAME = "jobservice.properties";
	private static final String PROPERTY_FILE_NAME1 = "jobservice.internal.properties";
	private PropertySubstitutor propertySubstitutor =
			new PropertySubstitutor( PROPERTY_FILE_NAME, PROPERTY_FILE_NAME1 );
	
	/**
	 * Create new job template from workflow definition
	 * @param xml
	 * @return
	 */
	@Transactional
	public JobTemplate createTemplate( String xml )
	{
		return jobTemplateFromRecord( loadTemplate( xml, true ), true );
	}
	
	@Transactional
	public JobTemplateRecord loadTemplate( final String xml0, boolean inheritJobs )
	{
		String xml = propertySubstitutor.substitute( xml0 );
		List<JobRecord> jobsToInherit = new ArrayList<JobRecord>();
		String templateName = getWorkflowManager().getTemplateName( xml );
		
		JobTemplateRecord jtr0 = getJobDao().findJobTemplate( templateName );
		if (jtr0 != null && jtr0.getTemplateXml().equals( xml )) {
			logger.debug( "Not updating template '" + templateName + "' - did not change" );
			return jtr0;
		}
		if (jtr0 != null && inheritJobs) {
			jobsToInherit = getJobDao().findJobsByTemplateId( jtr0.getId() );
		}
		
		JobTemplateRecord jtr = loadTemplate( templateName, xml );
		
		for (JobRecord jr: jobsToInherit) {
			logger.info( "Updating template in " + jr
					    + " to '" + jtr.getNameWithVersion() + "'" );
			jr.setJobTemplate( jtr );
		}
		return jtr;
	}
	
	@Transactional
	JobTemplateRecord loadTemplate( String templateName, String xml )
	{
		logger.info( "Importing workflow xml '" + templateName + "'" );
		logger.debug( xml );
		String tunedXml = xml;
		if (addValidationToWorkflow) {
			tunedXml = new WorkflowTuner().addValidationNode( tunedXml );
		}
		Graph graph = getWorkflowManager().importWorkflow( tunedXml );
		XmlExtendedDescription dsc =
				getWorkflowManager().geExtendedTemplateDescription( graph );
		String description = (dsc != null? dsc.getDescription(): "");
		validateArcConditions( graph );
		validateDefaultArcs( graph );
		JobTemplateRecord jtr = 
			new JobTemplateRecord( graph, xml, description );
		getJobDao().saveJobTemplate( jtr );
		getTaskChainFactory().updateTaskChain( jtr );
		return jtr;
	}
	
	private void validateArcConditions( Graph graph )
	{
		List<Node> nodes = graph.getNodes();
		ArcEvaluator.checkArcConditions( nodes, graph, config.isValidateArcs() );
	}
	
	private void validateDefaultArcs( Graph graph )
	{
		List<Node> nodes = graph.getNodes();
		for (Node node: nodes) {
			if (graph.getOutputArcs( node ).size() > 0) {
				// has some output arcs
				if (graph.getOutputArcs( node, Arc.DEFAULT_ARC ).size() == 0) {
					// but no default arcs
					throw new RuntimeException( Messages.getString("workflow.node.defaultArc.missing", node.getName()) );
				}
			}
		}
	}
	
	private JobTemplate jobTemplateFromRecord( JobTemplateRecord jtr, boolean withParameterDescriptors )
	{
		JobTemplate jt = null;
		if (jtr != null) {
			jt = new JobTemplate( jtr.getId(), jtr.getName(), jtr.getDescription() );
			jt.setVersion( jtr.getGraph().getVersion() );
			Calendar createDate = Calendar.getInstance();
			createDate.setTime( jtr.getCreateDate() );
			jt.setCreateDate( createDate );
			jt.setTasks( taskChainFactory.getTaskChain( jtr ).getTasks() );
			if (withParameterDescriptors) {
				ParameterManager pm = getParameterManager( jtr );
				jt.setParameterDefinitions( pm.createDefinitions() );
			}
		}
		return jt;
	}
	
	@Transactional
	public List<JobTemplate> getTemplates( boolean withParameterDescriptors )
	{
		List<JobTemplate> jtList = new ArrayList<JobTemplate>();
		List<JobTemplateRecord> jtrList = getJobDao().getJobTemplates();
		for (JobTemplateRecord jtr: jtrList) {
			if (getWorkflowManager().isUtilityTemplate( jtr.getGraph() )) {
				continue;
			}
			try {
				jtList.add( jobTemplateFromRecord( jtr, withParameterDescriptors ) );
			} 
			catch (Exception e) {
				logger.error( "Error loading template " + jtr.getName() + ": " + e.getMessage(), e );
			}
		}
		return jtList;
	}
	
	@Transactional
	public JobTemplate getTemplateById( long templateId )
	{
		JobTemplateRecord jtr = getJobDao().findJobTemplate( templateId );
		return jobTemplateFromRecord( jtr, true );
	}
	
	@Transactional
	public JobTemplate findTemplate( String name )
	{
		JobTemplateRecord jtr = getJobDao().findJobTemplate( name );
		return jobTemplateFromRecord( jtr, true );
	}
	
	@Transactional
	public String getTemplateXml( long templateId )
	{
		JobTemplateRecord jtr = getJobDao().findJobTemplate( templateId );
		return jtr.getTemplateXml();
	}
	
	public List<Task> getTasks( String templateName )
	{
		JobTemplateRecord jtr = getJobDao().findJobTemplate( templateName );
		TaskChain tc = getTaskChainFactory().getTaskChain( jtr );
		List<Task> tasks = new ArrayList<Task>();
		
		for (TaskSummary ts: tc.getChain()) {
			tasks.add( ts.getTask() );
		}
		return tasks;
	}
	
	@Transactional
	public Job createJob( Job job )
	{
		JobRecord jr = createJobRecord( job );
		job = jobFromRecord( jr );
		logger.info( "Created " + job + ", parameters=" + job.getParameters() );
		return job;
	}
	
	@Transactional
	public JobRecord createJobRecord( Job job ) 
	{
		JobTemplateRecord jtr = getJobDao().findJobTemplate( job.getTemplateId() );
		if (jtr  == null) {
			throw new RuntimeException(Messages.getString("create.templateId.notFound", job.getTemplateId()));
		}
		String name = job.getName();
		if (name == null || name.trim().length() == 0) {
			throw new RuntimeException(Messages.getString("create.job.name.unspecified"));
		}
		name = name.trim();
		JobRecord jr = getJobDao().findJob( name, job.getOrgId() );
		if (jr != null) {
			throw new RuntimeException(Messages.getString("create.job.alreadyExists", name));
		}
		jr = new JobRecord( name, job.getOrgId(), jtr );
		jr.setDescription( job.getDescription() );
		jr.setAutoRemoveCompletedInstancesAfterSeconds( job.getAutoRemoveCompletedInstancesAfterSeconds() );
		jr.setUtilityJob( job.getUtilityJob() );
		
		if (job.getParameters() != null) {
			ParameterManager pm = getParameterManager( jtr );
			for (ExternalParameterValue p: job.getParameters()) {
				logger.debug( "job("+ job.getName() + ") parameter: " + p );
				ParameterDescriptor pd = pm.getFreeParameterNotNull( p.getName() );
				String value = pd.externalParameterValueToString( p );
				jr.addParameter( p.getName(), value );
			}
		}
		getJobDao().saveJob( jr );
		return jr;
	}
	
	@Transactional
	public Job updateJob( Job job )
	{
		JobRecord jr = findJobRecord( job.getId() );
		jr.setName( job.getName() );
		jr.setDescription( job.getDescription() );
		jr.setUtilityJob( job.getUtilityJob() );
		jr.setAutoRemoveCompletedInstancesAfterSeconds( job.getAutoRemoveCompletedInstancesAfterSeconds() );
		if (jr.getOrgId() != job.getOrgId()) {
			throw new RuntimeException(Messages.getString("update.job.org.canNotChange")); 
		}
		
		ParameterManager pm = getParameterManager( jr.getJobTemplate() );
		for (ExternalParameterValue p: job.getParameters()) {
			logger.debug( "job("+ job.getName() + ") parameter: " + p );
			ParameterDescriptor pd = pm.getFreeParameterNotNull( p.getName() );
			String value = pd.externalParameterValueToString( p );
			jr.addParameter( p.getName(), value );
		}
		getJobDao().saveJob( jr );
		logger.info( "Updated " + job + ", parameters=" + job.getParameters() );
		return job;
	}
	
	@Transactional
	public Job findJob(String jobName, int orgId) {
		JobRecord jr = getJobDao().findJob( jobName, orgId );
		return jobFromRecord( jr );
	}
	
	@Transactional
	public Job findJob( long jobId ) {
		JobRecord jr = getJobDao().findJob( jobId );
		return jobFromRecord( jr );
	}
	
	@Transactional
	public List<Job> getAllJobs()
	{
		List<Job> jobs = new ArrayList<Job>();
		List<JobRecord> jrList = getJobDao().findAllJobs();
		for (JobRecord jr: jrList) {
			if ( !isUtilityJob(jr) ) {
				jobs.add( jobFromRecord( jr ) );
			}
		}
		return jobs;
	}
	
	@Transactional
	public Collection<Integer> getOrgIdsWithVisibleJobs() 
	{
		HashSet<Integer> orgIds = new HashSet<Integer>();
		List<JobRecord> jrList = getJobDao().findAllJobs();
		for (JobRecord jr: jrList) {
			if (!isUtilityJob( jr )) {
				orgIds.add( jr.getOrgId() );
			}
		}
		return orgIds;
	}
	
	public boolean isUtilityJob(JobRecord jr) 
	{
		if ( jr.getUtilityJob() != null ) {
			return jr.getUtilityJob(); 
		}
		Graph graph = jr.getJobTemplate().getGraph();
		return getWorkflowManager().isUtilityTemplate( graph );
	}
	
	public long getDeleteCompletedInstancesAftetrMilliseconds( JobRecord jr )
	{
		if (jr.getAutoRemoveCompletedInstancesAfterSeconds() != null) {
			return jr.getAutoRemoveCompletedInstancesAfterSeconds() * 1000;
		}
		Graph graph = jr.getJobTemplate().getGraph();
		Long deleteAfter = getWorkflowManager()
				          .getDeleteCompletedInstancesAfterMilliseconds( graph );
		if (deleteAfter != null) {
			return deleteAfter;
		}
		return -1;
	}

	@Transactional
	public List<Job> getJobsByTemplateId( long templateId )
	{
		List<Job> jobs = new ArrayList<Job>();
		List<JobRecord> jrList = getJobDao().findJobsByTemplateId( templateId );
		for (JobRecord jr: jrList) {
			jobs.add( jobFromRecord( jr ) );
		}
		return jobs;
	}
	
	@Transactional
	public List<String> validateJob( Job job )
	{
		List<String> errors = new ArrayList<String>();
		JobRecord jr = findJobRecord( job.getId() );
		ParameterManager pm = getParameterManager( jr.getJobTemplate() );
		try {
			JobContext context =
					new JobContextImpl( JobServiceContextImpl.getInstance(),
							            job.getName(), job.getOrgId(), job.getId() );
			pm.validateParameters( job.getParameters(), context, errors );
		}
		finally {
			JobServiceContextImpl.getInstance().closeAllEndpointsInCurrentThread();
		}
		return errors;
	}

	@Transactional
	public List<KeyValue> getPossibleParameterValues( long jobId, String parameterName )
	{
		JobRecord jr = findJobRecord( jobId );
		ParameterManager pm = getParameterManager( jr.getJobTemplate() );
		ParameterDescriptor descriptor = pm.getFreeParameterNotNull( parameterName );
		PossibleParameterValuesAdapter adapter = 
				new PossibleParameterValuesAdapter( descriptor );
		JobContext context =
				new JobContextImpl( JobServiceContextImpl.getInstance(),
						            jr.getName(), jr.getOrgId(), jobId );
		try {
			return adapter.getPossibleValues( context );
		}
		finally {
			JobServiceContextImpl.getInstance().closeAllEndpointsInCurrentThread();
		}
	}
	
	@Transactional
	public void deleteJob(long jobId)
	{
		JobRecord jr = getJobDao().findJob(jobId);
		if (jr != null) {
			for (JobProcessRecord process: jr.getProcesses()) {
				if (!process.isFinished()) {
					throw new RuntimeException(Messages.getString("delete.job.instance.running"));
				}
			}
			
			getJobDao().deleteJob(jr);
			logger.info("Deleted job with id=" + jobId);
		}
	}

	JobRecord findJobRecord( long jobId )
	{
		JobRecord jr = getJobDao().findJob( jobId );
		if (jr  == null) {
			throw new RuntimeException(Messages.getString("jobId.notFound", jobId));
		}
		return jr;
	}
	
	Job jobFromRecord( JobRecord jr )
	{
		if (jr == null) {
			return null;
		}
		Job job = new Job();
		job.setId( jr.getJobId() );
		job.setTemplateId( jr.getJobTemplate().getId() );
		job.setTemplateName( jr.getJobTemplate().getName() );
		job.setName( jr.getName() );
		job.setOrgId( jr.getOrgId() );
		job.setDescription( jr.getDescription() );
		job.setAutoRemoveCompletedInstancesAfterSeconds( jr.getAutoRemoveCompletedInstancesAfterSeconds() );
		job.setUtilityJob( jr.getUtilityJob() );
		
		// We need to return Job object even if there are problems
		// with parameters.
		ParameterManager pm = null;
		try {
			pm = getParameterManager( jr.getJobTemplate() );
			
			for (ParameterDescriptor pd: pm.getOrderedFreeParameters()) {
				if (!pd.isVisible()) {
					continue;
				}
				JobParameterRecord jpr = jr.getParameter( pd.getInputName() );
				try {
					String value = (jpr != null? jpr.getValue(): null); 
					ExternalParameterValue extParam = 
						pd.stringToExternalParameterValue( value );
				    job.getParameters().add( extParam );
				} 
				catch (Exception e) {
					logger.warn( "Error retrieving parameter "
							   + pd.getInputName() + " for  " + job + ": " + e, e );
					job.getProblems().add( e.getMessage() );
				}
			}
		} 
		catch (Exception e) {
			logger.warn( "Cannot process parameters for " + job + ": " + e );
			job.getProblems().add( e.getMessage() );
		}
		return job;
	}
	
	public Job getTestCredentialsJob(int orgId)
	{
		final String workflowName = "test-credentials";
		Job testJob = findJob( workflowName, orgId );
		if (testJob == null) {
			JobTemplate template = findTemplate( workflowName );
			if (template == null) {
				throw new RuntimeException(Messages.getString("workflow.notFound", workflowName));
			}
			Job job = new Job();
			job.setName(  workflowName );
			job.setTemplateId( template.getId() );
			job.setOrgId( orgId );
			testJob = createJob( job );
		}
		return testJob;
	}
	
	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------
	
	private Map<Long,ParameterManager> templateParametersMap = 
		new HashMap<Long, ParameterManager>();
	
	ParameterManager getParameterManager( JobTemplateRecord jtr )
	{
		return getParameterManager( jtr.getGraph() );
	}
	
	synchronized ParameterManager getParameterManager( Graph graph )
	{
		Long templateId = engineFactory.getGraphId(graph);
		ParameterManager pm = templateParametersMap.get( templateId );
		if (pm == null) {
			List<Node> nodes = WorkflowUtils.getGraphNodes( graph );
			XmlExtendedDescription dsc =
				workflowManager.geExtendedTemplateDescription( graph );
			XmlParameters declared = dsc.getParameters();
			// validate job parameters only for current (latest) job template: 
			Graph latestGraph = workflowManager.getLatestGraph(  graph.getName() );
			boolean isCurrent = graph.getVersion() == latestGraph.getVersion();
			pm = new ParameterManager( declared, nodes, environmentManager, isCurrent );
			templateParametersMap.put( templateId, pm );
		}
		return pm;
	}

	@Transactional
	public Job restoreJobBackup(Job backup) 
	{
		try { 
			JobTemplate template = findTemplate(backup.getTemplateName());
			backup.setTemplateId(template.getId());
			
			Job oldJob = findJob(backup.getName(), backup.getOrgId());
						
			if (oldJob != null) {
				backup.setId(oldJob.getId());
				logger.info("Updating job while restoring " + backup);
				backup = updateJob(backup);
			}
			else {
				logger.info("Creating new job while restoring " + backup);
				backup = createJob(backup);
			}
			
			return backup;
		}
		catch( Exception e ) {
			logger.error("Error while restoring backup", e);
			throw new RuntimeException(e);
		}
	}
	
	public void resetPropertySubstitutor()
	{
		propertySubstitutor = new PropertySubstitutor( PROPERTY_FILE_NAME, PROPERTY_FILE_NAME1 );
	}

	PropertySubstitutor getPropertySubstitutor() {
		return propertySubstitutor;
	}
	
	public WorkflowManager getWorkflowManager() {
		return workflowManager;
	}
	public void setWorkflowManager(WorkflowManager workflowManager) {
		this.workflowManager = workflowManager;
	}
	public JobDao getJobDao() {
		return jobDao;
	}
	public void setJobDao(JobDao jobDao) {
		this.jobDao = jobDao;
	}
	public TaskChainFactory getTaskChainFactory() {
		return taskChainFactory;
	}
	public void setTaskChainFactory(TaskChainFactory utcFactory) {
		this.taskChainFactory = utcFactory;
	}
	public Configuration getConfig() {
		return config;
	}
	public void setConfig(Configuration config) {
		this.config = config;
	}
	public boolean isAddValidationToWorkflow() {
		return addValidationToWorkflow;
	}
	public void setAddValidationToWorkflow(boolean addValidationToWorkflow) {
		this.addValidationToWorkflow = addValidationToWorkflow;
	}
	public EnvironmentManager getEnvironmentManager() {
		return environmentManager;
	}
	public void setEnvironmentManager(EnvironmentManager environmentManager) {
		this.environmentManager = environmentManager;
	}
	public void setEngineFactory(EngineFactory engineFactory) {
		this.engineFactory = engineFactory;
	}
}
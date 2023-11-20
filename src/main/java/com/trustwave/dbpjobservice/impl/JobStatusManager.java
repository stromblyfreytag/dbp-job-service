package com.trustwave.dbpjobservice.impl;







import static com.trustwave.dbpjobservice.interfaces.JobProcessInfo.CompletionState.CANCELED;
import static com.trustwave.dbpjobservice.interfaces.JobProcessInfo.CompletionState.FAILURE;
import static com.trustwave.dbpjobservice.interfaces.JobProcessInfo.CompletionState.PARTIAL;
import static com.trustwave.dbpjobservice.interfaces.JobProcessInfo.CompletionState.SUCCESS;

import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;












import com.googlecode.sarasvati.ArcToken;
import com.googlecode.sarasvati.Node;
import com.googlecode.sarasvati.NodeToken;
import com.googlecode.sarasvati.ProcessState;
import com.trustwave.dbpjobservice.backend.JobProcessDao;
import com.trustwave.dbpjobservice.backend.JobProcessRecord;
import com.trustwave.dbpjobservice.backend.JobStatusDao;
import com.trustwave.dbpjobservice.backend.UserTaskInfoRecord;
import com.trustwave.dbpjobservice.interfaces.JobProcessInfo;
import com.trustwave.dbpjobservice.interfaces.JobProcessInfo.CompletionState;
import com.trustwave.dbpjobservice.interfaces.TaskSummary;
import com.trustwave.dbpjobservice.parameters.TypedEnvironment;
import com.trustwave.dbpjobservice.workflow.JobActionNode;
import com.trustwave.dbpjobservice.workflow.WorkflowUtils;
import com.trustwave.dbpjobservice.workflow.api.action.IJobAction;
import com.trustwave.dbpjobservice.xml.XmlExitCondition;
import com.trustwave.dbpjobservice.xml.XmlParameter;

public class JobStatusManager
{
	private final static Logger logger = LogManager.getLogger( JobStatusManager.class );
	private static final String TaskRecordIdAttr = "-tid";

	private static JobStatusManager instance;

    public static JobStatusManager getInstance() {
		return instance;
	}
	public static JobStatusManager createInstance() {
		instance = new JobStatusManager();
		return instance;
	}
	// used for unit testing
	public static void setInstance( JobStatusManager inst ) {
		instance = inst;
	}

	private JobProcessDao processDao;
	private JobStatusDao statusDao;
	private JobManager jobManager;
    private TaskChainFactory taskChainFactory;

	UserTaskInfoRecord findOrCreateTaskRecord( IJobAction action, NodeToken token )
	{
    	UserTaskInfoRecord taskRecord = null;
    	
    	if (action.getTaskRecordId() != 0) {
    		taskRecord = getStatusDao().findTaskInfo( action.getTaskRecordId() );
    	}
    	else {
    		taskRecord = getStatusDao().findTaskInfo(
    				action.getProcessId(), action.getTaskName(), action.getItem() );
    	}
		if (taskRecord == null) {
			taskRecord = new UserTaskInfoRecord( action.getProcessId(), 
					                             action.getTaskName(),
					                             action.getItem(), 
					                             action.getBeginTime(),
					                             action.getTokenId(), 
					                             action.getNodeId() );
			logger.debug( "Creating " + taskRecord );
			getStatusDao().saveTaskInfo( taskRecord );
		}
		else {
			taskRecord.setLastTokenId( action.getTokenId() );
			taskRecord.setLastNodeId( action.getNodeId() );
			if (taskRecord.getEndTime() != null) {
				// re-opening task:
				logger.debug( "Re-openiong " + taskRecord );
				taskRecord.setEndTime( null );
			}
		}
		// save task record ID in action to use PK search,
		// in hibernate cache  (see above), rather than database search:
		action.setTaskRecordId( taskRecord.getId() );
		token.getEnv().setAttribute( TaskRecordIdAttr, taskRecord.getId() );
		
		removeHeadFromPreviousTasks( token, taskRecord.getId() );
		
		taskRecord.setCurrentState( action.getNodeName() );
		
		return taskRecord;
	}

	void removeHeadFromPreviousTasks( NodeToken token, long currTaskRecordId )
	{
		for (ArcToken arc: token.getParentTokens()) {
			NodeToken parent = arc.getParentToken();
			Long parentTaskRecordId =
					parent.getEnv().getAttribute( TaskRecordIdAttr, Long.class );
			if (parentTaskRecordId != null && parentTaskRecordId != currTaskRecordId) {
				UserTaskInfoRecord parenTaskRecord =
						getStatusDao().findTaskInfo( parentTaskRecordId );
				if (parenTaskRecord != null) {
					parenTaskRecord.setHead( null );
				}
			}
		}
	}
	
	public void onSaveActionOutput( IJobAction action,
			                        NodeToken token, 
			                        TypedEnvironment env,
			                        boolean actionComplete )
	{
		UserTaskInfoRecord taskRecord = findOrCreateTaskRecord( action, token );
		
		XmlExitCondition cond = action.getExitCondition();
		if (cond == XmlExitCondition.OK) {
			cond = null;
		}
		taskRecord.setCondition( action.isPaused()? "Paused"
				               : cond != null?      cond.value()
				               :                    null );
		if (cond == null) {
			// retrieve status message from environment, rather than directly from  action
			// to allow setting (default) status message directly in the workflolw,
			// e.g. <cs:output name="statusMessage" value="Credentials validated"/>
			// Message set directly in action will override workflow value, of course.
			String msg = (String)env.getAttribute( IJobAction.PARAMETER_STATUS_MESSAGE );
			taskRecord.setMessage( msg );
		}
		else {
			taskRecord.setMessage( action.getErrorDetails() );
		}
		
		if (action.isWaiting()) {
			String message = action.getWaitMessage();
			// ensure message not null - sign of waiting for status retriever
			if (message == null) { 
				message = "";
			}
			taskRecord.setWaitMessage( message, action.isTimeWaiting() );
		}
		else {
			taskRecord.setWaitMessage( null );
		}
		taskRecord.setFailedTaskName( action.getFailedAction() );
		
		JobProcessRecord jpr =
				getProcessDao().getProcess( action.getProcessId() );
		TaskChain tc = taskChainFactory.getTaskChainByProcess( jpr );
		
        updatePercentCompleted( taskRecord, action, (actionComplete? 1.0: -1.0), tc );
    }
	
	
	public void onActionCompleted( final IJobAction action, 
			                       NodeToken token,
			                       String arcName,
			                       boolean tokenSetSplitAction )
	{
		if (logger.isDebugEnabled()) {
			logger.debug( "On action completed: " + action + ", arc=" + arcName );
		}
		
		UserTaskInfoRecord taskRecord = findOrCreateTaskRecord( action, token );
		
		if (taskRecord.getLastTokenId() != token.getId().longValue() ) {
			logger.warn( "Cannot save state after completing " + action
					   + ": unexpected token, tokenId=" + token.getId()
					   + " != lastTokenId=" + taskRecord.getLastTokenId()
					   + ", item=" + action.getItem()
					   + ". Most probable cause is the same 'item' parameter used in different threads,"
					   + " so they are indistinguishable in status manager.");
			return;
		}
		
		JobProcessRecord jpr =
				getProcessDao().getProcess( action.getProcessId() );
		final List<Node> nextNodes =
			WorkflowUtils.getNextNodes( token.getNode(), arcName );
		
		TaskChain tc = taskChainFactory.getTaskChainByProcess( jpr );
		TaskSummary ts = tc.find( taskRecord.getTaskName() );
		
		// Check if task is completed.
		// Normally for that we analyze workflow to check if next node(s)
		// specify new task; if yes - current task is completed.
		// However that logic may fail if we are in a tokenset-splitting
		// node and task name is the same in post-split nodes. Task name
		// is the same, but items are different; so we have different tasks
		// for splitting action and post-split actions; but the above logic
		// will fail to detect that and will leave splitting action task
		// not completed. 
		// That's why we pass explicit tokenSetSplitAction flag and
		// use it as additional criterion for task completion detection.
		// 
		boolean taskCompleted =
				tokenSetSplitAction
			 || isTaskCompleted( taskRecord.getTaskName(), nextNodes )
		     || jpr.getState().equals( ProcessState.PendingCancel.name() );
		
        if (taskCompleted) {
        	completeTask( taskRecord, jpr, token.getCompleteDate() );
		}
        else if (jpr.isPaused()) {
        	taskRecord.setCondition( "Paused" );
        }
        else if (!action.hasErrors()) {
			// successful completion - set action name as the 'farthest achieved
			// node in task', but only if this is 'mainstream' action
			// (for calculating percentage):
			if (ts != null && ts.actionIndex( action.getActionName() ) >= 0) {
				taskRecord.setLastSuccessfulNodeName( action.getActionName() );
			}
		}
		
        updatePercentCompleted( taskRecord, action, 1.0, tc );
    }
	
	void updatePercentCompleted( UserTaskInfoRecord taskRecord,
			                     IJobAction action,
			                     double actionProgress,
			                     TaskChain tc )
	{
		TaskSummary ts = tc.find( taskRecord.getTaskName() );
		if (ts == null) {
			// something unexpected - task was not found in chain. 
			// Maybe it was defined outside of main stream?
			// Anyway, let's not touch percentage.
			logger.warn( "Cannot find task '" + taskRecord + "' in chain" );
			return;
		}
		
		// Task chain here is considered as a time scale, marked with average
		// execution times of tasks and actions. Every mainstream task and action
		// is presented as a known time point on this scale:
		// +--+-----------+-----+---+-+--------------+-----+-----+--------+
		//                          ^                                     ^
		//             current action time point                    end of job
		//                                                      (totalAverageTime)
		// Here we evaluate completion percent of the full scale, 
		// i.e. how close we are to the end of scale (job) for this thread.

		long timePoint = ts.getPreviousTasksExecutionTime();
		
		if (taskRecord.getEndTime() != null) {
			// task completed, move time point to the end of task:
			timePoint += ts.getAverageExecutionTime();
		}
		else {
			// move time point to the last successful action
			timePoint += ts.getAverageExecutionTimeUpTo( taskRecord.getLastSuccessfulNodeName() );

			if (action.getActionName().equals( taskRecord.getLastSuccessfulNodeName() )) {
				// we already accounted this action, do nothing 
			}
			else if (action.hasErrors()) {
				 // failed actions don't have progress, do nothing
			}
			else {
				// move time point forward according to explicit or estimated progress:
				long averageActionTime =
						ts.getAverageActionExecutionTime( action.getActionName() );
				if (actionProgress < 0) {
					actionProgress = estimateActionProgress( action, averageActionTime ); 
				}
				timePoint += (long)(averageActionTime * actionProgress);
			}
		}
		
		double percent = (timePoint*100.0) / tc.getTotalAverageTime();
		taskRecord.setPercentCompleted( percent );
	}

	
	double estimateActionProgress( IJobAction action, long averageActionTime )
	{
		// estimate action progress from actual elapsed time, with 99% cap:
		if (action.getBeginTime() == null) {
			return 0;
		}
		long actionBeginTime = action.getBeginTime().getTime();
		long elapsed = getCurrentTime() - actionBeginTime;
		if (elapsed <= 0 || averageActionTime <= 0) {
			return 0;
		}
		double progress = ((double)elapsed)/averageActionTime;
		return (progress > 0.99? 0.99: progress);
	}
	
	// used for unit tests only!
	private long currentTime = 0;
	
	long getCurrentTime() {
		return currentTime > 0? currentTime: System.currentTimeMillis();
	}
	void setCurrentTime ( long time ) {
		this.currentTime = time;
	}
	
	
	void completeTask( UserTaskInfoRecord taskRecord, JobProcessRecord jpr, Date completeDate )
	{
		if (logger.isDebugEnabled()) {
			logger.debug( "Completing task: " + taskRecord );
		}
		
		taskRecord.setEndTime( completeDate );
		taskRecord.setWaitMessage( null );

		if (taskRecord.getCondition() != null) {
			taskRecord.setCurrentState( "Failed" );
			if (taskRecord.getCondition().equalsIgnoreCase( XmlExitCondition.CANCELED.value() )) {
			    taskRecord.setCurrentState( "Canceled" );
			}
		}
		else {
		    taskRecord.setCurrentState("Completed");
		}
		
		updateCompletionState( taskRecord, jpr, taskRecord.getCondition() == null );
		getStatusDao().taskCompleted( taskRecord );
	}
	
	public void onProcessCompleted( JobProcessRecord jpr )
	{
		if (jpr.getState().equals( ProcessState.Canceled.name() )) {
			jpr.setCompletionState( CANCELED );
		}
		// completion state null on completion means that there were no essential
		// tasks in the job, and all non-essential completed OK (see table below):
		if (jpr.getCompletionState() == null) {
			jpr.setCompletionState( SUCCESS );
		}
		logger.info( "Proceess " + jpr + " " + jpr.getState()
				   + ", completionState=" + jpr.getCompletionState() );
	}
	
	private void updateCompletionState( UserTaskInfoRecord taskRecord,
			                            JobProcessRecord jpr,
			                            boolean isActionOK )
	{
		final CompletionState priorCompletionState = jpr.getCompletionState();

        final TaskChain taskChain = taskChainFactory.getTaskChainByProcess( jpr );
        final TaskSummary taskSummary = taskChain.find(taskRecord.getTaskName());
        final boolean isEssential =
        		(taskSummary != null? taskSummary.getTask().isEssential(): false);

        final CompletionState newCompletionState
                = calculateNewCompletionState(priorCompletionState, isActionOK, isEssential);
		if (logger.isDebugEnabled()) {
			logger.debug( priorCompletionState + " + '" 
		                + (taskSummary != null? taskSummary.getTask().getName(): "null")
					    + "'(success=" + isActionOK + ") -> " + newCompletionState );
		}
        jpr.setCompletionState(newCompletionState);
	}

    /**
     The transition table below supports the following definition of
     success, failure, and partial success:
        SUCCESS - all tasks (essential and non-essential) completed OK
        FAILURE - all essential tasks have failed
        PARTIAL - some tasks succeeded, some failed
     with additional transition I->S on job completion
     (for jobs without essential tasks). 

     <pre>
     Task outcome:
     S - success of essential task
     s - success of non-essential task
     F - failure of essential task
     f - failure of non-essential task

     Process completion states:
     I - initial (null)
     S - success
     P - partial success
     F - failure

        | S  s  F  f    <-- task outcome
     ---+----------------
     I  | S  I  F  F    <
     S  | S  S  P  P    < -- completion state ‘after’
     P  | P  P  P  P    <
     F  | P  F  F  F    <
     ^
     completion state ‘before’

     NOTE: For display purposes, a completionState of 'I' (null) will be converted to 'S' (SUCCESS).
     See JobStatusRetriever.getProcessInfo() for details.
     </pre>

     * @param priorCompletionState the completionState 'before' the current task completed.
     * @param isActionOK true is the completed action had no errors.
     * @param isEssential defined by the workflow xml for this job, true if the task is 'essential'.
     * @return the new completionState of this task.
     */
    // @todo May change to an object if needed to 'extend' calculation (rather than 'replace').
    static CompletionState calculateNewCompletionState(
            final CompletionState priorCompletionState, final boolean isActionOK, final boolean isEssential) {

        final CompletionState newCompletionState;

        if (null == priorCompletionState) {
            // this is first time a task has completed

            if (isActionOK) {
                if (isEssential) {
                    newCompletionState = SUCCESS;
                } else {
                    newCompletionState = null;
                }
            } else {
                newCompletionState = FAILURE;
            }

        } else if (SUCCESS.equals(priorCompletionState)) {

            if (isActionOK) {
                newCompletionState = SUCCESS;
            } else {
                newCompletionState = PARTIAL;
            }

        } else if (PARTIAL.equals(priorCompletionState)) {

            // no state change, because final CompletionState will always be partial in this case
            newCompletionState = priorCompletionState;

        } else if (FAILURE.equals(priorCompletionState)) {

            if (isActionOK && isEssential) {
                newCompletionState = PARTIAL;
            } else {
                newCompletionState = FAILURE;
            }
        } else {
            throw new IllegalArgumentException("Unrecognized state completion: " + priorCompletionState);
        }

        return newCompletionState;
    }


    private boolean isTaskCompleted( String taskName, List<Node> nextNodes )
	{
		for (Node n: nextNodes) {
			if (WorkflowUtils.isMergeNode( n )) {
				// merge node ALWAYS starts a new task - previous is completed.
				return true;
			}
			Node resolved = WorkflowUtils.resolveNodeReference( n );
			if (resolved instanceof JobActionNode) {
				JobActionNode node = (JobActionNode)resolved;
				XmlParameter ut = node.getTaskNameParameter();
				if (ut == null || taskName.equals( ut.getValue() )) {
					// next node has the same task name; so task is not completed yet
					return false;
				}
			}
		}
		return true;
	}

	public JobProcessDao getProcessDao() {
		return processDao;
	}
	public void setProcessDao(JobProcessDao processDao) {
		this.processDao = processDao;
	}
	public JobManager getJobManager() {
		return jobManager;
	}
	public void setJobManager(JobManager jobManager) {
		this.jobManager = jobManager;
	}
    public void setTaskChainFactory(final TaskChainFactory utcFactory) {
        this.taskChainFactory = utcFactory;
    }
	public JobStatusDao getStatusDao() {
		return statusDao;
	}
	public void setStatusDao(JobStatusDao statusDao) {
		this.statusDao = statusDao;
	}
}

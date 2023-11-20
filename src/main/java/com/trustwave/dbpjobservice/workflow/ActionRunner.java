package com.trustwave.dbpjobservice.workflow;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;







import com.trustwave.dbpjobservice.impl.JobProcessManager;
import com.trustwave.dbpjobservice.impl.Messages;
import com.trustwave.dbpjobservice.impl.exceptions.WorkflowException;
import com.trustwave.dbpjobservice.workflow.ActionExecutor.Command;
import com.trustwave.dbpjobservice.workflow.api.action.ActionException;
import com.trustwave.dbpjobservice.workflow.api.action.IJobAction;
import com.trustwave.dbpjobservice.workflow.api.action.JobServiceContextImpl;
import com.trustwave.dbpjobservice.xml.XmlExitCondition;

public class ActionRunner implements Runnable
{
	private static Logger logger = LogManager.getLogger( ActionRunner.class );
	private IJobAction action;
	private Command command;
	private ActionExecutor executor;
	private JobProcessManager jobProcessManager;
	
	public ActionRunner( IJobAction action, Command command )
	{
		this.action = action;
		this.command = command;
		this.executor = ActionExecutorFactory.getActionExecutor();
		this.jobProcessManager = JobProcessManager.getInstance();
	}

	@Override
	public void run() 
	{
		//MDC.put( "processId", String.valueOf( action.getProcessId() ) );
		//MDC.put( "item", action.getItem() );

		// lock action in memory while processing; unlock is done automatically
		// on saving action output, including action completion. 
		action.lockInMemory();
		
		Exception e = action.getInitializationError(); 
		if (e != null) {
			logger.error( "Action initialization failed: " + action + ": " + e.getMessage()
					    + "; failing action without execution" );
			action.setCondition( XmlExitCondition.WORKFLOW_ERROR, e.getMessage() );
			action.setErrorDetails( Messages.getString("workflow.action.init.error", e.getMessage()) );
			action.setFailedAction( action.getNodeName() );
			action.setState( null );
			saveActionOutput( action, true );
			getJobProcessManager().completeAction( action );
			getExecutor().actionCompleted( action );
			return;
		}
		
		
		boolean completed = runActionPhase();
			
		if (completed) {
			// set completed state:
			action.setState( null );
			logger.debug( "Completed: " + action + ", command=" + command
					   + ", exitCondition=" + action.getExitCondition() );
			saveActionOutput( action, true );
			// persist completion in workflow
			getJobProcessManager().completeAction( action );
			// notify executor for cleanup
			// TODO: use fireEvent + event listeners instead
			getExecutor().actionCompleted( action );
		}
		else {
			logger.debug( "Command " + command + " finished for " + action
					    + ", state=" + action.getState() );
			saveActionOutput( action, false );
			ActionStateExtended newState =
					ActionStateExtended.get( action.getState() );
			if (newState.needsCheckForCompletion( action )) {
				getExecutor().scheduleCheckCompleted( action );
			}
			getExecutor().actionCommandExecuted( action, command );
		}
	}
	
	private void saveActionOutput( final IJobAction action, boolean actionComplete )
	{
		try {
			getJobProcessManager().saveActionOutput( action, actionComplete );
		}
		catch (Exception e) {
			logger.error( "Cannot save action output of " + action + ": " + e, e );
		}
	}
	
	protected boolean runActionPhase()
	{
		try {
			ActionStateExtended state = ActionStateExtended.get( action.getState() );
			boolean completed = state.executeCommand( action, command );
			return completed;
		} 
		catch (Exception e) {
			processException( e );
			return true;
		}
		finally {
			JobServiceContextImpl.getInstance().closeAllEndpointsInCurrentThread();
		}
	}
	
	void processException( Exception ex )
	{
		if (ex instanceof ActionException) {
			ActionException tex = (ActionException)ex;
			logger.warn( "ActionException in " + action + ": "
					    + tex.getExitCondition().value() + ", " + ex.getMessage() );
			action.setCondition( tex.getExitCondition(), tex.getMessage() );
		}
		else {
			logger.warn( "Exception in " + action + ": " + ex.toString(), ex );
			if (ex instanceof WorkflowException) {
				action.setCondition( XmlExitCondition.WORKFLOW_ERROR, ex.getMessage() );
			}
			else {
				action.setCondition( XmlExitCondition.GENERAL_FAILURE, ex.getMessage() );
			}
			logger.warn( action + " exit condition is set to " + action.getExitCondition());
		}
	}
	
	
	public ActionExecutor getExecutor() {
		return executor;
	}
	public JobProcessManager getJobProcessManager() {
		return jobProcessManager;
	}

	public void setJobProcessManager(JobProcessManager processManager) {
		this.jobProcessManager = processManager;
	}
	public void setExecutor(ActionExecutor executor) {
		this.executor = executor;
	}

}

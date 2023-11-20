package com.trustwave.dbpjobservice.workflow;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.googlecode.sarasvati.NodeToken;
import com.googlecode.sarasvati.env.Env;
import com.trustwave.dbpjobservice.impl.JobProcessManager;
import com.trustwave.dbpjobservice.impl.exceptions.WorkflowException;
import com.trustwave.dbpjobservice.parameters.ParameterManager;
import com.trustwave.dbpjobservice.workflow.api.action.ActionState;
import com.trustwave.dbpjobservice.workflow.api.action.IJobAction;
import com.trustwave.dbpjobservice.workflow.api.action.JobAction;
import com.trustwave.dbpjobservice.workflow.api.action.JobContext;
import com.trustwave.dbpjobservice.workflow.api.action.JobContextImpl;
import com.trustwave.dbpjobservice.workflow.api.action.JobServiceContextImpl;

public class ActionFactory 
{
	private static Logger logger = LogManager.getLogger( ActionFactory.class );
	private static ActionFactory instance;
	
	public void init() 
	{
		instance = this;
	}

	public static ActionFactory getInstance()
	{
		return instance;
	}

	private WorkflowManager workflowManager;
	private JobProcessManager processManager;
	
	@Transactional
	public JobAction createAction( long tokenId, long processId )
	{
		if (logger.isDebugEnabled()) {
			logger.debug( "Creating action for tokenId=" + tokenId );
		}
		NodeToken token;
		try {
			token = workflowManager.getTokenById( tokenId, processId );
		} 
		catch (Exception e) {
			logger.error( "Cannot retrieve token id=" + tokenId + ": " + e, e );
			// fake action, fake initialization - just to fail action in caller
			JobAction action = new JobAction();
			action.init( "unknown", 0, tokenId, 0, null,
					     new JobContextImpl( JobServiceContextImpl.getInstance(),
					    		             "unknown", 0, 0) );
			action.setInitializationError( e );
			return action;
		}
		return createAction( token );
	}

	public JobAction createAction( NodeToken token )
	{
		JobAction action = doCreateAction( token );
		//MDC.put( "item", action.getItem() );
		
		if (logger.isDebugEnabled()) {
			logger.debug( "Created " + action );
		}
		return action;
	}
	
	private JobAction doCreateAction( NodeToken token )
	{
		// create action
		JobAction action;
		try {
			action = createAction0( token );
		} 
		catch (Exception e) {
			logger.error( "Cannot create action: " + e.toString(), e );
			// create pseudo-action, just for status logging and completing:
			action = new JobAction();
			initAction( action, token, (JobActionNode)token.getNode() );
			populateMinimalSetOfParameters( action, token );
			action.setInitializationError( e );
			return action;
		}
		
		// populate action parameters
		try {
			processManager.populateAction( action, token );
		} 
		catch (Exception e) {
			logger.error( "Cannot populate " + action + ": " + e.toString(), e );
			populateMinimalSetOfParameters( action, token );
			action.setInitializationError( e );
			return action;
		}

		if (ActionState.INITIAL != action.getState()) {
			// BeginTime field is set only in INITIAL state and not persisted.
			// However, it is used in percent evaluation, and leaving it null
			// may lead to strange percentage (no action progress is shown).
			// Let's set it to the closest known estimation of begin time:
			action.setBeginTime( token.getCreateDate() );
		}
		
		// initialize action
		try {
			action.init();
		} 
		catch (Exception e) {
			logger.error( "Cannot init " + action + ": " + e.toString(), e );
			action.setInitializationError( e );
		}
		finally {
			JobServiceContextImpl.getInstance().closeAllEndpointsInCurrentThread();
		}
		
		return action;
	}
	
	private void populateMinimalSetOfParameters( JobAction action, NodeToken token )
	{
		ParameterManager pm = processManager.getParameterManager( token );
		// safe populate call, should not throw, but just in case ...
		try {
			pm.populateActionParameters( action, token, true, IJobAction.STATUS_PARAMETERS );
		}
		catch (Exception e1) {
			logger.error( "Exception from safe populate: " + e1.toString(), e1 );
		}
	}

	protected JobAction createAction0( NodeToken token ) 
	{
		JobActionNode node =
				(JobActionNode)WorkflowUtils.resolveNodeReference( token.getNode() );
		JobAction action;
		try {
			action = node.getActionClass().newInstance();
		} 
		catch (Exception e) {
			throw new WorkflowException( "Cannot instantiate action "
					    + node.getAttributes().getActionType().getClazz()
					    + " in node " + node.getName()
					    + ": " + e.toString());
		}
		initAction( action, token, node );
		return action;
	}
	
	private void initAction( JobAction action, NodeToken token, JobActionNode node )
	{
		long procId = getProcessManager().getEngineFactory()
				                         .getProcessId( token.getProcess() );
		Env processEnv = token.getProcess().getEnv();
		JobContext context = new JobContextImpl(
				JobServiceContextImpl.getInstance(),
				processEnv.getAttribute( "jobName" ),
				Integer.valueOf( processEnv.getAttribute( "jobOrgId" ) ),
				Integer.valueOf( processEnv.getAttribute( "jobId" ) )
				);
		action.init( node.getName(),
				     node.getId(),
				     token.getId(), 
				     procId, 
				     node.getAttributes(),
				     context
					);
	}
	
	public WorkflowManager getWorkflowManager() {
		return workflowManager;
	}

	public void setWorkflowManager(WorkflowManager workflowManager) {
		this.workflowManager = workflowManager;
	}

	public JobProcessManager getProcessManager() {
		return processManager;
	}

	public void setProcessManager(JobProcessManager processManager) {
		this.processManager = processManager;
	}

}

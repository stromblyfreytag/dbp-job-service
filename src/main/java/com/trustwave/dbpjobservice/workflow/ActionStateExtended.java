package com.trustwave.dbpjobservice.workflow;








import static com.trustwave.dbpjobservice.workflow.api.action.ActionState.INITIAL;
import static com.trustwave.dbpjobservice.workflow.api.action.ActionState.PAUSED;
import static com.trustwave.dbpjobservice.workflow.api.action.ActionState.PAUSED_WAITING;
import static com.trustwave.dbpjobservice.workflow.api.action.ActionState.RUNNING;
import static com.trustwave.dbpjobservice.workflow.api.action.ActionState.WAITING;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;







import com.trustwave.dbpjobservice.impl.JobProcessManager;
import com.trustwave.dbpjobservice.impl.Messages;
import com.trustwave.dbpjobservice.impl.exceptions.WorkflowException;
import com.trustwave.dbpjobservice.workflow.ActionExecutor.Command;
import com.trustwave.dbpjobservice.workflow.api.action.ActionException;
import com.trustwave.dbpjobservice.workflow.api.action.ActionState;
import com.trustwave.dbpjobservice.workflow.api.action.IJobAction;
import com.trustwave.dbpjobservice.xml.XmlEventDescriptor;
import com.trustwave.dbpjobservice.xml.XmlExitCondition;

public abstract class ActionStateExtended
{
	protected static Logger logger = LogManager.getLogger( ActionStateExtended.class );
	
	private static Map<ActionState, ActionStateExtended> statemap;
	
	static {
		statemap = new HashMap<ActionState, ActionStateExtended>();
		add( new InitialState() );
		add( new RunningState() );
		add( new WaitingState() );
		add( new PausedState() );
		add( new PausedWaitingState() );
		add( new CompletedState() );
	}
	
	private static void add( ActionStateExtended estate )
	{
		statemap.put( estate.getActionState(), estate );
	}
	
	public static ActionStateExtended get( ActionState state )
	{
		return statemap.get( state );
	}
	
	public abstract ActionState getActionState();

	public abstract boolean executeCommand( IJobAction action, Command command );
	
	public boolean needsCheckForCompletion( IJobAction action )
	{
		return true;
	}
	
	protected boolean doBegin( IJobAction action )
	{
		boolean result;
		long time0 = System.currentTimeMillis();
		try {
			if (logger.isDebugEnabled()) {
				logger.debug( "Begin executing " + action );
			}
			result = action.begin();
		} 
		catch (Exception e) {
			processException( action, e );
			result = true;
		}
		checkReasonableExecutionTime( time0, "begin", action );
		return result;
	}

	protected boolean doCheckCompleted( IJobAction action )
	{
		boolean result;
		long time0 = System.currentTimeMillis();
		try {
			if (logger.isTraceEnabled()) {
				logger.trace( "Checking if completed: " + action );
			}
			result = action.checkCompleted();
		} 
		catch (Exception e) {
			processException( action, e );
			result = true;
		}
		checkReasonableExecutionTime( time0, "checkCompleted", action );
		return result;
	}
	
	private void checkReasonableExecutionTime( long time0, String exectype, IJobAction action)
	{
		long time = System.currentTimeMillis() - time0;
		if (time > 10*1000) {
			logger.warn( "Execution of '" + action.getActionTypeName() + "'."
		               + exectype + "() took " + (time/1000) + " sec." );
		}
	}
	
	protected boolean doProcessEvent( IJobAction action )
	{
		XmlEventDescriptor evt = action.getEventReceived();
		if (evt == null) {
			logger.warn( "No event received in " + action );
			return false;
		}
		if (evt.isCleanCondition()) {
			action.setExitCondition( null );
			action.setErrorDetails( null );
			action.setFailedAction( null );
		}
		return evt.isForceCompletion();
	}
	
	protected void doCancel( IJobAction action )
	{
		long time0 = System.currentTimeMillis();
		try {
			logger.debug( "Canceling " + action );
			action.cancel();
			action.setCondition( XmlExitCondition.CANCELED, Messages.getString("workflow.action.abondoned") );
		} 
		catch (Exception e) {
			processException( action, e );
		}
		checkReasonableExecutionTime( time0, "cancel", action );
	}
	
	protected void doPause( IJobAction action )
	{
		long time0 = System.currentTimeMillis();
		try {
			logger.debug( "Pausing " + action );
			action.pause();
		}
		catch (Exception e) {
			processException( action, e );
		}
		checkReasonableExecutionTime( time0, "pause", action );
	}
	
	protected void doResume(  IJobAction action )
	{
		long time0 = System.currentTimeMillis();
		try {
			logger.debug( "Resuming " + action );
			action.resume();
		}
		catch(Exception e) {
			processException( action, e );
		}
		checkReasonableExecutionTime( time0, "resume", action );
	}
	
	protected void processException( IJobAction action, Exception ex )
	{
		if (ex instanceof ActionException) {
			ActionException tex = (ActionException)ex;
			logger.warn( "TaskException in " + action + ": "
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
	
	protected JobProcessManager getJobProcessManager()
	{
		return JobProcessManager.getInstance();
	}
}


//-----------------------------------------------------------------------------
//              state-specific classes
//-----------------------------------------------------------------------------

class InitialState extends ActionStateExtended
{
	@Override
	public ActionState getActionState() { return INITIAL; }

	@Override
	public boolean executeCommand( IJobAction action, Command command) 
	{
		switch (command) {
		case RUN:
			action.setState( RUNNING );
			action.setBeginTime( new Date() );
			boolean completed = doBegin( action );
			return completed;
		case EVENT:
		case CANCEL:
		case PAUSE:
		case RESUME:
			logger.warn( "Ignoring " + command + " command for " + action + ", state=" + getActionState() );
		}
		return false;
	}
}

class RunningState extends ActionStateExtended
{
	@Override
	public ActionState getActionState() { return RUNNING; }

	protected ActionState pausedState() { return PAUSED; }
	
	@Override
	public boolean executeCommand( IJobAction action, Command command) 
	{
		switch (command) {
		case RUN:
			return doCheckCompleted( action );
		case CANCEL:
			doCancel( action );
			return true;
		case PAUSE:
			doPause( action );
			action.setState( pausedState() );
			return false;
		case EVENT:
		case RESUME:
			logger.warn( "Ignoring " + command + " command for " + action + ", state=" + getActionState() );
		}
		return false;
	}
	
}

class WaitingState extends RunningState
{
	@Override
	public ActionState getActionState() { return WAITING; }
	
	@Override
	protected ActionState pausedState() { return PAUSED_WAITING; }
	
	@Override
	public boolean executeCommand( IJobAction action, Command command) 
	{
		switch (command) {
		case EVENT:
			boolean completed = doProcessEvent( action );
			if (!completed) {
				// no force-completion in event, do normal completion check:
				completed = doCheckCompleted( action );
			}
			return completed;
			
		default: break;
		}
		return super.executeCommand( action, command );
	}
}

class PausedState extends ActionStateExtended
{
	@Override
	public ActionState getActionState() { return PAUSED; }

	protected ActionState runningState() { return RUNNING; }

	@Override
	public boolean needsCheckForCompletion( IJobAction action ) { return false;	}
	
	@Override
	public boolean executeCommand( IJobAction action, Command command) 
	{
		switch (command) {
		case RESUME:
			doResume( action );
			action.setState( runningState() );
			return doCheckCompleted( action );
		case CANCEL:
			doCancel( action );
			return true;
		case RUN:
			return false;
		case PAUSE:
		case EVENT:
			logger.warn( "Ignoring " + command + " command for " + action + ", state=" + getActionState() );
		}
		return false;
	}
}

class PausedWaitingState extends PausedState
{
	@Override
	public ActionState getActionState() { return PAUSED_WAITING; }

	@Override
	protected ActionState runningState() { return WAITING; }
}

class CompletedState extends ActionStateExtended
{
	@Override
	public ActionState getActionState() { return null; }

	@Override
	public boolean needsCheckForCompletion( IJobAction action ) { return false;	}
	
	@Override
	public boolean executeCommand( IJobAction action, Command command) 
	{
		logger.warn( "Ignoring " + command + " command for " + action + ", state=" + getActionState() );
		return true;
	}
}



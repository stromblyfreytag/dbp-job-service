package com.trustwave.dbpjobservice.workflow;

import static com.trustwave.dbpjobservice.workflow.ActionExecutor.Command.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;



import com.trustwave.dbpjobservice.workflow.api.action.ActionState;
import com.trustwave.dbpjobservice.workflow.api.action.IJobAction;

/**
 * <p>Synchronous action executor that executes submitted actions in the caller's
 * thread. Check for completion is supposed to be invoked explicitly, with
 * {@link #executeActions(long)} method.
 * </p>
 * <p>The class also provides possibility to force-complete actions.
 * This facility may be used in to complete actions waiting for event
 * or in unit tests.
 * </p>
 * 
 * @author vlad
 */
public class SynchronousActionExecutor implements ActionExecutor 
{
	private ArrayList<IJobAction> notCompletedActions = new ArrayList<IJobAction>();
	private boolean executeOnSubmit;
	
	public void registerWithFactory()
	{
		ActionExecutorFactory.setActionExecutor(  this );
	}
	
	public SynchronousActionExecutor() 
	{
		this.executeOnSubmit = true;
	}

	public SynchronousActionExecutor( boolean executeOnSubmit ) 
	{
		this.executeOnSubmit = executeOnSubmit;
	}

	public void submit( IJobAction action, Command command )
	{
		if (executeOnSubmit || command != RUN) {
			executeAction( action, false, command );
		}
		else {
			scheduleCheckCompleted( action );
		}
	}
	
	public synchronized void shutdown()
	{
	}

	public synchronized void scheduleCheckCompleted( final IJobAction action ) 
	{
		notCompletedActions.add( action );
	}
	
	public void actionCompleted( IJobAction action ) 
	{
		removeAction( action );
	}

	private synchronized void removeAction( IJobAction action )
	{
		notCompletedActions.remove( action );
	}
	
	
	/**
	 * @return list of not completed actions for the process, may be empty.
	 */
	public synchronized Collection<IJobAction> getNotCompletedActions( long processId )
	{
		ArrayList<IJobAction> processActions = new ArrayList<IJobAction>();
		for (IJobAction action: getNotCompletedActions() ) {
			if (processId == action.getProcessId()) {
				processActions.add( action );
			}
		}
		return processActions;
	}
	
	/**
	 * @return list of not completed actions, may be empty.
	 */
	protected synchronized Collection<IJobAction> getNotCompletedActions()
	{
		return new ArrayList<IJobAction>( notCompletedActions );
	}
	
	
	/**
	 * Runs checkComplete() call on all not-completed actions with positive
	 * checkCompletedInterval for the given process.
	 * @param processId - process ID
	 */
	public List<IJobAction> executeActions( long processId )
	{
		return executeActions( processId, false, false );
	}
	
	/**
	 * Runs checkComplete() call on all not-completed actions for the 
	 * given process; optionally force-completes actions.  
	 * @param processId - process ID
	 * @param forceComplete - if actions should be considered completed
	 *        after check.
	 * @param includeWaiting - if actions waiting for event (those with
	 *        non-positive check interval) should be checked and maybe
	 *        force-completed as well
	 */
	protected List<IJobAction> executeActions( long processId, boolean forceComplete, boolean includeWaiting )
	{
		List<IJobAction> executed = new ArrayList<>();
		for (IJobAction action: getNotCompletedActions( processId ) ) {
			if (action.getState() == ActionState.INITIAL
			 || action.getCheckCompletedInterval() > 0 || includeWaiting) {
				executeAction( action, forceComplete, RUN);
				executed.add( action );
			}
		}
		return executed;
	}
	
	/**
	 * @param name  - action name
	 * @param processId - process ID
	 * @return first not completed action with the given name in the given process
	 * or <code>null</code> 
	 */
	public IJobAction findActionByName( String name, long processId )
	{
		for (IJobAction action: getNotCompletedActions( processId ) ) {
			if (name.equals( action.getActionName() ))
				return action;
		}
		return null;
	}

	/**
	 * @param name  - action name
	 * @param processId - process ID
	 * @return first not completed action with the given name in the given process
	 * or <code>null</code> 
	 */
	public IJobAction findActionByNameAndItem( String name, String item, long processId )
	{
		for (IJobAction action: getNotCompletedActions( processId ) ) {
			if (name.equals( action.getActionName() )
			 && item.equals(action.getItem()))
				return action;
		}
		return null;
	}
	
	public void executeAction( IJobAction action, boolean forceComplete, Command command )
	{
		removeAction( action );
		new ForceCompletionRunner( action, command, forceComplete ).run();
	}
	
	public void executeAction( String actionName, long processId, boolean forceComplete )
	{
		IJobAction action = findActionByName( actionName, processId );
		if (action != null) {
			executeAction( action, forceComplete, RUN );
		}
	}
	
	/**
	 * force-completes given action - use with caution
	 * @param action
	 */
	public void completeAction( IJobAction action )
	{
		executeAction( action, true, RUN );
	}

	@Override
	public void actionCommandExecuted( IJobAction action, Command command ) {
	}
}

class ForceCompletionRunner extends ActionRunner
{
	private boolean forceComplete;
	
	public ForceCompletionRunner(IJobAction action, ActionExecutor.Command command, boolean forceComplete)
	{
		super( action, command );
		this.forceComplete = forceComplete  && (command == RUN || command == RESUME);
	}
	
	@Override
	protected boolean runActionPhase() 
	{
		boolean completed = super.runActionPhase();
		return (completed || forceComplete);
	}
}
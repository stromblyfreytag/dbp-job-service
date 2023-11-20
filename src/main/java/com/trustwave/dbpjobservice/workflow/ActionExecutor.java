package com.trustwave.dbpjobservice.workflow;

import com.trustwave.dbpjobservice.workflow.api.action.IJobAction;

public interface ActionExecutor
{
	public enum Command {
		RUN,    // call begin() or checkCompleted(), depending on action state
		EVENT,  // process received event, only in WAITING state; may call checkCompleted()
		CANCEL, // call cancel() method
		PAUSE,  // call pause() method - only in RUNNING or WAITING state
		RESUME  // call resume() and checkCompleted() - only in paused states.
		;
	}
	
	/** <p>Submit execution of command on action.</p>
	 * <p>Depending on command and the current action state, either begin(),
	 * or checCompleted(), cancel(), pause(), resume() method
	 * of the action may be executed.
	 * </p>
	 * <p> Command may also be ignored if action state is not valid for
	 *  the command; e.g. RESUME command may be executed only in one of the
	 *  paused states, EVENT - in waiting state, etc.
	 * </p>
	 *  
	 * @param action The action to submit for execution.
	 * @param command command to execute on action
	 */
	public void submit( IJobAction action, Command command );

	/**
	 * <p>Schedule execution of action's checkCompleted() call.
	 * The call will be invoked in {@link IJobAction#getCheckCompletedInterval()}
	 * milliseconds.</p>
	 * <p>The call will <b>not</b> be scheduled if number of milliseconds
	 *  returned by getCheckCompletedInterval() is less or equals to 0.</p>
	 *   
	 * @param action The action for which checkCompleted() call should be scheduled.
	 */
	public void scheduleCheckCompleted( final IJobAction action ); 

	/**
	 * <p>Callback to cleanup executor after action completion.
	 *  All scheduled calls for this action will be discarded.</p>
	 *  <p>Invoked by {@link ActionRunner} when action is completed.</p>
	 * @param action Completed action.
	 */
	public void actionCompleted( IJobAction action ); 
	
	/** 
	 * Callback to let executor know when current command for action has finished
	 * and it is safe to run the next command, if any.
	 * @param action  Action for which command has finished
	 * @param command finished command
	 */
	public void actionCommandExecuted( IJobAction action, Command command );
	
	/** Shutdown action executor.
	 * All further submit calls will be silently ignored.
	 * Previously submitted or scheduled task executions will be discarded. 
	 */
	public void shutdown();

}
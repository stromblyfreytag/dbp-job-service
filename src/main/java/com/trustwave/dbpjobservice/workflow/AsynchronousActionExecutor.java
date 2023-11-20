package com.trustwave.dbpjobservice.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.trustwave.dbpjobservice.impl.Configuration;
import com.trustwave.dbpjobservice.workflow.api.action.IJobAction;

public class AsynchronousActionExecutor implements ActionExecutor
{
	private static Logger logger = LogManager.getLogger( AsynchronousActionExecutor.class );
	
	private Timer timer = null;
	private ThreadPoolExecutor executor = null;
	private Map<IJobAction,List<TimerTask>> scheduledActionsMap =
		new HashMap<IJobAction, List<TimerTask>>();
	private Map<IJobAction,Command> currentActionCommand =
			new HashMap<IJobAction, Command>();
	private Map<IJobAction,List<Command>> delayedActionCommands =
			new HashMap<IJobAction, List<Command>>();
	private AtomicBoolean shutdownFlag = new AtomicBoolean( false );
	
	private Configuration config;
	
	public AsynchronousActionExecutor()
	{
	}

	// this constructor is called only in unit tests, with mock executor 
	public AsynchronousActionExecutor( ThreadPoolExecutor executor, Timer timer ) 
	{
		this.executor = executor;
		this.timer = timer;
	}
	
	public void registerWithFactory()
	{
		logger.info( "Registering AsynchronousActionExecutor" );
		ActionExecutorFactory.setActionExecutor(  this );
	}
	
	public void init() 
	{
		if (executor == null) {
			int minThreads = config.getMinActionThreads();
			int maxThreads = config.getMaxActionThreads();
			logger.info( "Initializing action executor, minThreads=" + minThreads
					   + ", maxThreads=" + maxThreads );
			executor = new MinimalWaitExecutor(	minThreads, maxThreads, 20L );
			
			timer = new Timer( "TaskTimer", true );
		}
		registerWithFactory();
	}
	
	public void submit( IJobAction action, Command command )
	{
		if (shutdownFlag.get()) {
			logger.info( "Shutdown: - not submitting " + action );
			return;
		}
		
		Command prevCommand;
		
		synchronized (this) {
			prevCommand = currentActionCommand.get(action);
			if (prevCommand == null) {
				currentActionCommand.put(action, command);
			} 
			else if (prevCommand != command) {
				// previous command is running or in execution queue;
				// let's put the new one into the action delayed list,
				// to ensure sequential execution of action commands.
				// Putting command into execution queue served by multiple threads
				// cannot guarantee sequential execution.
				addToDelayed(action, command);
			}
		}
		
		if (prevCommand != null && logger.isDebugEnabled()) {
			if (prevCommand != command) {
					logger.debug( "Previous command " + prevCommand + " for " + action
						    	+ " still executing, delaying command " + command );
			}
			else {
					logger.debug( "Skipping command " + command + " for " + action
						    	+ " - the same command is still running or queued" );
			}
		}
		
		if (prevCommand == null) {
			// no previous commands executed for this action, OK to submit:  
			doSubmit( action, command );
		}
	}
	
	private void doSubmit( IJobAction action, Command command )
	{
		if (logger.isDebugEnabled()) {
			logger.debug( "submitting for execution: " + command + ", " + action
					    + "; action queue=" + ((LinkedBlockingQueue<Runnable>)executor.getQueue()).size() );
		}
		executor.execute( new ActionRunner( action, command ) );
	}
	
	private void addToDelayed( IJobAction action, Command command )
	{
		List<Command> delayedCommands = delayedActionCommands.get( action );
		if (delayedCommands == null) {
			delayedCommands = new ArrayList<Command>();
			delayedActionCommands.put( action, delayedCommands );
		}
		delayedCommands.add( command );
	}
	
	// remove and return the first delayed command; 
	// returns null if there are no delayed commands.
	Command getFirstDelayedCommand( IJobAction action, boolean remove )
	{
		List<Command> delayedCommands = delayedActionCommands.get( action );
		if (delayedCommands == null || delayedCommands.size() == 0) {
			// no delayed commands
			return null;
		}
		// if delayedCommands exists, it should be non-empty:
		Command command = delayedCommands.get( 0 );
		if (remove) {
			delayedCommands.remove( 0 );
			if (delayedCommands.size() == 0) {
				delayedActionCommands.remove( action );
			}
		}
		return command;
	}
	
	public void shutdown()
	{
		logger.info( "Shutting down action executor" );
		shutdownFlag.set( true );
		if (executor != null) {
			timer.cancel();
			// we don't want to interrupt running actions (as shutdownNow() does)
			// but we don't want any actions remain in the queue:
			executor.getQueue().drainTo( new ArrayList<Runnable>() );
			executor.shutdown();
			executor = null;
		}
	}

	/**
	 * <p>Schedule execution of action's checkCompleted() call.
	 * The call will be invoked in {@link IJobAction#getCheckCompletedInterval()}
	 * milliseconds.</p>
	 * <p>The call will <b>not</b> be scheduled if number of milliseconds
	 *  returned by getCheckCompletedInterval() is less or equals to 0.</p>
	 *   
	 * @param action The action for which checkCompleted() call should be scheduled.
	 */
	public void scheduleCheckCompleted( final IJobAction action ) 
	{
		long interval = action.getCheckCompletedInterval();
		if (interval <= 0) {
			if (logger.isDebugEnabled()) {
				logger.debug( "Not scheduling completion check for " + action + " - will wait for external event" );
			}
			return;
		}
		TimerTask delayedCheck = new TimerTask() {
			public void run() {
				removeScheduledTimerTask( action, this );
				submit( action, Command.RUN );
			}
		};
		saveScheduledAction( action, delayedCheck);
		
		if (!shutdownFlag.get()) {
			timer.schedule( delayedCheck, interval );
			if (logger.isDebugEnabled()) {
				logger.debug( "Scheduled completion check for " + action + " in  " + interval + "ms." );
			}
		}
	}
	
	/**
	 * <p>Callback to cleanup any scheduled action calls.
	 *  Any scheduled action calls will be discarded.</p>
	 *  <p>Invoked by {@link ActionRunner} when action is completed.</p>
	 * @param action Completed action.
	 */
	public void actionCompleted( IJobAction action ) 
	{
		if (logger.isDebugEnabled()) {
			logger.debug( "Action completed: " + action );
		}
		removeCurrentAndScheduledActions( action );
	}

	public void actionCommandExecuted( IJobAction action, Command command )
	{
		Command delayed;
		synchronized (this) {
			currentActionCommand.remove( action );
			delayed = getFirstDelayedCommand( action, true );
			if (delayed != null) {
				currentActionCommand.put(action, delayed);
			}
		}
		if (delayed != null) {
			doSubmit( action, delayed );
		}
	}
	
	private synchronized void saveScheduledAction( IJobAction action, TimerTask timerTask )
	{
		List<TimerTask> scheduledActions = scheduledActionsMap.get( action );
		if (scheduledActions == null) {
			scheduledActions = new ArrayList<TimerTask>( 2 );
			scheduledActionsMap.put( action, scheduledActions );
		}
		scheduledActions.add( timerTask );
	}
	
	private synchronized void removeScheduledTimerTask( IJobAction action, TimerTask timerTask )
	{
		List<TimerTask> scheduledTasks = scheduledActionsMap.get( action );
		if (scheduledTasks != null) {
			scheduledTasks.remove( timerTask );
		}
	}
	
	private synchronized void removeCurrentAndScheduledActions( IJobAction action )
	{
		currentActionCommand.remove( action );
		List<TimerTask> scheduledTasks = scheduledActionsMap.get( action );
		if (scheduledTasks != null) {
			for (TimerTask t: scheduledTasks) {
				t.cancel();
			}
		}
		scheduledActionsMap.remove( action );
		delayedActionCommands.remove( action );
	}
	
	int getCurrentActionCommandSize()
	{
		return currentActionCommand.size();
	}
	
	int getScheduledActionsSize()
	{
		return scheduledActionsMap.size();
	}
	
	int getScheduledActionTasksSize( IJobAction action )
	{
		List<TimerTask> scheduledTasks = scheduledActionsMap.get( action );
		return scheduledTasks != null? scheduledTasks.size(): 0;
	}
	
	int getDelayedActionCommandsSize()
	{
		return delayedActionCommands.size();
	}

	public void setConfig(Configuration config) 
	{
		this.config = config;
	}
}

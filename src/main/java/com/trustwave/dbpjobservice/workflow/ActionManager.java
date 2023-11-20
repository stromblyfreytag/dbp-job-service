package com.trustwave.dbpjobservice.workflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.trustwave.dbpjobservice.impl.JobProcessManager;
import com.trustwave.dbpjobservice.workflow.api.action.IJobAction;
import com.trustwave.dbpjobservice.xml.XmlEventDescriptor;

public class ActionManager implements ActionExecutor
{
	private static Logger logger = LogManager.getLogger( ActionManager.class );
	
	private Map<Long,Set<IJobAction>> actions = new HashMap<Long, Set<IJobAction>>();
	private Map<String,List<IJobAction>> eventActionMap = new HashMap<String, List<IJobAction>>();
	private Set<Long> processesToCancel = new HashSet<Long>(); 
	
	private ActionExecutor executor;
	
	public ActionManager( ActionExecutor executor ) 
	{
		this.executor = executor;
	}

	// This method is invoked by Spring AFTER the wrapped executor
	// was created/registered, so ActionManager becomes the last
	// who is registered with factory.
	public void registerWithFactory()
	{
		logger.info( "Registering ActionManager" );
		ActionExecutorFactory.setActionExecutor(  this );
	}
	
	@Override
	public void scheduleCheckCompleted(IJobAction task) 
	{
		executor.scheduleCheckCompleted(task);
	}
	
	@Override
	public void shutdown() 
	{
		executor.shutdown();
	}
	
	@Override
	public void submit(IJobAction action, Command command ) 
	{
		// NOTE: we put synchronization here rather than in addAction() because we want actions
		// to be added to executor in the same order they arrived here;
		// also, and we don't wont to include submission to executor into synchronization block -
		// to avoid deadlock with e.g. removeAction() if we have to wait on full executor queue.
		synchronized(this) {
			if (command == Command.RUN || command == Command.RESUME) {
				addAction( action );
			}
		}
		executor.submit( action, command );
	}
	
	@Override
	public void actionCompleted( IJobAction action ) 
	{
		removeAction( action );
		executor.actionCompleted(action);
		long processId = action.getProcessId();
		if (getProcessActions(processId).isEmpty()) {
			fireNoActiveTasks(processId);
		}
	}
	
	@Override
	public void actionCommandExecuted( IJobAction action, Command command ) 
	{
		executor.actionCommandExecuted( action, command );
	}
	
	private void addAction( IJobAction action )
	{
		Set<IJobAction> processActions = actions.get( action.getProcessId() );
		if (processActions == null) {
			processActions = new HashSet<IJobAction>();
			actions.put( action.getProcessId(), processActions );
		}
		processActions.add( action );
		
		for (XmlEventDescriptor evt: action.getNodeAttributes().getOnEvent()) {
			List<IJobAction> eventActions = eventActionMap.get( evt.getName() );
			if (eventActions == null) {
				eventActions = new ArrayList<IJobAction>();
				eventActionMap.put( evt.getName(), eventActions );
			}
			eventActions.add( action );
		}
	}
	
	private synchronized void removeAction( IJobAction action )
	{
		Set<IJobAction> processActions = actions.get( action.getProcessId() );
		if (processActions != null) {
			processActions.remove( action );
			if (processActions.size() == 0) {
				actions.remove( action.getProcessId() );
			}
		}
		for (XmlEventDescriptor evt: action.getNodeAttributes().getOnEvent()) {
			List<IJobAction> eventActions = eventActionMap.get( evt.getName() );
			if (eventActions != null) {
				eventActions.remove( action );
			}
		}
	}
	
	public synchronized Collection<IJobAction> getProcessActions( long processId )
	{
		Set<IJobAction> processActions = actions.get( processId );
		return processActions != null? new ArrayList<>( processActions )
				                     : new ArrayList<IJobAction>();
	}
	
	public List<IJobAction> getActionsWaitingForEvent( String eventName )
	{
		List<IJobAction> waitingActions = new ArrayList<IJobAction>();
		List<IJobAction> eventActions = eventActionMap.get( eventName );
		if (eventActions != null) {
			for (IJobAction a: eventActions) {
				if (a.isWaiting()) {
					waitingActions.add( a );
				}
			}
		}
		return waitingActions;
	}
	
	public void eventReceived( IJobAction action, XmlEventDescriptor eventDsc )
	{
		logger.debug( "eventReceived: " + eventDsc.getName() + ", " + action );
		action.setEventReceived( eventDsc );
		executor.submit( action, Command.EVENT );
	}

	public void beginCancelingProcessActions(long processId) 
	{
		Collection<IJobAction> processActions = getProcessActions( processId );
		logger.info( "canceling actions for pid=" + processId
				    + ", #actions=" + processActions.size() );
		addProcessToCancel(processId);
		if (processActions.isEmpty()) {
			fireNoActiveTasks(processId);
		}
		else {
			for (IJobAction action : processActions) {
				executor.submit( action, Command.CANCEL );
			}
		}
	}
	
	
	/**
	 * This method will pause all running tasks
	 * @param processId
	 */
	public void pauseProcessActions(long processId) 
	{
		Collection<IJobAction> processActions = getProcessActions( processId );
		for (IJobAction action : processActions) {
			executor.submit( action, Command.PAUSE );
		}
	}
	
	private void fireNoActiveTasks(long processId)
	{
		if (isBeingCanceled(processId)) {
			JobProcessManager.getInstance().finalizeCancel(processId);
			markCanceled(processId);
		}
	}

	public synchronized void addProcessToCancel(Long processId)
	{
		this.processesToCancel.add(processId);
	}
	
	public synchronized boolean isBeingCanceled(long processId) 
	{
		return this.processesToCancel.contains(processId);
	}

	public synchronized void markCanceled(Long processId) 
	{
		this.processesToCancel.remove(processId);
	}

	public void resumeProcessActions(long processId) 
	{
		Collection<IJobAction> processActions = getProcessActions( processId );
		for (IJobAction action : processActions) {
			executor.submit( action, Command.RESUME );
		}
	}

}

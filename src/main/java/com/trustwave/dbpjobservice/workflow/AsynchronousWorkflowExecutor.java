package com.trustwave.dbpjobservice.workflow;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.trustwave.dbpjobservice.impl.Configuration;
import com.trustwave.dbpjobservice.workflow.api.util.TimeLogger;

/**
 * The class provides single-threaded access to workflow processes
 * as required by Sarasvati engine, as well as proper transaction management.
 * It accumulates and executes workflow tasks one by one, each task
 * in its own transaction.
 *  
 * @author vlad
 *
 */
public class AsynchronousWorkflowExecutor implements Runnable, WorkflowExecutor
{
	private static Logger logger = LogManager.getLogger( AsynchronousWorkflowExecutor.class );
	
	private WorkflowManager workflowManager;
	private Configuration config = new Configuration();
	private WfTaskQueue      queue;
	private Thread           thread = null;

	@Override
	public void run()
	{
		int batchSize = config.getWorkflowExecutorBatchSize();
		queue = new WfTaskQueue( config.getWorkflowExecutorQueueSize() );
		
		logger.info( "Workflow executor started; batchSize=" + batchSize
				   + ", maxQueueSize=" + queue.getMaxSize()
				   + ", saveInWorkflowThread=" + config.isSaveOutputInWorkflowThread() 
				   + ", executionPolicy="
				   + (config.isUseLifoExecutionPolicy()? "LIFO": "FIFO") );
		
		List<WorkflowTask> wftasks = new LinkedList<WorkflowTask>();
		Holder<WorkflowTask> lastTaskHolder = new Holder<WorkflowTask>();
		TimeLogger tlog = new TimeLogger( logger, "WorkflowExecutor", 20 );
		
		while (queue.getFirstTasks(wftasks, batchSize)) {
			try {
				tlog.restartTimer();
				int size = wftasks.size();
				workflowManager.executeWorkflowTasks( wftasks, lastTaskHolder );
				int qsize = queue.getSize();
				tlog.log( "Executed %d tasks, qsize=%d", size, qsize );
			} 
			catch (Exception e) {
				WorkflowTask wftask = lastTaskHolder.get();
				logger.error( "Exception in task " + wftask + ": " + e, e );
				// let's retry without offending task
				wftasks.remove( wftask );
			}
		}
		logger.info( "Workflow executor finished" );
	}
	
	@Override
	public boolean isAlive()
	{
		return thread != null && thread.isAlive();
	}
	
	/* (non-Javadoc)
	 * @see WorkflowExecutor#submit(WorkflowTask)
	 */
	public void submit( WorkflowTask wftask )
	{
		queue.add( wftask );
	}
	
	/* (non-Javadoc)
	 * @see WorkflowExecutor#pushProcess(long)
	 */
	public void pushProcess( long processId )
	{
		submit ( new ContinueProcessTask( processId ) );
	}
	
	// TODO unit tests for start/stop
	
	// invoked by Spring
	public synchronized void start()
	{
		if (thread == null) { 
			thread = new Thread( this, "Workflow" );
			thread.start();
		}
	}
	
	//invoked by Spring
	public void stop()
	{
		logger.info( "Stopping workflow executor" );
		queue.drainContinueTasksAndClose();
	}
	
	public WorkflowManager getWorkflowManager() {
		return workflowManager;
	}
	public void setWorkflowManager(WorkflowManager workflowManager) {
		this.workflowManager = workflowManager;
	}

	public Configuration getConfig() {
		return config;
	}
	public void setConfig(Configuration config) {
		this.config = config;
	}
}

class WfTaskQueue
{
	private static Logger logger = LogManager.getLogger( AsynchronousWorkflowExecutor.class );
	private LinkedList<WorkflowTask> queue = new LinkedList<WorkflowTask>();
	private HashSet<WorkflowTask> set = new HashSet<WorkflowTask>();
	private boolean stopFlag = false;
	private int maxSize;
	
	public WfTaskQueue(int maxSize) 
	{
		this.maxSize = maxSize;
	}

	public synchronized void add( WorkflowTask task )
	{
		if (stopFlag) {
			if (task instanceof ContinueProcessTask) {
				// no continuation in stop mode, only save parameters and such
				return;
			}
			if (logger.isDebugEnabled()) {
				logger.debug( "adding task after stop: " + task
						    + ", qsize=" + queue.size()+1 );
			}
		}
		if (set.contains(task)) {
			if (logger.isDebugEnabled()) {
				logger.debug( "not adding task: " + task
						    + " - already exists. qsize=" + queue.size()+1 );
			}
			// same tasks are equivalent in their outcome:
			return;
		}
		if (logger.isDebugEnabled()) {
			logger.debug( "Adding task: " + task + ", qsize=" + queue.size()+1 );
		}
		
		// we cannot wait for space in queue when ContinueProcessTask
		// is received - it is submitted by the workflow's own thread,
		// from JobProcessManager.doCompleteAction().
		if (!(task instanceof ContinueProcessTask)) {
			while (queue.size() >= maxSize) {
				try {
					wait();
				} 
				catch (InterruptedException e) {
				}
			}
		}
		queue.add( task );
		set.add( task );
		notify();
	}
	
	public synchronized boolean getFirstTasks( List<WorkflowTask> tasks, int maxTasks)
	{
		if (tasks.size() > 0) {
			// first consume what is already there
			return true;
		}
		while (queue.size() > 0 && tasks.size() < maxTasks) {
			WorkflowTask task = queue.remove(0);
			set.remove( task );
			tasks.add( task );
		}
		if (tasks.size() > 0) {
			notifyAll();
			return true;
		}
		
		while (queue.size() == 0 && !stopFlag) {
			try {
				wait();
			} 
			catch (InterruptedException e) {
			}
		}
		while (queue.size() > 0 && tasks.size() < maxTasks) {
			WorkflowTask task = queue.remove(0);
			set.remove( task );
			tasks.add( task );
		}
		return tasks.size() > 0;
	}
	
	public synchronized WorkflowTask getFirst()
	{
		while (queue.size() == 0 && !stopFlag) {
			try {
				wait();
			} 
			catch (InterruptedException e) {
			}
		}
		WorkflowTask task = null;
		if (queue.size() > 0) {
			task = queue.remove(0);
			set.remove( task );
		}
		return task;
	}
	
	public synchronized void drainContinueTasksAndClose()
	{
		Iterator<WorkflowTask> it = queue.iterator();
		while (it.hasNext()) {
			WorkflowTask wftask = it.next();
			if (wftask instanceof ContinueProcessTask) {
				it.remove();
			}
		}
		stopFlag = true;
		if (logger.isDebugEnabled()) {
			logger.debug( "on stop: qsize=" + queue.size() );
		}
		notify();
	}
	
	public int getMaxSize() {
		return maxSize;
	}

	public int getSize() {
		return queue.size();
	}
}

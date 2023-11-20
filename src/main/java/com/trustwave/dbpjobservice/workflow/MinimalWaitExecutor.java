package com.trustwave.dbpjobservice.workflow;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>ThreadPoolExecutor that increases number of processing threads to maximum not waiting for full queue.</p>
 * <p>See discussion and proposed solutions here:
 * http://stackoverflow.com/questions/19528304/how-to-get-the-threadpoolexecutor-to-increase-threads-to-max-before-queueing.
 * </p>
 * 
 * <p>Here we use solution with auto-increasing/auto-decreasing core pool size,
 * as the most reliable and non-tricky (no hidden problems).
 * </p>
 *  
 * @author vaverchenkov
 *
 */
public class MinimalWaitExecutor extends ThreadPoolExecutor 
{
	private static Logger logger = LogManager.getLogger( MinimalWaitExecutor.class );
	private int activeTaskCount = 0;
	private final int minPoolSize;

	public MinimalWaitExecutor(int minThreads, int maxThreads, long keepAliveSeconds) 
	{
		//    We use unlimited queue for action executor because limiting it may lead to
		// waiting in the workflow executor thread and even deadlock - when both queues are full
		// and waiting for each other - e.g. action threads hang on sending save-action-output task
		// to the workflow executor queue, and workflow executor thread hangs submitting action command.
		//    Typical max. size of this queue is number of active job threads (execution lines);
		// may grow as much as twice when canceling or pausing job (one more command per job thread).
		
		super( minThreads, maxThreads, keepAliveSeconds, TimeUnit.SECONDS,
			   new LinkedBlockingQueue<Runnable>() );
		minPoolSize = minThreads;
	}
	
	@Override
	public void execute(Runnable runnable) 
	{
		int tasks;
		synchronized (this) {
			tasks = ++activeTaskCount;
			if (tasks >= minPoolSize && tasks <= getMaximumPoolSize()) {
				setCorePoolSize( tasks );
			}
		}
		super.execute(runnable);
		
		if (logger.isDebugEnabled()) {
			logger.debug( "executor: queue=" + ((LinkedBlockingQueue<Runnable>)getQueue()).size()
					    + ", core threads=" + getCorePoolSize() );
		}
	}
	
	@Override
	protected void afterExecute( Runnable runnable, Throwable throwable ) 
	{
		super.afterExecute(runnable, throwable);
		synchronized (this) {
			int tasks = --activeTaskCount;
			if (tasks >= minPoolSize && tasks <= getMaximumPoolSize()) {
				setCorePoolSize( tasks );
			}
		}
	}

}

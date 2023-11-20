package com.trustwave.dbpjobservice.workflow;

/**
 *    This interface allows to abstract threading and transaction policy
 * used for accessing Sarasvati engine.
 *    Standard implementation ({@link AsynchronousWorkflowExecutor}) 
 * will execute submitted tasks in a dedicated thread, each task in a separate
 * transaction - to allow asynchronous node execution.
 *    Immediate workflow executor will execute tasks in the caller's thread
 * and transaction, which is useful e.g in unit tests.
 *  
 * @author vlad
 */
public interface WorkflowExecutor 
{
	/** Submit workflow task (e.g. node token completion) for execution
	 * @param wftask
	 */
	public void submit( WorkflowTask wftask );

	/** Shortcut for submitting 'continue process' workflow task
	 * @param processId
	 */
	public void pushProcess( long processId );

	/** Shutdown workflow executor
	 */
	public void stop();

	public boolean isAlive();
}
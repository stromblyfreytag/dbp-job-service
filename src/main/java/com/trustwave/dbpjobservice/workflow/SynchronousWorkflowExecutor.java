package com.trustwave.dbpjobservice.workflow;

import com.googlecode.sarasvati.Engine;

public class SynchronousWorkflowExecutor implements WorkflowExecutor 
{
	private WorkflowManager workflowManager;

	@Override
	public void pushProcess( long processId ) 
	{
		submit( new ContinueProcessTask( processId ) );
	}

	@Override
	public void stop() 
	{
	}

	@Override
	public void submit(WorkflowTask wftask) 
	{
		if (wftask.getEngine() == null) {
			Engine engine =
					getWorkflowManager().createEngine( wftask.getProcessId() );
			wftask.setEngine( engine );
			wftask.setWfManager( getWorkflowManager() );
		}
		wftask.run();
	}

	public WorkflowManager getWorkflowManager() {
		return workflowManager;
	}
	public void setWorkflowManager(WorkflowManager workflowManager) {
		this.workflowManager = workflowManager;
	}

	@Override
	public boolean isAlive() {
		return true;
	}
}

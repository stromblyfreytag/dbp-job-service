package com.trustwave.dbpjobservice.workflow;

public class ContinueProcessTask extends WorkflowTask
{
	public ContinueProcessTask( long processId ) 
	{
		super( "ContinueProcess", processId, null );
	}

	@Override
	public void run() 
	{
		getWfManager().continueProcess( getProcessId(), getEngine() );
	}
}
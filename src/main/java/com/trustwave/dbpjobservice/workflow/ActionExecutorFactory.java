package com.trustwave.dbpjobservice.workflow;

public class ActionExecutorFactory 
{
	private static ActionExecutor actionExecutor;

	public static ActionExecutor getActionExecutor() 
	{
		return actionExecutor;
	}
	
	public static void setActionExecutor( ActionExecutor taskExecutor ) 
	{
		ActionExecutorFactory.actionExecutor = taskExecutor;
	}
}

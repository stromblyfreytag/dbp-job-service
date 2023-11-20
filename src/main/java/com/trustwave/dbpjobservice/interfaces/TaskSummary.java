package com.trustwave.dbpjobservice.interfaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
public class TaskSummary 
{
	Task     task;
	long     averageExecutionTime;       
	long     previousTasksExecutionTime; // sum of average execution times of
	                                     // previous tasks
	private long averageActionTime[] = null;
	
	public TaskSummary( TaskSummary other ) 
	{
		this.task = new Task( other.task );
	}
	
	public TaskSummary() {
	}
	
	public Task getTask() {
		return task;
	}
	public void setTask(Task userTask) {
		this.task = userTask;
	}
	public List<String> getActionNames() {
		return task.getActionNames();
	}
	
	public void initAverageTimes( Map<String, Long> actionTimeMap )
	{
		int numberOfActions = task.getActionNames().size();
		averageActionTime = new long[ numberOfActions ];
		long total = 0;
		
		for (int i = 0;  i < numberOfActions;  i++) {
			String actionName = task.getActionNames().get( i );
			Long t = actionTimeMap.get( actionName );
			if (task.isNoTimeAction( actionName )) {
				// ignore action's time for percent evaluation
				t = new Long(0);
			}
			averageActionTime[i] = (t != null? t.longValue(): 100);
			total += averageActionTime[i];
		}
		averageExecutionTime = (total > 0? total: 100);
	}
	
	public long getAverageActionExecutionTime( String actionName )
	{
		int ind = actionIndex( actionName );
		return (ind >= 0? averageActionTime[ind]: 0);
	}
	
	/**
	 * @return estimated time of actionExecution
	 */
	public long getAverageExecutionTime() 
	{
		return averageExecutionTime;
	}
	
	public long getAverageExecutionTimeUpTo( String actionName )
	{
		long time = 0;
		int ind = actionIndex( actionName );
		if (ind >= 0) {
			for (int i = 0;  i <= ind;  i++) {
				time += averageActionTime[i];
			}
		}
		return time;
	}
	
	public void setAverageExecutionTime( long totalEstimatedTime ) {
		this.averageExecutionTime = totalEstimatedTime;
	}
	public long getPreviousTasksExecutionTime()	{
		return previousTasksExecutionTime;
	}
	public void setPreviousTasksExecutionTime( long previousTasksEstimatedTime ) {
		this.previousTasksExecutionTime = previousTasksEstimatedTime;
	}

	public int actionIndex( String actionName ) {
		return actionName != null? task.getActionNames().indexOf( actionName ): -1;
	}
	public int totalNumberOfActions() {
		return task.getActionNames().size();
	}
	
	@Override
	public String toString() 
	{
		return "TaskStatistics: " + task;
	}
}

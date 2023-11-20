package com.trustwave.dbpjobservice.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.trustwave.dbpjobservice.interfaces.Task;
import com.trustwave.dbpjobservice.interfaces.TaskSummary;
import com.trustwave.dbpjobservice.workflow.api.action.IJobAction;
import com.trustwave.dbpjobservice.xml.XmlTaskCategory;

/** <p>This class represents chain of all tasks of a process,
 * ordered as specified in the process graph.</p>
 * <p>Chain is built from the linearized process graph,
 * see {@link TaskChainFactory}.</p>
 *  <p>Task with the same name may be included into chain only
 * once, even if that task was mentioned several times in
 * the process graph.</p> 
 */
public class TaskChain 
{
	private final List<TaskSummary> chain = new ArrayList<TaskSummary>();
	private long totalAverageTime;
	
	public TaskChain() {
	}
	
	public TaskChain( TaskChain other )
	{
		for (TaskSummary taskSummary: other.chain) {
			this.chain.add( new TaskSummary( taskSummary ) );
		}
	}
	
	public List<TaskSummary> getChain() {
		return chain;
	}
	
	public List<Task> getTasks()
	{
		List<Task> tasks = new ArrayList<Task>();
		for (TaskSummary ts: chain) {
			tasks.add( ts.getTask() );
		}
		return tasks;
	}

	public TaskSummary find( Task task )
	{
		for (TaskSummary taskSummary: chain) {
			if (task.equals( taskSummary.getTask() )) {
				return taskSummary;
			}
		}
		return null;
	}
	
	public TaskSummary find( String taskName )
	{
		for (TaskSummary taskSummary: chain) {
			if (taskName.equals( taskSummary.getTask().getName() )) {
				return taskSummary;
			}
		}
		return null;
	}
	
	TaskSummary addTask( String name, XmlTaskCategory category,
			             boolean isEssential, String actionName )
	{
		TaskSummary ts = addTask( name, category, isEssential );
		ts.getTask().addAction( actionName, 1 );
		return ts;
	}
	
	TaskSummary addTask( String name, XmlTaskCategory category, boolean isEssential )
	{
		if (category == null) {
			category = XmlTaskCategory.OTHER;
		}
		if (name == null) {
			name = IJobAction.DEFAULT_TASK_NAME;
		}
		final Task task = new Task( name, category.value(), isEssential );
		
		// Task may be included into chain only once,
		// if summary for this task already exist, use it:
		TaskSummary ts = find( task );
		if (ts == null) {
			ts = new TaskSummary();
			ts.setTask( task );
			chain.add( ts );
		}
		return ts;
	}
	
	void initTaskTimes( Map<String, Long> averageActionTimeMap )
	{
		totalAverageTime = 0;
		for (TaskSummary ts: chain) {
			ts.initAverageTimes( averageActionTimeMap );
			ts.setPreviousTasksExecutionTime( totalAverageTime );
			totalAverageTime += ts.getAverageExecutionTime();
		}
		if (totalAverageTime == 0) {
			// just in case, to avoid devide by zero
			totalAverageTime = 100;
		}
	}
	
	public int getTotalTaskCount()
	{
		int count = 0;
		for (TaskSummary taskSummary: chain) {
			count += taskSummary.getActionNames().size();
		}
		return count;
	}

	public long getTotalAverageTime()
	{
		return totalAverageTime;
	}

	public void setTotalAverageTime( long totalEstimatedTime )
	{
		this.totalAverageTime = totalEstimatedTime;
	}

	@Override
	public String toString() 
	{
		return "TaskChain " + chain;
	}
	
}

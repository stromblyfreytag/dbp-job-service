package com.trustwave.dbpjobservice.workflow;

import com.googlecode.sarasvati.Engine;

public abstract class WorkflowTask implements Runnable 
{
	private WorkflowManager wfManager;
	private Engine          engine;
	private String          name;
	private long            processId;
	private Object          action;
	
	
	public WorkflowTask(String name, long processId, Object action ) {
		this.name = name;
		this.processId = processId;
		this.action = action;
	}
	
	public abstract void run();
	
	public WorkflowManager getWfManager() {
		return wfManager;
	}
	public void setWfManager(WorkflowManager wfManager) {
		this.wfManager = wfManager;
	}
	public Engine getEngine() {
		return engine;
	}
	public void setEngine(Engine engine) {
		this.engine = engine;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getProcessId() {
		return processId;
	}

	public String toString() 
	{
		return getName() + "(pid=" + processId + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (int) (processId ^ (processId >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WorkflowTask other = (WorkflowTask) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (processId != other.processId)
			return false;
		if (action != other.action) {
			return false;
		}
		return true;
	}
}

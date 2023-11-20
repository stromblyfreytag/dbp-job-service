package com.trustwave.dbpjobservice.interfaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Information about user task for a particular line of execution,
 * e.g. for one asset
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class TaskInfo 
{
	Task     task;  // Task object, see fields below:
	//       String name;           // task name
	//       String category;       // task category, one of:
	                                //      Prechecks, Audit, Pentest, Discovery,
	                                //      DataWarehousing, Report, Other
	//       boolean isEssential;   // true if task is essential for job success
	                                // i.e. if task failure cannot be ignored
	String   item;             // Workflow thread identifier, set by workflow; typically - per asset.
	                           // e.g assetId=123, or iprange=192.168.3.0/24
	Date     beginTime;        // When task begun
	Date     endTime;          // When task finished, only for completed tasks
	String   currentState;     // Name of the currently executed action or 'Waiting for join'.
	boolean  waiting;          // true if task is waiting for scheduled time or external event to resume
	boolean  timeWaiting;      // true if task is waiting for scheduled time
	boolean  needsAttention;   // true if task is waiting for user input
	boolean  paused;           // true if task is paused
	String   waitingMessage;   // Waiting message, e.g. 'Waiting for resume'
	String   errorCategory;    // If not null - task has error; for completed tasks indicates failure.
	String   details;          // error message or status message provided by action
	long     tokenId;          // token ID of action being executed; 0 for completed tasks
	double   percentCompleted; // percent completed for this task 
	String   lastMainstreamAction; // Name of the last main-stream action - the farthest node
	                               // in the 'normal' action chain reached so far.
	                               // Mainstream actions are those that can be accessed through default
	                               // (unnamed) arcs. Used as a measure of task progress.
	List<ResumeAction> resumeActions =  // only for waiting actions: list of resuming actions to display in UI
		new ArrayList<ResumeAction>();
	
	public Task getTask() {
		return task;
	}
	public void setTask(Task task) {
		this.task = task;
	}
	public void setItem(String item) {
		this.item = item;
	}
	public String getItem() {
		return item;
	}
	public void setBeginTime(Date beginTime) {
		this.beginTime = beginTime;
	}
	public Date getBeginTime() {
		return beginTime;
	}
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	public Date getEndTime() {
		return endTime;
	}
	public void setCurrentState(String currentState) {
		this.currentState = currentState;
	}
	public String getCurrentState() {
		return currentState;
	}
	public void setWaiting(boolean waiting) {
		this.waiting = waiting;
	}
	public boolean isWaiting() {
		return waiting;
	}
	public boolean isTimeWaiting() {
		return timeWaiting;
	}
	public void setTimeWaiting(boolean timeWaiting) {
		this.timeWaiting = timeWaiting;
	}
	public boolean isNeedsAttention() {
		return needsAttention;
	}
	public void setNeedsAttention(boolean needsAttention) {
		this.needsAttention = needsAttention;
	}
	public boolean isPaused() {
		return paused;
	}
	public void setPaused(boolean paused) {
		this.paused = paused;
	}
	public String getWaitingMessage() {
		return waitingMessage;
	}
	public void setWaitingMessage(String waitingMessage) {
		this.waitingMessage = waitingMessage;
	}
	public void setErrorCategory(String errorCategory) {
		this.errorCategory = errorCategory;
	}
	public String getErrorCategory() {
		return errorCategory;
	}
	public void setDetails(String details) {
		this.details = details;
	}
	public String getDetails() {
		return details;
	}
	public long getTokenId() {
		return tokenId;
	}
	public void setTokenId(long tokenId) {
		this.tokenId = tokenId;
	}
	public List<ResumeAction> getResumeActions() {
		return resumeActions;
	}
	public void setResumeActions(List<ResumeAction> resumeActions) {
		this.resumeActions = resumeActions;
	}
	public String getLastMainstreamAction() {
		return lastMainstreamAction;
	}
	public void setLastMainstreamAction(String lastNonExceptionalTask) {
		this.lastMainstreamAction = lastNonExceptionalTask;
	}
	public double getPercentCompleted() {
		return percentCompleted;
	}
	public void setPercentCompleted(double percentCompleted) {
		this.percentCompleted = percentCompleted;
	}

	@Override
	public String toString() {
		return "TaskInfo[ " + task.getName() + ", "
		      + (!getItem().isEmpty()? ", item=" + getItem() : "")
		      + ", currentState=" + currentState
		      + (getEndTime() != null? ", completed" : "")
			  +  "]";
	}
	
	
}

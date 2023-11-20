package com.trustwave.dbpjobservice.interfaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class JobProcessInfo 
{
    public static enum CompletionState {
        SUCCESS,  // All tasks (essential and non-essential) completed OK
        PARTIAL,  // Some tasks succeeded, some failed 
        FAILURE,  // All essential tasks failed
        CANCELED  // Job instance was canceled
    }
	
	long       processId;      // job instance id
	long       jobId;          // job id
	String     jobName;        // job name
	long       templateId;     // template (workflow) ID
	String     templateName;   // template (workflow) name
	@XmlTransient //Prevent interface change for ES 2.0. We may decide to remove this later.
	int        orgId;		   // org the process's job belongs to
	Date       beginTime;      // when job instance was created
	Date       endTime;        // when job instance finished, null for not finished
	boolean    active;         // true when job is active (started, not finished and not paused)
	boolean    finished;       // true when job is finished
	boolean    canceled;       // true when job was canceled
	boolean    paused;         // true when job is paused
	String     state;          // one of: Created, Executing, Paused, Completed, Canceled,
	                           //         PendingCancel, PendingCompletion
	CompletionState completionState; // one of SUCCESS, PARTIAL, FAILURE, CANCELED, or null.
	                                 // Not null when job is finished.
	
	int        needsAttentionCount;  // number of task waiting for user input  
	int        timeWaitingCount;     // number of task waiting for scheduled time
	int        activeTasksCount;     // total number of active tasks.
	                                 // NOTE: process is time-waiting when 
	                                 //      active AND timeWaitingCount > 0
	                                 //      AND timeWaitingCount == activeTasksCount  
	double            percentCompleted;  // percent completed estimation, 100% for completed

	// Fields below provide detailed process info (on all tasks/threads)
	// and are populated only by calls that return single status objects;
	// multi-process calls like getActiveProcesses() will leave these fields empty.

	List<TaskInfo>    taskInfoList =     // state of every task instance (1 per task per thread)
		new ArrayList<TaskInfo>();   

	public long getProcessId() {
		return processId;
	}
	public void setProcessId(long instanceId) {
		this.processId = instanceId;
	}
	public long getJobId() {
		return jobId;
	}
	public void setJobId(long jobId) {
		this.jobId = jobId;
	}
	public String getJobName() {
		return jobName;
	}
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}
	public long getTemplateId() {
		return templateId;
	}
	public void setTemplateId(long templateId) {
		this.templateId = templateId;
	}
	public String getTemplateName() {
		return templateName;
	}
	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}
	public Date getBeginTime() {
		return beginTime;
	}
	public void setBeginTime(Date beginTime) {
		this.beginTime = beginTime;
	}
	public double getPercentCompleted() {
		return percentCompleted;
	}
	public void setPercentCompleted(double percentCompleted) {
		this.percentCompleted = percentCompleted;
	}
	public Date getEndTime() {
		return endTime;
	}
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	public List<TaskInfo> getTaskInfoList() {
		return taskInfoList;
	}
	public void setTaskInfoList(List<TaskInfo> userTasksList) {
		this.taskInfoList = userTasksList;
	}
	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
	public boolean isFinished() {
		return finished;
	}
	public void setFinished(boolean finished) {
		this.finished = finished;
	}
	public boolean isCanceled() {
		return canceled;
	}
	public void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}
	public boolean isPaused() {
		return paused;
	}
	public void setPaused(boolean paused) {
		this.paused = paused;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
    public int getOrgId() {
		return orgId;
	}
	public void setOrgId(int orgId) {
		this.orgId = orgId;
	}
	
	/** @see CompletionState */
    public CompletionState getCompletionState() {
		return completionState;
	}
    /** @see CompletionState */
    public void setCompletionState(final CompletionState completionState) {
		this.completionState = completionState;
	}
	public int getNeedsAttentionCount() {
		return needsAttentionCount;
	}
	public void setNeedsAttentionCount( int needsAttentionCount ) {
		this.needsAttentionCount = needsAttentionCount;
	}
	public int getTimeWaitingCount() {
		return timeWaitingCount;
	}
	public void setTimeWaitingCount( int timeWaitingCount ) {
		this.timeWaitingCount = timeWaitingCount;
	}
	public int getActiveTasksCount() {
		return activeTasksCount;
	}
	public void setActiveTasksCount( int activeTasksCount ) {
		this.activeTasksCount = activeTasksCount;
	}
}

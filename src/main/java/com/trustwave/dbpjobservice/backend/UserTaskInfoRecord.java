package com.trustwave.dbpjobservice.backend;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;

@Entity
@Table(name = "js_usertask_info")
public class UserTaskInfoRecord 
{
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="usertask_id")
	private long id;
	
    @Column(name="process_id", nullable=false )
	private long processId;

	@Column(name="usertask_name", nullable=false)
	private String taskName;

	@Column(name="item", nullable=false)
	private String item;
	
	@Column(name="begin_time")
	private Date beginTime;
	
	@Column(name="end_time")
	private Date endTime;
	
	@Column(name="current_state")
	private String currentState;

	@Column(name="condition")
	private String condition;

	@Column(name="message")
	private String message;

	@Column(name="failed_task")
	private String failedTaskName;

	@Column(name="wait_message")
	private String waitMessage;

    @Column(name="last_token_id", nullable=false)
	private long lastTokenId;

    @Column(name="last_node_id", nullable=false)
	private long lastNodeId;
    
    @Column(name="head")
    private Boolean head;
    
    @Column(name="last_success_node_name")
	private String lastSuccessfulNodeName;
    
    @Column(name="percent_completed")
    private Double percentCompleted;
	
	public UserTaskInfoRecord() 
	{
	}

	
	public UserTaskInfoRecord( long   processId, 
			                   String usertaskName,
			                   String item,
			                   Date   beginTime, 
			                   long   firstTokenId, 
			                   long   firstNodeId )
	{
		this.processId = processId;
		this.taskName  = usertaskName;
		this.item      = item;
		this.beginTime = beginTime;
		this.lastTokenId = firstTokenId;
		this.lastNodeId  = firstNodeId;
		this.head = true;
	}
	
	public String toString()
	{
		return "{" + getId() + ", " + getTaskName()
			 + ", item=" + getItem()
			 + ", pid=" + processId
			 + "}";
	}
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	
	public long getProcessId() {
		return processId;
	}
	public void setProcessId(long processId) {
		this.processId = processId;
	}
	public String getTaskName() {
		return taskName;
	}
	public void setTaskName(String name) {
		this.taskName = name;
	}
	public String getItem() {
		return item;
	}
	public void setItem(String item) {
		this.item = item;
	}
	public Date getBeginTime() {
		return beginTime;
	}
	public void setBeginTime(Date beginTime) {
		this.beginTime = beginTime;
	}
	public Date getEndTime() {
		return endTime;
	}
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	public String getCurrentState() {
		return currentState;
	}
	public void setCurrentState(String currentState) {
		this.currentState = currentState;
	}
	public String getCondition() {
		return condition;
	}
	public void setCondition(String condition) {
		this.condition = condition;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getFailedTaskName() {
		return failedTaskName;
	}
	public void setFailedTaskName(String failedTaskName) {
		this.failedTaskName = failedTaskName;
	}
	public String getWaitMessage() {
		return waitMessage;
	}
	public void setWaitMessage(String waitMessage) {
		this.waitMessage = waitMessage;
	}
	public long getLastTokenId() {
		return lastTokenId;
	}
	public void setLastTokenId(long lastTokenId) {
		this.lastTokenId = lastTokenId;
	}
	public long getLastNodeId() {
		return lastNodeId;
	}
	public void setLastNodeId(long lastNodeId) {
		this.lastNodeId = lastNodeId;
	}
	public Boolean getHead() {
		return head;
	}
	public void setHead( Boolean head ) {
		this.head = head;
	}
	public String getLastSuccessfulNodeName() {
		return lastSuccessfulNodeName;
	}
	public void setLastSuccessfulNodeName( String lastNodeName ) {
		this.lastSuccessfulNodeName = lastNodeName;
	}
	public Double getPercentCompleted() {
		return percentCompleted;
	}
	public void setPercentCompleted( double percentCompleted ) {
		this.percentCompleted = percentCompleted;
	}

	@Transient
	public void setWaitMessage( String waitMsg, boolean timeWaiting ) {
		if (timeWaiting) {
			waitMsg = "T:" + waitMsg;
		}
		setWaitMessage( waitMsg );
	}
	
	@Transient
	public String getWaitMessageWithoutPrefix() {
		if (isTimeWaiting()) {
			return waitMessage.substring(2);
		}
		return waitMessage;
	}
	
	@Transient
	public boolean isTimeWaiting()
	{
		return (waitMessage != null && waitMessage.startsWith("T:")); 
	}
	
	@Transient
	public boolean isWaiting() {
		return waitMessage != null;
	}
	
	@Transient
	// Discriminator for LastTaskForItem mode: item if not empty, otherwise task.
	// Task is prefixed with '\n' to avoid accidental conflict with item
	public String getItemOrTask()
	{
		return item != null && item.length() > 0? item: "\n" + taskName;
	}
	
	@Override
	public int hashCode() 
	{
		return (int)id;
	}


	@Override
	public boolean equals(Object obj) 
	{
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof UserTaskInfoRecord)) {
			return false;
		}
		return id == ((UserTaskInfoRecord)obj).id;
	}
	
}

package com.trustwave.dbpjobservice.backend;

import static com.googlecode.sarasvati.ProcessState.Canceled;
import static com.googlecode.sarasvati.ProcessState.Created;
import static com.googlecode.sarasvati.ProcessState.PendingCancel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;


import com.googlecode.sarasvati.ProcessState;
import com.trustwave.dbpjobservice.interfaces.JobProcessInfo;

@Entity
@Table(name = "js_job_process")
public class JobProcessRecord implements Serializable
{
	private static final long serialVersionUID = 1L;

	@Id	@Column(name="process_id")
	private long id;
	
	@ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="job_id" )
	private JobRecord job;
	
	@Column(name="paused")
	private boolean paused;
	
	@Column(name="completion_state")
    @Enumerated(EnumType.STRING)
	private JobProcessInfo.CompletionState completionState;
	
	@Column(name="complete_date") 
	@Temporal(TemporalType.TIMESTAMP)
	private Date completeDate;

	@Column(name="job_name") 
	private String jobName;

	@Column(name="template_id") 
	private int templateId;

	@Column(name="create_date") 
	@Temporal(TemporalType.TIMESTAMP)
	private Date createDate;
	
	@Column(name="process_state") 
	private String state;
	
	@Transient
	private static Set<Long> activeSet = new HashSet<Long>();
	
    public JobProcessRecord() {
	}
	
	public JobProcessRecord( long id, JobRecord job ) 
	{
		this.id = id;
		this.createDate = new Date();
		this.job = job;
		// save original job name and template with the process
		// - job may be renamed or updated (change template version) later:
		this.jobName = job.getName();
		this.templateId = job.getJobTemplate().getId().intValue();
		this.state = Created.name();
	}
	
	@Transient
	public boolean isFinished()
	{
		return completeDate != null;
	}

	@Transient
	public boolean isCanceled()
	{
		return Canceled.name().equals( state )
			|| PendingCancel.name().equals( state );
	}

	@Transient
	public boolean isActive()
	{
		if (isFinished()) {
			return false;
		}
		return isActive( getId() );
	}
	
	public void setActive( boolean active )
	{
		setActive( getId(), active );
	}
	
	public static synchronized boolean isActive( Long procId ) 
	{
		return activeSet.contains( procId );
	}
	
	public static synchronized void setActive( Long procId, boolean active ) 
	{
		if (active)
			activeSet.add( procId );
		else 
			activeSet.remove( procId );
	}
	
	public void updateStatus( ProcessState processState )
	{
		this.state = processState.name();
	}

	public String toString()
	{
		return "'" + jobName + "[" + id+ "]'";
	}

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public JobRecord getJob() {
		return job;
	}
	public void setJob(JobRecord job) {
		this.job = job;
	}
	public boolean isPaused() {
		return paused;
	}
	public void setPaused(boolean paused) {
		this.paused = paused;
	}
    public JobProcessInfo.CompletionState getCompletionState() {
        return completionState;
    }
    public void setCompletionState(final JobProcessInfo.CompletionState completionState) {
        this.completionState = completionState;
    }
	public Date getCompleteDate() {
		return completeDate;
	}
	public void setCompleteDate(Date completeDate) {
		this.completeDate = completeDate;
	}
	public Date getCreateDate() {
		return createDate;
	}
	public void setCreateDate( Date createDate ) {
		this.createDate = createDate;
	}
	public String getJobName()	{
		return jobName;
	}
	public void setJobName( String jobName ) {
		this.jobName = jobName;
	}
	public int getTemplateId() {
		return templateId;
	}
	public void setTemplateId( int templateId )	{
		this.templateId = templateId;
	}
	public String getState() {
		return state;
	}
	public void setState( String status )	{
		this.state = status;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (int)(id ^ (id >>> 32));
		return result;
	}


	@Override
	public boolean equals( Object obj )
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JobProcessRecord other = (JobProcessRecord)obj;
		if (id != other.id)
			return false;
		return true;
	}
	
}

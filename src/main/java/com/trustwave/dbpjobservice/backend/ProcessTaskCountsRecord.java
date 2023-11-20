package com.trustwave.dbpjobservice.backend;

import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;

// we evaluate completion percent for the job as average over heads of thread

@Entity
@NamedNativeQuery( name = "taskCounts",
  query = "SELECT u.process_id processId "
        + ",COUNT(wait_message) totalWaitCount"
        + ",COUNT(CASE WHEN wait_message LIKE 'T:%' THEN 1 ELSE NULL END) timeWaitCount"
        + ",COUNT(CASE WHEN end_time IS NULL THEN 1 ELSE NULL END) activeCount"
        + ",AVG(CASE WHEN head = 1 THEN percent_completed ELSE NULL END) percentCompleted"
		+ "  FROM js_usertask_info u, js_job_process j"
        + " WHERE u.process_id = j.process_id"
		+   " AND j.complete_date IS NULL"
        + " GROUP BY u.process_id",
    resultSetMapping="implicit")
@SqlResultSetMapping( name="implicit",
    entities=@EntityResult(entityClass=ProcessTaskCountsRecord.class))

public class ProcessTaskCountsRecord
{
	@Id
	long processId;
	int  totalWaitCount;
	int  timeWaitCount;
	int  activeCount;
	Double percentCompleted;
	
	public long getProcessId() 	{
		return processId;
	}
	public void setProcessId( long processId ) 	{
		this.processId = processId;
	}
	public int getTotalWaitCount() {
		return totalWaitCount;
	}
	public void setTotalWaitCount( int waitTotalCount ) {
		this.totalWaitCount = waitTotalCount;
	}
	public int getTimeWaitCount() {
		return timeWaitCount;
	}
	public void setTimeWaitCount( int timeWaitCount ) {
		this.timeWaitCount = timeWaitCount;
	}
	public int getActiveCount()	{
		return activeCount;
	}
	public void setActiveCount( int activeCount ) {
		this.activeCount = activeCount;
	}
	public Double getPercentCompleted() {
		return percentCompleted;
	}
	public void setPercentCompleted( Double percent ) {
		this.percentCompleted = percent;
	}
}

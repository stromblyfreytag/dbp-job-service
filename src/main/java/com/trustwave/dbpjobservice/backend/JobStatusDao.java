package com.trustwave.dbpjobservice.backend;

import java.util.List;
import java.util.Map;

public interface JobStatusDao 
{
	List<UserTaskInfoRecord> getProcessTasks( long processId );
	
	List<UserTaskInfoRecord> getActiveProcessTasks( long processId );
	
	UserTaskInfoRecord findTaskInfo( long processId, String taskName, String item );
	
	UserTaskInfoRecord findTaskInfo( long taskRecordId );

	void saveTaskInfo( UserTaskInfoRecord taskRecord );
	
	void taskCompleted( UserTaskInfoRecord taskRecord );

	Map<String, Long> getAverageActionTimesForJob( long jobId );
	
	Map<String, Long> getAverageActionTimesForTemplate( long templateId );
}

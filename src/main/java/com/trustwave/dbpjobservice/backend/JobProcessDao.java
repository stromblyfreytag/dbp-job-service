package com.trustwave.dbpjobservice.backend;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface JobProcessDao 
{
	void saveJobProcess( JobProcessRecord jpr );

	JobProcessRecord getProcess( long id );

	List<JobProcessRecord> getRunningProcesses();
	
	List<JobProcessRecord> getAllProcesses();
	
	List<JobProcessRecord> getProcessesForJob( long jobId );
	
	List<JobProcessRecord> getCompletedProcesses( Date from, Date to );
	
	Map<Long,ProcessTaskCountsRecord> getActiveTaskCounts();
	
	void deleteProcess( JobProcessRecord jpr );
}

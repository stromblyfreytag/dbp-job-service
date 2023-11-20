package com.trustwave.dbpjobservice.backend;

import java.util.List;

public interface JobDao 
{
	List<JobTemplateRecord> getJobTemplates();
	
	JobTemplateRecord findJobTemplate( String name );
	
	JobTemplateRecord findJobTemplate( Long graphId );
	
	void saveJobTemplate( JobTemplateRecord jobTemplate );
	
	void deleteJobTemplate( JobTemplateRecord jobTemplate );
	
	JobRecord findJob( long jobId );
	
	JobRecord findJob( String jobName, int orgId );
	
	List<JobRecord> findJobsByTemplateId( long templateId );
	
	List<JobRecord> findAllJobs();
	
	void saveJob( JobRecord job );

	void deleteJob( JobRecord job );
}

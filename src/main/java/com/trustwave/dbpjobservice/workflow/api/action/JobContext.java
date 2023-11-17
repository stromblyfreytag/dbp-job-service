package com.trustwave.dbpjobservice.workflow.api.action;

public interface JobContext extends JobServiceContext {
    /**
     * @return job name
     */
    public String getJobName();

    /**
     * @return ID of the job's organization (organization in which job is defined)
     */
    public int getJobOrgId();

    /**
     * Job ID
     */
    public long getJobId();
}

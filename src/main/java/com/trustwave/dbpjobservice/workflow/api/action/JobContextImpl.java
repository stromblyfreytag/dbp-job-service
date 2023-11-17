package com.trustwave.dbpjobservice.workflow.api.action;

import javax.xml.ws.Service;

public class JobContextImpl implements JobContext {
    private JobServiceContext serviceContext;
    private String jobName;
    private int jobOrgId;
    private long jobId;

    public JobContextImpl(JobServiceContext serviceContext,
            String jobName,
            int jobOrgId,
            long jobId) {
        this.serviceContext = serviceContext;
        this.jobName = jobName;
        this.jobOrgId = jobOrgId;
        this.jobId = jobId;
    }

    @Override
    public <T> T getBean(String name, Class<T> clazz) {
        return serviceContext.getBean(name, clazz);
    }

    @Override
    public <T> T getServiceEndpoint(Class<T> endpointInterface,
            String serviceNameInOpenDs) {
        return serviceContext.getServiceEndpoint(endpointInterface, serviceNameInOpenDs);
    }

    @Override
    public <T> T getSecureServiceEndpoint(Class<T> endpointInterface,
            String serviceNameInOpenDs) {
        return serviceContext.getSecureServiceEndpoint(endpointInterface, serviceNameInOpenDs);
    }

    @Override
    public <T> T getServiceEndpointWithUrl(
            Class<? extends Service> serviceType, String endpointUrl) {
        return serviceContext.getServiceEndpointWithUrl(serviceType, endpointUrl);
    }

    @Override
    public String getJobName() {
        return jobName;
    }

    @Override
    public int getJobOrgId() {
        return jobOrgId;
    }

    @Override
    public long getJobId() {
        return jobId;
    }

}

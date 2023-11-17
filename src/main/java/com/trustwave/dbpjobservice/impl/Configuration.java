package com.trustwave.dbpjobservice.impl;

public class Configuration {
    // these are just defaults, normally set by Spring, see jobservice-config.xml:
    private int minActionThreads = 1;
    private int maxActionThreads = 1;
    private boolean validateArcs = true;
    private boolean resumeJobInstancesOnStartup = true;
    private int workflowExecutorBatchSize = 10;
    private int workflowExecutorQueueSize = 100;
    private boolean saveOutputInWorkflowThread = true;
    private int processCacheSize = 10;
    private int tokenCacheSize = 0;
    private boolean useLifoExecutionPolicy = false;
    private boolean useAverageTimeForPercentage = false;

    public int getMinActionThreads() {
        return minActionThreads;
    }

    public void setMinActionThreads(int minActionThreads) {
        this.minActionThreads = minActionThreads;
    }

    public int getMaxActionThreads() {
        return maxActionThreads;
    }

    public void setMaxActionThreads(int maxActionThreads) {
        this.maxActionThreads = maxActionThreads;
    }

    public boolean isValidateArcs() {
        return validateArcs;
    }

    public void setValidateArcs(boolean validateArcs) {
        this.validateArcs = validateArcs;
    }

    public boolean isResumeJobInstancesOnStartup() {
        return resumeJobInstancesOnStartup;
    }

    public void setResumeJobInstancesOnStartup(
            boolean activateJobInstancesOnStartup) {
        this.resumeJobInstancesOnStartup = activateJobInstancesOnStartup;
    }

    public int getWorkflowExecutorBatchSize() {
        return workflowExecutorBatchSize;
    }

    public void setWorkflowExecutorBatchSize(int maxWorkflowBatchSize) {
        this.workflowExecutorBatchSize = maxWorkflowBatchSize;
    }

    public int getWorkflowExecutorQueueSize() {
        return workflowExecutorQueueSize;
    }

    public void setWorkflowExecutorQueueSize(int workflowExecutorQueueSize) {
        this.workflowExecutorQueueSize = workflowExecutorQueueSize;
    }

    public int getProcessCacheSize() {
        return processCacheSize;
    }

    public void setProcessCacheSize(int processCacheSize) {
        this.processCacheSize = processCacheSize;
    }

    public int getTokenCacheSize() {
        return tokenCacheSize;
    }

    public void setTokenCacheSize(int tokenCacheSize) {
        this.tokenCacheSize = tokenCacheSize;
    }

    public boolean isUseLifoExecutionPolicy() {
        return useLifoExecutionPolicy;
    }

    public void setUseLifoExecutionPolicy(boolean useLifoExecutionPolicy) {
        this.useLifoExecutionPolicy = useLifoExecutionPolicy;
    }

    public boolean isSaveOutputInWorkflowThread() {
        return saveOutputInWorkflowThread;
    }

    public void setSaveOutputInWorkflowThread(boolean saveOutputInWorkflowThread) {
        this.saveOutputInWorkflowThread = saveOutputInWorkflowThread;
    }

    public boolean isUseAverageTimeForPercentage() {
        return useAverageTimeForPercentage;
    }

    public void setUseAverageTimeForPercentage(boolean useAverageTimeForPercentage) {
        this.useAverageTimeForPercentage = useAverageTimeForPercentage;
    }
}

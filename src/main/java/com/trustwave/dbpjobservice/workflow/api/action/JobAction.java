package com.trustwave.dbpjobservice.workflow.api.action;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.trustwave.dbpjobservice.xml.XmlAttributes;
import com.trustwave.dbpjobservice.xml.XmlEventDescriptor;
import com.trustwave.dbpjobservice.xml.XmlExitCondition;
import com.trustwave.dbpjobservice.xml.XmlNameValuePair;
import com.trustwave.dbpjobservice.xml.XmlParameter;

/**
 * This is a base class for all job actions.
 *
 * @author vlad
 */
public class JobAction implements IJobAction {
    private static Logger logger = LogManager.getLogger(JobAction.class);

    private String nodeName;
    private long nodeId;
    private long tokenId;
    private long processId;
    private XmlAttributes nodeAttributes;
    private boolean conditionGeneratingAction = true;
    private JobContext context;
    /**
     * Not null when action receives event:
     */
    private XmlEventDescriptor eventReceived;
    private Map<String, Boolean> populatedParams = new HashMap<String, Boolean>();
    private boolean timeWaiting = false;
    private long taskRecordId = 0;
    private Date beginTime = null;

    @InputParameter(internal = true, optional = true)
    private long checkCompletedInterval = DefaultCheckCompletedInterval;

    @OutputParameter()
    private ActionState state = ActionState.INITIAL;

    @OutputParameter
    private XmlExitCondition exitCondition;

    @OutputParameter
    private String errorDetails;

    @OutputParameter
    private String statusMessage;

    @OutputParameter
    private String failedAction;

    @InputParameter(internal = true, optional = true)
    @OutputParameter
    private String taskName = DEFAULT_TASK_NAME;

    @InputParameter(internal = true, optional = true)
    @OutputParameter
    private String item = DEFAULT_ITEM;
    private Exception initializationError;

    public void init(String nodeName,
            long nodeId,
            long tokenId,
            long processId,
            XmlAttributes nodeAttributes,
            JobContext context
    ) {
        this.nodeName = nodeName;
        this.nodeId = nodeId;
        this.tokenId = tokenId;
        this.processId = processId;
        this.nodeAttributes = nodeAttributes;
        this.context = context;
    }

    /**
     * <p>This method is called by Job engine immediately after populating
     * action parameters, before any 'action' method (begin(), checkCompleted,
     * or cancel()).
     * </p> Any initialization actions should be performed in this method
     * rather than in constructor; particularly those that require
     * {@link JobServiceContext} or action parameters.
     * </p>
     * <p>The method is <i>guaranteed</i> to be called, as opposed to e.g.
     * {@link #begin()}, which may or may not be called depending on
     * initial action state.
     * </p>
     */
    @Override
    public void init() {
    }

    /**
     * <p>This method is called after {@link #init()} to begin action execution.</p>
     * <p>The method will not be called if action execution was started already,
     * e.g. when process was resumed after pausing or Job Service restart.
     * </p>
     * <p>Mandatory action input parameters are <i>guaranteed</i> to be set
     * (not <code>null</code>) when this method is invoked.
     * </p>
     * <p>The method should return <code>true</code> if action execution completes
     * after calling this method; or <code>false</code> when <i>begin()</i>
     * only initiates execution, and completion should be checked later with
     * {@link #checkCompleted()} method.
     * </p>
     * <p>Default implementation always returns <code>true</code>, without doing anything.</p>
     */
    @Override
    public boolean begin() {
        return true;
    }

    /**
     * <p>This method is called periodically if:
     * <ul>
     *    <li>{@link #begin()} method returned <code>false</code></li>,
     *    <li>and {@link #checkCompletedInterval} &gt; 0</li>
     * </ul>
     *   until it returns <code>true</code>.
     *  </p>
     *  <p>
     *  </p>
     *  <p>The method should return <code>true</code> if action execution
     *  is  completed, <code>false</code> otherwise (another
     *  checkCompleted() call is required.)
     *  </p>
     *  <p>Default implementation always returns <code>true</code>, without doing anything.</p>
     */
    @Override
    public boolean checkCompleted() {
        return true;
    }

    /**
     * </p>The method is called by Job Engine when action execution should be
     * canceled.</p>
     * <p>Default implementation does not do anything.</p>
     */
    @Override
    public void cancel() {
    }

    /**
     * <p>This method is called by the Job Engine when action execution should be paused</p>
     * <p>Default implementation does not do anything.</p>
     */
    @Override
    public void pause() {
    }

    /**
     * <p>This method is called by the Job Engine when a paused action should be resumed</p>
     * <p>Default implementation does not do anything.</p>
     */
    @Override
    public void resume() {
    }

    @Override
    public boolean isWaiting() {
        return ActionState.WAITING == state
                || (ActionState.RUNNING == state && getCheckCompletedInterval() <= 0);
    }

    @Override
    public boolean isTimeWaiting() {
        return timeWaiting;
    }

    @Override
    public void setTimeWaiting(boolean timeWaiting) {
        this.timeWaiting = timeWaiting;
    }

    @Override
    public boolean isPaused() {
        return (ActionState.PAUSED == state || ActionState.PAUSED_WAITING == state);
    }

    @Override
    public String getWaitMessage() {
        return null;
    }

    @Override
    public void setCondition(XmlExitCondition cond, String errorMessage) {
        if (!isConditionGeneratingAction()) {
			if (logger.isDebugEnabled()) {
				logger.debug("skipping settting condition " + cond
						+ " on " + this + " - not a condition-generating action");
			}
            return;
        }
        if (getExitCondition() != null
                && getExitCondition() != XmlExitCondition.OK) {
			if (logger.isDebugEnabled()) {
				logger.debug("skipping settting condition " + cond
						+ " on " + this + " - condition already set");
			}
            return;
        }
        setExitCondition(cond);
        setErrorDetails(errorMessage);
        setFailedAction(getNodeName());
    }

    @Override
    public boolean hasErrors() {
        return exitCondition != null && exitCondition != XmlExitCondition.OK;
    }

    @Override
    public Map<String, String> getParameterMetadata(String parameterName) {
        Map<String, String> metadata = new HashMap<String, String>();

        for (XmlParameter p : nodeAttributes.getParameter()) {
            if (parameterName.equals(p.getInternalName())) {
                for (XmlNameValuePair nvp : p.getMetadata()) {
                    // value may be specified both as 'value' attribute and as element text:
                    String value = nvp.getValueAttribute();
                    if (value == null) {
                        value = nvp.getValue();
                    }
                    metadata.put(nvp.getName(), value);
                }
            }
        }
        return metadata;
    }

    @Override
    public final String getActionTypeName() {
        String typeName = getClass().getName();
        int ind = typeName.lastIndexOf('.');
        return typeName.substring(ind + 1);
    }

    public String toString() {
        return "Action[" + tokenId + ": " + getActionName() + "]";
    }

    @Override
    public final void markParameterPopulated(String name, boolean valuePresent) {
        populatedParams.put(name, valuePresent);
    }

    @Override
    public final boolean isParameterPopulated(String name) {
        return populatedParams.containsKey(name);
    }

    @Override
    public final boolean isParameterValuePresent(String name) {
        return populatedParams.containsKey(name) && populatedParams.get(name);
    }

    @Override
    public String getActionName() {
        return getNodeName();
    }

    @Override
    public final long getNodeId() {
        return nodeId;
    }

    @Override
    public final long getTokenId() {
        return tokenId;
    }

    @Override
    public final XmlAttributes getNodeAttributes() {
        return nodeAttributes;
    }

    @Override
    public final long getCheckCompletedInterval() {
        return checkCompletedInterval;
    }

    @Override
    public final void setCheckCompletedInterval(long checkCompletedInterval) {
        this.checkCompletedInterval = checkCompletedInterval;
    }

    @Override
    public final void setCheckCompletedIntervalIfNotSet(long interval) {
        if (checkCompletedInterval == DefaultCheckCompletedInterval) {
            this.checkCompletedInterval = interval;
        }
    }

    @Override
    public final XmlExitCondition getExitCondition() {
        return exitCondition;
    }

    @Override
    public void setExitCondition(XmlExitCondition exitCondition) {
        this.exitCondition = exitCondition;
        if (exitCondition != null && failedAction == null) {
            setFailedAction(getNodeName());
        }
    }

    @Override
    public final String getErrorDetails() {
        return errorDetails;
    }

    @Override
    public final void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    @Override
    public String getStatusMessage() {
        return statusMessage;
    }

    // this call is protected intentionally, to prevent auto-population
    // with value from previous actions
    protected void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    @Override
    public final ActionState getState() {
        return state;
    }

    @Override
    public final void setState(ActionState state) {
        this.state = state;
    }

    @Override
    public final String getFailedAction() {
        return failedAction;
    }

    @Override
    public final void setFailedAction(String failedAction) {
        this.failedAction = failedAction;
    }

    @Override
    public final boolean isConditionGeneratingAction() {
        return conditionGeneratingAction;
    }

    @Override
    public final void setConditionGeneratingAction(boolean conditionGeneratingAction) {
        this.conditionGeneratingAction = conditionGeneratingAction;
    }

    @Override
    public final String getNodeName() {
        return nodeName;
    }

    @Override
    public final long getProcessId() {
        return processId;
    }

    @Override
    public final String getJobName() {
        return getContext().getJobName();
    }

    @Override
    public int getJobOrgId() {
        return getContext().getJobOrgId();
    }

    @Override
    public final String getTaskName() {
        return taskName;
    }

    @Override
    public final void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    @Override
    public final String getItem() {
        return item;
    }

    @Override
    public final void setItem(String item) {
        this.item = item;
    }

    @Override
    public final XmlEventDescriptor getEventReceived() {
        return eventReceived;
    }

    @Override
    public final void setEventReceived(XmlEventDescriptor event) {
        this.eventReceived = event;
    }

    @Override
    public JobContext getContext() {
        return context;
    }

    public void setContext(JobContext context) {
        this.context = context;
    }

    @Override
    public long getTaskRecordId() {
        return taskRecordId;
    }

    @Override
    public void setTaskRecordId(long taskRecordId) {
        if (this.taskRecordId == 0) {
            this.taskRecordId = taskRecordId;
        }
    }

    @Override
    public Date getBeginTime() {
        return beginTime;
    }

    @Override
    public void setBeginTime(Date beginTime) {
        if (this.beginTime == null) {
            this.beginTime = beginTime;
        }
    }

    @Override
    public JobAction getOriginalAction() {
        return this;
    }

    @Override
    public void lockInMemory() {
    }

    @Override
    public void releaseLockInMemory() {
    }

    @Override
    public Exception getInitializationError() {
        return initializationError;
    }

    @Override
    public void setInitializationError(Exception ex) {
        initializationError = ex;
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (nodeId ^ (nodeId >>> 32));
        result = prime * result + (int) (processId ^ (processId >>> 32));
        result = prime * result + (int) (tokenId ^ (tokenId >>> 32));
        return result;
    }

    @Override
    public final boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
        JobAction other = (JobAction) obj;
		if (nodeId != other.nodeId) {
			return false;
		}
		if (processId != other.processId) {
			return false;
		}
		if (tokenId != other.tokenId) {
			return false;
		}
        return true;
    }
}

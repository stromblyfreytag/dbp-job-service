package com.trustwave.dbpjobservice.workflow.api.action;

import java.util.Date;
import java.util.Map;

import com.trustwave.dbpjobservice.xml.XmlAttributes;
import com.trustwave.dbpjobservice.xml.XmlEventDescriptor;
import com.trustwave.dbpjobservice.xml.XmlExitCondition;

public interface IJobAction {

    public static final String DEFAULT_TASK_NAME = "";
    public static final String DEFAULT_ITEM = "";
    public static final String PARAMETER_TASK_NAME = "taskName";
    public static final String PARAMETER_ITEM = "item";
    public static final String PARAMETER_EXIT_CONDITION = "exitCondition";
    public static final String PARAMETER_ERROR_DETAILS = "errorDetails";
    public static final String PARAMETER_STATUS_MESSAGE = "statusMessage";
    public static final String PARAMETER_FAILED_ACTION = "failedAction";
    public static final String PARAMETER_STATE = "state";
    public static final String[] STATUS_PARAMETERS = new String[]{
            PARAMETER_TASK_NAME, PARAMETER_ITEM, PARAMETER_EXIT_CONDITION,
            PARAMETER_ERROR_DETAILS, PARAMETER_STATUS_MESSAGE,
            PARAMETER_FAILED_ACTION};
    public static final long DefaultCheckCompletedInterval = 20 * 1000 + 100;

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
    public void init();

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
    public boolean begin();

    /**
     * <p>This method is called periodically if:
     * <ul>
     *    <li>begin() method returned <code>false</code></li>,
     *    <li>and checkCompletedInterval &gt; 0</li>
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
    public boolean checkCompleted();

    /**
     * </p>The method is called by Job Engine when action execution should be
     * canceled.</p>
     * <p>Default implementation does not do anything.</p>
     */
    public void cancel();

    /**
     * <p>This method is called by the Job Engine when action execution should be paused</p>
     * <p>Default implementation does not do anything.</p>
     */
    public void pause();

    /**
     * <p>This method is called by the Job Engine when a paused action should be resumed</p>
     * <p>Default implementation does not do anything.</p>
     */
    public void resume();

    public boolean isWaiting();

    public boolean isTimeWaiting();

    public void setTimeWaiting(boolean timeWaiting);

    public boolean isPaused();

    public String getWaitMessage();

    public void setCondition(XmlExitCondition cond, String errorMessage);

    public boolean hasErrors();

    public Map<String, String> getParameterMetadata(String parameterName);

    public String getActionTypeName();

    public void markParameterPopulated(String name, boolean valuePresent);

    public boolean isParameterPopulated(String name);

    public boolean isParameterValuePresent(String name);

    public String getActionName();

    public long getNodeId();

    public long getTokenId();

    public XmlAttributes getNodeAttributes();

    public long getCheckCompletedInterval();

    public void setCheckCompletedInterval(long checkCompletedInterval);

    public void setCheckCompletedIntervalIfNotSet(long interval);

    public XmlExitCondition getExitCondition();

    public void setExitCondition(XmlExitCondition exitCondition);

    public String getErrorDetails();

    public void setErrorDetails(String errorDetails);

    public String getStatusMessage();

    public ActionState getState();

    public void setState(ActionState state);

    public String getFailedAction();

    public void setFailedAction(String failedAction);

    public boolean isConditionGeneratingAction();

    public void setConditionGeneratingAction(boolean conditionGeneratingAction);

    public String getNodeName();

    public long getProcessId();

    public String getJobName();

    public int getJobOrgId();

    public String getTaskName();

    public void setTaskName(String taskName);

    public String getItem();

    public void setItem(String item);

    public XmlEventDescriptor getEventReceived();

    public void setEventReceived(XmlEventDescriptor event);

    public JobContext getContext();

    public long getTaskRecordId();

    public void setTaskRecordId(long taskRecordId);

    public Date getBeginTime();

    public void setBeginTime(Date beginTime);

    /**
     * Original (not wrapped) action implementation object, used for retrieving
     * action parameters by reflection
     */
    public JobAction getOriginalAction();

    public void lockInMemory();

    public void releaseLockInMemory();

    /**
     * Indicates error occurred during action creation or restoring.
     * If action has creation error, failing is the only safe thing
     * that can be done with action.
     */
    public Exception getInitializationError();

    public void setInitializationError(Exception ex);

}
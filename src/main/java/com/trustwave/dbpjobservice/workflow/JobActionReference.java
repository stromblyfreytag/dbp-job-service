package com.trustwave.dbpjobservice.workflow;

import java.lang.ref.SoftReference;
import java.util.Date;
import java.util.Map;

import com.trustwave.dbpjobservice.workflow.api.action.ActionState;
import com.trustwave.dbpjobservice.workflow.api.action.IJobAction;
import com.trustwave.dbpjobservice.workflow.api.action.JobAction;
import com.trustwave.dbpjobservice.workflow.api.action.JobContext;
import com.trustwave.dbpjobservice.xml.XmlAttributes;
import com.trustwave.dbpjobservice.xml.XmlEventDescriptor;
import com.trustwave.dbpjobservice.xml.XmlExitCondition;

public class JobActionReference implements IJobAction
{
	private SoftReference<IJobAction> softRef;
	private long tokenId;
	private long processId;
	private volatile IJobAction hardRef; 

	public JobActionReference( IJobAction action )
	{
		this.softRef = new SoftReference<IJobAction>( action );
		this.tokenId = action.getTokenId();
		this.processId = action.getProcessId();
		this.hardRef = null;
	}
	
	public IJobAction getAction()
	{
		IJobAction action = hardRef != null? hardRef: softRef.get();
		if (action == null) {
			action = ActionFactory.getInstance().createAction( tokenId, processId );
			softRef = new SoftReference<IJobAction>( action );
		}
		return action;
	}
	
	@Override
	public void lockInMemory() 
	{
		hardRef = getAction();
	}

	@Override
	public void releaseLockInMemory() 
	{
		hardRef = null;
	}


	@Override
	public void init() {
		getAction().init();
	}

	@Override
	public boolean begin() {
		return getAction().begin();
	}

	@Override
	public boolean checkCompleted() {
		return getAction().checkCompleted();
	}

	@Override
	public void cancel() {
		getAction().cancel();
	}

	@Override
	public void pause() {
		getAction().pause();
	}

	@Override
	public void resume() {
		getAction().resume();
	}

	@Override
	public boolean isWaiting() {
		return getAction().isWaiting();
	}

	@Override
	public boolean isTimeWaiting() {
		return getAction().isTimeWaiting();
	}

	@Override
	public void setTimeWaiting(boolean timeWaiting) {
		getAction().setTimeWaiting(timeWaiting);
	}

	@Override
	public boolean isPaused() {
		return getAction().isPaused();
	}

	@Override
	public String getWaitMessage() {
		return getAction().getWaitMessage();
	}

	@Override
	public void setCondition(XmlExitCondition cond, String errorMessage) {
		getAction().setCondition(cond, errorMessage);
	}

	@Override
	public boolean hasErrors() {
		return getAction().hasErrors();
	}

	@Override
	public Map<String, String> getParameterMetadata(String parameterName) {
		return getAction().getParameterMetadata(parameterName);
	}

	@Override
	public String getActionTypeName() {
		return getAction().getActionTypeName();
	}

	@Override
	public void markParameterPopulated(String name, boolean valuePresent) {
		getAction().markParameterPopulated(name, valuePresent);
	}

	@Override
	public boolean isParameterPopulated(String name) {
		return getAction().isParameterPopulated(name);
	}

	@Override
	public boolean isParameterValuePresent(String name) {
		return getAction().isParameterValuePresent(name);
	}

	@Override
	public String getActionName() {
		return getAction().getActionName();
	}

	@Override
	public long getNodeId() {
		return getAction().getNodeId();
	}

	@Override
	public XmlAttributes getNodeAttributes() {
		return getAction().getNodeAttributes();
	}

	@Override
	public long getCheckCompletedInterval() {
		return getAction().getCheckCompletedInterval();
	}

	@Override
	public void setCheckCompletedInterval(long checkCompletedInterval) {
		getAction().setCheckCompletedInterval(checkCompletedInterval);
	}

	@Override
	public void setCheckCompletedIntervalIfNotSet(long interval) {
		getAction().setCheckCompletedIntervalIfNotSet(interval);
	}

	@Override
	public XmlExitCondition getExitCondition() {
		return getAction().getExitCondition();
	}

	@Override
	public void setExitCondition(XmlExitCondition exitCondition) {
		getAction().setExitCondition(exitCondition);
	}

	@Override
	public String getErrorDetails() {
		return getAction().getErrorDetails();
	}

	@Override
	public void setErrorDetails(String errorDetails) {
		getAction().setErrorDetails(errorDetails);
	}

	@Override
	public String getStatusMessage() {
		return getAction().getStatusMessage();
	}

	@Override
	public ActionState getState() {
		return getAction().getState();
	}

	@Override
	public void setState(ActionState state) {
		getAction().setState(state);
	}

	@Override
	public String getFailedAction() {
		return getAction().getFailedAction();
	}

	@Override
	public void setFailedAction(String failedAction) {
		getAction().setFailedAction(failedAction);
	}

	@Override
	public boolean isConditionGeneratingAction() {
		return getAction().isConditionGeneratingAction();
	}

	@Override
	public void setConditionGeneratingAction(boolean conditionGeneratingAction) {
		getAction().setConditionGeneratingAction(conditionGeneratingAction);
	}

	@Override
	public String getNodeName() {
		return getAction().getNodeName();
	}

	@Override
	public long getProcessId() {
		return processId;
	}

	@Override
	public String getJobName() {
		return getAction().getJobName();
	}

	@Override
	public int getJobOrgId() {
		return getAction().getJobOrgId();
	}

	@Override
	public String getTaskName() {
		return getAction().getTaskName();
	}

	@Override
	public void setTaskName(String taskName) {
		getAction().setTaskName(taskName);
	}

	@Override
	public String getItem() {
		return getAction().getItem();
	}

	@Override
	public void setItem(String item) {
		getAction().setItem(item);
	}

	@Override
	public XmlEventDescriptor getEventReceived() {
		return getAction().getEventReceived();
	}

	@Override
	public void setEventReceived(XmlEventDescriptor event) {
		getAction().setEventReceived(event);
	}

	@Override
	public JobContext getContext() {
		return getAction().getContext();
	}
	public long getTaskRecordId() {
		return getAction().getTaskRecordId();
	}

	public void setTaskRecordId(long taskRecordId) {
		getAction().setTaskRecordId(taskRecordId);
	}

	public Date getBeginTime() {
		return getAction().getBeginTime();
	}

	public void setBeginTime(Date beginTime) {
		getAction().setBeginTime(beginTime);
	}


	@Override
	public JobAction getOriginalAction() {
		return getAction().getOriginalAction();
	}

	@Override
	public Exception getInitializationError() {
		return getAction().getInitializationError();
	}

	@Override
	public void setInitializationError(Exception ex) {
		getAction().setInitializationError( ex );
	}
	
	public long getTokenId() 
	{
		return tokenId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (tokenId ^ (tokenId >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JobActionReference other = (JobActionReference) obj;
		if (tokenId != other.tokenId)
			return false;
		return true;
	}

	@Override
	public String toString() 
	{
		IJobAction action = hardRef != null? hardRef: softRef.get();
		return "Ref[" + (action != null? action.toString(): "tokenId=" + tokenId) + "]";
	}
}

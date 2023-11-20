package com.trustwave.dbpjobservice.interfaces;

public class ResumeAction 
{
	/** Name of the resume button re.g. 'Rerun Checks' */
	private String buttonLabel;

	/** Name of the associated event, e.g. 'RerunChecksRequested' */
	private String eventName;
	
	/** description of the action. e.g. 'Rerun all checks with new credentials' */
	private String description;

	public String getButtonLabel() {
		return buttonLabel;
	}
	public void setButtonLabel(String buttonLabel) {
		this.buttonLabel = buttonLabel;
	}
	public String getEventName() {
		return eventName;
	}
	public void setEventName(String eventName) {
		this.eventName = eventName;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
}

package com.trustwave.dbpjobservice.actions;

import java.util.ArrayList;
import java.util.List;

import com.trustwave.dbpjobservice.impl.Messages;
import com.trustwave.dbpjobservice.workflow.api.action.InputParameter;
import com.trustwave.dbpjobservice.workflow.api.action.JobAction;
import com.trustwave.dbpjobservice.workflow.api.action.OutputParameter;

public class ValidationAction extends JobAction
{
	private static final String LINE_BREAK = "<br/>";
	
	@InputParameter( internal=true )
	private List<ValidationDescriptor> validationList;
	
	@OutputParameter
	private List<String> validationErrors = new ArrayList<String>();
	
	public boolean begin()
	{
		List<String> unformattedErrors = new ArrayList<String>();
		// first pass - detect errors, output (into process environment)
		for (ValidationDescriptor vd: validationList) {
			if (!vd.validate( getContext() )) {
				unformattedErrors.addAll(vd.getErrors());
				validationErrors.addAll( vd.getFormattedErrors() );
			}
		}
		ValidationDescriptor.cleanRequiredErrorsForGenericParams( validationList );
		setStatusMessage(generateStatusMessage(unformattedErrors));
		
		// second pass - fail on errors preventing execution:
		for (ValidationDescriptor vd: validationList) {
			if (vd.hasErrors() && vd.isPreventsExecution()) {
				throw new RuntimeException( vd.getErrors().get(0) );
			}
		}
		
		return true;
	}

	public List<ValidationDescriptor> getValidationList() {
		return validationList;
	}

	public void setValidationList(List<ValidationDescriptor> validationList) {
		this.validationList = validationList;
	}

	public List<String> getValidationErrors() {
		return validationErrors;
	}
	
	private String generateStatusMessage(List<String> errorMessages) {
		StringBuilder statusMessage = new StringBuilder();
		if (errorMessages != null && errorMessages.size() > 0) {
			statusMessage.append(Messages.getString("job.validation.error.summary", errorMessages.size()));
			for (String errorMessage : errorMessages) {
				statusMessage.append(LINE_BREAK);
				statusMessage.append("- ");
				statusMessage.append(errorMessage);
			}
		}
		return statusMessage.toString();
	}
}

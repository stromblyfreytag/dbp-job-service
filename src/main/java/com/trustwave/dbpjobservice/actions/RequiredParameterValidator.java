package com.trustwave.dbpjobservice.actions;

import java.util.Collection;
import java.util.List;

import com.trustwave.dbpjobservice.impl.Messages;
import com.trustwave.dbpjobservice.workflow.api.action.JobContext;
import com.trustwave.dbpjobservice.workflow.api.action.NullValueSafe;
import com.trustwave.dbpjobservice.workflow.api.action.ParameterValidator;
import com.trustwave.dbpjobservice.xml.XmlValidator;

/**
 * Parameter validator that checks the parameter is not null / not empty.
 */
@NullValueSafe
public class RequiredParameterValidator implements ParameterValidator
{
	public static XmlValidator createXmlValidator()
	{
		XmlValidator xv = new XmlValidator();
		xv.setClazz( RequiredParameterValidator.class.getName() );
		return xv;
	}
	
	@Override
	public void init(JobContext context) {
	}

	@Override
	public void validate( Object paramValue, String paramName, 
			              List<String> errors )
	{
		if (valueIsEmpty( paramValue )) {
			errors.add(Messages.getString("validation.parameter.missing", paramName));
		}
	}

	private boolean valueIsEmpty( Object value )
	{
		if (value == null) {
			return true;
		}
		if (value instanceof Collection<?>) {
			return  ((Collection<?>)value).isEmpty();
		}
		if (value instanceof String) {
			return (((String)value).trim()).isEmpty();
		}
		return false;
	}
}

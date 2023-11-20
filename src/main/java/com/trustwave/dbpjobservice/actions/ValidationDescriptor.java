package com.trustwave.dbpjobservice.actions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.trustwave.dbpjobservice.parameters.ParameterValidatorCreator;
import com.trustwave.dbpjobservice.workflow.api.action.JobContext;
import com.trustwave.dbpjobservice.workflow.api.action.NullValueSafe;
import com.trustwave.dbpjobservice.workflow.api.action.ParameterValidator;
import com.trustwave.dbpjobservice.xml.XmlValidator;

public class ValidationDescriptor implements Serializable
{
	private static final long serialVersionUID = 1L;
	private static Logger logger = LogManager.getLogger( ValidationDescriptor.class );
	
	private XmlValidator xv;
	private String       parameterName;
	private Object       parameterValue;
	private boolean      nullValueSafe = false;
	private String       genericParameterName;
	private ParameterValidator validator;
	private List<String> errors = new ArrayList<String>();
	
	public ValidationDescriptor( XmlValidator xmlValidator,
			                     String parameterName,
			                     Object parameterValue,
			                     String genericParameterName)
	{
		this.xv = xmlValidator;
		this.parameterName = parameterName;
		this.parameterValue = parameterValue;
		this.genericParameterName = genericParameterName;
	}
	
	public ValidationDescriptor( ValidationDescriptor other )
	{
		this.xv                   = other.xv;
		this.parameterName        = other.parameterName;
		this.parameterValue       = other.parameterValue;
		this.nullValueSafe        = other.nullValueSafe;
		this.genericParameterName = other.genericParameterName;
	}
	
	public String getParameterName() {
		return parameterName;
	}
	public Object getParameterValue() {
		return parameterValue;
	}
	public boolean isPreventsExecution() {
		return xv.isPreventsExecution();
	}
	public String getGenericParameterName() {
		return genericParameterName;
	}

	public ParameterValidator getValidator() {
		if (validator == null) {
			validator =	ParameterValidatorCreator.createValidator( xv );
			if (validator.getClass().getAnnotation(NullValueSafe.class) != null) {
				nullValueSafe = true;
			}
		}
		return validator;
	}
	
	public List<String> getErrors()
	{
		return errors;
	}
	
	public boolean hasErrors()
	{
		return errors.size() > 0;
	}
	
	public List<String> getFormattedErrors()
	{
		ArrayList<String> formatted = new ArrayList<String>();
		if (xv.isPreventsExecution()) {
			formatted.addAll( errors );
		}
		else {
			// non-job-failing, present as warnings:
			for (String e: errors) {
				formatted.add( "WARNING: " + e );
			}
		}
		return formatted;
	}
	
	public boolean validate( JobContext context )
	{
		return validate( context, parameterValue );
	}

	// TODO: this is method is not thread-safe; 
	// errors should be passed as a parameter instead of using internal field
	public boolean validate( JobContext context, Object value )
	{
		errors.clear();
		getValidator();
		if (valueIsEmpty(value) && !nullValueSafe) {
			logger.debug( this + ": empty value, skipping" );
		}
		else {
			doValidate( context, value );
			logger.debug( this + ": validation finished, errors=" + errors );
		}
		return errors.size() == 0;
	}

	private void doValidate( JobContext context, Object value )
	{
		try {
			ParameterValidator validator = getValidator(); 
			validator.init( context );
			validator.validate( value, parameterName, errors );
		}
		catch (Exception e) {
			logger.info( "Error validating " + this + ": " + e, e );
			errors.add( e.toString() );
		}
	}
	
	// This call will look for validation descriptors with the same
	// generic parameter name (i.e. alternative representations of
	// logically the same parameter) and clean errors on all such
	// descriptors if there is at least one without errors.
	// Generic names are set only with RequredParameter validators,
	// so if one parameter passed validation (present), all other
	// parameters with the same generic name should be ignored.
	public static void cleanRequiredErrorsForGenericParams( List<ValidationDescriptor> validationList )
	{
		HashSet<String> validGenericNames = new HashSet<String>();
		
		for (ValidationDescriptor vd: validationList) {
			String gname = vd.getGenericParameterName();
			if (!vd.hasErrors() && gname != null) {
				validGenericNames.add( gname );
			}
		}
		
		for (ValidationDescriptor vd: validationList) {
			String gname = vd.getGenericParameterName();
			if (gname != null && validGenericNames.contains(gname)) {
				if (vd.hasErrors()) {
					logger.debug( vd + ": cleaning errors" );
					vd.errors.clear();
				}
			}
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
	
	@Override
	public String toString() 
	{
		return "Validator(param=" + parameterName
				+ ", validator=" + xv.getClazz()
				+ (xv.getProperty() != null? "," + xv.getProperty(): "")
				+ (xv.isPreventsExecution()? ", failing": "")
				+ ")";
	}
}

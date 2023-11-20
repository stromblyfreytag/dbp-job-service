package com.trustwave.dbpjobservice.parameters;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.trustwave.dbpjobservice.impl.Messages;
import com.trustwave.dbpjobservice.interfaces.ExternalParameterDefinition;
import com.trustwave.dbpjobservice.interfaces.ExternalParameterValue;
import com.trustwave.dbpjobservice.interfaces.KeyValue;
import com.trustwave.dbpjobservice.workflow.api.action.ValueConverter;
import com.trustwave.dbpjobservice.workflow.api.action.valueconverters.ValueConverterFactory;
import com.trustwave.dbpjobservice.xml.XmlNameValuePair;
import com.trustwave.dbpjobservice.xml.XmlOutputEnv;
import com.trustwave.dbpjobservice.xml.XmlSelector;
import com.trustwave.dbpjobservice.xml.XmlValidator;

public class ParameterDescriptor
{
	// (internal) name of parameter == property name in action
	//                              == internalName in <parameter> or <output>
	private String   name;         
	
	// input name in token - same as name unless changed with <parameter> element
	private String   inputName;     
	
	// output name in token - same as name unless changed with <output> element
	private String   outputName;   // output name in token
	
	// class of parameter - as declared in action class
	private Class<?> valueType;
	
	// class of list element
	private Class<?> elementValueType;
	
	// class of action that defines parameter 
	private Class<?> actionType;
	
	// action getter method for parameter 
	private Method   getter;
	
	// action setter method for parameter 
	private Method   setter;
	
	// name used in UI 
	private String   displayName;
	
	// name that marks several alternative input parameters;
	// only one of parameters with the same generic name should be set.
	// e.g. alternative parameters 'assetTag' and assetIdList may
	// have the same generic name 'assetSet'
	private String   genericName;
	
	// description (for UI)
	private String   description;
	
	// default value
	private String   defaultValue;
	
	// class that validates parameter value
	private List<XmlValidator> validators =	new ArrayList<XmlValidator>();
	
	// true if parameter is optional
	private boolean  optional;
	
	// true if this is input parameter (marked with @InputParameter annotation)
	private boolean  inputParameter;
	
	// true if this is output parameter (marked with @OutputParameter annotation)
	private boolean  outputParameter;
	
	// true if value for input parameters must be present in token
	// (set with xml <parameter> 'valuePresent' attribute)
	private boolean  valuePresent;
	
	// expression provided with xml <parameter> 'value' attribute 
	private String   valueExpression;
	
	// true if value expression overrides value from token (default)
	// Set by <parameter> 'override' attribute, default is true
	private boolean  overrideTokenValue = true;
	
	// if errors in expression should be ignored  
	// Set by <parameter> 'ignoreExpressionErrors' attribute, default is false
	private boolean ignoreExpressionErrors = true;

	// expression provided with xml <output> 'value' attribute
	// Will be used as output value if action does not set parameter
	// or overrideActionOutputValue is true (see below)
	private String  outputValueExpression;
	
	// true if expression provided with xml <output> 'value' attribute
	// should override value set by action. 
	// Set by xml <output> 'override' property 
	private boolean overrideActionOutputValue;
	
	// if true, output parameter will be removed from token
	// Set by xml <output> 'clearValue' property, default is false. 
 	private boolean clearOutputValue;
 	
 	// target environment for output parameter
 	private XmlOutputEnv outputEnv;
	
	// true if parameter value is transient (not persisted in db) 
	private boolean  Transient;
	
	// Value to/from string converter for parameter
	private ValueConverter valueConverter;
	
	// token set name for 'splitting' action parameter.
	// Parameter must be a List.
	private String   tokenSet;
	
	// List or Map input parameter used for collecting info from
	// parent tokens in join node.
	// Set by join=true property of @InputParameter annotation 
	private String   joinType;
	
	// Name of parameter used as a key in Map join parameter
	// E.g. 'assetId' for joining different asset nodes. Default - item
	private String   joinDiscriminator = "item";
	
	// If parameter class is not externalizable (i.e. not a simple type or list),
    // presence of externalizableAsString attribute makes parameter externalizable
	// as a STRING type; parameter's value converter is used for externalization.
	private boolean externalizableAsString = false;
	
	// descriptor of the possible values selector
	private XmlSelector selector;
	
	private List<KeyValue> metadata = new ArrayList<KeyValue>();
	
	private boolean visible = true;


	public ParameterDescriptor() {
	}
	
	public ParameterDescriptor(String name, Class<?> valueClass, Class<?> actionType, boolean input ) 
	{
		this.setName( name );
		this.setInputName( name );
		this.setOutputName( name );
		this.setValueType( valueClass );
		this.setDisplayName( name );
		this.setActionType( actionType );
		if (input)
			setInputParameter( true );
		else
			setOutputParameter( true );
		
		resetConverter();
	}
	
	void resetConverter()
	{
		ValueConverter converter = ValueConverterFactory.getInstance()
				                  .getConverterFor( valueType, elementValueType ); 
		if (converter == null) {
			throw new RuntimeException( Messages.getString("param.converter.noValue", this, actionType.getName()) );
		}
		setValueConverter( converter );
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getInputName() {
		return inputName;
	}
	public void setInputName(String inputName) {
		this.inputName = inputName;
	}
	public String getOutputName() {
		return outputName;
	}
	public void setOutputName(String outputName) {
		this.outputName = outputName;
	}
	public Class<?> getValueType() {
		return valueType;
	}
	public void setValueType(Class<?> valueType) {
		this.valueType = valueType;
	}
	public Class<?> getElementValueType() {
		return elementValueType;
	}
	public void setElementValueType(Class<?> elementValueType) {
		this.elementValueType = elementValueType;
	}
	public Class<?> getActionType() {
		return actionType;
	}
	public void setActionType(Class<?> actionType) {
		this.actionType = actionType;
	}
	public Method getGetter() {
		return getter;
	}
	public void setGetter(Method getter) {
		this.getter = getter;
	}
	public Method getSetter() {
		return setter;
	}
	public void setSetter(Method setter) {
		this.setter = setter;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public String getGenericName() {
		return genericName;
	}
	public void setGenericName(String genericName) {
		this.genericName = genericName;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDefaultValue() {
		return defaultValue;
	}
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
	public List<XmlValidator> getValidators() {
		return validators;
	}
	public boolean isOptional() {
		return optional;
	}
	public void setOptional(boolean optional) {
		this.optional = optional;
	}
	public boolean isInputParameter() {
		return inputParameter;
	}
	public void setInputParameter(boolean inputParameter) {
		this.inputParameter = inputParameter;
	}
	public boolean isOutputParameter() {
		return outputParameter;
	}
	public void setOutputParameter(boolean outputParameter) {
		this.outputParameter = outputParameter;
	}
	public boolean isValuePresent() {
		return valuePresent;
	}
	public void setValuePresent(boolean valuePresent) {
		this.valuePresent = valuePresent;
	}
	public String getValueExpression() {
		return valueExpression;
	}
	public void setValueExpression(String valueExpression) {
		this.valueExpression = valueExpression;
	}
	public boolean isOverrideTokenValue() {
		return overrideTokenValue;
	}
	public void setOverrideTokenValue(boolean overrideTokenValue) {
		this.overrideTokenValue = overrideTokenValue;
	}
	public boolean isIgnoreExpressionErrors() {
		return ignoreExpressionErrors;
	}
	public void setIgnoreExpressionErrors(boolean ignoreExpressionErrors) {
		this.ignoreExpressionErrors = ignoreExpressionErrors;
	}
	public String getOutputValueExpression() {
		return outputValueExpression;
	}
	public void setOutputValueExpression(String outputValueExpression) {
		this.outputValueExpression = outputValueExpression;
	}
	public boolean isOverrideActionOutputValue() {
		return overrideActionOutputValue;
	}
	public void setOverrideActionOutputValue(boolean overrideActionOutputValue) {
		this.overrideActionOutputValue = overrideActionOutputValue;
	}
	public boolean isClearOutputValue() {
		return clearOutputValue;
	}
	public void setClearOutputValue(boolean clearOutputValue) {
		this.clearOutputValue = clearOutputValue;
	}
	public XmlOutputEnv getOutputEnv() {
		return outputEnv;
	}
	public void setOutputEnv(XmlOutputEnv outputEnv) {
		this.outputEnv = outputEnv;
	}
	public boolean isTransient() {
		return Transient;
	}
	public void setTransient(boolean transient1) {
		Transient = transient1;
	}
	public ValueConverter getValueConverter() {
		return valueConverter;
	}
	public void setValueConverter(ValueConverter valueConverter) {
		this.valueConverter = valueConverter;
	}
	public String getTokenSet() {
		return tokenSet;
	}
	public void setTokenSet(String tokenSet) {
		this.tokenSet = tokenSet;
	}
	public String getJoinType() {
		return joinType;
	}
	public void setJoinType(String joinType) {
		this.joinType = joinType;
	}
	public String getJoinDiscriminator() {
		return joinDiscriminator;
	}
	public void setJoinDiscriminator(String joinDiscriminator) {
		this.joinDiscriminator = joinDiscriminator;
	}
	public boolean isExternalizableAsString() {
		return externalizableAsString;
	}
	public void setExternalizableAsString(boolean externalizableAsString) {
		this.externalizableAsString = externalizableAsString;
	}
	public XmlSelector getSelector() {
		return selector;
	}
	public void setSelector(XmlSelector selector) {
		this.selector = selector;
	}
	public List<KeyValue> getMetadata() {
		return metadata;
	}
	public boolean isVisible() {
		return visible;
	}
	public void setVisible(boolean hidden) {
		this.visible = hidden;
	}

	public void addMetadataFromXml( List<XmlNameValuePair> xmlMetadata )
	{
		for (XmlNameValuePair nvp: xmlMetadata) {
			// value may be specified both as 'value' attr and as element text:
			String value = nvp.getValueAttribute();
			if (value == null) {
				value = nvp.getValue();
			}
			metadata.add( new KeyValue( nvp.getName(), value ) );
		}
	}

	public boolean isFreeParameter()
	{
		return isInputParameter()
		    && !isValuePresent()
		    && getValueExpression() == null
		    && getJoinType() == null;
	}

	public String toShortString()
	{
		return "[\"" + getName() + "\", " + getValueType().getName() + "]" ;
	}
	
	public String toString()
	{
		return toShortString();
	}
	
	/** Convenience method for getValueConverter().objectToString().
	 *  Takes care about null object (returns null) and converter exceptions
	 *  (re-throws as {@link ParameterException}).
	 * @param obj Object to convert to string. Should have parameter's value class.
	 * @return String representation of object or null if object was null.
	 * @throws ParameterException when conversion error occurs.
	 */
	public String objectToString( Object obj )
	{
		String str = null;
		if (obj != null) {
			try {
				str = getValueConverter().objectToString( obj );
			} 
			catch (Exception e) {
				throw new ParameterException( e.getMessage(), e );
			}
		}
		return str;
	}
	
	/** Convenience method for getValueConverter().stringToObject().
	 *  Takes care about null or empty strings (returns null)
	 *  and converter exceptions  (re-throws as {@link ParameterException}).
	 * @param str String to convert into object.
	 * @return Object represented by the string or null if string was null.
	 * @throws ParameterException when conversion error occurs.
	 */
	public Object stringToObject( String str )
	{
		Object obj = null;
        // empty strings should not be passed to converters
        // - generally  they are not ready for that.
		if (str != null && str.length() > 0) {
			try {
				obj = getValueConverter().stringToObject( str, getValueType() );
			} 
			catch (Exception e) {
				throw new ParameterException( e.getMessage(), e);
			}
		}
		return obj;
	}
	
	public boolean valueIsEmpty( Object value )
	{
		if (value == null) {
			return true;
		}
		if (value instanceof Collection<?>
		 && Collection.class.isAssignableFrom(valueType)) {
			return  ((Collection<?>)value).isEmpty();
		}
		if (value instanceof String) {
			return (((String)value).trim()).isEmpty();
		}
		return false;
	}
	
	private ParameterExternalizer externalizer = null;
	
	/** <p>Get parameter's externalizer - object that converts parameter value
	 * to i's external presentation ({@link ExternalParameterValue}) and back.</p>
	 * 
	 * @return {@link ParameterExternalizer} object for parameter.
	 */
	public ParameterExternalizer getExternalizer() 
	{
		if (externalizer == null) {
			externalizer =
				new ParameterExternalizerFactory().createExternalizer( this );
			// externalizer cannot be null since we use json for externalization.
		}
		return externalizer;
	}
	
	/** <p>Converts job parameter value string (as saved in {@link JobRecord})
	 * to its external representation ({@link ExternalParameterValue}).</p>
	 * <p>If sting is null or empty (value not specified), uses default value
	 * if it is present (specified in workflow.xml with 'defaultValue'
	 *  attribute).</p>
	 *   
	 * @param strValue job parameter value string
	 * @return ParameterValue object created from the string
	 * @throws ParameterException when conversion error occurs. 
	 */
	public ExternalParameterValue stringToExternalParameterValue( String strValue )
	{
		Object obj = stringToObject( strValue );
        // if value was not specified, use default value 
		if (obj == null && getDefaultValue() != null) {
			obj = stringToObject( getDefaultValue() );
		}
		ExternalParameterValue p = getExternalizer().objectToExternalValue( obj );
		return p;
	}
	
	/** Converts job parameter value to a string.  
	 * @param p @{link ParameterValue} object that should be converted to string.
	 * @throws ParameterException when conversion error occurs. 
	 */
	public String externalParameterValueToString( ExternalParameterValue p )
	{
		Object obj = getExternalizer().externalValueToObject( p );
		String str = objectToString( obj );
		return str;
	}
	
	public ExternalParameterDefinition getExternalDefinition()
	{
		return getExternalizer().getExternalDefinition();
	}
	
	public boolean isExternallyCompatibleWith( ParameterDescriptor other )
	{
		// TODO - compare selectors
		return getExternalDefinition().isCompatibleWith( other.getExternalDefinition() );
	}
	
	public boolean isOutputToProcessEnvironment()
	{
		return XmlOutputEnv.PROCESS.equals( outputEnv ); 
	}

    // name to use in the external parameter definition:
    // output name (specified in the '<output>' element) - for environment variables,
    // input name  - for everything else (e.g. job parameters) 
	public String getExternalName()
	{
		if (isOutputToProcessEnvironment()) {
			return outputName;
		}
		return inputName;
	}
}

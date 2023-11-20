package com.trustwave.dbpjobservice.interfaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

import com.trustwave.dbpjobservice.workflow.api.action.valueconverters.ValueConverterFactory;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ExternalParameterDefinition 
{   
	/** Name of the parameter as specified in workflow.xml or task class. */
	private String           name;        // parameter name, e.g.assetTag
	
	/** Parameter label to display in UI for parameter identification 
	 */
	private String           displayName;
	
	/** Verbose parameter description */
	private String           description;
	
	/** <p>Generic parameter name. Parameters with the same generic name
	 *  are considered as alternative representations of logically the same
	 *  parameter.
	 *  </p>
	 *  <p>Only one of the parameters with the same generic names should be set
	 *  in UI.
	 *  </p>
	 *  <p>For example, parameters <code>assetTag</code>, <code>assetIdList</code>,
	 *  and <code>assetSearchExpression</code> are alternative representations
	 *  of the same entity - set of assets to work with; they may have the same
	 *  generic name <code>assetSet</code>.
	 */
	private String           genericName;
	
	/** Type of parameter value, e.g. STRING.
	 *  For list parameters - type of list elements */
	private ValueType        valueType;
	
	/** True if parameter value is a list. */
	private boolean          list;
	
	/** True if parameter is optional. */
	private boolean          required;
	
	/** Default parameter value, may be null */
	private ExternalParameterValue defaultValue;
	
	/** class name of the parameter. Guaranteed to be set for ValuType.OBJJECT */
	private String className; 
	
	/** Arbitrary metadata (UI hints), e.g.
	 * icon=... widget=checkbox, knownType=assetIdList, group=assets, ... */
	private List<KeyValue> metadata = new ArrayList<KeyValue>();    
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String label) {
		this.displayName = label;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getGenericName() {
		return genericName;
	}
	public void setGenericName(String genericName) {
		this.genericName = genericName;
	}
	public ValueType getValueType() {
		return valueType;
	}
	public void setValueType(ValueType valueType) {
		this.valueType = valueType;
	}
	public boolean isList() {
		return list;
	}
	public void setList(boolean list) {
		this.list = list;
	}
	public boolean isRequired() {
		return required;
	}
	public void setRequired(boolean required) {
		this.required = required;
	}
	public ExternalParameterValue getDefaultValue() {
		return defaultValue;
	}
	public void setDefaultValue(ExternalParameterValue defaultValue) {
		this.defaultValue = defaultValue;
	}
	public List<KeyValue> getMetadata() {
		return metadata;
	}
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	
	public String getMetadataValue( String key )
	{
		for (KeyValue kv: metadata) {
			if (key.equals( kv.getKey() ))
				return kv.getValue();
		}
		return null;
	}
	
	public boolean isCompatibleWith( ExternalParameterDefinition other )
	{
		if (valueType != other.valueType)
			return false;
		if (list != other.list)
			return false;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		return true;
	}

	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("ParameterDef(name=");
		sb.append( getName() );
		if (getGenericName() != null) {
			sb.append( ", genericName=" )
			  .append( getGenericName() );
		}
		sb.append( ", type=" ).append( getValueType() );
		if (getDefaultValue() != null) {
			String value = 
				ValueConverterFactory.objectShortString( getDefaultValue() );
			sb.append( ", value=" ).append( value );
		}
		if (getMetadata().size() > 0) {
			sb.append( ", metadata=" ).append( getMetadata() );
		}
		sb.append( ")" );
		return sb.toString();
	}
}

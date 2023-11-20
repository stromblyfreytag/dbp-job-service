package com.trustwave.dbpjobservice.parameters;

import java.util.ArrayList;
import java.util.List;


import com.trustwave.dbpjobservice.impl.Messages;
import com.trustwave.dbpjobservice.interfaces.KeyValue;
import com.trustwave.dbpjobservice.workflow.api.action.JobContext;
import com.trustwave.dbpjobservice.workflow.api.action.KeyValuePair;
import com.trustwave.dbpjobservice.workflow.api.action.PossibleParameterValuesSelector;
import com.trustwave.dbpjobservice.workflow.api.action.ValueConverter;
import com.trustwave.dbpjobservice.workflow.api.action.valueconverters.ValueConverterFactory;
import com.trustwave.dbpjobservice.workflow.api.util.Bean;
import com.trustwave.dbpjobservice.xml.XmlNameValuePair;
import com.trustwave.dbpjobservice.xml.XmlSelector;

public class PossibleParameterValuesAdapter
{
	private ParameterDescriptor descriptor;
	
	public PossibleParameterValuesAdapter( ParameterDescriptor descriptor )
	{
		this.descriptor = descriptor;
	}

	public List<KeyValue> getPossibleValues( JobContext context )
	{
		PossibleParameterValuesSelector selector = createSelector();
		selector.init( context );
		List<KeyValuePair> possibleValues =
				selector.selectPossibleValues( descriptor.getInputName() );
		
		ArrayList<KeyValue> keyValues = new ArrayList<KeyValue>();
		for (KeyValuePair entry: possibleValues) {
			keyValues.add( new KeyValue( entry.getKey(), entry.getValue() ) );
		}
		return keyValues;
	}
	
	PossibleParameterValuesSelector createSelector()
	{
		if (descriptor.getSelector() != null) {
			return createSelectorFromDescriptor( descriptor.getSelector() );
		}
		else if (Enum.class.isAssignableFrom( descriptor.getValueType() )) {
			return new EnumValuesSelector( descriptor.getValueType() );
		}
		else {
			throw new RuntimeException( Messages.getString("param.noSelector", descriptor.getName()) );
		}
	}
	
	static PossibleParameterValuesSelector createSelectorFromDescriptor(
			                                           XmlSelector selectorDsc )
	{
		PossibleParameterValuesSelector selector;
		try {
			Class<?> clazz = Class.forName( selectorDsc.getClazz() );
			selector = (PossibleParameterValuesSelector)clazz.newInstance();
		} 
		catch (Exception e) {
			throw new RuntimeException( Messages.getString("param.selector.class.invalid", selectorDsc.getClazz(), e) );
		}
		
		// populate selector properties:
		List<XmlNameValuePair> properties = selectorDsc.getProperty();
		if (properties.size() > 0) {
			Bean selectorBean = new Bean( selector );
			for (XmlNameValuePair nvp: properties) {
				// value may be specified both as 'value' attr and as element text:
				String value = nvp.getValueAttribute();
				if (value == null) {
					value = nvp.getValue();
				}
				try {
					selectorBean.setPropertyString( nvp.getName(), value, true );
				} 
				catch (Exception e) {
					throw new RuntimeException( Messages.getString("param.selector.prop.canNotSet",
							                    nvp.getName(), value, selectorDsc.getClazz(), e.getMessage()), e );
				}
			}
		}
		return selector;
	}
	
}

class EnumValuesSelector implements PossibleParameterValuesSelector
{
	private Class<?> enumType ;
	
	public EnumValuesSelector( Class<?> enumType ) 
	{
		this.enumType = enumType;
	}

	@Override
	public void init(JobContext context) {
	}

	@Override
	public List<KeyValuePair> selectPossibleValues(String parameterName) 
	{
		List<KeyValuePair> enumMap = new ArrayList<KeyValuePair>();
		Object[] values = enumType.getEnumConstants();
		ValueConverter converter =
				ValueConverterFactory.getInstance().getConverterFor( enumType );
		if (values != null) {
			for (Object value: values) {
				String svalue = converter.objectToString( value );
				enumMap.add( new KeyValuePair( svalue, svalue ) );
			}
		}
		return enumMap;
	}
}
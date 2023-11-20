package com.trustwave.dbpjobservice.parameters.converters;


import com.googlecode.sarasvati.env.AttributeConverter;
import com.trustwave.dbpjobservice.workflow.api.action.ValueConverter;

public class ValueAttributeConverterAdapter 
       implements AttributeConverter, ValueConverter
{
	private ValueConverter valueConverter;

	public ValueAttributeConverterAdapter(ValueConverter valueConverter) {
		this.valueConverter = valueConverter;
	}

	@Override
	public String objectToString(Object object) {
		return valueConverter.objectToString( object );
	}

	@Override
	public Object stringToObject(String string, Class<?> type) {
		return valueConverter.stringToObject(string, type);
	}

	@Override
	public String getShortString(Object obj) {
		return valueConverter.getShortString(obj);
	}
}

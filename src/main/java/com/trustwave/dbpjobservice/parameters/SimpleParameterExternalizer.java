package com.trustwave.dbpjobservice.parameters;

import java.util.List;

import com.trustwave.dbpjobservice.impl.Messages;
import com.trustwave.dbpjobservice.interfaces.ValueType;
import com.trustwave.dbpjobservice.workflow.api.action.ValueConverter;

public class SimpleParameterExternalizer extends ParameterExternalizer
{
	SimpleParameterExternalizer( ParameterDescriptor descriptor, 
			               	     ValueType externalValueType,
			                     ValueConverter valueConverter )
	{
		super( descriptor, externalValueType, valueConverter );
	}
	
	
	protected void populateParameterValue( final List<String> values, final Object obj )
	{
		values.add( getValueConverter().objectToString( obj ) );
	}
	
	protected Object valueListToObject( final List<String> values )
	{
		if (values == null || values.size() == 0) {
			return null;
		}
		if (values.size() > 1) {
			throw new ParameterException( Messages.getString("param.externalizer.tooManyValues", getParametername()) );
		}
		return getValueConverter().stringToObject(
				values.get(0), getDescriptor().getValueType() );	
	}
}

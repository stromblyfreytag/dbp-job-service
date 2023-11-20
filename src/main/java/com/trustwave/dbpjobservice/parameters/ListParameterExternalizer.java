package com.trustwave.dbpjobservice.parameters;

import java.util.ArrayList;
import java.util.List;

import com.trustwave.dbpjobservice.interfaces.ExternalParameterDefinition;

public class ListParameterExternalizer extends ParameterExternalizer
{
	ListParameterExternalizer( ParameterExternalizer externalizer ) 
	{
		super( externalizer.getDescriptor(), 
			   externalizer.getExternalValueType(), 
			   externalizer.getValueConverter() );
	}

	@Override
	protected ExternalParameterDefinition createExternalDefinition()
	{
		ExternalParameterDefinition extpd = super.createExternalDefinition();
		extpd.setList( true );
		return extpd;
	}
	
	@Override
	protected void populateParameterValue( final List<String> values, final Object obj )
	{
		final List<?> list = (List<?>)obj;
		for (Object o: list) {
			values.add( getValueConverter().objectToString( o ) );
		}
	}
	
	@Override
	protected Object valueListToObject( final List<String> values )
	{
		// TODO: let descriptor create object (e.g. LinkedList)
		final List<Object> list = new ArrayList<Object>();
		Class<?> elemType = getDescriptor().getElementValueType();
		if (values != null) {
			for (String s: values) {
				list.add( getValueConverter().stringToObject( s, elemType ) );
			}
		}
		return list;	
	}
}

package com.trustwave.dbpjobservice.parameters;

import com.trustwave.dbpjobservice.interfaces.ValueType;
import com.trustwave.dbpjobservice.workflow.api.action.valueconverters.JsonConverter;

public class JsonParameterExternalizer extends SimpleParameterExternalizer
{
	JsonParameterExternalizer( ParameterDescriptor descriptor ) 
	{
		super( descriptor, ValueType.OBJECT, new JsonConverter() );
	}
}

package com.trustwave.dbpjobservice.parameters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.trustwave.dbpjobservice.interfaces.ValueType;
import com.trustwave.dbpjobservice.workflow.api.action.ValueConverter;
import com.trustwave.dbpjobservice.workflow.api.action.valueconverters.ValueConverterFactory;

public class ParameterExternalizerFactory
{
	private static final Map<Class<?>, ValueType> simpleTypeMap;
	
	static {
		simpleTypeMap = new HashMap<Class<?>, ValueType>();
		simpleTypeMap.put( String.class,  ValueType.STRING );
		simpleTypeMap.put( Boolean.class, ValueType.BOOLEAN );
		simpleTypeMap.put( Boolean.TYPE,  ValueType.BOOLEAN );
		simpleTypeMap.put( Integer.class, ValueType.INTEGER );
		simpleTypeMap.put( Integer.TYPE,  ValueType.INTEGER );
		simpleTypeMap.put( Long.class,    ValueType.LONG );
		simpleTypeMap.put( Long.TYPE,     ValueType.LONG );
	}
	
	private ValueType getSimpleValuType( Class<?> clazz )
	{
		ValueType vt = null;
		if (clazz != null) {
			vt = simpleTypeMap.get( clazz );
			if (vt == null && Enum.class.isAssignableFrom( clazz )) {
				vt = ValueType.ENUM;
			}
		}
		return vt;
	}
	
	public ParameterExternalizer createExternalizer( ParameterDescriptor descriptor )
	{
		ParameterExternalizer externalizer = null;
		Class<?> clazz = descriptor.getValueType();
		
		if (List.class.isAssignableFrom( clazz )) {
			externalizer = createFor( descriptor, descriptor.getElementValueType() );
			if (externalizer != null) {
				externalizer = new ListParameterExternalizer( externalizer );
			}
		}
		else {
			externalizer = createFor( descriptor, clazz );
		}
		return externalizer;
	}
	
	private ParameterExternalizer createFor( ParameterDescriptor descriptor, 
			                                 Class<?> clazz )
	{
		ValueType vt = getSimpleValuType( clazz );
		if (vt != null) {
			ValueConverter cnv =
					ValueConverterFactory.getInstance().getConverterFor( clazz );
			if (descriptor.getSelector() != null) {
				vt = ValueType.ENUM;
			}
			return new SimpleParameterExternalizer( descriptor, vt, cnv );
		}
		if (descriptor.isExternalizableAsString() ) {
			ValueConverter cnv =
					ValueConverterFactory.getInstance().getConverterFor( clazz );
			return new SimpleParameterExternalizer( descriptor, ValueType.STRING, cnv );
		}
		// everything else is externalized as JSON string:
		return new JsonParameterExternalizer( descriptor );
	}
	
}

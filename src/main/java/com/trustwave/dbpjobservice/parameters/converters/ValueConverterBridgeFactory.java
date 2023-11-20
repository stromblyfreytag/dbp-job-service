package com.trustwave.dbpjobservice.parameters.converters;



import com.googlecode.sarasvati.env.AttributeConverters;
import com.trustwave.dbpjobservice.workflow.api.action.ValueConverter;
import com.trustwave.dbpjobservice.workflow.api.action.valueconverters.ValueConverterFactory;

/**
 * <p>This class is used as a bridge between {@link ValueConverterFactory} and
 * {@link AttributeConverters}: when converter is added ValueConverterFactory,
 * it is also added to AttributeConverters.</p>
 * <p>Instance of this class is supposed to be created at least once, e.g. by
 * Spring. On creation it replaces default ValueConverterFactory instance.</p>
 * 
 * @author vlad
 *
 */
public class ValueConverterBridgeFactory extends ValueConverterFactory
{
	public ValueConverterBridgeFactory() 
	{
		if (!this.getClass().equals( getInstance().getClass() )) {
			setInstance( this );
		}
	}
	
	public void addConverter( Class<?> clazz, ValueConverter converter )
	{
		super.addConverter( clazz, converter );
		AttributeConverters.setConverterForType( clazz,
			new ValueAttributeConverterAdapter(converter) );
	}

}

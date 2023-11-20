package com.trustwave.dbpjobservice.parameters;

import java.util.List;

import com.trustwave.dbpjobservice.impl.Messages;
import com.trustwave.dbpjobservice.interfaces.ExternalParameterDefinition;
import com.trustwave.dbpjobservice.interfaces.ExternalParameterValue;
import com.trustwave.dbpjobservice.interfaces.ValueType;
import com.trustwave.dbpjobservice.workflow.api.action.ValueConverter;

/**
 * <p>Parameter externalizer converts job parameters to their external
 *  representation (to send job parameters to clients) and back (when parameters
 *  are received from client).
 * </p>
 * <p>It also provides full parameter description for clients as specified in
 * {@link ExternalParameterDefinition}. 
 * </p>
 * <p>Parameter externalizers are created by {@link ParameterExternalizerFactory}
 *   from internal parameter descriptors (see {@link ParameterDescriptor})
 *  </p> 
 * @author vlad
 */
public abstract class ParameterExternalizer 
{
	private final ParameterDescriptor descriptor;
	private final ValueType externalValueType;
	private final ValueConverter valueConverter;
	private final ExternalParameterDefinition externalDefinition;
	
	ParameterExternalizer( ParameterDescriptor descriptor, 
			               ValueType           externalValueType,
			               ValueConverter      valueConverter ) 
	{
		this.descriptor         = descriptor;
		this.externalValueType  = externalValueType;
		this.valueConverter     = valueConverter;
		this.externalDefinition = createExternalDefinition();
	}
	
	public final String getParametername() 
	{
		return descriptor.getExternalName();
	}
	
	public final ValueType getExternalValueType() {
		return externalValueType;
	}

	/** Get external parameter definition (from parameter descriptor)
	 */
	public final ExternalParameterDefinition getExternalDefinition() {
		return externalDefinition;
	}
	
	protected ExternalParameterDefinition createExternalDefinition()
	{
		final ExternalParameterDefinition extpd = new ExternalParameterDefinition();
		extpd.setName(        getParametername() );
		extpd.setDisplayName( descriptor.getDisplayName() );
		extpd.setDescription( descriptor.getDescription() );
		extpd.setGenericName( descriptor.getGenericName() );
		extpd.setValueType(   externalValueType );
		extpd.setList(        false );
		extpd.setRequired(    !descriptor.isOptional() );
		String dfltstr = descriptor.getDefaultValue();
		if (dfltstr != null) {
			Object dflt = valueConverter.stringToObject( dfltstr, descriptor.getElementValueType() );
			extpd.setDefaultValue( objectToExternalValue( dflt, false ) );
		}
		Class<?> clazz = getDescriptor().getElementValueType();
		if (clazz == null) {
			clazz = getDescriptor().getValueType();
		}
		extpd.setClassName( clazz.getName() );
		extpd.getMetadata().addAll( descriptor.getMetadata() );
		return extpd;
	}

	/** Convert parameter object to its external representation.
	 * @param obj object to convert
	 * @return external parameter value for the object
	 */
	public final ExternalParameterValue objectToExternalValue( final Object obj )
	{
		return objectToExternalValue( obj, true );
	}
	
	protected final ExternalParameterValue objectToExternalValue(
			final Object obj, boolean withDefinition )
	{
		ExternalParameterValue pValue = new ExternalParameterValue();
		pValue.setName( getParametername() );
		if (obj != null) {
			populateParameterValue( pValue.getValue(), obj );
		}
		pValue.setDefinition( getExternalDefinition() );
		return pValue;
	}
	
	/** Convert external parameter value to an object of parameter's class.
	 * @param pValue external value of the parameter
	 * @return Object of parameters class or <code>null</code>.
	 */
	public final Object externalValueToObject( final ExternalParameterValue pValue )
	{
		if (pValue == null) {
			return null;
		}
		if (!getParametername().equals( pValue.getName() )) {
			throw new RuntimeException( Messages.getString("param.toObject.name.invalid", getParametername(), pValue.getName()) );
		}
		return valueListToObject( pValue.getValue() );	
	}
	
	abstract protected void populateParameterValue( final List<String> values, final Object obj );
	
	abstract protected Object valueListToObject( final List<String> values );
	
	protected ValueConverter getValueConverter() {
		return valueConverter;
	}

	protected final ParameterDescriptor getDescriptor() {
		return descriptor;
	}

}

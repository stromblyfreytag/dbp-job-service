package com.trustwave.dbpjobservice.parameters;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.trustwave.dbpjobservice.impl.Messages;
import com.trustwave.dbpjobservice.workflow.api.action.InputParameter;
import com.trustwave.dbpjobservice.workflow.api.action.OutputParameter;
import com.trustwave.dbpjobservice.xml.XmlOutput;
import com.trustwave.dbpjobservice.xml.XmlParameter;
import com.trustwave.dbpjobservice.xml.XmlParameters;
import com.trustwave.dbpjobservice.xml.XmlValidator;

public class ParameterUtils
{
	public static List<ParameterDescriptor> retrieveParameterDescriptors( Class<?> clazz )
	{
		List<ParameterDescriptor> list = new ArrayList<ParameterDescriptor>();
		try {
			BeanInfo bi = Introspector.getBeanInfo( clazz );
			PropertyDescriptor[] pdList = bi.getPropertyDescriptors();
			for (PropertyDescriptor pd: pdList) {
				ParameterDescriptor descriptor = getForProperty( pd, clazz );
				if (descriptor != null) {
					list.add( descriptor );
				}
			}
		} 
		catch (IntrospectionException e) {
		}
		return list;
	}
	
	static ParameterDescriptor getForProperty( PropertyDescriptor pd, Class<?> clazz )
	{
		ParameterDescriptor descriptor = null;
		
		InputParameter inpAnnotation =
			BeanUtils.getAnnotation( InputParameter.class, pd, clazz );
		if (inpAnnotation != null) {
			descriptor = new ParameterDescriptor( pd.getName(), pd.getPropertyType(), clazz, true );
			descriptor.setOptional( inpAnnotation.optional() );
			if (!InputParameter.DEFAULT_STR.equals( inpAnnotation.displayName() )) {
				descriptor.setDisplayName( inpAnnotation.displayName() );
			}
			if (!InputParameter.DEFAULT_STR.equals( inpAnnotation.description() )) {
				descriptor.setDescription( inpAnnotation.description() );
			}
			if (!InputParameter.DEFAULT_STR.equals( inpAnnotation.genericName() )) {
				descriptor.setGenericName( inpAnnotation.genericName() );
			}
			if (inpAnnotation.join()) {
				processJoinParameter( descriptor, clazz );
			}
			if (!InputParameter.DEFAULT_STR.equals( inpAnnotation.joinDiscriminator() )) {
				descriptor.setJoinDiscriminator( inpAnnotation.joinDiscriminator() );
			}
			if (inpAnnotation.validators().length > 0) {
				for (Class<?> c: inpAnnotation.validators()) {
					XmlValidator validatorXml = new XmlValidator();
					validatorXml.setClazz( c.getName() );
					descriptor.getValidators().add( validatorXml );
				}
			}
			descriptor.setValuePresent( inpAnnotation.internal() );
			descriptor.setExternalizableAsString( inpAnnotation.externalizableAsString() );
		}
		OutputParameter outAnnotation =
			BeanUtils.getAnnotation( OutputParameter.class, pd, clazz );
		if (outAnnotation != null) {
			if (descriptor == null) {
				descriptor = new ParameterDescriptor( pd.getName(), pd.getPropertyType(), clazz, false );
			}
			descriptor.setOutputParameter( true );
			if (!OutputParameter.DEFAULT_STR.equals( outAnnotation.tokenSet())) {
				descriptor.setTokenSet( outAnnotation.tokenSet() );
				if (!List.class.isAssignableFrom( descriptor.getValueType() )) {
					throw new RuntimeException(Messages.getString("param.typeNotList", descriptor, clazz.getName()));
				}
			}
		}
		if (descriptor != null) {
			descriptor.setActionType( clazz );
			descriptor.setGetter( pd.getReadMethod() );
			descriptor.setSetter( pd.getWriteMethod() );
			if (descriptor.isInputParameter() && descriptor.getSetter() == null) {
				throw new RuntimeException( Messages.getString("param.setter.missing", clazz.getName(), descriptor.getName()) );
			}
			if (descriptor.isOutputParameter() && descriptor.getGetter() == null) {
				throw new RuntimeException( Messages.getString("param.getter.missing", clazz.getName(), descriptor.getName()) );
			}
			// populate list element type: 
			Class<?> elType = BeanUtils.getListElementType( descriptor.getGetter() );
			if (elType == null && List.class.isAssignableFrom( descriptor.getValueType() )) {
				throw new RuntimeException( Messages.getString("param.value.canNotRetrieve", descriptor, clazz.getName()) );
			}
			descriptor.setElementValueType( elType );
			if (elType != null) {
				// use converter for list with fallback to single element:
				descriptor.resetConverter();
			}
		}
		return descriptor;
	}
	
	private static void processJoinParameter( ParameterDescriptor descriptor, Class<?> clazz )
	{
		if (List.class.isAssignableFrom( descriptor.getValueType() )) {
			descriptor.setJoinType( "list" );
		}
		else if (Map.class.isAssignableFrom( descriptor.getValueType() )) {
			descriptor.setJoinType( "map" );
		}
		else {
			throw new RuntimeException( Messages.getString("param.type.join.notListOrMap", descriptor, clazz.getName()) );
		}
	}
	
	static void completeWithXmlParameter( ParameterDescriptor pd,
			                              XmlParameter in, XmlOutput out )
	{
		if (in != null) {
			if (in.getInternalName() != null && !in.getInternalName().equals( pd.getName())) {
				throw new RuntimeException( Messages.getString("param.ambiguous", in.getName(), in.getInternalName()) );
			}
			pd.setInputName( in.getName() );
			if (in.getDisplayName() != null) {
				pd.setDisplayName( in.getDisplayName() );
			}
			if (in.getDescription() != null) {
				pd.setDescription( in.getDescription() );
			}
			pd.setVisible( in.isVisible() );
			pd.setValuePresent( in.isValuePresent() );
			String expr = (in.getText() != null? in.getText(): in.getValue() );
			pd.setValueExpression( expr  );
			if (in.getDefaultValue() != null) {
				pd.setDefaultValue( in.getDefaultValue() );
				pd.setOptional( true );
			}
			if (in.getJoinDiscriminator() != null) {
				pd.setJoinDiscriminator( in.getJoinDiscriminator() );
			}
			pd.setIgnoreExpressionErrors( in.isIgnoreExpressionErrors() );
			pd.setOverrideTokenValue( in.isOverride() );
			if (in.getValidator().size() > 0) {
				// Validators specified in workflow xml override any validators
				// defined in action code, clean validators:
				pd.getValidators().clear();
				setValidators( pd, in );
			}
			if (in.getSelector() != null) {
				// this call is just for validation of selector descriptor:
				PossibleParameterValuesAdapter
						.createSelectorFromDescriptor( in.getSelector() );
				pd.setSelector( in.getSelector() );
			}
			pd.addMetadataFromXml( in.getMetadata() );
		}
		if (out != null) {
			if (out.getInternalName() != null && !out.getInternalName().isEmpty()
				&& !out.getInternalName().equals( pd.getName())) {
				throw new RuntimeException( Messages.getString("param.ambiguous.output", pd.getName(), out.getInternalName()) );
			}
			pd.setOutputName( out.getName() );
			pd.setOutputValueExpression( out.getValue() );
			pd.setOverrideActionOutputValue( out.isOverride() );
			pd.setClearOutputValue( out.isClearValue() );
			pd.setOutputEnv( out.getEnv() );
		}
	}
	
	static void setValidators( ParameterDescriptor pd, XmlParameter in )
	{
		for (XmlValidator v: in.getValidator()) {
			// this call will throw if validator has errors:
			ParameterValidatorCreator.createValidator( v );
			pd.getValidators().add( v );
		}
	}
	
	
	static void appendFreeOutputDescriptors( List<ParameterDescriptor> pdList, 
			                                 List<XmlOutput>           outList, 
			                                 Class<?>                  taskType,
			                                 String                    nodeName )
	{
		Set<String> names = new HashSet<String>();
		for (ParameterDescriptor pd: pdList) {
			names.add( pd.getOutputName() );
		}
		for (XmlOutput out: outList) {
			if (!names.contains( out.getName() )) {
				if ("".equals( out.getInternalName() )) {
					ParameterDescriptor pd = createFromXmlOutput( out, taskType );
					pdList.add( pd );
				}
				else {
					throw new RuntimeException( Messages.getString("param.unnamed", out.getName(), nodeName) );
				}
			}
		}
	}

	static ParameterDescriptor createFromXmlOutput( XmlOutput out, Class<?> taskType)
	{
		ParameterDescriptor pd = 
			new ParameterDescriptor( out.getName(), String.class, taskType, false );
		completeWithXmlParameter( pd, null, out );
		return pd;
	}
	
	static List<ParameterDescriptor> orderParametersByDeclaredList(
			Collection<ParameterDescriptor> unordered,
			XmlParameters declaredParameters,
			boolean validate )
	{
		Map<String,ParameterDescriptor> pmap = new HashMap<String, ParameterDescriptor>();
		for (ParameterDescriptor pd: unordered) {
			pmap.put( pd.getInputName(), pd );
		}
			
		List<ParameterDescriptor> ordered = new ArrayList<ParameterDescriptor>();
		for (XmlParameter declared: declaredParameters.getParameter()) {
			ParameterDescriptor pd = pmap.get( declared.getName() );
			if (pd == null) {
				if (validate) {
					throw new RuntimeException(Messages.getString("param.notAJobParam", declared.getName()));
				}
			}
			else {
				pmap.remove( declared.getName() );
				ordered.add( pd );
			}
		}
		
		if (pmap.size() > 0) {
			if (validate && declaredParameters.isAllJobParameters()) {
				throw new RuntimeException(Messages.getString("param.notDeclared", pmap.keySet()));
			}
			ordered.addAll( pmap.values() );
		}
		return ordered;
	}
	
}

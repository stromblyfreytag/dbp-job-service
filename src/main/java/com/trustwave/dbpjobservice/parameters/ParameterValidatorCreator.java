package com.trustwave.dbpjobservice.parameters;

import java.util.List;

import com.trustwave.dbpjobservice.impl.Messages;
import com.trustwave.dbpjobservice.workflow.api.action.ParameterValidator;
import com.trustwave.dbpjobservice.workflow.api.util.Bean;
import com.trustwave.dbpjobservice.xml.XmlNameValuePair;
import com.trustwave.dbpjobservice.xml.XmlValidator;

public class ParameterValidatorCreator
{

	public static ParameterValidator createValidator( XmlValidator validatorXml )
	{
		ParameterValidator validator;
		try {
			Class<?> clazz = Class.forName( validatorXml.getClazz() );
			if (!ParameterValidator.class.isAssignableFrom( clazz )) {
				throw new RuntimeException( Messages.getString("param.validator.interface.invalid", validatorXml.getClazz()) );
			}
			validator = (ParameterValidator)clazz.newInstance();
		} 
		catch (Exception e) {
			throw new RuntimeException( Messages.getString("param.validator.class.notFound", validatorXml.getClazz()) );
		}
		
		// populate validator properties:
		List<XmlNameValuePair> properties = validatorXml.getProperty();
		if (properties.size() > 0) {
			Bean validatorBean = new Bean( validator );
			for (XmlNameValuePair nvp: properties) {
				// value may be specified both as 'value' attr and as element text:
				String value = nvp.getValueAttribute();
				if (value == null) {
					value = nvp.getValue();
				}
				try {
					validatorBean.setPropertyString( nvp.getName(), value, true );
				} 
				catch (Exception e) {
					throw new RuntimeException( Messages.getString("param.validator.prop.canNotSet",
							                    nvp.getName(), value, validatorXml.getClazz(), e.getMessage()), e );
				}
			}
		}
		return validator;
	}
}

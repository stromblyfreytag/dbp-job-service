package com.trustwave.dbpjobservice.workflow;

import static com.trustwave.dbpjobservice.impl.Messages.*;

import javax.xml.bind.JAXBElement;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;



import com.googlecode.sarasvati.Node;
import com.googlecode.sarasvati.load.GraphFactory;
import com.googlecode.sarasvati.load.NodeFactory;
import com.googlecode.sarasvati.load.SarasvatiLoadException;
import com.trustwave.dbpjobservice.impl.Messages;
import com.trustwave.dbpjobservice.workflow.api.util.XmlUtil;

public class JaxbNodeFactory implements NodeFactory 
{
	private Class<? extends Node> nodeClass;
	private String                xmlBindingPackage;

	
	public JaxbNodeFactory( Class<? extends Node> nodeClass, String xmlBindingPackage) 
	{
		this.nodeClass = nodeClass;
		this.xmlBindingPackage = xmlBindingPackage;
	}

	public void registerWithGraphFactory( GraphFactory graphFactory, String nodeType )
	{
		graphFactory.addNodeFactory( nodeType, this );
	}


	@Override
	public Node newNode(String type) throws SarasvatiLoadException 
	{
		try {
			Node node = nodeClass.newInstance();
			return node;
		} 
		catch (Exception e) {
			throw new SarasvatiLoadException( getString("workflow.node.class.create.error", nodeClass, e) );
		}
	}

	@Override
	public Map<String, String> loadCustom( Node node, Object custom )
			throws SarasvatiLoadException 
	{
		if (custom == null) {
			return null;
		}		
		if (custom instanceof Element) {
			custom = XmlUtil.fromDom( (Element)custom, xmlBindingPackage);
		}
		if (custom instanceof JAXBElement<?>) {
			JAXBElement<?> jaxbValue = (JAXBElement<?>)custom;
			String propertyName = jaxbValue.getName().getLocalPart();
			assignToBean( node, propertyName, jaxbValue.getValue(), jaxbValue.getDeclaredType() );
			return null;
			//return getPropertyValueMap( node, propertyName + "String", jaxbValue );
		}
		else {
			throw new SarasvatiLoadException( getString("workflow.customObject.unsupported", custom) );
		}
	}
	
	Map<String,String> getPropertyValueMap( Object bean, String propertyName, JAXBElement<?> jaxbObj )
	{
		Map<String,String> map = new HashMap<String, String>();
		PropertyDescriptor pd = 
			findPropertyByName( bean, propertyName, String.class );
		if (pd != null) {
			String xmlValue = XmlUtil.toXmlString( jaxbObj );
			map.put( propertyName, xmlValue );
		}
		return map;
	}
	
	void assignToBean( Object bean, String propertyName, Object value, Class<?> clazz )
	{
		PropertyDescriptor pd = 
			findPropertyByName( bean, propertyName, clazz );
		if (pd == null) {
			throw new SarasvatiLoadException( getString("workflow.bean.prop.notFound", propertyName, bean.getClass().getName()) );
		}
		Method setter = pd.getWriteMethod();
		if (setter == null) {
			throw new SarasvatiLoadException( getString("workflow.bean.prop.noSetter", pd.getName(), bean.getClass()) );
		}
		try {
			setter.invoke( bean, value );
		} 
		catch (Exception e) {
			throw new SarasvatiLoadException( getString("workflow.bean.prop.set.error", pd.getName(), e), e );
		}
	}
	
	PropertyDescriptor findPropertyByName( Object obj, String name, Class<?> cls )
	{
		BeanInfo beanInfo;
		try {
			beanInfo = Introspector.getBeanInfo( obj.getClass() );
		}
		catch (IntrospectionException e) {
			throw new RuntimeException( e );
		}
		PropertyDescriptor pds[] = beanInfo.getPropertyDescriptors();
		for (PropertyDescriptor pd: pds) {
			if (name.equals( pd.getName() ))
			    return pd;
		}
		return null;
	}

}

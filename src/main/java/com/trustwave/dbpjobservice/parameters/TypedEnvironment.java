package com.trustwave.dbpjobservice.parameters;

import java.util.HashMap;
import java.util.Map;




import com.googlecode.sarasvati.env.Env;
import com.trustwave.dbpjobservice.impl.Messages;
import com.trustwave.dbpjobservice.workflow.api.action.ValueConverter;
import com.trustwave.dbpjobservice.workflow.api.action.valueconverters.ValueConverterFactory;

public class TypedEnvironment 
{
	private Env env;
	private CachedEnvironment higherLevelEnv;

	public TypedEnvironment(Env env) 
	{
		this.setEnv(env);
	}
	
	public TypedEnvironment(Env env, CachedEnvironment upperEnv) 
	{
		this.setEnv(env);
		this.higherLevelEnv = upperEnv;
	}
	
	public boolean hasAttribute( String name )
	{
		return getEnv().hasAttribute( name ) 
			|| (higherLevelEnv != null && higherLevelEnv.hasAttribute( name ));
	}
	
	public Object getAttribute( String name )
	{
		if (getEnv().hasAttribute(name)) {
			return getTypedAttribute( name );
		}
		else if (higherLevelEnv != null && higherLevelEnv.hasAttribute( name )) {
			return higherLevelEnv.getAttribute( name );
		}
		return null;
	}
	
	public void setAttribute( String name, Object value )
	{
		setAttribute( name, value, false );
		
	}
	public void setAttribute( String name, Object value, boolean Transient )
	{
		if (value == null) {
			if (getEnv().hasAttribute(name)) {
				getEnv().removeAttribute( name );
			}
		}
		else {
			setTypedAttribute( name, value, value.getClass() ); 
		}
	}
	
	private Object getTypedAttribute( String name )
	{
		String valueStr = getEnv().getAttribute(name);
		Class<?> clazz = getAttributeType( name ); 
		ValueConverter cnv = getConverter( clazz, name );
		return cnv.stringToObject( valueStr, clazz );
	}
	
	private void setTypedAttribute( String name, Object value, Class<?> clazz ) 
	{
		if (clazz == null) {
			clazz = value.getClass();
		}
		ValueConverter cnv = getConverter( clazz, name );
		String valueStr = cnv.objectToString( value );
		
		if (getEnv().hasAttribute(name)) {
			if (valueStr.equals( getEnv().getAttribute(name) ))
				return;
		}
		saveAttributeType( name, clazz );
		getEnv().setAttribute( name, valueStr );
	}
	
	public void saveAttributeType( String attrName, Class<?> type )
	{
		String typeName = type.getName();
		String attrTypeName = "type-" + attrName;
		if (!typeName.equals( getEnv().getAttribute( attrTypeName ) )) {
			getEnv().setAttribute( attrTypeName, typeName );
		}
	}
	
	public Class<?> getAttributeType( String attrName )
	{
		String attrTypeName = "type-" + attrName;
		String typeName = getEnv().getAttribute( attrTypeName );
		if (typeName == null) {
			if (higherLevelEnv != null) {
				return higherLevelEnv.getAttributeType( attrName );
			}
			throw new RuntimeException( Messages.getString("param.attribute.type.notSaved", attrName) );
		}
		return getClass( typeName );
	}
	
	private ValueConverter getConverter( Class<?> clazz, String name )
	{
		ValueConverter cnv =
			ValueConverterFactory.getInstance().getConverterFor( clazz );
		if (cnv == null) {
			throw new RuntimeException( Messages.getString("param.noConverter", name, clazz.getName()) );
		}
		return cnv;
	}

	private static Map<String, Class<?>> classMap =
		new HashMap<String, Class<?>>();
	
	private Class<?> getClass( String className )
	{
		Class<?> clazz = classMap.get( className );
		if (clazz == null) {
			try {
				clazz = Class.forName( className );
			} 
			catch (ClassNotFoundException e) {
				throw new RuntimeException( e.toString() );
			}
			classMap.put( className, clazz );
		}
		return clazz;
	}

	protected Env getEnv() {
		return env;
	}

	protected void setEnv(Env env) {
		this.env = env;
	}
	
	protected CachedEnvironment getHigherLevelEnv() {
		return higherLevelEnv;
	}
}

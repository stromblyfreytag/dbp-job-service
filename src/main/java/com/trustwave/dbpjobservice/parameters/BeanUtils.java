package com.trustwave.dbpjobservice.parameters;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.trustwave.dbpjobservice.impl.Messages;

public class BeanUtils
{
	public static PropertyDescriptor getPropertyDescriptor( Class<?> clazz, String name ) throws IntrospectionException
	{
		BeanInfo bi = Introspector.getBeanInfo( clazz );
		PropertyDescriptor[] pds = bi.getPropertyDescriptors();
		for (PropertyDescriptor pd: pds) {
			if (name.equals(pd.getName())) {
				return pd;
			}
		}
		throw new RuntimeException(Messages.getString("bean.prop.notFound", name, clazz.getName()));
	}
	
	public static Object evaluateBeanProperty( Object obj, String propertyExpr ) 
	{
		return evaluateBeanProperty( obj, propertyExpr, (obj != null? obj.getClass(): null) );
	}
	
	public static Object evaluateBeanProperty( Object obj, String propertyExpr, Class<?> objClass )
	{
		if (obj == null) {
			throw new RuntimeException( Messages.getString("bean.object.null", propertyExpr, (objClass != null? objClass.getName(): "(unknown)")) );
		}
		int k = propertyExpr.indexOf( '.' );
		String propertyName =  (k > 0? propertyExpr.substring( 0, k ): propertyExpr);
		Object value = null;
		PropertyDescriptor pd;
		try {
			pd = getPropertyDescriptor( obj.getClass(), propertyName );
			Method getter = pd.getReadMethod();
			if (getter == null)
				throw new RuntimeException(Messages.getString("bean.prop.noGetter", propertyName));
			
			value = getter.invoke( obj );
		} 
		catch (Exception e) {
			throw new RuntimeException(Messages.getString("bean.prop.retrieval.error", obj.getClass().getName(), propertyName, e));
		}
		
		if (!propertyName.equals(propertyExpr)) {
			value = evaluateBeanProperty( value, propertyExpr.substring( k+1 ), pd.getPropertyType() );
		}
		return value;
	}

	static <T extends Annotation> T getAnnotation(
			Class<T> annotationClass, PropertyDescriptor pd, Class<?> clazz )
	{
		Method getter = pd.getReadMethod();
		if (getter != null && getter.isAnnotationPresent(annotationClass)) {
			return getter.getAnnotation(annotationClass);
		}
		Method setter = pd.getWriteMethod();
		if (setter != null && setter.isAnnotationPresent(annotationClass)) {
			return setter.getAnnotation(annotationClass);
		}
		Field  field  = getFieldByName( clazz, pd.getName() );
		if (field != null && field.isAnnotationPresent(annotationClass)) {
			return field.getAnnotation(annotationClass);
		}
		return null;
	}
	
	public static List<String> getAnnotatedFields( Class<?> clazz, Class<?> ... annotationClass )
	{
		List<String> annotated = new ArrayList<String>();
		
		Set<Class<?>> targetAnnotations = new HashSet<Class<?>>();
		targetAnnotations.addAll( Arrays.asList( annotationClass ) );
		
		for (Field f: clazz.getDeclaredFields()) {
			Annotation[] annotations =  f.getAnnotations();
			for (Annotation a: annotations) {
				if (targetAnnotations.contains(a.annotationType()) ) {
					annotated.add( f.getName() );
				}
			}
		}
		return annotated;
	}
	
	static Field getFieldByName( Class<?> clazz, String name )
	{
		for (Field f: clazz.getDeclaredFields()) {
			if (name.equals( f.getName() ))
				return f;
		}
		Class<?> parent = clazz.getSuperclass();
		if (parent != null) {
			return getFieldByName( parent, name );
		}
		return null;
	}
	
	public static Class<?> getInterfaceClass( Class<?> clazz )
	{
		if (clazz != null) {
			Class<?>[] interfaces = clazz.getInterfaces();
			if (interfaces != null && interfaces.length == 1) {
				return interfaces[0];
			}
		}
		return clazz;
	}
	
	public static Class<?> getListElementType( Type t )
	{
		if (t instanceof ParameterizedType){
		    ParameterizedType type = (ParameterizedType)t;
		    Type rt = type.getRawType();
		    if (rt instanceof Class<?>
		     && List.class.isAssignableFrom((Class<?>)rt) ) {
		    	return (Class<?>)type.getActualTypeArguments()[0];
		    }
		}
		return null;
	}
	
	public static Class<?> getListElementType( Method getter )
	{
		if (getter != null) {
			return getListElementType( getter.getGenericReturnType() );
		}
		return null;
	}
}

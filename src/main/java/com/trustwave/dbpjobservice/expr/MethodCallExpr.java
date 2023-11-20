package com.trustwave.dbpjobservice.expr;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class MethodCallExpr extends Expression 
{
	private Expression expr;
	private String methodName;
	private Object[] args;
	
	public MethodCallExpr( Expression expr, String methodName ) {
		this.expr = expr;
		this.methodName = methodName;
		this.args = new Object[0];
		setName( methodName + "()" );
	}

	public MethodCallExpr( Expression expr, String methodName, List<Object> params ) {
		this.expr = expr;
		this.methodName = methodName;
		this.args = new Object[params.size()];
		for (int i=0;  i< args.length;  i++) {
			args[i] = params.get(i);
		}
		String sparams = params.toString();
		sparams = sparams.substring( 1,  sparams.length()-1 );
		setName( methodName + "(" + sparams + ")" );
	}


	@Override
	public Object evaluate(ExpressionContext ctx) 
	{
		Object obj = expr.evaluate( ctx );
		if (obj == null) {
			throw new ExpressionException( "Null object in " + methodName + "() call" );
		}
		
		Method method = findMethod( obj.getClass() );
		try {
			Object result = method.invoke( obj, args );
			return result;
		} 
		catch (Exception e) {
			throw new ExpressionException( "Error evaluating ." + methodName + "(): " + e );
		}
	}
	
	Method findMethod( Class<?> clazz )
	{
		// array of parameter types:
		Class<?>[] argtypes = new Class[ args.length ];
		for (int i=0;  i< argtypes.length;  i++) {
			argtypes[i] = (args[i] != null? args[i].getClass(): String.class);
		}
		
		Method method;

		// first, look method in interfaces (if any) to avoid attempt of direct call
		// to public method of private implementation class (see e.g. Arrays.asList, 
		// Collections.synchronizedCollection). Such attempt will throw IllegalAccessException:
		
		Class<?> clazz1 = clazz;
		while (clazz1 != null) {
			Class<?>[] classes = clazz1.getInterfaces();
			for (Class<?> c: classes) {
				try {
					method = c.getMethod( methodName, argtypes );
					return method;
				} 
				catch (Exception e) {
				}
			}
			clazz1 = clazz1.getSuperclass();
		}
		
		// no such method in interfaces, try direct call:
		try {
			method =clazz.getMethod( methodName, argtypes );
		} 
		catch (Exception e) {
			throw new ExpressionException( "No '" + methodName + "' method in " + clazz );
		}
		return method;
	}
	
	
	@Override
	public Object walk( ExpressionWalker walker ) 
	{
		return walker.visit( this, expr.walk(walker), methodName, Arrays.asList(args) );
	}
}

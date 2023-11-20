package com.trustwave.dbpjobservice.parameters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;







import com.googlecode.sarasvati.env.AttributeConverters;
import com.trustwave.dbpjobservice.expr.BooleanExpr;
import com.trustwave.dbpjobservice.expr.Expression;
import com.trustwave.dbpjobservice.expr.Parser;
import com.trustwave.dbpjobservice.impl.Messages;
import com.trustwave.dbpjobservice.impl.exceptions.WorkflowException;
import com.trustwave.dbpjobservice.workflow.api.action.valueconverters.ValueConverterFactory;

public class ExpressionEvaluator
{
	private static Logger logger = LogManager.getLogger( ExpressionEvaluator.class );
	
	private String sResult;
	private Object objResult;
	boolean ignoreExpressionErrors;
	
	public ExpressionEvaluator() {
	}

	public boolean isIgnoreExpressionErrors() {
		return ignoreExpressionErrors;
	}

	public void setIgnoreExpressionErrors(boolean ignoreExpressionErrors) {
		this.ignoreExpressionErrors = ignoreExpressionErrors;
	}

	@SuppressWarnings("unchecked")
	public <T> T evaluate( String exprString, TypedEnvironment env, Class<T> targetClass )
	{
		ExpressionFinder finder = new ExpressionFinder( exprString );
		
		while (finder.findNext()) {
			addString( finder.getBeginStr() );
			if (finder.getExpr() != null) {
				Object obj = evaluateExpr( finder.getExpr(), env );
				addObj( obj );
			}
		}
		if (objResult != null) {
			if (!targetClass.isAssignableFrom( objResult.getClass() )) {
				if (String.class.equals(targetClass)) {
					// special case, anything can be converted to string:
					objResult = objResult.toString();
				}
				else if (targetClass.isPrimitive()) {
					return AttributeConverters.stringToObject( objResult.toString(), targetClass );
				}
				else {
					throw new WorkflowException( Messages.getString("param.expr.type.incompatible", exprString, objResult.getClass(), targetClass.getName()) );
				}
			}
			return (T)objResult;
		}
		return AttributeConverters.stringToObject( sResult, targetClass );
	}
	
	private void addString( String s )
	{
		if (s == null) {
			return;
		}
		if (objResult != null) {
			sResult = objToString( objResult );
			objResult = null;
		}
		if (sResult != null) {
			sResult += s;
		}
		else {
			sResult = s;
		}
	}
	
	private void addObj( Object obj )
	{
		if (sResult != null || objResult != null) {
			addString( objToString( obj ) );
		}
		else {
			objResult = obj;
		}
	}
	
	private static String objToString( Object obj )
	{
		return ValueConverterFactory.objectShortString( obj );
	}
	

	public Object evaluateExpr( String exprStr, TypedEnvironment env ) 
	{
		Object obj;
		try {
			Expression expr = new Parser( exprStr ).getExpression();
			ExpressionContext ctx = new ExpressionContext( env, true );
			obj = expr.evaluate( ctx );
		} 
		catch (RuntimeException e) {
			if (!ignoreExpressionErrors) {
				throw new WorkflowException( Messages.getString("param.expr.error", exprStr, e.getMessage()), e );
			}
			obj = "#{" + exprStr + "}";
			logger.info( "Ignoring error: '" + e.getMessage() + "' in '" + obj );
		}
		return obj;
	}

	public boolean evaluateCondition( String exprString, TypedEnvironment env )
	{
		ExpressionFinder finder = new ExpressionFinder( exprString );
		finder.findNext();
		String exprStr = (finder.getExpr() != null? finder.getExpr(): finder.getBeginStr());

		if (finder.getExpr() != null && finder.getBeginStr() != null) {
			throw new RuntimeException(Messages.getString("param.expr.condition.invalid", exprString)); 
		}
		if (finder.findNext()) {
			throw new RuntimeException(Messages.getString("param.expr.condition.invalid", exprString));
		}

		BooleanExpr bexpr = new Parser( exprStr ).getBooleanExpression();
		try {
			ExpressionContext ctx = new ExpressionContext( env, true );
			return bexpr.isTrue( ctx );
		} 
		catch (RuntimeException e) {
			if (!ignoreExpressionErrors) {
				throw new RuntimeException(Messages.getString("param.expr.error", exprString, e.getMessage()), e);
			}
			logger.debug( "Ignoring error in condition: " + e + ", returning 'false'" );
			return false;
		}
	}
	
}

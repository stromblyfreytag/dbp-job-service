package com.trustwave.dbpjobservice.expr;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Expression 
{
	private static Logger logger = LogManager.getLogger( Expression.class );
	private String name;

	public String getName() {
		return name;
	}

	protected void setName(String name) {
		this.name = name;
	}
	
	public abstract Object evaluate( ExpressionContext ctx );
	
	public abstract Object walk( ExpressionWalker walker );
	
	public Collection<String> validateNames( ExpressionContext ctx )
	{
		NameValidationWalker walker = new NameValidationWalker( ctx );
		walk( walker );
		return walker.getAbsentNames();
	}
	

	protected Long asLong( ExpressionContext ctx ) {
		return toLongNumber( evaluate(ctx) );
	}
	
	protected Long toLongNumber( Object value )
	{
		if (value instanceof Long) {
			return (Long)value;
		}
		if (value instanceof Number) {
			return Long.valueOf( ((Number)value).longValue() );
		}
		if (value instanceof String) {
			return Long.valueOf( (String)value );
		}
		throw new ExpressionException( "Cannot convert to number: " + value );
	}
	
	protected void debug( Object ... msg )
	{
		if (logger.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder();
			for (Object m: msg) {
				sb.append( m );
			}
			logger.debug( sb.toString() );
		}
	}
}

package com.trustwave.dbpjobservice.expr;

import java.util.Collection;

public class EmptyExpr extends BooleanExpr
{
	private String property;
	
	public EmptyExpr(String property) {
		this.property = property;
		this.setName( "empty" );
	}
	
	// exact logic of operation:
	// not exists(X) || X == null || X.trim().isEmpty()

	@Override
	public boolean isTrue(ExpressionContext ctx) 
	{
		if (!ctx.hasProperty(property)) {
			debug( "empty ", property, ": (absent) ", true );
			return true;
		}
		Object value = ctx.getProperty( property );
		if (value == null) {
			debug( "empty ", property, ": (null) ", true );
			return true;
		}
		if (value instanceof Collection<?>) {
			debug( "empty ", property, ": ([]) ", true );
			return ((Collection<?>)value).size() == 0;
		}
		boolean yes = value.toString().trim().isEmpty();
		debug( "empty ", property, ": (", value, ") ", true );
		return yes;
	}

	@Override
	public Object walk( ExpressionWalker walker ) 
	{
		return walker.visit( this, property );
	}
}

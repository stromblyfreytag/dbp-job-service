package com.trustwave.dbpjobservice.expr;

public class PropertyExpr extends Expression 
{
	private String property;
	
	public PropertyExpr(String property) {
		this.property = property;
		this.setName( "Property" );
	}

	@Override
	public Object evaluate(ExpressionContext ctx) {
		Object value = ctx.getProperty( property );
		debug( "Property: ", property, "=", value );
		return value;
	}
	
	@Override
	public Object walk( ExpressionWalker walker ) 
	{
		return walker.visit( this , property );
	}
}

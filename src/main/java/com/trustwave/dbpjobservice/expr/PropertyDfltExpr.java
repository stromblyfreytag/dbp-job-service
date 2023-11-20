package com.trustwave.dbpjobservice.expr;

public class PropertyDfltExpr extends Expression 
{
	private String property;
	private Expression dflt;
	
	public PropertyDfltExpr(String property, Expression dflt) {
		this.property = property;
		this.dflt = dflt;
		setName( "?:" );
	}

	@Override
	public Object evaluate(ExpressionContext ctx) {
		Object value;
		boolean exists = false;
		if (ctx.hasProperty( property )) {
			value = ctx.getProperty( property );
			exists = true;
		}
		else {
			value = dflt.evaluate(ctx);
		}
		debug( "PropertyDflt: ", property, ", exists=", exists, ": ", value );
		return value;
	}

	@Override
	public Object walk( ExpressionWalker walker ) 
	{
		return walker.visit( this, property, dflt.walk( walker ) );
	}
}

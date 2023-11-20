package com.trustwave.dbpjobservice.expr;

public class UminusExpr extends Expression 
{
	private Expression expr;
	
	public UminusExpr( Expression expr ) {
		this.expr = expr;
		setName( "-" );
	}

	@Override
	public Object evaluate(ExpressionContext ctx) {
		return Long.valueOf( -expr.asLong(ctx).longValue() );
	}

	@Override
	public Object walk( ExpressionWalker walker ) 
	{
		return walker.visit( this, expr.walk( walker ) );
	}
}

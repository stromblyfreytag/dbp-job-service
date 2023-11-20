package com.trustwave.dbpjobservice.expr;

public class ExistsExpr extends BooleanExpr
{
	private String property;
	
	public ExistsExpr(String property) {
		this.property = property;
		this.setName( "exists" );
	}

	@Override
	public boolean isTrue(ExpressionContext ctx) {
		boolean yes = ctx.hasProperty( property );
		debug( "exists ", property, ": ", yes );
		return yes;
	}
	
	@Override
	public Object walk( ExpressionWalker walker ) 
	{
		return walker.visit( this, property );
	}
}

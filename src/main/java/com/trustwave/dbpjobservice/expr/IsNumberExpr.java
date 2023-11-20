package com.trustwave.dbpjobservice.expr;

public class IsNumberExpr extends BooleanExpr
{
	private String property;
	
	public IsNumberExpr(String property) {
		this.property = property;
		this.setName( "isNumber" );
	}

	@Override
	public boolean isTrue(ExpressionContext ctx) {
		if (ctx.hasProperty(property)) {
			try {
				Object value = ctx.getProperty( property );
				toLongNumber( value ); // this will throw if not a number
				debug( "IsNumber ", property, ": ", true );
				return true;
			} 
			catch (Exception e) {
			}
		}
		debug( "IsNumber ", property, ": ", false );
		return false;
	}

	@Override
	public Object walk( ExpressionWalker walker ) 
	{
		return walker.visit( this, property );
	}
}

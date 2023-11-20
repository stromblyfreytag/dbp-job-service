package com.trustwave.dbpjobservice.expr;

public class BooleanConstExpr extends BooleanExpr 
{
	private boolean value;
	
	public BooleanConstExpr(boolean value) {
		this.value = value;
		this.setName( "Constant" );
	}
	
	public boolean isTrue( ExpressionContext ctx )
	{
		return value;
	}

	@Override
	public Object walk( ExpressionWalker walker ) 
	{
		return walker.visit( this, new Boolean( value ) );
	}
}

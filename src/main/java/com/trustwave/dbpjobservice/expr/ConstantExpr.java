package com.trustwave.dbpjobservice.expr;

public class ConstantExpr extends Expression 
{
	private Object constant;
	
	public ConstantExpr( String s ) {
		this.constant = s;
		this.setName( "Constant" );
	}

	public ConstantExpr( long l ) {
		this.constant = Long.valueOf( l );
		this.setName( "Constant" );
	}
	
	@Override
	public Object evaluate(ExpressionContext ctx) {
		return constant;
	}

	@Override
	public Object walk( ExpressionWalker walker ) 
	{
		return walker.visit( this, constant );
	}
}

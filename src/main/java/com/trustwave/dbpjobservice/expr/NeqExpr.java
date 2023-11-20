package com.trustwave.dbpjobservice.expr;

public class NeqExpr extends EqExpr
{
	public NeqExpr( Expression left, Expression right ) {
		super( left, right );
		setName( "!=" );
	}
	
	@Override
	public boolean isTrue( ExpressionContext ctx ) {
		boolean yes = !super.isTrue(ctx);
		debug( "!=: ", yes );
		return yes;
	}
}

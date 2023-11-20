package com.trustwave.dbpjobservice.expr;

public class LtExpr extends BooleanExpr2
{
	public LtExpr( Expression left, Expression right ) {
		super( "<", left, right );
	}
	
	@Override
	public boolean isTrue( ExpressionContext ctx ) {
		Long a = getLeft().asLong(ctx);
		Long b = getRight().asLong(ctx);
		boolean yes = (a < b);
		debug( a, " < ", b,  ": ", yes );
		return yes;
	}
}

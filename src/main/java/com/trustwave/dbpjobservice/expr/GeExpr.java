package com.trustwave.dbpjobservice.expr;

public class GeExpr extends BooleanExpr2
{
	public GeExpr( Expression left, Expression right ) {
		super( ">=", left, right );
	}
	
	@Override
	public boolean isTrue( ExpressionContext ctx ) {
		Long a = getLeft().asLong(ctx);
		Long b = getRight().asLong(ctx);
		boolean yes = (a >= b);
		debug( a, " >= ", b,  ": ", yes );
		return yes;
	}
}

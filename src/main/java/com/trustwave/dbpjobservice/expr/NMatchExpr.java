package com.trustwave.dbpjobservice.expr;

public class NMatchExpr extends MatchExpr
{
	public NMatchExpr( Expression left, Expression right ) {
		super( left, right );
		setName( "!~" );
	}
	
	@Override
	public boolean isTrue( ExpressionContext ctx )
	{
		boolean yes = !super.isTrue(ctx);
		debug( "!~: ", yes );
		return yes;
 	}
}

package com.trustwave.dbpjobservice.expr;

public class AndExpr extends BooleanExpr
{
	private BooleanExpr left;
	private BooleanExpr right;

	public AndExpr( BooleanExpr left, BooleanExpr right ) {
		this.left = left;
		this.right = right;
		this.setName( "AND" );
	}
	
	@Override
	public boolean isTrue( ExpressionContext ctx )
	{
		if (!left.isTrue(ctx)) {
			debug( "AND: ", false );
			return false;
		}
		boolean yes = right.isTrue(ctx);
		debug( "AND: ", yes );
		return yes;
	}

	@Override
	public Object walk( ExpressionWalker walker ) 
	{
		return walker.visit( this, left.walk(walker), right.walk(walker) );
	}
}

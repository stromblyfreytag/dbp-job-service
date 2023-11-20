package com.trustwave.dbpjobservice.expr;

public class OrExpr extends BooleanExpr
{
	private BooleanExpr left;
	private BooleanExpr right;

	public OrExpr( BooleanExpr left, BooleanExpr right ) {
		this.left = left;
		this.right = right;
		this.setName( "OR" );
	}
	
	@Override
	public boolean isTrue( ExpressionContext ctx )
	{
		if (left.isTrue(ctx)) {
			debug( "OR: ", true );
			return true;
		}
		boolean yes = right.isTrue(ctx);
		debug( "OR: ", yes );
		return yes;
	}

	@Override
	public Object walk( ExpressionWalker walker ) 
	{
		return walker.visit( this, left.walk(walker), right.walk(walker) );
	}
}

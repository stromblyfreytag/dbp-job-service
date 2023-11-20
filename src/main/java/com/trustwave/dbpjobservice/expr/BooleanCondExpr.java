package com.trustwave.dbpjobservice.expr;

public class BooleanCondExpr extends BooleanExpr
{
	private BooleanExpr condition;
	private BooleanExpr left;
	private BooleanExpr right;


	public BooleanCondExpr( BooleanExpr condition,
			                BooleanExpr left, BooleanExpr right) {
		this.condition = condition;
		this.left = left;
		this.right = right;
		this.setName( "Conditional" );
	}

	@Override
	public boolean isTrue(ExpressionContext ctx) 
	{
		if (condition.isTrue(ctx)) {
			return left.isTrue(ctx);
		}
		return right.isTrue(ctx);
	}
	
	@Override
	public Object walk( ExpressionWalker walker ) 
	{
		return walker.visit( this, condition.walk(walker),
				                       left.walk(walker), right.walk(walker) );
	}
}

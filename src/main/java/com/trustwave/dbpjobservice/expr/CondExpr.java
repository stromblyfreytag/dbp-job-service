package com.trustwave.dbpjobservice.expr;

public class CondExpr extends Expression
{
	private BooleanExpr condition;
	private Expression left;
	private Expression right;


	public CondExpr( BooleanExpr condition, Expression left, Expression right) {
		this.condition = condition;
		this.left = left;
		this.right = right;
		this.setName( "Conditional" );
	}

	@Override
	public Object evaluate(ExpressionContext ctx) 
	{
		if (condition.isTrue(ctx)) {
			return left.evaluate(ctx);
		}
		return right.evaluate(ctx);
	}

	@Override
	public Object walk( ExpressionWalker walker ) 
	{
		return walker.visit( this, condition.walk(walker),
				                       left.walk(walker), right.walk(walker) );
	}
}

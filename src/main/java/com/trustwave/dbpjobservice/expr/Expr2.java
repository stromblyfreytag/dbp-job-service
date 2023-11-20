package com.trustwave.dbpjobservice.expr;

public abstract class Expr2 extends Expression 
{
	private Expression left;
	private Expression right;
	
	public Expr2( String name, Expression left, Expression right) {
		this.left = left;
		this.right = right;
		setName(name);
	}

	public Expression getLeft() {
		return left;
	}

	public Expression getRight() {
		return right;
	}

	@Override
	public Object walk( ExpressionWalker walker ) 
	{
		return walker.visit( this, left.walk(walker), right.walk(walker) );
	}
}

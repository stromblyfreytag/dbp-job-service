package com.trustwave.dbpjobservice.expr;

public abstract class BooleanExpr extends Expression {

	@Override
	public Object evaluate(ExpressionContext ctx) 
	{
		return Boolean.valueOf( isTrue(ctx) );
	}
	
	public abstract boolean isTrue( ExpressionContext ctx );

}

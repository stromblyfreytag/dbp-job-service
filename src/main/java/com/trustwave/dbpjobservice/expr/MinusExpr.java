package com.trustwave.dbpjobservice.expr;

public class MinusExpr extends Expr2 
{

	public MinusExpr( Expression left, Expression right ) {
		super( "-", left, right);
	}

	@Override
	public Object evaluate(ExpressionContext ctx) 
	{
		Long a = getLeft().asLong(ctx);
		Long b = getRight().asLong(ctx);
		return Long.valueOf( a - b  );
	}

}

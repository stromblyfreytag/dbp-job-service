package com.trustwave.dbpjobservice.expr;

public class PlusExpr extends Expr2 
{

	public PlusExpr(Expression left, Expression right) {
		super( "+", left, right);
	}

	@Override
	public Object evaluate(ExpressionContext ctx) 
	{
		Object a = getLeft().evaluate(ctx);
		Object b = getRight().evaluate(ctx);
		if (a instanceof Number || b instanceof Number) {
			Long na = toLongNumber( a );
			Long nb = toLongNumber( b );
			return Long.valueOf( na.longValue() + nb.longValue() );
		}
		if (a instanceof String || b instanceof String) {
			return  a.toString() + b.toString();
		}
		throw new ExpressionException( "Operation '+' is not applicable to '"
				   + a + "' and '" + b + "'" );
	}

}

package com.trustwave.dbpjobservice.expr;

import java.util.Collection;

public class EqExpr extends BooleanExpr2
{
	public EqExpr( Expression left, Expression right ) {
		super( "==", left, right );
	}
	
	@Override
	public boolean isTrue( ExpressionContext ctx )
	{
		Object a = getLeft().evaluate( ctx );
		Object b = getRight().evaluate( ctx );
		
		boolean yes = isTrue( ctx, a, b );
		debug( a, "==", b, ": ", yes );
		return yes;
 	}
	
	public boolean isTrue( ExpressionContext ctx, Object a, Object b )
	{
		if (eq( a, b, ctx.caseInsensitive() )) {
			return true;
		}
		if (a instanceof Collection<?>) {
			for (Object a0: (Collection<?>)a) {
				if (eq( a0, b,  ctx.caseInsensitive() )) {
					return true;
				}
			}
		}
		if (b instanceof Collection<?>) {
			for (Object b0: (Collection<?>)b) {
				if (eq( a, b0,  ctx.caseInsensitive() )) {
					return true;
				}
			}
		}
		return false;
	}
	
	static boolean eq( Object a, Object b, boolean caseInsensitive )
	{
		if (a == null) {
			return b == null;
		}
		if (a.equals(b)) {
			return true;
		}
		String sa = String.valueOf(a);
		String sb = String.valueOf(b);
		return (caseInsensitive? sa.equalsIgnoreCase( sb ): sa.equals( sb )); 
	}
}

package com.trustwave.dbpjobservice.expr;

import java.util.Collection;
import java.util.List;

/**
 * evaluates expressions like:
 *    expression IN (value1, value2, ...)
 */
public class InExpr extends BooleanExpr
{
	private Expression   expr;
	private List<Object> list;

	public InExpr( Expression expr, List<Object> list ) {
		this.expr = expr;
		this.list = list;
		setName( "IN" );
	}
	
	@Override
	public boolean isTrue( ExpressionContext ctx ) 
	{
		Object a = expr.evaluate( ctx );

		if (a instanceof Collection<?>) {
			Collection<?> collection = (Collection<?>)a;
			for (Object o: collection) {
				if (contains( list, o, ctx.caseInsensitive() )) {
					debug( a, " IN ", list, ": ", true );
					return true;
				}
			}
			debug( a, " IN ", list, ": ", false );
			return false;
		}
		boolean yes = contains( list, a, ctx.caseInsensitive() );
		debug( a, " IN ", list, ": ", yes );
		return yes;
	}
	
	static boolean contains( List<Object> list, Object obj, boolean caseInsensitive )
	{
		for (Object elem: list) {
			if (EqExpr.eq( elem, obj, caseInsensitive)) {
				return true;
			}
		}
		return false;
	}

	public Expression getExpr() {
		return expr;
	}

	public List<Object> getList() {
		return list;
	}

	@Override
	public Object walk( ExpressionWalker walker ) 
	{
		return walker.visit( this, expr.walk(walker), list );
	}
}

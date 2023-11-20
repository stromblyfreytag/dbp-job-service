package com.trustwave.dbpjobservice.expr;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MatchExpr extends BooleanExpr2
{
	public MatchExpr( Expression left, Expression right ) {
		super( "~", left, right );
	}
	
	@Override
	public boolean isTrue( ExpressionContext ctx )
	{
		Object a = getLeft().evaluate( ctx );
		Object b = getRight().evaluate( ctx );
		
		boolean yes = false;
		if (a instanceof Collection<?>) {
			for (Object a0: (Collection<?>)a) {
				if ((yes = matches( a0, b, ctx.caseInsensitive() )) == true) {
					break;
				}
			}
		}
		else {
			yes = matches( a, b, ctx.caseInsensitive() );
		}
		debug( a, "~", b, ": ", yes );
		return yes;
 	}
	
	public boolean matches( Object a, Object b, boolean caseInsensitive )
	{
		if (a == null || b == null) {
			return false;
		}
		String sa = a.toString();
		
		String regex = b.toString();
		if (!regex.contains(".*")) {
			if (!regex.startsWith("^")) {
				regex = ".*" + regex;
			}
			if (!regex.endsWith("$")) {
				regex = regex + ".*";
			}
		}
		int flags = caseInsensitive? Pattern.CASE_INSENSITIVE: 0;
        Pattern p = Pattern.compile( regex, flags );
        Matcher m = p.matcher( sa );
        return m.matches();
	}
}

package com.trustwave.dbpjobservice.expr;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LikeExpr extends BooleanExpr2
{
	public LikeExpr( Expression left, Expression right ) {
		super( "like", left, right );
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
		debug( a, "like", b, ": ", yes );
		return yes;
 	}
	
	public boolean matches( Object a, Object b, boolean caseInsensitive )
	{
		if (a == null || b == null) {
			return false;
		}
		String sa = a.toString();
		
		String likeStr = b.toString();
		if (!likeStr.contains("%")) {
				likeStr = "%" + likeStr + "%";
		}
		// escape everything that may be treated as a start of regexp sequence:
		String regex = likeStr.replaceAll( "([.*\\\\^$+{\\[])", "\\\\$1" );
		// Now, treat '%' as any-substring regex:
		regex = regex.replaceAll( "%", ".*" );
		// special treatment for [%] - sql-escaped literal % in like pattern:
		regex = regex.replaceAll( "\\\\\\[\\.\\*]", "%" );
		
		int flags = caseInsensitive? Pattern.CASE_INSENSITIVE: 0;
        Pattern p = Pattern.compile( regex, flags );
        Matcher m = p.matcher( sa );
        return m.matches();
	}
	
}

package com.trustwave.dbpjobservice.expr;

import java.util.HashSet;

/**
 *    This expression walker collects all invalid names used in expression -
 * those that are not present in context and are not protected with operations
 * 'exists', 'empty' and 'isNumber' (operations that call hasProperty() method
 * on context). 
 *  
 * @author vlad
 */
public class NameValidationWalker implements ExpressionWalker 
{
	private ExpressionContext ctx;
	private HashSet<String> safeNames = new HashSet<String>();
	private HashSet<String> absentNames = new HashSet<String>();
	
	public NameValidationWalker( ExpressionContext ctx ) 
	{
		this.ctx = ctx;
	}

	@Override
	public Object visit(Expression expression, Object... arg) 
	{
		if (expression instanceof PropertyExpr) {
			String name = (String)arg[0];
			if (!safeNames.contains( name ) && !ctx.hasProperty(name)) {
				absentNames.add( name );
			}
		}
		else if ((expression instanceof ExistsExpr)
			  || (expression instanceof EmptyExpr)
			  || (expression instanceof IsNumberExpr)) {
			// these operations call hasProperty() on context,
			// so names are kind-of-safe: 
			String name = (String)arg[0];
			safeNames.add( name );
		}
		return null;
	}

	public HashSet<String> getAbsentNames() 
	{
		return absentNames;
	}
}

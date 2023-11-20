package com.trustwave.dbpjobservice.parameters;

import com.trustwave.dbpjobservice.impl.Messages;
import com.trustwave.dbpjobservice.impl.exceptions.WorkflowException;
import com.trustwave.dbpjobservice.workflow.api.util.Bean;

public class ExpressionContext implements com.trustwave.dbpjobservice.expr.ExpressionContext
{
	private TypedEnvironment env;
	private boolean caseInsensitive;

	public ExpressionContext( TypedEnvironment tenv, boolean caseInsensitive ) 
	{
		this.env = tenv;
		this.caseInsensitive = caseInsensitive;
	}

	@Override
	public boolean hasProperty(String property) 
	{
		try {
			getProperty( property );
			return true;
		} 
		catch (Exception e) {
		}
		return false;
	}

	@Override
	public Object getProperty( String property ) 
	{
		String objName = findLongestName( property );
		if (objName == null) {
			throw new WorkflowException(Messages.getString("workflow.error.token.prop.notFound", property));
		}
		Object obj = env.getAttribute( objName );
		if (objName.length() < property.length()) {
			String prop = property.substring( objName.length() + 1);
			Bean b = new Bean( obj );
			obj = b.getProperty( prop );
		}
		return obj;
	}

	@Override
	public boolean caseInsensitive() 
	{
		return caseInsensitive;
	}
	
	private String findLongestName( String dotExpr )
	{
		String name = dotExpr;
		while (name != null) {
			if (env.hasAttribute(name))
				break;
			int k = name.lastIndexOf( '.' );
			if (k <= 0) {
				// no more dots or name starts with dot (k==0)
				// Anyway, we failed to find name in context
				// (empty string is not valid name), give up:
				name = null;
				break;
			}
			name = name.substring( 0, k );
		}
		return name;
	}
}

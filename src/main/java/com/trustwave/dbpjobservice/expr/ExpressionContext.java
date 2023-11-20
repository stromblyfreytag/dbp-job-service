package com.trustwave.dbpjobservice.expr;

public interface ExpressionContext 
{
	/** Should return <code>true</code> if property exists in context,
	 * <code>false</code> otherwise.
	 */
	public boolean hasProperty( String property );

	/** Get property value by name. Should throw if property does not exist 
	 */
	public Object getProperty( String property );
	
	/** If <code>true</code>, all comparisons in expressions will be
	 *  case-insensitive.
	 */
	public boolean caseInsensitive();
}

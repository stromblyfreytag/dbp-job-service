package com.trustwave.dbpjobservice.parameters;

class ExpressionFinder 
{
	private String rest;
	private String beginStr;
	private String expr;
	
	public ExpressionFinder( String rest )
	{
		this.rest = rest;
	}
	
	/** <p>Find next 'expression string' in the input line :
     *  ....#{.....}..#{..}...
     * where expression parts start with '#{' or '${', and end with '}'.
     * </p>
     * <p> Beginning non-expression part and expression part then can be
     * retrieved with getBeginStr() and getExpr() correspondingly;
     *  either may return <code>null</code>.
     * </p>  
     * @return true if there is more data in the input line, false otherwise.
     */
	boolean findNext()
	{
		if (rest == null) {
			return false;
		}
		int i1 = rest.indexOf( "#{" );
		int i2 = rest.indexOf( "${" );
		int exprBegin = (i1 >= 0 && i2 >= 0? (i1 < i2? i1: i2)
				                           : (i1 < 0? i2: i1) );
		int exprEnd = (exprBegin >= 0? rest.indexOf( "}", exprBegin+2 ): -1);
		if (exprBegin < 0) {
			beginStr = rest;
			expr = null;
			rest = null;
			return true;
		}
		beginStr = (exprBegin == 0? null: rest.substring( 0, exprBegin ));
		expr = rest.substring( exprBegin+2, exprEnd );
		rest = (exprEnd+1 < rest.length()? rest.substring( exprEnd+1 ): null);
		return true;
	}

	public String getBeginStr() {
		return beginStr;
	}

	public String getExpr() {
		return expr;
	}
}

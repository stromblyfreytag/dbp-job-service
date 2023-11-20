package com.trustwave.dbpjobservice.expr;

public class NotExpr extends BooleanExpr
{
	private BooleanExpr expr;
	
	public NotExpr( BooleanExpr expr ) {
		this.expr = expr;
		this.setName( "NOT" );
	}
	
	@Override
	public boolean isTrue( ExpressionContext ctx )
	{
		boolean yes = !expr.isTrue(ctx);
		debug( "NOT: ", yes );
		return yes;
	}

	@Override
	public Object walk( ExpressionWalker walker ) 
	{
		return walker.visit( this, expr.walk(walker) );
	}
}

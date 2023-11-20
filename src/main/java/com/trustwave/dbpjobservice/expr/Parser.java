package com.trustwave.dbpjobservice.expr;

import java.io.StringReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java_cup.runtime.Symbol;

public class Parser 
{
	private static Logger logger = LogManager.getLogger( Parser.class );
	private String exprString;
	private Expression expr = null;
	
	public Parser(String expr) {
		logger.debug("Parser construct");
		this.exprString = expr;
	}
	
	public Expression getExpression()
	{
		if (expr == null) {
			logger.debug("not null parsing");
			expr = parse();
		}else {
			logger.debug(" null expression");
		}

		return expr;
	}
	
	public BooleanExpr getBooleanExpression() {
		logger.debug("getBooleanExpression");
		if (!(getExpression() instanceof BooleanExpr)) {
			throw new RuntimeException( "Not a Boolean expression: " + exprString );
		}
		logger.debug("getExpression");
		return (BooleanExpr)getExpression();
	}
	
	public Expression parse() 
	{
		com.trustwave.dbpjobservice.expr._parser parser = new com.trustwave.dbpjobservice.expr._parser();
		parser.input = exprString;
		parser.setScanner( new com.trustwave.dbpjobservice.expr.Lexer( new StringReader( exprString ) ) );
		try {
			Symbol sym = parser.parse();
			return (Expression)sym.value;
		} 
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		catch (Error e) {
			throw new RuntimeException(e.getMessage());
		}
	}
}

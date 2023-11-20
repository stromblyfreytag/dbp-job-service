package com.trustwave.dbpjobservice.expr;

public class ExpressionException extends RuntimeException 
{
	private static final long serialVersionUID = 1L;

	public ExpressionException() {
		super();
	}

	public ExpressionException(String message, Throwable cause) {
		super(message, cause);
	}

	public ExpressionException(String message) {
		super(message);
	}

	public ExpressionException(Throwable cause) {
		super(cause);
	}

}

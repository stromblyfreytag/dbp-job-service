package com.trustwave.dbpjobservice.parameters;

public class ParameterException extends RuntimeException 
{
	private static final long serialVersionUID = 1L;

	public ParameterException(String message, Throwable cause) {
		super(message, cause);
	}

	public ParameterException(String message) {
		super(message);
	}
}

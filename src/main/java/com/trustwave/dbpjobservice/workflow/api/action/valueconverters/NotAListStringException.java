package com.trustwave.dbpjobservice.workflow.api.action.valueconverters;

public class NotAListStringException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NotAListStringException() {
    }

    public NotAListStringException(String message) {
        super(message);
    }
}

package com.policyserve.exception;

public class ProcedureExecutionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ProcedureExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}

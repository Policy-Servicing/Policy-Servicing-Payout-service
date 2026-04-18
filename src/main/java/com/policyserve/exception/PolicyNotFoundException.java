package com.policyserve.exception;

public class PolicyNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PolicyNotFoundException(String policyNo, String requestId) {
        super("No policy found for policyNo=" + policyNo + " and requestId=" + requestId);
    }
}

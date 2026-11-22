package com.playtomic.tests.wallet.domain.exception;

public class TopUpNotFoundException extends RuntimeException {

    public TopUpNotFoundException() {
        super("Top-up not found");
    }
}

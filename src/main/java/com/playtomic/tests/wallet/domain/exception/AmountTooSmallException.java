package com.playtomic.tests.wallet.domain.exception;

public class AmountTooSmallException extends RuntimeException {

    public AmountTooSmallException(String message) {
        super(message);
    }
}

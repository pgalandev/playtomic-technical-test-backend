package com.playtomic.tests.wallet.domain.exception;

public class InvalidTopUpRequestException extends RuntimeException {

    public InvalidTopUpRequestException(String message) {
        super(message);
    }
}

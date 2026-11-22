package com.playtomic.tests.wallet.domain.exception;

public class WalletAlreadyExistsException extends RuntimeException {

    public WalletAlreadyExistsException(String userId) {
        super("User " + userId + " already has a wallet");
    }
}

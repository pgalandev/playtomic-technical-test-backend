package com.playtomic.tests.wallet.domain.exception;

public class WalletNotFoundException extends RuntimeException {

    public WalletNotFoundException() {
        super("Wallet not found");
    }
}

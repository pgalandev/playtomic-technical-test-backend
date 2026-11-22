package com.playtomic.tests.wallet.domain.dto.wallet;

import com.playtomic.tests.wallet.domain.entity.Wallet;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletResponse(String id, Money balance, Instant createdAt, Instant updatedAt) {

    public record Money(String amount, String currency) {
        static Money of(BigDecimal amount, String currency) {
            return new Money(amount.setScale(2).toPlainString(), currency);
        }
    }

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                Money.of(wallet.getBalance(), wallet.getCurrency()),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt());
    }
}

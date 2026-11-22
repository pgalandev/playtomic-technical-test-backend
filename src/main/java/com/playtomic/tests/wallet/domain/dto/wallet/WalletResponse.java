package com.playtomic.tests.wallet.domain.dto.wallet;

import com.playtomic.tests.wallet.domain.Money;
import com.playtomic.tests.wallet.domain.entity.Wallet;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletResponse(String id, Money balance, Instant createdAt, Instant updatedAt) {

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                Money.of(wallet.getBalance(), wallet.getCurrency()),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt());
    }
}

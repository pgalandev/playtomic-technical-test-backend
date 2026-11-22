package com.playtomic.tests.wallet.domain;

import com.playtomic.tests.wallet.domain.dto.wallet.WalletResponse;

import java.math.BigDecimal;

public record Money(String amount, String currency) {
    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount.setScale(2).toPlainString(), currency);
    }
}

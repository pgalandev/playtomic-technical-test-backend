package com.playtomic.tests.wallet.domain.entity;

import org.springframework.data.annotation.TypeAlias;

@TypeAlias("TOP_UP")
public record TopUpData(String stripePaymentId, String cardLast4) implements TransactionData {
}

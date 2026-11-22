package com.playtomic.tests.wallet.domain.dto.wallet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.playtomic.tests.wallet.domain.Money;
import com.playtomic.tests.wallet.domain.enums.FailureReason;
import com.playtomic.tests.wallet.domain.entity.TopUpData;
import com.playtomic.tests.wallet.domain.entity.Transaction;
import com.playtomic.tests.wallet.domain.enums.TransactionStatus;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TopUpResponse(String id, String walletId, TransactionStatus status, FailureReason failureReason,
                            Money amount, String cardLast4, Instant createdAt) {

    public static TopUpResponse from(Transaction transaction) {
        TopUpData data = (TopUpData) transaction.getData();
        return new TopUpResponse(
                transaction.getId(),
                transaction.getWalletId(),
                transaction.getStatus(),
                transaction.getFailureReason(),
                Money.of(transaction.getAmount(), transaction.getCurrency()),
                data.cardLast4(),
                transaction.getCreatedAt());
    }
}

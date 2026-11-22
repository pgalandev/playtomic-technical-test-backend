package com.playtomic.tests.wallet.domain.entity;

import com.playtomic.tests.wallet.domain.enums.FailureReason;
import com.playtomic.tests.wallet.domain.enums.TransactionStatus;
import com.playtomic.tests.wallet.domain.enums.TransactionType;
import lombok.Getter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

import static com.playtomic.tests.wallet.helper.WalletManagerConstants.TRANSACTION_COLLECTION;

@Getter
@Document(TRANSACTION_COLLECTION)
@CompoundIndex(name = "wallet_idempotency_key", def = "{'walletId': 1, 'idempotencyKey': 1}", unique = true)
public class Transaction extends Entity {

    private String walletId;

    private String userId;

    private TransactionType type;

    private TransactionStatus status;

    private FailureReason failureReason;

    private BigDecimal amount;

    private String currency;

    private String idempotencyKey;

    private TransactionData data;

    public static Transaction pendingTopUp(String walletId, String userId, BigDecimal amount, String currency,
                                           String idempotencyKey, String cardLast4, Instant now) {
        Transaction transaction = new Transaction();
        transaction.walletId = walletId;
        transaction.userId = userId;
        transaction.type = TransactionType.TOP_UP;
        transaction.status = TransactionStatus.PENDING;
        transaction.amount = amount;
        transaction.currency = currency;
        transaction.idempotencyKey = idempotencyKey;
        transaction.data = new TopUpData(null, cardLast4);
        transaction.createdAt = now;
        transaction.updatedAt = now;
        transaction.createdBy = userId;
        transaction.updatedBy = userId;
        return transaction;
    }
}

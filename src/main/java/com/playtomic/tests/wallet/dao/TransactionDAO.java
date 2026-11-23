package com.playtomic.tests.wallet.dao;

import com.playtomic.tests.wallet.domain.entity.Transaction;
import com.playtomic.tests.wallet.domain.enums.FailureReason;

import java.time.Instant;
import java.util.Optional;

public interface TransactionDAO {

    Transaction insert(Transaction transaction);

    Optional<Transaction> findTopUp(String topUpId, String walletId);

    /**
     * Atomically marks the pending top-up as SUCCEEDED and increments the wallet balance by its amount.
     * Both writes commit together or not at all.
     *
     * @return the updated transaction
     * @throws IllegalStateException when the top-up is no longer PENDING or the wallet does not exist
     */
    Transaction credit(Transaction pendingTopUp, String stripePaymentId, Instant now);

    /**
     * Marks the pending top-up as FAILED with the given reason. The wallet is not touched.
     *
     * @throws IllegalStateException when the top-up is no longer PENDING
     */
    Transaction fail(Transaction pendingTopUp, FailureReason reason, Instant now);
}

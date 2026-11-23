package com.playtomic.tests.wallet.dao.impl;

import com.mongodb.client.result.UpdateResult;
import com.playtomic.tests.wallet.dao.TransactionDAO;
import com.playtomic.tests.wallet.domain.entity.Transaction;
import com.playtomic.tests.wallet.domain.enums.FailureReason;
import com.playtomic.tests.wallet.domain.enums.TransactionStatus;
import com.playtomic.tests.wallet.domain.enums.TransactionType;
import com.playtomic.tests.wallet.domain.entity.Wallet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class TransactionDAOImpl implements TransactionDAO {

    private static final String MONGO_ID = "_id";
    private static final String WALLET_ID = "walletId";
    private static final String TYPE = "type";
    private static final String STATUS = "status";
    private static final String FAILURE_REASON = "failureReason";
    private static final String STRIPE_PAYMENT_ID = "data.stripePaymentId";
    private static final String BALANCE = "balance";
    private static final String UPDATED_AT = "updatedAt";
    private static final String VERSION = "version";

    private final MongoTemplate mongoTemplate;

    @Override
    public Transaction insert(Transaction transaction) {
        return mongoTemplate.insert(transaction);
    }

    @Override
    public Optional<Transaction> findTopUp(String topUpId, String walletId) {
        Query query = Query.query(Criteria.where(MONGO_ID).is(topUpId)
                .and(WALLET_ID).is(walletId)
                .and(TYPE).is(TransactionType.TOP_UP));
        return Optional.ofNullable(mongoTemplate.findOne(query, Transaction.class));
    }

    @Override
    @Transactional(rollbackForClassName = "RuntimeException")
    public Transaction credit(Transaction pendingTopUp, String stripePaymentId, Instant now) {
        UpdateResult marked = mongoTemplate.updateFirst(
                Query.query(Criteria.where(MONGO_ID).is(pendingTopUp.getId())
                        .and(STATUS).is(TransactionStatus.PENDING)),
                new Update()
                        .set(STATUS, TransactionStatus.SUCCEEDED)
                        .set(STRIPE_PAYMENT_ID, stripePaymentId)
                        .set(UPDATED_AT, now)
                        .inc(VERSION, 1),
                Transaction.class);
        if (!isSingleUpdate(marked)) {
            log.error("Top-up {} is no longer PENDING, cannot mark it as SUCCEEDED", pendingTopUp.getId());
            throw new IllegalStateException("Top-up " + pendingTopUp.getId() + " is no longer PENDING");
        }

        UpdateResult credited = mongoTemplate.updateFirst(
                Query.query(Criteria.where(MONGO_ID).is(pendingTopUp.getWalletId())),
                new Update()
                        .inc(BALANCE, pendingTopUp.getAmount())
                        .set(UPDATED_AT, now)
                        .inc(VERSION, 1),
                Wallet.class);
        if (!isSingleUpdate(credited)) {
            log.error("Could not credit wallet {} for top-up {}", pendingTopUp.getWalletId(), pendingTopUp.getId());
            throw new IllegalStateException("Could not credit wallet " + pendingTopUp.getWalletId());
        }

        return mongoTemplate.findById(pendingTopUp.getId(), Transaction.class);
    }

    @Override
    public Transaction fail(Transaction pendingTopUp, FailureReason reason, Instant now) {
        UpdateResult marked = mongoTemplate.updateFirst(
                Query.query(Criteria.where(MONGO_ID).is(pendingTopUp.getId())
                        .and(STATUS).is(TransactionStatus.PENDING)),
                new Update()
                        .set(STATUS, TransactionStatus.FAILED)
                        .set(FAILURE_REASON, reason)
                        .set(UPDATED_AT, now)
                        .inc(VERSION, 1),
                Transaction.class);
        if (!isSingleUpdate(marked)) {
            log.error("Top-up {} is no longer PENDING, cannot mark it as FAILED", pendingTopUp.getId());
            throw new IllegalStateException("Top-up " + pendingTopUp.getId() + " is no longer PENDING");
        }
        return mongoTemplate.findById(pendingTopUp.getId(), Transaction.class);
    }

    private static boolean isSingleUpdate(UpdateResult result) {
        return result.getMatchedCount() == 1 && result.getModifiedCount() == 1;
    }
}

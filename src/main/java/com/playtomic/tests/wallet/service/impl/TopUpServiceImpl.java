package com.playtomic.tests.wallet.service.impl;

import com.playtomic.tests.wallet.client.rest.StripeRestClient;
import com.playtomic.tests.wallet.dao.TransactionDAO;
import com.playtomic.tests.wallet.domain.entity.Transaction;
import com.playtomic.tests.wallet.domain.entity.Wallet;
import com.playtomic.tests.wallet.domain.exception.TopUpNotFoundException;
import com.playtomic.tests.wallet.domain.external.stripe.Payment;
import com.playtomic.tests.wallet.service.TopUpService;
import com.playtomic.tests.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;

@Service
@RequiredArgsConstructor
@Slf4j
public class TopUpServiceImpl implements TopUpService {

    private final WalletService walletService;
    private final TransactionDAO transactionDAO;
    private final StripeRestClient stripeRestClient;
    private final Clock clock;

    @Override
    public Transaction topUp(String walletId, String userId, String idempotencyKey, BigDecimal amount,
                             String creditCardNumber) {
        log.info("Top up request received for walletId: {}, userId: {}, amount: {}, creditCardNumber: XXX-{}", walletId, userId, amount, last4(creditCardNumber));
        Wallet wallet = walletService.getOwned(walletId, userId);
        Transaction pending = transactionDAO.insert(Transaction.pendingTopUp(
                wallet.getId(), userId, amount, wallet.getCurrency(), idempotencyKey,
                last4(creditCardNumber), clock.instant()));

        Payment payment = stripeRestClient.charge(creditCardNumber, amount);
        Transaction transaction = transactionDAO.credit(pending, payment.getId(), clock.instant());
        log.info("Top up completed for walletId: {}, userId: {}, amount: {}, creditCardNumber: XXX-{}", walletId, userId, amount, last4(creditCardNumber));
        return transaction;
    }

    @Override
    public Transaction getTopUp(String walletId, String topUpId, String userId) {
        walletService.getOwned(walletId, userId);
        return transactionDAO.findTopUp(topUpId, walletId).orElseThrow(TopUpNotFoundException::new);
    }

    private static String last4(String creditCardNumber) {
        return creditCardNumber.substring(Math.max(0, creditCardNumber.length() - 4));
    }
}

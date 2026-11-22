package com.playtomic.tests.wallet.service;

import com.playtomic.tests.wallet.domain.entity.Transaction;

import java.math.BigDecimal;

public interface TopUpService {

    Transaction topUp(String walletId, String userId, String idempotencyKey, BigDecimal amount, String creditCardNumber);

    Transaction getTopUp(String walletId, String topUpId, String userId);
}

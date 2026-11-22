package com.playtomic.tests.wallet.service;

import com.playtomic.tests.wallet.domain.entity.Wallet;

public interface WalletService {

    Wallet create(String userId);

    Wallet getOwned(String walletId, String userId);
}

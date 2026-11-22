package com.playtomic.tests.wallet.dao;

import com.playtomic.tests.wallet.domain.entity.Wallet;

import java.util.Optional;

public interface WalletDAO {

    /**
     * @throws org.springframework.dao.DuplicateKeyException when the user already has a wallet
     */
    Wallet insert(Wallet wallet);

    Optional<Wallet> findById(String walletId);
}

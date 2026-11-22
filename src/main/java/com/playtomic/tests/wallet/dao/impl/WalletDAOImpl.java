package com.playtomic.tests.wallet.dao.impl;

import com.playtomic.tests.wallet.dao.WalletDAO;
import com.playtomic.tests.wallet.domain.entity.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WalletDAOImpl implements WalletDAO {

    private final MongoTemplate mongoTemplate;

    @Override
    public Wallet insert(Wallet wallet) {
        return mongoTemplate.insert(wallet);
    }

    @Override
    public Optional<Wallet> findById(String walletId) {
        return Optional.ofNullable(mongoTemplate.findById(walletId, Wallet.class));
    }
}

package com.playtomic.tests.wallet.service.impl;

import com.playtomic.tests.wallet.dao.WalletDAO;
import com.playtomic.tests.wallet.domain.entity.Wallet;
import com.playtomic.tests.wallet.domain.exception.WalletAlreadyExistsException;
import com.playtomic.tests.wallet.domain.exception.WalletNotFoundException;
import com.playtomic.tests.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletDAO walletDAO;
    private final Clock clock;

    @Override
    public Wallet getOwned(String walletId, String userId) {
        return walletDAO.findById(walletId)
                .filter(wallet -> wallet.getUserId().equals(userId))
                .orElseThrow(WalletNotFoundException::new);
    }

    public Wallet create(String userId) {
        try {
            return walletDAO.insert(Wallet.openFor(userId, clock.instant()));
        } catch (DuplicateKeyException e) {
            log.error("Wallet already exists for userId: {}", userId, e);
            throw new WalletAlreadyExistsException(userId);
        }
    }
}

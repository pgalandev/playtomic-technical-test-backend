package com.playtomic.tests.wallet.domain.entity;

import lombok.Getter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

import static com.playtomic.tests.wallet.helper.WalletManagerConstants.WALLET_COLLECTION;

@Getter
@Document(WALLET_COLLECTION)
public class Wallet extends Entity {

    public static final String CURRENCY = "EUR";

    @Indexed(unique = true)
    private String userId;

    private BigDecimal balance;

    private String currency;

    public static Wallet openFor(String userId, Instant now) {
        Wallet wallet = new Wallet();
        wallet.userId = userId;
        wallet.balance = new BigDecimal("0.00");
        wallet.currency = CURRENCY;
        wallet.createdAt = now;
        wallet.updatedAt = now;
        wallet.createdBy = userId;
        wallet.updatedBy = userId;
        return wallet;
    }
}

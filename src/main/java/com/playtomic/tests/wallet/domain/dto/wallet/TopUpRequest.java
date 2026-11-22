package com.playtomic.tests.wallet.domain.dto.wallet;

import com.playtomic.tests.wallet.domain.Money;

public record TopUpRequest(Money amount, String creditCardNumber) {

    @Override
    public String toString() {
        return "TopUpRequest[amount=" + amount + ", creditCardNumber=***]";
    }
}

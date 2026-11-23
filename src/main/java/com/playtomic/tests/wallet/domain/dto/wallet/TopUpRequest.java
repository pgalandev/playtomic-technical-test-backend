package com.playtomic.tests.wallet.domain.dto.wallet;

import com.playtomic.tests.wallet.domain.Money;
import com.playtomic.tests.wallet.domain.exception.InvalidTopUpRequestException;

import java.math.BigDecimal;

public record TopUpRequest(Money amount, String creditCardNumber) {

    /** Validates the payload and returns the amount as a positive number with at most 2 decimals. */
    public BigDecimal validatedAmount() {
        if (amount == null || amount.amount() == null) {
            throw new InvalidTopUpRequestException("amount is required");
        }
        if (!"EUR".equals(amount.currency())) {
            throw new InvalidTopUpRequestException("currency must be EUR");
        }
        if (creditCardNumber == null || creditCardNumber.isBlank()) {
            throw new InvalidTopUpRequestException("creditCardNumber is required");
        }
        BigDecimal value;
        try {
            value = new BigDecimal(amount.amount());
        } catch (NumberFormatException e) {
            throw new InvalidTopUpRequestException("amount must be a valid number");
        }
        if (value.signum() <= 0) {
            throw new InvalidTopUpRequestException("amount must be greater than zero");
        }
        if (value.stripTrailingZeros().scale() > 2) {
            throw new InvalidTopUpRequestException("amount must have at most 2 decimals");
        }
        return value.setScale(2);
    }

    @Override
    public String toString() {
        return "TopUpRequest[amount=" + amount + ", creditCardNumber=***]";
    }
}

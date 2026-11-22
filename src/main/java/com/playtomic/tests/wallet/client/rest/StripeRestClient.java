package com.playtomic.tests.wallet.client.rest;

import com.playtomic.tests.wallet.domain.Payment;
import com.playtomic.tests.wallet.domain.exception.StripeServiceException;

import java.math.BigDecimal;

/**
 * Handles the communication with Stripe.
 *
 * Blocking calls: the top-up flow is synchronous.
 */
public interface StripeRestClient {

    /**
     * Charges money in the credit card.
     *
     * @throws com.playtomic.tests.wallet.domain.exception.StripeAmountTooSmallException when Stripe rejects the amount (422)
     * @throws StripeServiceException for any other 4xx/5xx answer
     */
    Payment charge(String creditCardNumber, BigDecimal amount) throws StripeServiceException;

    /**
     * Refunds the specified payment.
     */
    void refund(String paymentId) throws StripeServiceException;
}

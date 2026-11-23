package com.playtomic.tests.wallet.api;

import com.playtomic.tests.wallet.domain.exception.AmountTooSmallException;
import com.playtomic.tests.wallet.domain.exception.InvalidTopUpRequestException;
import com.playtomic.tests.wallet.domain.exception.PaymentProviderException;
import com.playtomic.tests.wallet.domain.exception.TopUpNotFoundException;
import com.playtomic.tests.wallet.domain.exception.WalletAlreadyExistsException;
import com.playtomic.tests.wallet.domain.exception.WalletNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(WalletAlreadyExistsException.class)
    ProblemDetail handle(WalletAlreadyExistsException e) {
        return ProblemTypes.problem(HttpStatus.CONFLICT, "wallet-already-exists", "Wallet already exists", e.getMessage());
    }
    @ExceptionHandler(WalletNotFoundException.class)
    ProblemDetail handle(WalletNotFoundException e) {
        return ProblemTypes.problem(HttpStatus.NOT_FOUND, "wallet-not-found", "Wallet not found", e.getMessage());
    }

    @ExceptionHandler(TopUpNotFoundException.class)
    ProblemDetail handle(TopUpNotFoundException e) {
        return ProblemTypes.problem(HttpStatus.NOT_FOUND, "top-up-not-found", "Top-up not found", e.getMessage());
    }

    @ExceptionHandler(InvalidTopUpRequestException.class)
    ProblemDetail handle(InvalidTopUpRequestException e) {
        return ProblemTypes.problem(HttpStatus.BAD_REQUEST, "validation-error", "Validation error", e.getMessage());
    }

    @ExceptionHandler(AmountTooSmallException.class)
    ProblemDetail handle(AmountTooSmallException e) {
        return ProblemTypes.problem(HttpStatus.UNPROCESSABLE_ENTITY, "amount-too-small", "Amount too small", e.getMessage());
    }

    @ExceptionHandler(PaymentProviderException.class)
    ProblemDetail handle(PaymentProviderException e) {
        return ProblemTypes.problem(HttpStatus.BAD_GATEWAY, "payment-provider-error", "Payment provider error", e.getMessage());
    }

    public static final class ProblemTypes {

        private static final String BASE = "https://wallet.playtomic.io/errors/";

        private ProblemTypes() {
        }

        public static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
            problem.setType(URI.create(BASE + type));
            problem.setTitle(title);
            return problem;
        }
    }
}

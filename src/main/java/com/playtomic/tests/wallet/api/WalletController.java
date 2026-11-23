package com.playtomic.tests.wallet.api;

import com.playtomic.tests.wallet.domain.dto.wallet.TopUpRequest;
import com.playtomic.tests.wallet.domain.dto.wallet.TopUpResponse;
import com.playtomic.tests.wallet.domain.dto.wallet.WalletResponse;
import com.playtomic.tests.wallet.domain.entity.Transaction;
import com.playtomic.tests.wallet.domain.entity.Wallet;
import com.playtomic.tests.wallet.service.TopUpService;
import com.playtomic.tests.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final TopUpService topUpService;

    @GetMapping("/{walletId}")
    WalletResponse get(@PathVariable String walletId, @AuthenticationPrincipal Jwt jwt) {
        return WalletResponse.from(walletService.getOwned(walletId, jwt.getSubject()));
    }

    @PostMapping
    ResponseEntity<WalletResponse> create(@AuthenticationPrincipal Jwt jwt) {
        Wallet wallet = walletService.create(jwt.getSubject());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(wallet.getId())
                .toUri();
        return ResponseEntity.created(location).body(WalletResponse.from(wallet));
    }

    @PostMapping("/{walletId}/top-ups")
    ResponseEntity<TopUpResponse> topUp(@PathVariable String walletId,
                                        @RequestHeader("Idempotency-Key") String idempotencyKey,
                                        @RequestBody TopUpRequest request,
                                        @AuthenticationPrincipal Jwt jwt) {
        BigDecimal amount = request.validatedAmount();
        Transaction topUp = topUpService.topUp(walletId, jwt.getSubject(), idempotencyKey,
                amount, request.creditCardNumber());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(topUp.getId())
                .toUri();
        return ResponseEntity.created(location).body(TopUpResponse.from(topUp));
    }

    @GetMapping("/{walletId}/top-ups/{topUpId}")
    TopUpResponse getTopUp(@PathVariable String walletId, @PathVariable String topUpId,
                           @AuthenticationPrincipal Jwt jwt) {
        return TopUpResponse.from(topUpService.getTopUp(walletId, topUpId, jwt.getSubject()));
    }
}

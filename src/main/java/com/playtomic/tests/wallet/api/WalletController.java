package com.playtomic.tests.wallet.api;

import com.playtomic.tests.wallet.domain.dto.wallet.WalletResponse;
import com.playtomic.tests.wallet.domain.entity.Wallet;
import com.playtomic.tests.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

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
}

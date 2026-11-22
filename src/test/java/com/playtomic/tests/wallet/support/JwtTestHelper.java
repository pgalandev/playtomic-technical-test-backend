package com.playtomic.tests.wallet.support;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.util.Date;

public final class JwtTestHelper {

    public static final String TEST_SECRET = "test-secret-test-secret-test-secret-test-secret";

    private JwtTestHelper() {
    }

    public static String bearerFor(String userId) {
        return "Bearer " + tokenFor(userId, TEST_SECRET);
    }

    public static String tokenFor(String userId, String secret) {
        return sign(new JWTClaimsSet.Builder().subject(userId), secret);
    }

    public static String tokenWithoutSubject() {
        return sign(new JWTClaimsSet.Builder(), TEST_SECRET);
    }

    private static String sign(JWTClaimsSet.Builder builder, String secret) {
        try {
            JWTClaimsSet claims = builder
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + 3_600_000))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

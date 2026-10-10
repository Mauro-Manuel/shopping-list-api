
package com.masprog.shopping_list_api.auth.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import io.jsonwebtoken.security.SignatureException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String TEST_SECRET =
            "shopping-list-test-secret-key-32-bytes-minimum";

    private static final long EXPIRATION_SECONDS = 900;

    @Test
    void shouldGenerateValidAccessToken() {

        // Arrange
        JwtService jwtService = new JwtService(
                TEST_SECRET,
                EXPIRATION_SECONDS
        );

        Long userId = 1L;

        // Act
        String token = jwtService.generateAccessToken(userId);

        // Assert
        SecretKey key = Keys.hmacShaKeyFor(
                TEST_SECRET.getBytes(StandardCharsets.UTF_8)
        );

        String subject = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();

        assertThat(token).isNotBlank();
        assertThat(subject).isEqualTo("1");
    }


    @Test
    void shouldGenerateTokenWithCorrectExpiration() {

        // Arrange
        JwtService jwtService = new JwtService(
                TEST_SECRET,
                EXPIRATION_SECONDS
        );

        SecretKey key = Keys.hmacShaKeyFor(
                TEST_SECRET.getBytes(StandardCharsets.UTF_8)
        );

        Instant beforeGeneration = Instant.now();

        // Act
        String token = jwtService.generateAccessToken(1L);

        Instant afterGeneration = Instant.now();

        var claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // Assert
        Instant issuedAt = claims.getIssuedAt().toInstant();
        Instant expiresAt = claims.getExpiration().toInstant();

        assertThat(issuedAt)
                .isBetween(
                        beforeGeneration.minusSeconds(1),
                        afterGeneration
                );

        assertThat(expiresAt)
                .isEqualTo(issuedAt.plusSeconds(EXPIRATION_SECONDS));
    }


    @Test
    void shouldRejectTokenWithInvalidSignature() {

        // Arrange
        JwtService jwtService = new JwtService(
                TEST_SECRET,
                EXPIRATION_SECONDS
        );

        String token = jwtService.generateAccessToken(1L);

        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "another-secret-key-with-at-least-32-bytes"
                        .getBytes(StandardCharsets.UTF_8)
        );

        // Act & Assert
        assertThatThrownBy(() ->
                Jwts.parser()
                        .verifyWith(wrongKey)
                        .build()
                        .parseSignedClaims(token)
        ).isInstanceOf(SignatureException.class);
    }


    @Test
    void shouldRejectExpiredToken() throws InterruptedException {

        // Arrange
        JwtService jwtService = new JwtService(
                TEST_SECRET,
                1
        );

        String token = jwtService.generateAccessToken(1L);

        SecretKey key = Keys.hmacShaKeyFor(
                TEST_SECRET.getBytes(StandardCharsets.UTF_8)
        );

        Thread.sleep(2100);

        // Act & Assert
        assertThatThrownBy(() ->
                Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
        ).isInstanceOf(ExpiredJwtException.class);
    }


    @Test
    void shouldExtractUserIdFromValidToken() {

        // Arrange
        JwtService jwtService = new JwtService(
                TEST_SECRET,
                EXPIRATION_SECONDS
        );

        Long expectedUserId = 1L;

        String token = jwtService.generateAccessToken(expectedUserId);

        // Act
        Long actualUserId = jwtService.extractUserId(token);

        // Assert
        assertThat(actualUserId).isEqualTo(expectedUserId);
    }


    @Test
    void shouldRejectInvalidSignatureWhenExtractingUserId() {

        // Arrange
        JwtService jwtService = new JwtService(
                TEST_SECRET,
                EXPIRATION_SECONDS
        );

        JwtService anotherJwtService = new JwtService(
                "another-secret-key-with-at-least-32-bytes",
                EXPIRATION_SECONDS
        );

        String token = anotherJwtService.generateAccessToken(1L);

        // Act & Assert
        assertThatThrownBy(() -> jwtService.extractUserId(token))
                .isInstanceOf(SignatureException.class);
    }


    @Test
    void shouldRejectExpiredTokenWhenExtractingUserId()
            throws InterruptedException {

        // Arrange
        JwtService jwtService = new JwtService(
                TEST_SECRET,
                1
        );

        String token = jwtService.generateAccessToken(1L);

        Thread.sleep(2100);

        // Act & Assert
        assertThatThrownBy(() -> jwtService.extractUserId(token))
                .isInstanceOf(ExpiredJwtException.class);
    }


}

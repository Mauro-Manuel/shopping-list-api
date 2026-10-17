package com.masprog.shopping_list_api.auth.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;

import java.time.ZoneId;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimitFilterTest {

    @Test
    void shouldRemoveExpiredRequestWindows() {

        // Arrange
        Clock clock = Clock.fixed(
                Instant.parse("2026-10-17T10:00:00Z"),
                ZoneOffset.UTC
        );

        AuthRateLimitFilter filter = new AuthRateLimitFilter(clock);

        // Act
        int removed = filter.removeExpiredWindows();

        // Assert
        assertThat(removed).isZero();
    }
    @Test
    void shouldRemoveExpiredWindowsAndPreserveActiveWindows()
            throws Exception {

        // Arrange
        MutableClock clock = new MutableClock(
                Instant.parse("2026-10-17T10:00:00Z")
        );

        AuthRateLimitFilter filter = new AuthRateLimitFilter(clock);

        MockHttpServletRequest expiredRequest =
                new MockHttpServletRequest("POST", "/api/v1/auth/login");
        expiredRequest.setRemoteAddr("192.0.2.10");

        MockHttpServletRequest activeRequest =
                new MockHttpServletRequest("POST", "/api/v1/auth/login");
        activeRequest.setRemoteAddr("192.0.2.20");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // Primeiro IP: janela iniciada às 10:00
        filter.doFilter(expiredRequest, response, chain);

        // Avançar 30 segundos
        clock.advanceSeconds(30);

        // Segundo IP: janela iniciada às 10:00:30
        filter.doFilter(
                activeRequest,
                new MockHttpServletResponse(),
                new MockFilterChain()
        );

        // Avançar mais 31 segundos
        clock.advanceSeconds(31);

        // Act
        int removed = filter.removeExpiredWindows();

        // Assert
        assertThat(removed).isEqualTo(1);
    }

    @Test
    void shouldAutomaticallyRemoveExpiredWindows()
            throws Exception {

        // Arrange
        MutableClock clock = new MutableClock(
                Instant.parse("2026-10-17T10:00:00Z")
        );

        AuthRateLimitFilter filter = new AuthRateLimitFilter(clock);

        MockHttpServletRequest firstRequest =
                new MockHttpServletRequest("POST", "/api/v1/auth/login");
        firstRequest.setRemoteAddr("192.0.2.10");

        // Criar uma janela de rate limiting
        filter.doFilter(
                firstRequest,
                new MockHttpServletResponse(),
                new MockFilterChain()
        );

        // Simular a passagem de 61 segundos
        clock.advanceSeconds(61);

        MockHttpServletRequest secondRequest =
                new MockHttpServletRequest("POST", "/api/v1/auth/login");
        secondRequest.setRemoteAddr("192.0.2.20");

        // Act: um novo pedido deve desencadear a limpeza
        filter.doFilter(
                secondRequest,
                new MockHttpServletResponse(),
                new MockFilterChain()
        );

        // Assert: a janela do primeiro IP já não deve existir
        assertThat(filter.removeExpiredWindows()).isZero();
    }

    private static class MutableClock extends Clock {

        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
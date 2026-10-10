package com.masprog.shopping_list_api.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.assertThat;

public class PasswordConfigTest {

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new PasswordConfig().passwordEncoder();
    }

    @Test
    void shouldEncodePassword() {
        String rawPassword = "MinhaSenha123!";

        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertThat(encodedPassword).isNotBlank();
        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        assertThat(encodedPassword).startsWith("$2a$12$");
    }

    @Test
    void shouldMatchCorrectPassword() {
        String rawPassword = "MinhaSenha123!";

        String encodedPassword = passwordEncoder.encode(rawPassword);

        boolean matches = passwordEncoder.matches(
                rawPassword,
                encodedPassword
        );

        assertThat(matches).isTrue();
    }

    @Test
    void shouldRejectIncorrectPassword() {
        String encodedPassword = passwordEncoder.encode("MinhaSenha123!");

        boolean matches = passwordEncoder.matches(
                "SenhaErrada123!",
                encodedPassword
        );

        assertThat(matches).isFalse();
    }

    @Test
    void shouldGenerateDifferentHashesForSamePassword() {
        String rawPassword = "MinhaSenha123!";

        String firstHash = passwordEncoder.encode(rawPassword);
        String secondHash = passwordEncoder.encode(rawPassword);

        assertThat(firstHash).isNotEqualTo(secondHash);

        assertThat(passwordEncoder.matches(rawPassword, firstHash)).isTrue();
        assertThat(passwordEncoder.matches(rawPassword, secondHash)).isTrue();
    }


    @Test
    void shouldEncodeAndMatchUnicodePassword() {

        String password = "MinhaSenhaÁngola2026!";

        String encodedPassword = passwordEncoder.encode(password);

        assertThat(encodedPassword).isNotBlank();
        assertThat(encodedPassword).isNotEqualTo(password);

        assertThat(passwordEncoder.matches(password, encodedPassword))
                .isTrue();

        assertThat(passwordEncoder.matches(
                "MinhaSenhaAngola2026!",
                encodedPassword
        )).isFalse();
    }

}


package com.masprog.shopping_list_api.auth.refresh;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenGeneratorTest {

    private final RefreshTokenGenerator generator =
            new RefreshTokenGenerator();

    @Test
    void shouldGenerateUniqueSecureTokens() {

        String firstToken = generator.generate();
        String secondToken = generator.generate();

        assertThat(firstToken).isNotBlank();
        assertThat(secondToken).isNotBlank();
        assertThat(firstToken).isNotEqualTo(secondToken);

        // 32 bytes representados em Base64 URL sem padding
        assertThat(firstToken).matches("[A-Za-z0-9_-]{43}");
    }

    @Test
    void shouldGenerateConsistentSha256Hash() {

        String token = generator.generate();

        String firstHash = generator.hash(token);
        String secondHash = generator.hash(token);

        assertThat(firstHash).hasSize(64);
        assertThat(firstHash).matches("[0-9a-f]{64}");
        assertThat(firstHash).isEqualTo(secondHash);
        assertThat(firstHash).isNotEqualTo(token);
    }
}

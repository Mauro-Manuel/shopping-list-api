
package com.masprog.shopping_list_api.auth.refresh;

import com.masprog.shopping_list_api.user.User;
import com.masprog.shopping_list_api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class RefreshTokenServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RefreshTokenGenerator refreshTokenGenerator;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldRotateRefreshTokenAndPersistRevocation() {

        // Arrange: criar um utilizador
        User user = new User();
        user.setName("Refresh Integration Test");
        user.setEmail("refresh.integration@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        // Emitir o primeiro refresh token
        String oldRawToken = refreshTokenService.issueToken(savedUser);
        String oldTokenHash = refreshTokenGenerator.hash(oldRawToken);

        // Act: efectuar a rotação
        RefreshTokenRotationResult rotationResult =
                refreshTokenService.rotateToken(oldRawToken);

        String newRawToken = rotationResult.refreshToken();

        String newTokenHash = refreshTokenGenerator.hash(newRawToken);

        // Assert: os tokens devem ser diferentes
        assertThat(newRawToken).isNotEqualTo(oldRawToken);

        // Consultar novamente os registos na base de dados
        RefreshToken oldToken = refreshTokenRepository
                .findByTokenHash(oldTokenHash)
                .orElseThrow();

        RefreshToken newToken = refreshTokenRepository
                .findByTokenHash(newTokenHash)
                .orElseThrow();

        // O token antigo deve estar revogado
        assertThat(oldToken.getRevokedAt()).isNotNull();
        assertThat(rotationResult.user().getId())
                .isEqualTo(savedUser.getId());

        // O novo token deve estar activo
        assertThat(newToken.getRevokedAt()).isNull();
        assertThat(newToken.getExpiresAt())
                .isAfter(newToken.getCreatedAt());

        // Ambos pertencem ao mesmo utilizador
        assertThat(oldToken.getUser().getId())
                .isEqualTo(savedUser.getId());

        assertThat(newToken.getUser().getId())
                .isEqualTo(savedUser.getId());

        assertThatThrownBy(() -> refreshTokenService.rotateToken(oldRawToken))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Invalid or expired refresh token.");
    }
}

package com.masprog.shopping_list_api.auth.refresh;

import com.masprog.shopping_list_api.user.User;
import com.masprog.shopping_list_api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class RefreshTokenRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindRefreshTokenByHash() {

        // Arrange
        User user = new User();
        user.setName("Refresh Token Test");
        user.setEmail("refresh.repository@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(savedUser);
        refreshToken.setTokenHash("a".repeat(64));
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));

        // Act
        refreshTokenRepository.saveAndFlush(refreshToken);

        // Assert
        RefreshToken savedToken = refreshTokenRepository
                .findByTokenHash("a".repeat(64))
                .orElseThrow();

        assertThat(savedToken.getId()).isNotNull();
        assertThat(savedToken.getUser().getId()).isEqualTo(savedUser.getId());
        assertThat(savedToken.getRevokedAt()).isNull();
    }
}

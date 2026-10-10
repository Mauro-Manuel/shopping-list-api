
package com.masprog.shopping_list_api.auth.refresh;

import com.masprog.shopping_list_api.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    private RefreshTokenRepository refreshTokenRepository;
    private RefreshTokenGenerator refreshTokenGenerator;
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        refreshTokenGenerator = mock(RefreshTokenGenerator.class);

        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository,
                refreshTokenGenerator
        );
    }

    @Test
    void shouldIssueAndPersistRefreshToken() {

        // Arrange
        User user = new User();
        user.setId(1L);

        String rawToken = "generated-refresh-token";
        String tokenHash = "a".repeat(64);

        when(refreshTokenGenerator.generate()).thenReturn(rawToken);
        when(refreshTokenGenerator.hash(rawToken)).thenReturn(tokenHash);

        // Act
        String result = refreshTokenService.issueToken(user);

        // Assert
        assertThat(result).isEqualTo(rawToken);

        ArgumentCaptor<RefreshToken> captor =
                ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken savedToken = captor.getValue();

        assertThat(savedToken.getUser()).isSameAs(user);
        assertThat(savedToken.getTokenHash()).isEqualTo(tokenHash);
        assertThat(savedToken.getCreatedAt()).isNotNull();
        assertThat(savedToken.getExpiresAt())
                .isAfter(savedToken.getCreatedAt());
        assertThat(savedToken.getRevokedAt()).isNull();
    }


    @Test
    void shouldValidateActiveRefreshToken() {

        // Arrange
        String rawToken = "valid-refresh-token";
        String tokenHash = "b".repeat(64);

        User user = new User();
        user.setId(1L);

        RefreshToken storedToken = new RefreshToken();
        storedToken.setUser(user);
        storedToken.setTokenHash(tokenHash);
        storedToken.setExpiresAt(LocalDateTime.now().plusDays(7));

        when(refreshTokenGenerator.hash(rawToken))
                .thenReturn(tokenHash);

        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(storedToken));

        // Act
        User result = refreshTokenService.validateToken(rawToken);

        // Assert
        assertThat(result).isSameAs(user);
    }


    @Test
    void shouldRejectNonexistentRefreshToken() {

        // Arrange
        String rawToken = "nonexistent-refresh-token";
        String tokenHash = "c".repeat(64);

        when(refreshTokenGenerator.hash(rawToken))
                .thenReturn(tokenHash);

        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                refreshTokenService.validateToken(rawToken)
        )
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Invalid or expired refresh token.");
    }


    @Test
    void shouldRejectExpiredRefreshToken() {

        // Arrange
        String rawToken = "expired-refresh-token";
        String tokenHash = "d".repeat(64);

        User user = new User();
        user.setId(1L);

        RefreshToken storedToken = new RefreshToken();
        storedToken.setUser(user);
        storedToken.setTokenHash(tokenHash);
        storedToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(refreshTokenGenerator.hash(rawToken))
                .thenReturn(tokenHash);

        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(storedToken));

        // Act & Assert
        assertThatThrownBy(() ->
                refreshTokenService.validateToken(rawToken)
        )
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Invalid or expired refresh token.");
    }



    @Test
    void shouldRejectRevokedRefreshToken() {

        // Arrange
        String rawToken = "revoked-refresh-token";
        String tokenHash = "e".repeat(64);

        User user = new User();
        user.setId(1L);

        RefreshToken storedToken = new RefreshToken();
        storedToken.setUser(user);
        storedToken.setTokenHash(tokenHash);
        storedToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        storedToken.setRevokedAt(LocalDateTime.now().minusMinutes(1));

        when(refreshTokenGenerator.hash(rawToken))
                .thenReturn(tokenHash);

        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(storedToken));

        // Act & Assert
        assertThatThrownBy(() ->
                refreshTokenService.validateToken(rawToken)
        )
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Invalid or expired refresh token.");
    }


    @Test
    void shouldRotateRefreshTokenSuccessfully() {

        // Arrange
        String oldRawToken = "old-refresh-token";
        String oldTokenHash = "a".repeat(64);

        String newRawToken = "new-refresh-token";
        String newTokenHash = "b".repeat(64);

        User user = new User();
        user.setId(1L);

        RefreshToken oldToken = new RefreshToken();
        oldToken.setUser(user);
        oldToken.setTokenHash(oldTokenHash);
        oldToken.setCreatedAt(LocalDateTime.now().minusDays(1));
        oldToken.setExpiresAt(LocalDateTime.now().plusDays(6));

        when(refreshTokenGenerator.hash(oldRawToken))
                .thenReturn(oldTokenHash);

        when(refreshTokenRepository.findByTokenHashForUpdate(oldTokenHash))
                .thenReturn(Optional.of(oldToken));

        when(refreshTokenGenerator.generate())
                .thenReturn(newRawToken);

        when(refreshTokenGenerator.hash(newRawToken))
                .thenReturn(newTokenHash);

        // Act
        RefreshTokenRotationResult result =
                refreshTokenService.rotateToken(oldRawToken);

        // Assert
        assertThat(result.user()).isSameAs(user);
        assertThat(result.refreshToken()).isEqualTo(newRawToken);

        // O token antigo deve ficar revogado
        assertThat(oldToken.getRevokedAt()).isNotNull();

        // Um novo token deve ser persistido
        ArgumentCaptor<RefreshToken> captor =
                ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken newToken = captor.getValue();

        assertThat(newToken.getUser()).isSameAs(user);
        assertThat(newToken.getTokenHash()).isEqualTo(newTokenHash);
        assertThat(newToken.getCreatedAt()).isNotNull();
        assertThat(newToken.getExpiresAt())
                .isAfter(newToken.getCreatedAt());
        assertThat(newToken.getRevokedAt()).isNull();
    }


    @Test
    void shouldRejectRotationOfRevokedRefreshToken() {

        // Arrange
        String rawToken = "already-used-refresh-token";
        String tokenHash = "f".repeat(64);

        User user = new User();
        user.setId(1L);

        RefreshToken revokedToken = new RefreshToken();
        revokedToken.setUser(user);
        revokedToken.setTokenHash(tokenHash);
        revokedToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        revokedToken.setRevokedAt(LocalDateTime.now().minusMinutes(1));

        when(refreshTokenGenerator.hash(rawToken))
                .thenReturn(tokenHash);

        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.of(revokedToken));

        // Act & Assert
        assertThatThrownBy(() ->
                refreshTokenService.rotateToken(rawToken)
        )
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Invalid or expired refresh token.");

        // Nenhum novo token deve ser gerado ou persistido
        verify(refreshTokenGenerator, never()).generate();
        verify(refreshTokenRepository, never()).save(any());
    }


    @Test
    void shouldRevokeRefreshTokenSuccessfully() {

        // Arrange
        String rawToken = "valid-refresh-token";
        String tokenHash = "a".repeat(64);

        User user = new User();
        user.setId(1L);

        RefreshToken storedToken = new RefreshToken();
        storedToken.setUser(user);
        storedToken.setTokenHash(tokenHash);
        storedToken.setExpiresAt(LocalDateTime.now().plusDays(7));

        when(refreshTokenGenerator.hash(rawToken))
                .thenReturn(tokenHash);

        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.of(storedToken));

        // Act
        refreshTokenService.revokeToken(rawToken, user.getId());

        // Assert
        assertThat(storedToken.getRevokedAt()).isNotNull();

        verify(refreshTokenRepository)
                .findByTokenHashForUpdate(tokenHash);

        verify(refreshTokenGenerator, never()).generate();
        verify(refreshTokenRepository, never()).save(any());
    }


    @Test
    void shouldRejectRevocationWhenTokenBelongsToAnotherUser() {

        // Arrange
        String rawToken = "another-user-refresh-token";
        String tokenHash = "b".repeat(64);

        User tokenOwner = new User();
        tokenOwner.setId(1L);

        Long authenticatedUserId = 2L;

        RefreshToken storedToken = new RefreshToken();
        storedToken.setUser(tokenOwner);
        storedToken.setTokenHash(tokenHash);
        storedToken.setExpiresAt(LocalDateTime.now().plusDays(7));

        when(refreshTokenGenerator.hash(rawToken))
                .thenReturn(tokenHash);

        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.of(storedToken));

        // Act & Assert
        assertThatThrownBy(() ->
                refreshTokenService.revokeToken(
                        rawToken,
                        authenticatedUserId
                )
        ).isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Invalid or expired refresh token.");

        // O token não pode ser revogado
        assertThat(storedToken.getRevokedAt()).isNull();
    }


    @Test
    void shouldRejectRevocationWhenRefreshTokenIsAlreadyRevoked() {

        // Arrange
        String rawToken = "already-revoked-refresh-token";
        String tokenHash = "c".repeat(64);

        User user = new User();
        user.setId(1L);

        RefreshToken storedToken = new RefreshToken();
        storedToken.setUser(user);
        storedToken.setTokenHash(tokenHash);
        storedToken.setExpiresAt(LocalDateTime.now().plusDays(7));

        LocalDateTime revokedAt = LocalDateTime.now().minusMinutes(10);
        storedToken.setRevokedAt(revokedAt);

        when(refreshTokenGenerator.hash(rawToken))
                .thenReturn(tokenHash);

        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.of(storedToken));

        // Act & Assert
        assertThatThrownBy(() ->
                refreshTokenService.revokeToken(rawToken, user.getId())
        )
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Invalid or expired refresh token.");

        // A data original de revogação deve permanecer inalterada
        assertThat(storedToken.getRevokedAt())
                .isEqualTo(revokedAt);

        verify(refreshTokenRepository)
                .findByTokenHashForUpdate(tokenHash);
    }

}

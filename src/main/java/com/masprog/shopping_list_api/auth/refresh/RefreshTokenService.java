
package com.masprog.shopping_list_api.auth.refresh;

import com.masprog.shopping_list_api.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RefreshTokenService {

    private static final long EXPIRATION_DAYS = 7;

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenGenerator refreshTokenGenerator;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenGenerator refreshTokenGenerator
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenGenerator = refreshTokenGenerator;
    }

    @Transactional
    public String issueToken(User user) {

        String rawToken = refreshTokenGenerator.generate();
        String tokenHash = refreshTokenGenerator.hash(rawToken);

        LocalDateTime now = LocalDateTime.now();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setCreatedAt(now);
        refreshToken.setExpiresAt(now.plusDays(EXPIRATION_DAYS));

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }


    @Transactional(readOnly = true)
    public User validateToken(String rawToken) {

        String tokenHash = refreshTokenGenerator.hash(rawToken);

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (refreshToken.getRevokedAt() != null) {
            throw new InvalidRefreshTokenException();
        }

        if (!refreshToken.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException();
        }

        return refreshToken.getUser();
    }



    @Transactional
    public RefreshTokenRotationResult rotateToken(String rawToken) {

        String tokenHash = refreshTokenGenerator.hash(rawToken);

        RefreshToken currentToken = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (currentToken.getRevokedAt() != null) {
            throw new InvalidRefreshTokenException();
        }

        if (!currentToken.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException();
        }

        currentToken.setRevokedAt(LocalDateTime.now());

        String newRefreshToken = issueToken(currentToken.getUser());

        return new RefreshTokenRotationResult(
                currentToken.getUser(),
                newRefreshToken
        );
    }


    @Transactional
    public void revokeToken(String rawToken, Long userId) {

        // 1. Calcular o hash do token recebido
        String tokenHash = refreshTokenGenerator.hash(rawToken);

        // 2. Procurar e bloquear o token na base de dados
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        // 3. Verificar se pertence ao utilizador autenticado
        if (!refreshToken.getUser().getId().equals(userId)) {
            throw new InvalidRefreshTokenException();
        }

        // 4. Verificar se já foi revogado
        if (refreshToken.getRevokedAt() != null) {
            throw new InvalidRefreshTokenException();
        }

        // 5. Verificar se expirou
        if (!refreshToken.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException();
        }

        // 6. Revogar o refresh token
        refreshToken.setRevokedAt(LocalDateTime.now());
    }



}

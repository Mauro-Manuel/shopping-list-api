
package com.masprog.shopping_list_api.auth;

import com.masprog.shopping_list_api.auth.dto.LoginRequest;
import com.masprog.shopping_list_api.auth.dto.LoginResponse;
import com.masprog.shopping_list_api.auth.dto.RegisterRequest;
import com.masprog.shopping_list_api.auth.dto.RegisterResponse;
import com.masprog.shopping_list_api.auth.jwt.JwtService;
import com.masprog.shopping_list_api.auth.refresh.RefreshTokenService;
import com.masprog.shopping_list_api.user.User;
import com.masprog.shopping_list_api.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @Test
    void shouldRegisterUserSuccessfully() {

        // Arrange
        AuthService authService = mock(AuthService.class);
        JwtService jwtService = mock(JwtService.class);
        RefreshTokenService refreshTokenService =
                mock(RefreshTokenService.class);
        UserRepository userRepository = mock(UserRepository.class);

        AuthController authController = new AuthController(
                authService,
                jwtService,
                refreshTokenService,
                userRepository
        );

        RegisterRequest request = new RegisterRequest(
                "Mauro Manuel",
                "mauro@email.com",
                "MinhaSenha123!",
                "MinhaSenha123!"
        );

        RegisterResponse expectedResponse = new RegisterResponse(
                1L,
                "Mauro Manuel",
                "mauro@email.com"
        );

        when(authService.register(request))
                .thenReturn(expectedResponse);

        // Act
        ResponseEntity<RegisterResponse> response =
                authController.register(request);

        // Assert
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        assertThat(response.getBody())
                .isEqualTo(expectedResponse);

        verify(authService).register(request);
        verifyNoMoreInteractions(authService);
        verifyNoInteractions(
                jwtService,
                refreshTokenService,
                userRepository
        );
    }

    @Test
    void shouldLoginSuccessfully() {

        // Arrange
        AuthService authService = mock(AuthService.class);
        JwtService jwtService = mock(JwtService.class);
        RefreshTokenService refreshTokenService =
                mock(RefreshTokenService.class);
        UserRepository userRepository = mock(UserRepository.class);

        AuthController authController = new AuthController(
                authService,
                jwtService,
                refreshTokenService,
                userRepository
        );

        LoginRequest request = new LoginRequest(
                "mauro@email.com",
                "MinhaSenha123!"
        );

        User user = new User();
        user.setId(1L);

        when(authService.authenticate(request))
                .thenReturn(user);

        when(jwtService.generateAccessToken(1L))
                .thenReturn("generated.jwt.token");

        when(refreshTokenService.issueToken(user))
                .thenReturn("generated-refresh-token");

        // Act
        ResponseEntity<LoginResponse> response =
                authController.login(request);

        // Assert
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
                .isEqualTo(new LoginResponse(
                        "generated.jwt.token",
                        "generated-refresh-token",
                        "Bearer",
                        900
                ));

        verify(authService).authenticate(request);
        verify(jwtService).generateAccessToken(1L);
        verify(refreshTokenService).issueToken(user);

        verifyNoMoreInteractions(
                authService,
                jwtService,
                refreshTokenService
        );

        verifyNoInteractions(userRepository);
    }
}

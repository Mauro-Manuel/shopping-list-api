
package com.masprog.shopping_list_api.auth;

import com.masprog.shopping_list_api.auth.dto.LoginRequest;
import com.masprog.shopping_list_api.auth.exception.InvalidCredentialsException;
import com.masprog.shopping_list_api.user.User;
import com.masprog.shopping_list_api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldAuthenticateUserWithValidCredentials() {

        // Arrange
        LoginRequest request = new LoginRequest(
                "  MAURO@EMAIL.COM  ",
                "MinhaSenha123!"
        );

        User user = new User();
        user.setId(1L);
        user.setEmail("mauro@email.com");
        user.setPasswordHash("encoded-password");

        when(userRepository.findByEmail("mauro@email.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "MinhaSenha123!",
                "encoded-password"
        )).thenReturn(true);

        // Act
        User authenticatedUser = authService.authenticate(request);

        // Assert
        assertThat(authenticatedUser.getId()).isEqualTo(1L);
        assertThat(authenticatedUser.getEmail())
                .isEqualTo("mauro@email.com");

        verify(userRepository).findByEmail("mauro@email.com");
        verify(passwordEncoder).matches(
                "MinhaSenha123!",
                "encoded-password"
        );
    }


    @Test
    void shouldRejectLoginWithIncorrectPassword() {

        // Arrange
        LoginRequest request = new LoginRequest(
                "mauro@email.com",
                "SenhaErrada123!"
        );

        User user = new User();
        user.setId(1L);
        user.setEmail("mauro@email.com");
        user.setPasswordHash("encoded-password");

        when(userRepository.findByEmail("mauro@email.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "SenhaErrada123!",
                "encoded-password"
        )).thenReturn(false);

        // Act & Assert
        assertThat(
                org.junit.jupiter.api.Assertions.assertThrows(
                        InvalidCredentialsException.class,
                        () -> authService.authenticate(request)
                )
        ).hasMessage("Invalid email or password.");

        verify(userRepository).findByEmail("mauro@email.com");
        verify(passwordEncoder).matches(
                "SenhaErrada123!",
                "encoded-password"
        );
    }


    @Test
    void shouldRejectLoginWhenEmailDoesNotExist() {

        // Arrange
        LoginRequest request = new LoginRequest(
                "unknown@email.com",
                "MinhaSenha123!"
        );

        when(userRepository.findByEmail("unknown@email.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThat(
                org.junit.jupiter.api.Assertions.assertThrows(
                        InvalidCredentialsException.class,
                        () -> authService.authenticate(request)
                )
        ).hasMessage("Invalid email or password.");

        verify(userRepository).findByEmail("unknown@email.com");
        verifyNoInteractions(passwordEncoder);
    }


}

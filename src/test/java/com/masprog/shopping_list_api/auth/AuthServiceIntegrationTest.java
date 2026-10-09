
package com.masprog.shopping_list_api.auth;

import com.masprog.shopping_list_api.auth.dto.RegisterRequest;
import com.masprog.shopping_list_api.auth.dto.RegisterResponse;
import com.masprog.shopping_list_api.user.User;
import com.masprog.shopping_list_api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.masprog.shopping_list_api.auth.exception.EmailAlreadyRegisteredException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class AuthServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldRegisterAndPersistUserSuccessfully() {

        RegisterRequest request = new RegisterRequest(
                "  Mauro Manuel  ",
                "  MAURO@EMAIL.COM  ",
                "MinhaSenha123!",
                "MinhaSenha123!"
        );

        RegisterResponse response = authService.register(request);

        User savedUser = userRepository.findByEmail("mauro@email.com")
                .orElseThrow();

        assertThat(response.id()).isEqualTo(savedUser.getId());
        assertThat(response.name()).isEqualTo("Mauro Manuel");
        assertThat(response.email()).isEqualTo("mauro@email.com");

        assertThat(savedUser.getPasswordHash())
                .isNotEqualTo(request.password());

        assertThat(passwordEncoder.matches(
                request.password(),
                savedUser.getPasswordHash()
        )).isTrue();

        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
    }


    @Test
    void shouldRejectDuplicateEmailInDatabase() {

        RegisterRequest firstRequest = new RegisterRequest(
                "Mauro Manuel",
                "mauro.duplicate@email.com",
                "MinhaSenha123!",
                "MinhaSenha123!"
        );

        RegisterRequest secondRequest = new RegisterRequest(
                "Outro Utilizador",
                "MAURO.DUPLICATE@EMAIL.COM",
                "OutraSenha123!",
                "OutraSenha123!"
        );

        authService.register(firstRequest);

        assertThatThrownBy(() -> authService.register(secondRequest))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("Email already registered.");
    }

}

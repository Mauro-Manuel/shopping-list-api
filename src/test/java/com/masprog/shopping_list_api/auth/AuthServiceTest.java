
package com.masprog.shopping_list_api.auth;

import com.masprog.shopping_list_api.auth.dto.RegisterRequest;
import com.masprog.shopping_list_api.auth.dto.RegisterResponse;
import com.masprog.shopping_list_api.auth.exception.EmailAlreadyRegisteredException;
import com.masprog.shopping_list_api.user.User;
import com.masprog.shopping_list_api.user.UserRepository;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.hibernate.exception.ConstraintViolationException;
import java.sql.SQLException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRegisterUserSuccessfully() {

        RegisterRequest request = new RegisterRequest(
                "  Mauro Manuel  ",
                "  Mauro@Email.com  ",
                "MinhaSenha123!",
                "MinhaSenha123!"
        );

        when(userRepository.existsByEmail("mauro@email.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("MinhaSenha123!"))
                .thenReturn("hashed-password");

        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(1L);
                    return user;
                });

        RegisterResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).saveAndFlush(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getName()).isEqualTo("Mauro Manuel");
        assertThat(savedUser.getEmail()).isEqualTo("mauro@email.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("hashed-password");

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Mauro Manuel");
        assertThat(response.email()).isEqualTo("mauro@email.com");

        verify(userRepository).existsByEmail("mauro@email.com");
        verify(passwordEncoder).encode("MinhaSenha123!");
    }


    @Test
    void shouldRejectRegistrationWhenEmailAlreadyExists() {

        RegisterRequest request = new RegisterRequest(
                "Mauro Manuel",
                "  MAURO@EMAIL.COM  ",
                "MinhaSenha123!",
                "MinhaSenha123!"
        );

        when(userRepository.existsByEmail("mauro@email.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("Email already registered.");

        verify(userRepository).existsByEmail("mauro@email.com");
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }



    @Test
    void shouldRejectRegistrationWhenDatabaseDetectsDuplicateEmail() {

        // 1. Preparar os dados do utilizador
        RegisterRequest request = new RegisterRequest(
                "Mauro Manuel",
                "mauro@email.com",
                "MinhaSenha123!",
                "MinhaSenha123!"
        );

        // 2. Simular que o email ainda não existe
        when(userRepository.existsByEmail("mauro@email.com"))
                .thenReturn(false);

        // 3. Simular a geração do hash da password
        when(passwordEncoder.encode("MinhaSenha123!"))
                .thenReturn("hashed-password");

        // 4. Simular o erro de unicidade do PostgreSQL
        SQLException sqlException = new SQLException(
                "duplicate key value violates unique constraint \"uk_users_email\"",
                "23505"
        );

        // 5. Simular a exceção lançada pelo Hibernate
        ConstraintViolationException constraintException =
                new ConstraintViolationException(
                        "Duplicate email",
                        sqlException,
                        "uk_users_email"
                );

        // 6. Simular a exceção recebida pelo Spring Data
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Database constraint violation",
                        constraintException
                ));

        // 7. Verificar se o serviço lança a exceção correta
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("Email already registered.");

        // 8. Verificar as interações com as dependências
        verify(userRepository).existsByEmail("mauro@email.com");
        verify(passwordEncoder).encode("MinhaSenha123!");
        verify(userRepository).saveAndFlush(any(User.class));
    }


}

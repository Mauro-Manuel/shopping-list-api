
package com.masprog.shopping_list_api.auth;

import com.masprog.shopping_list_api.auth.dto.RegisterRequest;
import com.masprog.shopping_list_api.auth.dto.RegisterResponse;
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

        AuthController authController =
                new AuthController(authService);

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
    }
}

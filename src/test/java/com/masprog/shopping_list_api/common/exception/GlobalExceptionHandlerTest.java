
package com.masprog.shopping_list_api.common.exception;

import com.masprog.shopping_list_api.auth.dto.RegisterRequest;
import com.masprog.shopping_list_api.auth.exception.EmailAlreadyRegisteredException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler =
            new GlobalExceptionHandler();

    @Test
    void shouldReturnConflictWhenEmailAlreadyRegistered() {

        // Arrange: preparar a exceção
        EmailAlreadyRegisteredException exception =
                new EmailAlreadyRegisteredException(
                        "Email already registered."
                );

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setRequestURI("/api/v1/auth/register");

        // Act: executar o handler
        ResponseEntity<ApiErrorResponse> response =
                exceptionHandler.handleEmailAlreadyRegistered(
                        exception,
                        request
                );

        // Assert: verificar o resultado
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        ApiErrorResponse body = response.getBody();

        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(409);
        assertThat(body.error()).isEqualTo("Conflict");
        assertThat(body.code())
                .isEqualTo("EMAIL_ALREADY_REGISTERED");
        assertThat(body.message())
                .isEqualTo("Email already registered.");
        assertThat(body.path())
                .isEqualTo("/api/v1/auth/register");
        assertThat(body.timestamp()).isNotNull();
        assertThat(body.fields()).isNull();
    }

    @Test
    void shouldReturnBadRequestWhenValidationFails() throws NoSuchMethodException {

        // Arrange: preparar um pedido inválido
        RegisterRequest request = new RegisterRequest(
                "Mauro Manuel",
                "invalid-email",
                "123456",
                "123456"
        );

        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(request, "registerRequest");

        // Simular os erros de validação encontrados pelo Spring
        bindingResult.addError(new FieldError(
                "registerRequest",
                "email",
                "Email format is invalid."
        ));

        bindingResult.addError(new FieldError(
                "registerRequest",
                "password",
                "Password must contain at least 8 characters."
        ));

        // Obter a referência de um método com o parâmetro RegisterRequest
        Method method = TestController.class.getDeclaredMethod(
                "register",
                RegisterRequest.class
        );

        MethodParameter methodParameter = new MethodParameter(method, 0);

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(
                        methodParameter,
                        bindingResult
                );

        MockHttpServletRequest httpRequest =
                new MockHttpServletRequest();

        httpRequest.setRequestURI("/api/v1/auth/register");

        // Act: executar o handler
        ResponseEntity<ApiErrorResponse> response =
                exceptionHandler.handleValidationException(
                        exception,
                        httpRequest
                );

        // Assert: verificar a resposta
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        ApiErrorResponse body = response.getBody();

        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.error()).isEqualTo("Bad Request");
        assertThat(body.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.message()).isEqualTo("Request validation failed.");
        assertThat(body.path()).isEqualTo("/api/v1/auth/register");
        assertThat(body.timestamp()).isNotNull();

        assertThat(body.fields())
                .containsEntry("email", "Email format is invalid.")
                .containsEntry(
                        "password",
                        "Password must contain at least 8 characters."
                )
                .hasSize(2);
    }

    private static class TestController {

        public void register(RegisterRequest request) {
            // Método auxiliar utilizado apenas no teste
        }
    }

}

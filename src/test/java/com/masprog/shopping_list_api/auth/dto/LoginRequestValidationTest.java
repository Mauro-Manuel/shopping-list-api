
package com.masprog.shopping_list_api.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void shouldAcceptValidLoginRequest() {

        LoginRequest request = new LoginRequest(
                "mauro@email.com",
                "MinhaSenha123!"
        );

        Set<ConstraintViolation<LoginRequest>> violations =
                validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldRejectBlankEmail() {

        LoginRequest request = new LoginRequest(
                "",
                "MinhaSenha123!"
        );

        Set<ConstraintViolation<LoginRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .anyMatch(violation ->
                        violation.getPropertyPath()
                                .toString()
                                .equals("email")
                );
    }

    @Test
    void shouldRejectInvalidEmail() {

        LoginRequest request = new LoginRequest(
                "invalid-email",
                "MinhaSenha123!"
        );

        Set<ConstraintViolation<LoginRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .anyMatch(violation ->
                        violation.getPropertyPath()
                                .toString()
                                .equals("email")
                );
    }

    @Test
    void shouldRejectBlankPassword() {

        LoginRequest request = new LoginRequest(
                "mauro@email.com",
                ""
        );

        Set<ConstraintViolation<LoginRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .anyMatch(violation ->
                        violation.getPropertyPath()
                                .toString()
                                .equals("password")
                );
    }
}

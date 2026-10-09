
package com.masprog.shopping_list_api.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void shouldAcceptValidRegisterRequest() {
        RegisterRequest request = new RegisterRequest(
                "Mauro Manuel",
                "mauro@email.com",
                "MinhaSenha123!",
                "MinhaSenha123!"
        );

        var violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "A"})
    void shouldRejectInvalidName(String name) {
        RegisterRequest request = new RegisterRequest(
                name,
                "mauro@email.com",
                "MinhaSenha123!",
                "MinhaSenha123!"
        );

        var violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(violation ->
                        violation.getPropertyPath()
                                .toString()
                                .equals("name"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "invalid-email",
            "mauro@",
            "@email.com"
    })
    void shouldRejectInvalidEmail(String email) {
        RegisterRequest request = new RegisterRequest(
                "Mauro Manuel",
                email,
                "MinhaSenha123!",
                "MinhaSenha123!"
        );

        var violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(violation ->
                        violation.getPropertyPath()
                                .toString()
                                .equals("email"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "123456",
            "1234567"
    })
    void shouldRejectInvalidPassword(String password) {
        RegisterRequest request = new RegisterRequest(
                "Mauro Manuel",
                "mauro@email.com",
                password,
                password
        );

        var violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(violation ->
                        violation.getPropertyPath()
                                .toString()
                                .equals("password"));
    }

    @Test
    void shouldRejectNullRequiredFields() {
        RegisterRequest request = new RegisterRequest(
                null,
                null,
                null,
                null
        );

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation ->
                        violation.getPropertyPath().toString())
                .contains(
                        "name",
                        "email",
                        "password",
                        "confirmPassword"
                );
    }

    @Test
    void shouldRejectNameAndEmailExceedingMaxLength() {
        RegisterRequest request = new RegisterRequest(
                "A".repeat(101),
                "a".repeat(145) + "@email.com",
                "MinhaSenha123!",
                "MinhaSenha123!"
        );

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation ->
                        violation.getPropertyPath().toString())
                .contains("name", "email");
    }

    @Test
    void shouldRejectPasswordConfirmationMismatch() {
        RegisterRequest request = new RegisterRequest(
                "Mauro Manuel",
                "mauro@email.com",
                "MinhaSenha123!",
                "SenhaDiferente123!"
        );

        var violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(violation ->
                        violation.getPropertyPath()
                                .toString()
                                .equals("confirmPassword"));
    }

    @Test
    void shouldRejectPasswordLongerThan72Bytes() {

        String password = "a".repeat(73);

        RegisterRequest request = new RegisterRequest(
                "Mauro Manuel",
                "mauro@email.com",
                password,
                password
        );

        Set<ConstraintViolation<RegisterRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .anyMatch(violation ->
                        violation.getPropertyPath()
                                .toString()
                                .equals("password")
                );
    }


    @Test
    void shouldRejectPasswordWhenUtf8BytesExceed72() {

        String password = "á".repeat(37);

        RegisterRequest request = new RegisterRequest(
                "Mauro Manuel",
                "mauro@email.com",
                password,
                password
        );

        Set<ConstraintViolation<RegisterRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .anyMatch(violation ->
                        violation.getPropertyPath()
                                .toString()
                                .equals("password")
                );
    }


    @Test
    void shouldAcceptPasswordWithExactly72Bytes() {

        String password = "a".repeat(72);

        RegisterRequest request = new RegisterRequest(
                "Mauro Manuel",
                "mauro@email.com",
                password,
                password
        );

        Set<ConstraintViolation<RegisterRequest>> violations =
                validator.validate(request);

        assertThat(violations).isEmpty();
    }


}

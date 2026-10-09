package com.masprog.shopping_list_api.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class PasswordLengthValidator
        implements ConstraintValidator<ValidPasswordLength, String> {

    private static final int MAX_PASSWORD_BYTES = 72;

    @Override
    public boolean isValid(
            String password,
            ConstraintValidatorContext context
    ) {
        if (password == null) {
            return true;
        }

        return password.getBytes(StandardCharsets.UTF_8).length
                <= MAX_PASSWORD_BYTES;
    }
}

package com.masprog.shopping_list_api.auth.validation;

import com.masprog.shopping_list_api.auth.dto.RegisterRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Objects;

public class PasswordMatchesValidator
        implements ConstraintValidator<PasswordMatches, RegisterRequest> {

    @Override
    public boolean isValid(
            RegisterRequest request,
            ConstraintValidatorContext context) {

        if (request == null) {
            return true;
        }

        if (request.password() == null ||
                request.confirmPassword() == null) {
            return true;
        }

        boolean matches = Objects.equals(
                request.password(),
                request.confirmPassword()
        );

        if (!matches) {
            context.disableDefaultConstraintViolation();

            context.buildConstraintViolationWithTemplate(
                            "Password and confirmation do not match."
                    )
                    .addPropertyNode("confirmPassword")
                    .addConstraintViolation();
        }

        return matches;
    }
}

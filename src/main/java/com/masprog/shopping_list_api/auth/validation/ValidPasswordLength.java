package com.masprog.shopping_list_api.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordLengthValidator.class)
public @interface ValidPasswordLength {

    String message() default
            "Password must not exceed 72 bytes in UTF-8.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
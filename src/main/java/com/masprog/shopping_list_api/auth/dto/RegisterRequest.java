package com.masprog.shopping_list_api.auth.dto;

import com.masprog.shopping_list_api.auth.validation.PasswordMatches;
import com.masprog.shopping_list_api.auth.validation.ValidPasswordLength;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@PasswordMatches
public record RegisterRequest(

        @NotBlank(message = "Name is required.")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters.")
        String name,

        @NotBlank(message = "Email is required.")
        @Email(message = "Email format is invalid.")
        @Size(max = 150, message = "Email must not exceed 150 characters.")
        String email,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, message = "Password must contain at least 8 characters.")
        @ValidPasswordLength
        String password,

        @NotBlank(message = "Password confirmation is required.")
        String confirmPassword

) {
}

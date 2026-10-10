package com.masprog.shopping_list_api.auth.dto;


public record RegisterResponse(
        Long id,
        String name,
        String email
) {
}

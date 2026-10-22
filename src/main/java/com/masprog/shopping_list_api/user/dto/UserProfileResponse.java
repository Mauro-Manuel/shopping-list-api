package com.masprog.shopping_list_api.user.dto;

public record UserProfileResponse(
        Long id,
        String name,
        String email
) {
}
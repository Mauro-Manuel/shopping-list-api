package com.masprog.shopping_list_api.category.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(
        @NotBlank(message = "Category name is required")
        String name,
        String icon
) {
}
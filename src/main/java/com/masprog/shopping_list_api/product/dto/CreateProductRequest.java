package com.masprog.shopping_list_api.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProductRequest(
        @NotBlank(message = "Product name is required")
        String name,

        @NotNull(message = "Category is required")
        Long categoryId
) {
}
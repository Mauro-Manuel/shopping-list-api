package com.masprog.shopping_list_api.product.dto;

public record ProductResponse(
        Long id,
        String name,
        Boolean active,
        ProductCategoryResponse category
) {
}
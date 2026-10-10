package com.masprog.shopping_list_api.category.dto;

import com.masprog.shopping_list_api.category.CategoryType;

public record CategoryResponse(
        Long id,
        String name,
        String icon,
        CategoryType type
) {
}
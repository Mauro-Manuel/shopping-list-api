package com.masprog.shopping_list_api.shoppinglist.dto;

import com.masprog.shopping_list_api.shoppinglist.ShoppingListStatus;

import java.time.LocalDateTime;

public record ShoppingListResponse(
        Long id,
        String name,
        ShoppingListStatus status,
        long totalItems,
        long checkedItems,
        int progress,
        LocalDateTime createdAt
) {
}
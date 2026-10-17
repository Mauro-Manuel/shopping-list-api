package com.masprog.shopping_list_api.shoppinglist.dto;

import com.masprog.shopping_list_api.shoppingitem.Unit;
import com.masprog.shopping_list_api.shoppinglist.ShoppingListStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ShoppingListDetailResponse(
        Long id,
        String name,
        ShoppingListStatus status,
        long totalItems,
        long checkedItems,
        long pendingItems,
        int progress,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        List<ItemInfo> items
) {

    public record ItemInfo(
            Long id,
            ProductInfo product,
            CategoryInfo category,
            BigDecimal quantity,
            Unit unit,
            boolean checked
    ) {
    }

    public record ProductInfo(
            Long id,
            String name
    ) {
    }

    public record CategoryInfo(
            Long id,
            String name,
            String icon
    ) {
    }
}
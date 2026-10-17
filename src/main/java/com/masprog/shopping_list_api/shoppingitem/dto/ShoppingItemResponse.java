package com.masprog.shopping_list_api.shoppingitem.dto;

import com.masprog.shopping_list_api.shoppingitem.Unit;

import java.math.BigDecimal;

public record ShoppingItemResponse(
        Long id,
        ProductInfo product,
        BigDecimal quantity,
        Unit unit,
        boolean checked
) {

    public record ProductInfo(
            Long id,
            String name
    ) {
    }
}
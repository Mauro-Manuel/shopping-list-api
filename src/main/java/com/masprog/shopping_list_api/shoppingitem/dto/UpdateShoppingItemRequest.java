package com.masprog.shopping_list_api.shoppingitem.dto;

import com.masprog.shopping_list_api.shoppingitem.Unit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateShoppingItemRequest(

        @NotNull(message = "Shopping item quantity is required")
        @DecimalMin(
                value = "0.0",
                inclusive = false,
                message = "Shopping item quantity must be greater than zero"
        )
        BigDecimal quantity,

        @NotNull(message = "Shopping item unit is required")
        Unit unit
) {
}
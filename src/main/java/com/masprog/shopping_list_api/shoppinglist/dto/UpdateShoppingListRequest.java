package com.masprog.shopping_list_api.shoppinglist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateShoppingListRequest(

        @NotBlank(message = "Shopping list name is required")
        @Size(max = 150, message = "Shopping list name must not exceed 150 characters")
        String name

) {
}
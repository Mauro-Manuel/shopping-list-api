package com.masprog.shopping_list_api.shoppinglist.exception;

public class ShoppingListValidationException extends RuntimeException {

    public ShoppingListValidationException(String message) {
        super(message);
    }
}
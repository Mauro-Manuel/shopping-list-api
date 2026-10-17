package com.masprog.shopping_list_api.shoppingitem.exception;

public class ShoppingItemNotFoundException extends RuntimeException {

    public ShoppingItemNotFoundException(String message) {
        super(message);
    }
}
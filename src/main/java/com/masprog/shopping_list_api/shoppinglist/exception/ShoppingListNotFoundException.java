package com.masprog.shopping_list_api.shoppinglist.exception;

public class ShoppingListNotFoundException extends RuntimeException {

    public ShoppingListNotFoundException(String message) {
        super(message);
    }
}
package com.masprog.shopping_list_api.shoppinglist.exception;

public class EmptyShoppingListCompletionException
        extends RuntimeException {

    public EmptyShoppingListCompletionException(String message) {
        super(message);
    }
}
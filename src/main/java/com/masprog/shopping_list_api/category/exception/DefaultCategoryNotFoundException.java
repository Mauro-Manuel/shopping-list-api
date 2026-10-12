package com.masprog.shopping_list_api.category.exception;

public class DefaultCategoryNotFoundException extends IllegalStateException {

    public DefaultCategoryNotFoundException(String message) {
        super(message);
    }
}
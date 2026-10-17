package com.masprog.shopping_list_api.shoppingitem.exception;

public class ProductAlreadyInListException extends RuntimeException {

    public ProductAlreadyInListException(String message) {
        super(message);
    }
}
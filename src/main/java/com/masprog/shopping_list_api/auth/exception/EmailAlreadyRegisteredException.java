package com.masprog.shopping_list_api.auth.exception;

public class EmailAlreadyRegisteredException extends RuntimeException{

    public EmailAlreadyRegisteredException(String message) {
        super(message);
    }
}

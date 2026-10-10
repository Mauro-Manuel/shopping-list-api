
package com.masprog.shopping_list_api.auth.refresh;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token.");
    }
}

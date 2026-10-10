
package com.masprog.shopping_list_api.auth.refresh;

import com.masprog.shopping_list_api.user.User;

public record RefreshTokenRotationResult(
        User user,
        String refreshToken
) {
}

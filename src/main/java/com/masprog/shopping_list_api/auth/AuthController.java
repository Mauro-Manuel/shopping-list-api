
package com.masprog.shopping_list_api.auth;

import com.masprog.shopping_list_api.auth.dto.LoginRequest;
import com.masprog.shopping_list_api.auth.dto.LoginResponse;
import com.masprog.shopping_list_api.auth.dto.RegisterRequest;
import com.masprog.shopping_list_api.auth.dto.RegisterResponse;
import com.masprog.shopping_list_api.auth.jwt.JwtService;
import com.masprog.shopping_list_api.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.masprog.shopping_list_api.auth.dto.RefreshTokenRequest;
import com.masprog.shopping_list_api.auth.dto.RefreshTokenResponse;
import com.masprog.shopping_list_api.auth.refresh.RefreshTokenService;
import com.masprog.shopping_list_api.auth.refresh.RefreshTokenRotationResult;
import org.springframework.security.core.Authentication;
import com.masprog.shopping_list_api.auth.dto.LogoutRequest;

import com.masprog.shopping_list_api.user.User;
import com.masprog.shopping_list_api.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, JwtService jwtService, RefreshTokenService refreshTokenService, UserRepository userRepository) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        RegisterResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        User user = authService.authenticate(request);

        String accessToken = jwtService.generateAccessToken(user.getId());

        String refreshToken = refreshTokenService.issueToken(user);
        LoginResponse response = new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                900
        );

        return ResponseEntity.ok(response);
    }


    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {

        RefreshTokenRotationResult result =
                refreshTokenService.rotateToken(request.refreshToken());

        String newAccessToken = jwtService.generateAccessToken(
                result.user().getId()
        );

        RefreshTokenResponse response = new RefreshTokenResponse(
                newAccessToken,
                result.refreshToken(),
                "Bearer",
                900
        );
        return ResponseEntity.ok(response);
    }



    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody LogoutRequest request,
            Authentication authentication
    ) {
        // O JwtAuthenticationFilter guarda o email como principal
        String email = authentication.getName();

        // Recuperar o utilizador autenticado
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"
                ));

        // Revogar o refresh token pertencente ao utilizador
        refreshTokenService.revokeToken(
                request.refreshToken(),
                user.getId()
        );

        return ResponseEntity.noContent().build();
    }



}


package com.masprog.shopping_list_api.auth;

import com.masprog.shopping_list_api.auth.dto.LoginRequest;
import com.masprog.shopping_list_api.auth.dto.LoginResponse;
import com.masprog.shopping_list_api.auth.dto.RegisterRequest;
import com.masprog.shopping_list_api.auth.dto.RegisterResponse;
import com.masprog.shopping_list_api.auth.jwt.JwtService;
import com.masprog.shopping_list_api.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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


import com.masprog.shopping_list_api.user.UserRepository;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;



@Tag(name = "Authentication",
        description = "Endpoints for user registration, login, token refresh, and logout"
)
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

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account."
    )
    @SecurityRequirements
    @ApiResponses({@ApiResponse(responseCode = "201", description = "User registered successfully"
    ),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "409", description = "User already exists"),
            @ApiResponse(responseCode = "429", description = "Too many registration attempts")})
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        RegisterResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "Authenticate user and issue tokens",
            description = "Authenticates a user using email and password, "
                    + "returning an access token and a refresh token."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "429", description = "Too many authentication attempts"
            )})
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

    @Operation(
            summary = "Refresh access token",
            description = "Generates a new access token using a valid refresh token."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens refreshed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token"),
            @ApiResponse(responseCode = "429", description = "Too many refresh attempts")
    })
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


    @Operation(summary = "Logout user", description = "Invalidates the current authentication session."
    )
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Logout successful"
            ),
            @ApiResponse(responseCode = "401", description = "Authentication required or invalid token")
    })
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

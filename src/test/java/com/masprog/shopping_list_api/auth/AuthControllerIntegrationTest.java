
package com.masprog.shopping_list_api.auth;

import com.masprog.shopping_list_api.auth.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.context.annotation.Import;

import com.masprog.shopping_list_api.auth.refresh.RefreshTokenService;
import com.masprog.shopping_list_api.user.User;
import com.masprog.shopping_list_api.user.UserRepository;

import com.masprog.shopping_list_api.auth.refresh.RefreshToken;
import com.masprog.shopping_list_api.auth.refresh.RefreshTokenRepository;
import com.masprog.shopping_list_api.auth.refresh.RefreshTokenGenerator;
import java.time.LocalDateTime;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(AuthControllerIntegrationTest.ProtectedTestController.class)
class AuthControllerIntegrationTest {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RefreshTokenGenerator refreshTokenGenerator;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRegisterUserAndReturnCreated() throws Exception {

        String requestBody = """
                {
                    "name": "Mauro Manuel",
                    "email": "mauro.controller@email.com",
                    "password": "MinhaSenha123!",
                    "confirmPassword": "MinhaSenha123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name")
                        .value("Mauro Manuel"))
                .andExpect(jsonPath("$.email")
                        .value("mauro.controller@email.com"));
    }

    @Test
    void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {

        String requestBody = """
            {
                "name": "Mauro Manuel",
                "email": "duplicate@email.com",
                "password": "MinhaSenha123!",
                "confirmPassword": "MinhaSenha123!"
            }
            """;

        // Primeiro registo: deve ser criado com sucesso
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        // Segundo registo: o email já existe
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.code")
                        .value("EMAIL_ALREADY_REGISTERED"))
                .andExpect(jsonPath("$.message")
                        .value("Email already registered."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/auth/register"));
    }


    @Test
    void shouldReturnBadRequestWhenRegistrationDataIsInvalid()
            throws Exception {

        String requestBody = """
            {
                "name": "Mauro Manuel",
                "email": "invalid-email",
                "password": "123456",
                "confirmPassword": "123456"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("Request validation failed."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/auth/register"))
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists());
    }


    @Test
    void shouldReturnBadRequestWhenPasswordsDoNotMatch()
            throws Exception {

        String requestBody = """
            {
                "name": "Mauro Manuel",
                "email": "mauro.password@email.com",
                "password": "MinhaSenha123!",
                "confirmPassword": "OutraSenha123!"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("Request validation failed."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/auth/register"))
                .andExpect(jsonPath("$.fields.confirmPassword").exists());
    }



    @Test
    void shouldLoginSuccessfullyAndReturnAccessToken() throws Exception {

        // Arrange - Registar um utilizador
        String registerRequest = """
            {
                "name": "Mauro Manuel",
                "email": "mauro.login@email.com",
                "password": "MinhaSenha123!",
                "confirmPassword": "MinhaSenha123!"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequest))
                .andExpect(status().isCreated());

        // Arrange - Preparar o login
        String loginRequest = """
            {
                "email": "mauro.login@email.com",
                "password": "MinhaSenha123!"
            }
            """;

        // Act & Assert - Login sem token JWT
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }


    @Test
    void shouldReturnUnauthorizedWhenLoginPasswordIsInvalid()
            throws Exception {

        // Arrange
        String registerRequest = """
            {
                "name": "Mauro Manuel",
                "email": "mauro.invalid.login@email.com",
                "password": "MinhaSenha123!",
                "confirmPassword": "MinhaSenha123!"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequest))
                .andExpect(status().isCreated());

        String loginRequest = """
            {
                "email": "mauro.invalid.login@email.com",
                "password": "SenhaErrada123!"
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid email or password."));
    }


    @Test
    void shouldReturnUnauthorizedWhenLoginEmailDoesNotExist()
            throws Exception {

        // Arrange
        String loginRequest = """
            {
                "email": "nonexistent@email.com",
                "password": "MinhaSenha123!"
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid email or password."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/auth/login"));
    }


    @Test
    void shouldRejectProtectedEndpointWithoutToken()
            throws Exception {

        mockMvc.perform(get("/api/v1/test/protected"))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void shouldAllowAccessToProtectedEndpointWithValidToken()
            throws Exception {

        // Arrange - Registar um utilizador
        String registerRequest = """
            {
                "name": "JWT Test User",
                "email": "jwt.protected@email.com",
                "password": "MinhaSenha123!",
                "confirmPassword": "MinhaSenha123!"
            }
            """;

        String responseBody = mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registerRequest))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Obter o ID do utilizador criado
        JsonNode responseJson = objectMapper.readTree(responseBody);
        Long userId = responseJson.get("id").asLong();

        // Gerar JWT válido para o utilizador
        String accessToken = jwtService.generateAccessToken(userId);

        // Act & Assert
        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Access granted"));
    }


    @Test
    void shouldRejectProtectedEndpointWithInvalidToken()
            throws Exception {

        // Arrange
        String invalidToken = "invalid.jwt.token";

        // Act & Assert
        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void shouldRejectProtectedEndpointWhenUserDoesNotExist()
            throws Exception {

        // Arrange - Gerar um JWT válido para um utilizador inexistente
        Long nonexistentUserId = Long.MAX_VALUE;

        String accessToken = jwtService.generateAccessToken(nonexistentUserId);

        // Act & Assert
        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void shouldRejectProtectedEndpointWithExpiredToken()
            throws Exception {

        // Arrange - Criar um JWT com validade de 1 segundo
        JwtService shortLivedJwtService = new JwtService(
                "shopping-list-test-secret-key-32-bytes-minimum",
                1
        );

        String expiredToken =
                shortLivedJwtService.generateAccessToken(1L);

        // Aguardar a expiração do token
        Thread.sleep(2100);

        // Act & Assert
        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void shouldRefreshAccessTokenSuccessfully() throws Exception {

        // Arrange: criar um utilizador
        User user = new User();
        user.setName("Refresh Endpoint Test");
        user.setEmail("refresh.endpoint@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        // Emitir um refresh token válido
        String refreshToken = refreshTokenService.issueToken(savedUser);

        String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of("refreshToken", refreshToken)
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }


    @Test
    void shouldReturnUnauthorizedWhenRefreshTokenIsInvalid()
            throws Exception {

        // Arrange
        String requestBody = """
            {
                "refreshToken": "invalid-refresh-token"
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid or expired refresh token."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/auth/refresh"));
    }


    @Test
    void shouldReturnUnauthorizedWhenRefreshTokenIsRevoked()
            throws Exception {

        // Arrange: criar um utilizador
        User user = new User();
        user.setName("Revoked Refresh Test");
        user.setEmail("refresh.revoked@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        // Emitir um refresh token válido
        String oldRefreshToken =
                refreshTokenService.issueToken(savedUser);

        // Rodar o token, revogando o anterior
        refreshTokenService.rotateToken(oldRefreshToken);

        String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of(
                        "refreshToken",
                        oldRefreshToken
                )
        );

        // Act & Assert: tentar reutilizar o token revogado
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid or expired refresh token."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/auth/refresh"));
    }



    @Test
    void shouldReturnUnauthorizedWhenRefreshTokenIsExpired()
            throws Exception {

        // Arrange: criar um utilizador
        User user = new User();
        user.setName("Expired Refresh Test");
        user.setEmail("refresh.expired@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        // Criar um refresh token expirado
        String rawToken = refreshTokenGenerator.generate();

        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setUser(savedUser);
        expiredToken.setTokenHash(refreshTokenGenerator.hash(rawToken));
        expiredToken.setCreatedAt(LocalDateTime.now().minusDays(8));
        expiredToken.setExpiresAt(LocalDateTime.now().minusDays(1));

        refreshTokenRepository.saveAndFlush(expiredToken);

        String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of("refreshToken", rawToken)
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid or expired refresh token."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/auth/refresh"));
    }



    @Test
    void shouldLogoutSuccessfullyAndReturnNoContent() throws Exception {

        // Arrange: criar um utilizador
        User user = new User();
        user.setName("Logout Test");
        user.setEmail("logout.success@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        // Gerar os tokens
        String accessToken = jwtService.generateAccessToken(savedUser.getId());
        String refreshToken = refreshTokenService.issueToken(savedUser);

        String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of("refreshToken", refreshToken)
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());

        // O refresh token deve deixar de ser válido
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REFRESH_TOKEN"));
    }


    @Test
    void shouldReturnUnauthorizedWhenLogoutWithoutAccessToken()
            throws Exception {

        // Arrange
        String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of(
                        "refreshToken",
                        "some-refresh-token"
                )
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void shouldReturnUnauthorizedWhenLogoutWithInvalidAccessToken()
            throws Exception {

        // Arrange
        String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of(
                        "refreshToken",
                        "some-refresh-token"
                )
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(
                                "Authorization",
                                "Bearer invalid.jwt.token"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }





    @RestController
static class ProtectedTestController {

    @GetMapping("/api/v1/test/protected")
    public ResponseEntity<String> protectedEndpoint() {
        return ResponseEntity.ok("Access granted");
    }
}
}

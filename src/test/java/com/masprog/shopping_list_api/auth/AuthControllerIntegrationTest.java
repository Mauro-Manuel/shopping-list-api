
package com.masprog.shopping_list_api.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthControllerIntegrationTest {

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


}

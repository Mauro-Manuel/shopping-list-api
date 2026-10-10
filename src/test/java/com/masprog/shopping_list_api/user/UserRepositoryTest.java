package com.masprog.shopping_list_api.user;


import com.masprog.shopping_list_api.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@Import(JpaAuditingConfig.class)
public class UserRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUserByEmail() {
        User user = new User();
        user.setName("Mauro Manuel");
        user.setEmail("mauro@email.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();

        var result = userRepository.findByEmail("mauro@email.com");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Mauro Manuel");
        assertThat(result.get().getEmail()).isEqualTo("mauro@email.com");
    }

    @Test
    void shouldReturnTrueWhenEmailExists() {
        User user = new User();
        user.setName("Test User");
        user.setEmail("test@email.com");
        user.setPasswordHash("hashed-password");

        userRepository.saveAndFlush(user);

        boolean exists = userRepository.existsByEmail("test@email.com");

        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenEmailDoesNotExist() {
        boolean exists = userRepository.existsByEmail("unknown@email.com");

        assertThat(exists).isFalse();
    }
}

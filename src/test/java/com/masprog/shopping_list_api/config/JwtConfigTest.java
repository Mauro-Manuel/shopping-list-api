
package com.masprog.shopping_list_api.config;

import com.masprog.shopping_list_api.auth.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class JwtConfigTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(JwtConfig.class)
                    .withPropertyValues(
                            "app.jwt.secret=shopping-list-test-secret-key-32-bytes-minimum",
                            "app.jwt.expiration-seconds=900"
                    );

    @Test
    void shouldCreateJwtServiceBean() {

        contextRunner.run(context -> {

            assertThat(context).hasSingleBean(JwtService.class);

            JwtService jwtService = context.getBean(JwtService.class);

            String token = jwtService.generateAccessToken(1L);

            assertThat(jwtService.extractUserId(token)).isEqualTo(1L);
        });
    }
}

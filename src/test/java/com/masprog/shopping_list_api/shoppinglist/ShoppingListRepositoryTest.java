package com.masprog.shopping_list_api.shoppinglist;

import com.masprog.shopping_list_api.config.JpaAuditingConfig;
import com.masprog.shopping_list_api.user.User;
import com.masprog.shopping_list_api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(JpaAuditingConfig.class)
class ShoppingListRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveShoppingListWithActiveStatus() {

        User user = new User();
        user.setName("Shopping List User");
        user.setEmail("shopping.list@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Outubro");
        shoppingList.setUser(savedUser);

        ShoppingList savedList = shoppingListRepository.saveAndFlush(shoppingList);

        assertThat(savedList.getId()).isNotNull();
        assertThat(savedList.getName()).isEqualTo("Compras de Outubro");
        assertThat(savedList.getStatus()).isEqualTo(ShoppingListStatus.ACTIVE);
        assertThat(savedList.getUser().getId()).isEqualTo(savedUser.getId());
    }
}
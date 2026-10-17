package com.masprog.shopping_list_api.shoppingitem;

import com.masprog.shopping_list_api.category.Category;
import com.masprog.shopping_list_api.category.CategoryRepository;
import com.masprog.shopping_list_api.category.CategoryType;
import com.masprog.shopping_list_api.product.ProductRepository;
import com.masprog.shopping_list_api.shoppinglist.ShoppingList;
import com.masprog.shopping_list_api.shoppinglist.ShoppingListRepository;
import com.masprog.shopping_list_api.user.User;
import com.masprog.shopping_list_api.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@Testcontainers
class ShoppingItemTransactionIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private ShoppingItemService shoppingItemService;

    @MockitoSpyBean
    private ShoppingItemRepository shoppingItemRepository;

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldRollbackNewProductWhenShoppingItemPersistenceFails() {

        // Arrange
        User user = new User();
        user.setName("Shopping Item Transaction User");
        user.setEmail("shopping.item.rollback."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Alimentação"))
                .findFirst()
                .orElseThrow();

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras Transaccionais");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        String productName = "Produto rollback " + UUID.randomUUID();

        // Simular falha ao persistir ShoppingItem
        doThrow(new IllegalStateException("Simulated shopping item persistence failure"))
                .when(shoppingItemRepository)
                .save(any(ShoppingItem.class));

        // Act & Assert
        assertThatThrownBy(() ->
                shoppingItemService.addNewProduct(
                        savedList.getId(),
                        productName,
                        category.getId(),
                        BigDecimal.ONE,
                        Unit.UNIT,
                        savedUser.getEmail()
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Simulated shopping item persistence failure");

        // O produto criado deve ter sido revertido
        assertThat(productRepository
                .findByUserIdAndNameIgnoreCase(
                        savedUser.getId(),
                        productName
                )).isEmpty();

        // Nenhum item deve ter sido persistido
        assertThat(shoppingItemRepository.findAll()
                .stream()
                .noneMatch(item ->
                        item.getShoppingList().getId()
                                .equals(savedList.getId())
                )).isTrue();
    }
}
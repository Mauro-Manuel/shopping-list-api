package com.masprog.shopping_list_api.shoppingitem;

import com.masprog.shopping_list_api.auth.jwt.JwtService;
import com.masprog.shopping_list_api.category.Category;
import com.masprog.shopping_list_api.category.CategoryRepository;
import com.masprog.shopping_list_api.category.CategoryType;
import com.masprog.shopping_list_api.product.Product;
import com.masprog.shopping_list_api.product.ProductRepository;
import com.masprog.shopping_list_api.product.ProductService;
import com.masprog.shopping_list_api.shoppinglist.ShoppingList;
import com.masprog.shopping_list_api.shoppinglist.ShoppingListRepository;
import com.masprog.shopping_list_api.shoppinglist.ShoppingListStatus;
import com.masprog.shopping_list_api.user.User;
import com.masprog.shopping_list_api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ShoppingItemControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private ShoppingItemRepository shoppingItemRepository;

    @Autowired
    private JwtService jwtService;


    @Test
    void shouldAddExistingProductToShoppingList() throws Exception {

        // Arrange: criar utilizador
        User user = new User();
        user.setName("Shopping Item User");
        user.setEmail("shopping.item." + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        // Obter categoria DEFAULT
        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Alimentação"))
                .findFirst()
                .orElseThrow();

        // Criar produto
        Product product = new Product();
        product.setName("Arroz");
        product.setCategory(category);
        product.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        // Criar lista
        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Outubro");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
                {
                    "productId": %d,
                    "quantity": 2.5,
                    "unit": "KG"
                }
                """.formatted(savedProduct.getId());

        // Act & Assert
        mockMvc.perform(post("/api/v1/shopping-lists/{listId}/items",
                        savedList.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.product.id")
                        .value(savedProduct.getId()))
                .andExpect(jsonPath("$.quantity").value(2.5))
                .andExpect(jsonPath("$.unit").value("KG"))
                .andExpect(jsonPath("$.checked").value(false));

        // Confirmar persistência no PostgreSQL
        assertThat(shoppingItemRepository
                .existsByShoppingListIdAndProductId(
                        savedList.getId(),
                        savedProduct.getId()
                )).isTrue();
    }
    @Test
    void shouldUseDefaultQuantityWhenAddingShoppingItem() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping Item Default Quantity User");
        user.setEmail("shopping.item.default." + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Alimentação"))
                .findFirst()
                .orElseThrow();

        Product product = new Product();
        product.setName("Arroz");
        product.setCategory(category);
        product.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Outubro");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Quantity omitida intencionalmente
        String requestBody = """
            {
                "productId": %d,
                "unit": "KG"
            }
            """.formatted(savedProduct.getId());

        // Act & Assert
        mockMvc.perform(post("/api/v1/shopping-lists/{listId}/items",
                        savedList.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(1))
                .andExpect(jsonPath("$.unit").value("KG"))
                .andExpect(jsonPath("$.checked").value(false));

        // Confirmar quantidade persistida
        ShoppingItem savedItem = shoppingItemRepository.findAll()
                .stream()
                .filter(item -> item.getShoppingList().getId().equals(savedList.getId())
                        && item.getProduct().getId().equals(savedProduct.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(savedItem.getQuantity())
                .isEqualByComparingTo("1");
    }

    @Test
    void shouldRejectAddingShoppingItemWithoutUnit() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping Item Missing Unit User");
        user.setEmail("shopping.item.missing.unit."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Outubro");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Unit omitida intencionalmente
        String requestBody = """
            {
                "productId": 20,
                "quantity": 2
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/v1/shopping-lists/{listId}/items",
                        savedList.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.unit")
                        .value("Shopping item unit is required"));

        // Confirmar que nenhum item foi criado
        assertThat(shoppingItemRepository
                .findAll()
                .stream()
                .noneMatch(item ->
                        item.getShoppingList().getId().equals(savedList.getId())
                )).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-2.5"})
    void shouldRejectAddingShoppingItemWithInvalidQuantity(
            String quantity
    ) throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping Item Invalid Quantity User");
        user.setEmail("shopping.item.invalid.quantity."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Outubro");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
            {
                "productId": 20,
                "quantity": %s,
                "unit": "KG"
            }
            """.formatted(quantity);

        // Act & Assert
        mockMvc.perform(post(
                        "/api/v1/shopping-lists/{listId}/items",
                        savedList.getId()
                )
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.quantity")
                        .value("Shopping item quantity must be greater than zero"));

        // Confirmar que não foram criados itens
        assertThat(shoppingItemRepository
                .findAll()
                .stream()
                .noneMatch(item ->
                        item.getShoppingList().getId().equals(savedList.getId())
                )).isTrue();
    }

    @Test
    void shouldRejectAddingDuplicateProductToShoppingList() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping Item Duplicate User");
        user.setEmail("shopping.item.duplicate."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Alimentação"))
                .findFirst()
                .orElseThrow();

        Product product = new Product();
        product.setName("Arroz");
        product.setCategory(category);
        product.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Outubro");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        ShoppingItem existingItem = new ShoppingItem();
        existingItem.setShoppingList(savedList);
        existingItem.setProduct(savedProduct);
        existingItem.setQuantity(BigDecimal.ONE);
        existingItem.setUnit(Unit.KG);

        shoppingItemRepository.saveAndFlush(existingItem);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
            {
                "productId": %d,
                "quantity": 2,
                "unit": "KG"
            }
            """.formatted(savedProduct.getId());

        // Act & Assert
        mockMvc.perform(post("/api/v1/shopping-lists/{listId}/items",
                        savedList.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("PRODUCT_ALREADY_IN_LIST"))
                .andExpect(jsonPath("$.message")
                        .value("Product already exists in shopping list"));

        // Confirmar que permanece apenas um item na lista
        assertThat(shoppingItemRepository
                .findAll()
                .stream()
                .filter(item -> item.getShoppingList().getId()
                        .equals(savedList.getId()))
                .count()).isEqualTo(1);
    }
    @Test
    void shouldRejectAddingItemToAnotherUsersShoppingList() throws Exception {

        // Arrange: proprietário da lista
        User owner = new User();
        owner.setName("Shopping List Owner");
        owner.setEmail("shopping.item.owner."
                + UUID.randomUUID() + "@test.com");
        owner.setPasswordHash("hashed-password");

        User savedOwner = userRepository.saveAndFlush(owner);

        // Utilizador que tenta adicionar o item
        User otherUser = new User();
        otherUser.setName("Other Shopping Item User");
        otherUser.setEmail("shopping.item.other."
                + UUID.randomUUID() + "@test.com");
        otherUser.setPasswordHash("hashed-password");

        User savedOtherUser = userRepository.saveAndFlush(otherUser);

        // Categoria DEFAULT
        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Alimentação"))
                .findFirst()
                .orElseThrow();

        // Produto pertencente ao utilizador autenticado
        Product product = new Product();
        product.setName("Arroz");
        product.setCategory(category);
        product.setUser(savedOtherUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        // Lista pertencente ao primeiro utilizador
        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras Privadas");
        shoppingList.setUser(savedOwner);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        String token = jwtService.generateAccessToken(savedOtherUser.getId());

        String requestBody = """
            {
                "productId": %d,
                "quantity": 2,
                "unit": "KG"
            }
            """.formatted(savedProduct.getId());

        // Act & Assert
        mockMvc.perform(post("/api/v1/shopping-lists/{listId}/items",
                        savedList.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("SHOPPING_LIST_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("Shopping list not found"));

        // Confirmar que nenhum item foi criado na lista
        assertThat(shoppingItemRepository.findAll())
                .noneMatch(item ->
                        item.getShoppingList().getId().equals(savedList.getId())
                );
    }

    @Test
    void shouldRejectAddingAnotherUsersProductToShoppingList() throws Exception {

        // Arrange: utilizador autenticado
        User user = new User();
        user.setName("Shopping Item Product Access User");
        user.setEmail("shopping.item.product.access."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        // Proprietário do produto
        User productOwner = new User();
        productOwner.setName("Product Owner");
        productOwner.setEmail("shopping.item.product.owner."
                + UUID.randomUUID() + "@test.com");
        productOwner.setPasswordHash("hashed-password");

        User savedProductOwner = userRepository.saveAndFlush(productOwner);

        // Categoria DEFAULT
        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Alimentação"))
                .findFirst()
                .orElseThrow();

        // Produto pertencente a outro utilizador
        Product product = new Product();
        product.setName("Arroz");
        product.setCategory(category);
        product.setUser(savedProductOwner);

        Product savedProduct = productRepository.saveAndFlush(product);

        // Lista pertencente ao utilizador autenticado
        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Outubro");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
            {
                "productId": %d,
                "quantity": 2,
                "unit": "KG"
            }
            """.formatted(savedProduct.getId());

        // Act & Assert
        mockMvc.perform(post("/api/v1/shopping-lists/{listId}/items",
                        savedList.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("Product not found"));

        // Confirmar que nenhum item foi criado
        assertThat(shoppingItemRepository
                .existsByShoppingListIdAndProductId(
                        savedList.getId(),
                        savedProduct.getId()
                )).isFalse();
    }

    @Test
    void shouldRejectAddingItemToCompletedShoppingList() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping Item Completed List User");
        user.setEmail("shopping.item.completed."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Alimentação"))
                .findFirst()
                .orElseThrow();

        Product product = new Product();
        product.setName("Arroz");
        product.setCategory(category);
        product.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras Concluídas");
        shoppingList.setStatus(ShoppingListStatus.COMPLETED);
        shoppingList.setCompletedAt(LocalDateTime.now());
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
            {
                "productId": %d,
                "quantity": 2,
                "unit": "KG"
            }
            """.formatted(savedProduct.getId());

        // Act & Assert
        mockMvc.perform(post("/api/v1/shopping-lists/{listId}/items",
                        savedList.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("SHOPPING_LIST_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("Only active shopping lists can be modified"));

        // Confirmar que nenhum item foi criado
        assertThat(shoppingItemRepository
                .existsByShoppingListIdAndProductId(
                        savedList.getId(),
                        savedProduct.getId()
                )).isFalse();
    }

    @Test
    void shouldCreateProductWhenAddingNewShoppingItem() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping Item New Product User");
        user.setEmail("shopping.item.new.product."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Laticínios"))
                .findFirst()
                .orElseThrow();

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Outubro");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
            {
                "product": {
                    "name": "Leite em pó",
                    "categoryId": %d
                },
                "quantity": 2,
                "unit": "UNIT"
            }
            """.formatted(category.getId());

        // Act & Assert
        mockMvc.perform(post("/api/v1/shopping-lists/{listId}/items",
                        savedList.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.product.id").isNumber())
                .andExpect(jsonPath("$.product.name").value("Leite em pó"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.unit").value("UNIT"))
                .andExpect(jsonPath("$.checked").value(false));

        // Confirmar que o produto foi criado no catálogo
        Product savedProduct = productRepository.findAll()
                .stream()
                .filter(p -> p.getName().equals("Leite em pó")
                        && p.getUser().getId().equals(savedUser.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(savedProduct.getCategory().getId())
                .isEqualTo(category.getId());

        // Confirmar que o item foi associado ao produto criado
        assertThat(shoppingItemRepository
                .existsByShoppingListIdAndProductId(
                        savedList.getId(),
                        savedProduct.getId()
                )).isTrue();
    }

    @Test
    void shouldRejectDuplicateProductWhenAddingByName() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping Item Rollback User");
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
        shoppingList.setName("Compras de Outubro");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        // Criar um item associado à lista
        Product existingProduct = new Product();
        existingProduct.setName("Arroz");
        existingProduct.setCategory(category);
        existingProduct.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(existingProduct);

        ShoppingItem existingItem = new ShoppingItem();
        existingItem.setShoppingList(savedList);
        existingItem.setProduct(savedProduct);
        existingItem.setQuantity(BigDecimal.ONE);
        existingItem.setUnit(Unit.KG);

        shoppingItemRepository.saveAndFlush(existingItem);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Tentar adicionar novamente o mesmo produto pelo nome
        String requestBody = """
            {
                "product": {
                    "name": "Arroz",
                    "categoryId": %d
                },
                "quantity": 2,
                "unit": "KG"
            }
            """.formatted(category.getId());

        // Act & Assert
        mockMvc.perform(post("/api/v1/shopping-lists/{listId}/items",
                        savedList.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("PRODUCT_ALREADY_IN_LIST"));

        // O produto existente continua no catálogo
        assertThat(productRepository.findById(savedProduct.getId()))
                .isPresent();

        // Continua a existir apenas um item na lista
        assertThat(shoppingItemRepository
                .findAll()
                .stream()
                .filter(item -> item.getShoppingList().getId()
                        .equals(savedList.getId()))
                .count()).isEqualTo(1);
    }

     // PATCH

    @Test
    void shouldUpdateShoppingItemQuantityAndUnit() throws Exception {

        // Arrange: utilizador autenticado
        User user = new User();
        user.setName("Shopping Item Update User");
        user.setEmail("shopping.item.update."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        // Categoria DEFAULT
        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Alimentação"))
                .findFirst()
                .orElseThrow();

        // Produto
        Product product = new Product();
        product.setName("Arroz");
        product.setCategory(category);
        product.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        // Lista de compras
        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Outubro");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        // Item existente
        ShoppingItem item = new ShoppingItem();
        item.setShoppingList(savedList);
        item.setProduct(savedProduct);
        item.setQuantity(BigDecimal.ONE);
        item.setUnit(Unit.UNIT);

        ShoppingItem savedItem = shoppingItemRepository.saveAndFlush(item);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
            {
                "quantity": 2.5,
                "unit": "KG"
            }
            """;

        // Act & Assert
        mockMvc.perform(patch(
                        "/api/v1/shopping-lists/{listId}/items/{itemId}",
                        savedList.getId(),
                        savedItem.getId()
                )
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedItem.getId()))
                .andExpect(jsonPath("$.product.id").value(savedProduct.getId()))
                .andExpect(jsonPath("$.product.name").value("Arroz"))
                .andExpect(jsonPath("$.quantity").value(2.5))
                .andExpect(jsonPath("$.unit").value("KG"))
                .andExpect(jsonPath("$.checked").value(false));

        // Confirmar que as alterações foram persistidas
        ShoppingItem updatedItem = shoppingItemRepository
                .findById(savedItem.getId())
                .orElseThrow();

        assertThat(updatedItem.getQuantity())
                .isEqualByComparingTo("2.5");

        assertThat(updatedItem.getUnit())
                .isEqualTo(Unit.KG);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-2.5"})
    void shouldRejectUpdatingShoppingItemWithInvalidQuantity(
            String quantity
    ) throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping Item Invalid Update User");
        user.setEmail("shopping.item.invalid.update."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Alimentação"))
                .findFirst()
                .orElseThrow();

        Product product = new Product();
        product.setName("Arroz");
        product.setCategory(category);
        product.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Outubro");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        ShoppingItem item = new ShoppingItem();
        item.setShoppingList(savedList);
        item.setProduct(savedProduct);
        item.setQuantity(BigDecimal.ONE);
        item.setUnit(Unit.UNIT);

        ShoppingItem savedItem = shoppingItemRepository.saveAndFlush(item);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
            {
                "quantity": %s,
                "unit": "KG"
            }
            """.formatted(quantity);

        // Act & Assert
        mockMvc.perform(patch(
                        "/api/v1/shopping-lists/{listId}/items/{itemId}",
                        savedList.getId(),
                        savedItem.getId()
                )
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.quantity")
                        .value("Shopping item quantity must be greater than zero"));

        // Confirmar que os dados originais não foram alterados
        ShoppingItem unchangedItem = shoppingItemRepository
                .findById(savedItem.getId())
                .orElseThrow();

        assertThat(unchangedItem.getQuantity())
                .isEqualByComparingTo("1");

        assertThat(unchangedItem.getUnit())
                .isEqualTo(Unit.UNIT);
    }

    @Test
    void shouldRejectUpdatingShoppingItemWithoutUnit() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping Item Missing Unit Update User");
        user.setEmail("shopping.item.missing.unit.update."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Alimentação"))
                .findFirst()
                .orElseThrow();

        Product product = new Product();
        product.setName("Arroz");
        product.setCategory(category);
        product.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Outubro");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        ShoppingItem item = new ShoppingItem();
        item.setShoppingList(savedList);
        item.setProduct(savedProduct);
        item.setQuantity(BigDecimal.ONE);
        item.setUnit(Unit.UNIT);

        ShoppingItem savedItem = shoppingItemRepository.saveAndFlush(item);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Campo unit omitido intencionalmente
        String requestBody = """
            {
                "quantity": 2.5
            }
            """;

        // Act & Assert
        mockMvc.perform(patch(
                        "/api/v1/shopping-lists/{listId}/items/{itemId}",
                        savedList.getId(),
                        savedItem.getId()
                )
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.unit")
                        .value("Shopping item unit is required"));

        // Confirmar que o item permanece inalterado
        ShoppingItem unchangedItem = shoppingItemRepository
                .findById(savedItem.getId())
                .orElseThrow();

        assertThat(unchangedItem.getQuantity())
                .isEqualByComparingTo("1");

        assertThat(unchangedItem.getUnit())
                .isEqualTo(Unit.UNIT);
    }

    // DELETE

    @Test
    void shouldDeleteShoppingItemWithoutDeletingProduct() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping Item Delete User");
        user.setEmail("shopping.item.delete."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Alimentação"))
                .findFirst()
                .orElseThrow();

        Product product = new Product();
        product.setName("Arroz");
        product.setCategory(category);
        product.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Outubro");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        ShoppingItem item = new ShoppingItem();
        item.setShoppingList(savedList);
        item.setProduct(savedProduct);
        item.setQuantity(BigDecimal.ONE);
        item.setUnit(Unit.KG);

        ShoppingItem savedItem = shoppingItemRepository.saveAndFlush(item);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act & Assert
        mockMvc.perform(delete(
                        "/api/v1/shopping-lists/{listId}/items/{itemId}",
                        savedList.getId(),
                        savedItem.getId()
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        // O item deve ter sido removido
        assertThat(shoppingItemRepository.findById(savedItem.getId()))
                .isEmpty();

        // O produto deve continuar no catálogo
        assertThat(productRepository.findById(savedProduct.getId()))
                .isPresent();
    }


    @Test
    void shouldCheckShoppingItem() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping Item Check User");
        user.setEmail("shopping.item.check."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Alimentação"))
                .findFirst()
                .orElseThrow();

        Product product = new Product();
        product.setName("Arroz");
        product.setCategory(category);
        product.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        ShoppingList list = new ShoppingList();
        list.setName("Compras de Outubro");
        list.setUser(savedUser);

        ShoppingList savedList = shoppingListRepository.saveAndFlush(list);

        ShoppingItem item = new ShoppingItem();
        item.setShoppingList(savedList);
        item.setProduct(savedProduct);
        item.setQuantity(BigDecimal.ONE);
        item.setUnit(Unit.KG);

        ShoppingItem savedItem = shoppingItemRepository.saveAndFlush(item);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act & Assert
        mockMvc.perform(patch(
                        "/api/v1/shopping-lists/{listId}/items/{itemId}/check",
                        savedList.getId(),
                        savedItem.getId()
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedItem.getId()))
                .andExpect(jsonPath("$.checked").value(true))
                .andExpect(jsonPath("$.product.name").value("Arroz"));

        // Confirmar a persistência
        ShoppingItem checkedItem = shoppingItemRepository
                .findById(savedItem.getId())
                .orElseThrow();

        assertThat(checkedItem.isChecked()).isTrue();
        assertThat(checkedItem.getCheckedAt()).isNotNull();
    }

    @Test
    void shouldUncheckShoppingItem() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping Item Uncheck User");
        user.setEmail("shopping.item.uncheck."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Alimentação"))
                .findFirst()
                .orElseThrow();

        Product product = new Product();
        product.setName("Arroz");
        product.setCategory(category);
        product.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        ShoppingList list = new ShoppingList();
        list.setName("Compras de Outubro");
        list.setUser(savedUser);

        ShoppingList savedList = shoppingListRepository.saveAndFlush(list);

        ShoppingItem item = new ShoppingItem();
        item.setShoppingList(savedList);
        item.setProduct(savedProduct);
        item.setQuantity(BigDecimal.ONE);
        item.setUnit(Unit.KG);
        item.setChecked(true);
        item.setCheckedAt(LocalDateTime.now().minusMinutes(10));

        ShoppingItem savedItem = shoppingItemRepository.saveAndFlush(item);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act & Assert
        mockMvc.perform(patch(
                        "/api/v1/shopping-lists/{listId}/items/{itemId}/uncheck",
                        savedList.getId(),
                        savedItem.getId()
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedItem.getId()))
                .andExpect(jsonPath("$.product.name").value("Arroz"))
                .andExpect(jsonPath("$.checked").value(false));

        // Confirmar que as alterações foram persistidas
        ShoppingItem uncheckedItem = shoppingItemRepository
                .findById(savedItem.getId())
                .orElseThrow();

        assertThat(uncheckedItem.isChecked()).isFalse();
        assertThat(uncheckedItem.getCheckedAt()).isNull();

        // Confirmar que o produto continua no catálogo
        assertThat(productRepository.findById(savedProduct.getId()))
                .isPresent();
    }

}
package com.masprog.shopping_list_api.shoppinglist;

import com.masprog.shopping_list_api.auth.jwt.JwtService;
import com.masprog.shopping_list_api.category.CategoryRepository;
import com.masprog.shopping_list_api.product.ProductRepository;
import com.masprog.shopping_list_api.shoppingitem.ShoppingItemRepository;
import com.masprog.shopping_list_api.user.User;
import com.masprog.shopping_list_api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;


import com.masprog.shopping_list_api.category.Category;
import com.masprog.shopping_list_api.category.CategoryType;
import com.masprog.shopping_list_api.product.Product;
import com.masprog.shopping_list_api.shoppingitem.ShoppingItem;
import com.masprog.shopping_list_api.shoppingitem.Unit;

// Java
import java.math.BigDecimal;
import java.util.UUID;

import java.time.LocalDateTime;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ShoppingListControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @MockitoSpyBean
    private ShoppingItemRepository shoppingItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JwtService jwtService;


    // CREATE

    @Test
    void shouldCreateShoppingListWithActiveStatus() throws Exception {

        // Arrange: criar utilizador
        User user = new User();
        user.setName("Shopping List Integration User");
        user.setEmail("shopping.list.create@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
                {
                    "name": "  Compras   de   Outubro  "
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/v1/shopping-lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Compras de Outubro"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.checkedItems").value(0))
                .andExpect(jsonPath("$.progress").value(0))
                .andExpect(jsonPath("$.createdAt").exists());

        // Confirmar persistência na base de dados
        ShoppingList savedList = shoppingListRepository.findAll()
                .stream()
                .filter(list -> list.getName().equals("Compras de Outubro")
                        && list.getUser().getId().equals(savedUser.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(savedList.getStatus())
                .isEqualTo(ShoppingListStatus.ACTIVE);

        assertThat(savedList.getCompletedAt()).isNull();
        assertThat(savedList.getUser().getId())
                .isEqualTo(savedUser.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void shouldRejectShoppingListCreationWhenNameIsBlank(String name) throws Exception {

        User user = new User();
        user.setName("Shopping List Validation User");
        user.setEmail("shopping.list.validation."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
            {
                "name": "%s"
            }
            """.formatted(name);

        mockMvc.perform(post("/api/v1/shopping-lists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.name")
                        .value("Shopping list name is required"));
    }

    @Test
    void shouldRejectShoppingListCreationWithoutAuthentication() throws Exception {

        String requestBody = """
            {
                "name": "Compras de Outubro"
            }
            """;

        mockMvc.perform(post("/api/v1/shopping-lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    // GET
    @Test
    void shouldListOnlyAuthenticatedUsersShoppingLists() throws Exception {

        // Arrange: utilizador autenticado
        User user = new User();
        user.setName("Shopping List Owner");
        user.setEmail("shopping.list.owner@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        // Outro utilizador
        User otherUser = new User();
        otherUser.setName("Other Shopping List User");
        otherUser.setEmail("shopping.list.other@test.com");
        otherUser.setPasswordHash("hashed-password");

        User savedOtherUser = userRepository.saveAndFlush(otherUser);

        // Lista do utilizador autenticado
        ShoppingList ownList = new ShoppingList();
        ownList.setName("Compras de Outubro");
        ownList.setStatus(ShoppingListStatus.ACTIVE);
        ownList.setUser(savedUser);

        shoppingListRepository.saveAndFlush(ownList);

        // Lista de outro utilizador
        ShoppingList otherList = new ShoppingList();
        otherList.setName("Compras Privadas");
        otherList.setStatus(ShoppingListStatus.ACTIVE);
        otherList.setUser(savedOtherUser);

        shoppingListRepository.saveAndFlush(otherList);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act & Assert
        mockMvc.perform(get("/api/v1/shopping-lists")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name")
                        .value("Compras de Outubro"))
                .andExpect(jsonPath("$.content[0].status")
                        .value("ACTIVE"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }
    @Test
    void shouldFilterShoppingListsByCompletedStatus() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping List Status User");
        user.setEmail("shopping.list.status@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        // Lista activa
        ShoppingList activeList = new ShoppingList();
        activeList.setName("Compras de Outubro");
        activeList.setStatus(ShoppingListStatus.ACTIVE);
        activeList.setUser(savedUser);
        shoppingListRepository.saveAndFlush(activeList);

        // Lista concluída
        ShoppingList completedList = new ShoppingList();
        completedList.setName("Compras de Setembro");
        completedList.setStatus(ShoppingListStatus.COMPLETED);
        completedList.setCompletedAt(LocalDateTime.now());
        completedList.setUser(savedUser);
        shoppingListRepository.saveAndFlush(completedList);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act & Assert
        mockMvc.perform(get("/api/v1/shopping-lists")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "COMPLETED")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name")
                        .value("Compras de Setembro"))
                .andExpect(jsonPath("$.content[0].status")
                        .value("COMPLETED"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }
    @Test
    void shouldPaginateAuthenticatedUsersShoppingLists() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping List Pagination User");
        user.setEmail("shopping.list.pagination@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        // Criar cinco listas
        for (int i = 1; i <= 5; i++) {
            ShoppingList shoppingList = new ShoppingList();
            shoppingList.setName("Compras " + i);
            shoppingList.setStatus(ShoppingListStatus.ACTIVE);
            shoppingList.setUser(savedUser);

            shoppingListRepository.save(shoppingList);
        }

        shoppingListRepository.flush();

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act & Assert: segunda página, com dois elementos
        mockMvc.perform(get("/api/v1/shopping-lists")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Compras 3"))
                .andExpect(jsonPath("$.content[1].name").value("Compras 4"))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(5))
                .andExpect(jsonPath("$.page.totalPages").value(3));
    }
    @Test
    void shouldFindOwnShoppingListById() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping List Detail User");
        user.setEmail("shopping.list.detail@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Outubro");
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act & Assert
        mockMvc.perform(get("/api/v1/shopping-lists/{listId}", savedList.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedList.getId()))
                .andExpect(jsonPath("$.name").value("Compras de Outubro"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.checkedItems").value(0))
                .andExpect(jsonPath("$.progress").value(0))
                .andExpect(jsonPath("$.createdAt").exists());
    }
    @Test
    void shouldReturnNotFoundWhenShoppingListDoesNotExist() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping List Not Found User");
        user.setEmail("shopping.list.notfound@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act & Assert
        mockMvc.perform(get("/api/v1/shopping-lists/{listId}", -1L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("SHOPPING_LIST_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("Shopping list not found"));
    }
    @Test
    void shouldRejectAccessToAnotherUsersShoppingList() throws Exception {

        // Arrange: proprietário da lista
        User owner = new User();
        owner.setName("Shopping List Owner");
        owner.setEmail("shopping.list.access.owner@test.com");
        owner.setPasswordHash("hashed-password");

        User savedOwner = userRepository.saveAndFlush(owner);

        // Outro utilizador
        User otherUser = new User();
        otherUser.setName("Shopping List Other User");
        otherUser.setEmail("shopping.list.access.other@test.com");
        otherUser.setPasswordHash("hashed-password");

        User savedOtherUser = userRepository.saveAndFlush(otherUser);

        // Criar lista pertencente ao proprietário
        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras Privadas");
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);
        shoppingList.setUser(savedOwner);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        // Autenticar o segundo utilizador
        String token = jwtService.generateAccessToken(savedOtherUser.getId());

        // Act & Assert: tentar consultar a lista de outro utilizador
        mockMvc.perform(get("/api/v1/shopping-lists/{listId}", savedList.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("SHOPPING_LIST_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("Shopping list not found"));

        // Confirmar que a lista permanece na base de dados
        ShoppingList unchangedList = shoppingListRepository
                .findById(savedList.getId())
                .orElseThrow();

        assertThat(unchangedList.getName()).isEqualTo("Compras Privadas");
        assertThat(unchangedList.getStatus()).isEqualTo(ShoppingListStatus.ACTIVE);
        assertThat(unchangedList.getUser().getId()).isEqualTo(savedOwner.getId());
    }

     // PATCH

    @Test
    void shouldRenameOwnActiveShoppingList() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping List Update User");
        user.setEmail("shopping.list.update@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Setembro");
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
            {
                "name": "  Compras   de   Outubro  "
            }
            """;

        // Act & Assert
        mockMvc.perform(patch("/api/v1/shopping-lists/{listId}", savedList.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedList.getId()))
                .andExpect(jsonPath("$.name").value("Compras de Outubro"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Confirmar que a alteração foi persistida na base de dados
        ShoppingList updatedList = shoppingListRepository
                .findById(savedList.getId())
                .orElseThrow();

        assertThat(updatedList.getName())
                .isEqualTo("Compras de Outubro");
    }
    @Test
    void shouldRejectRenamingCompletedShoppingList() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping List Completed User");
        user.setEmail("shopping.list.completed.update@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Setembro");
        shoppingList.setStatus(ShoppingListStatus.COMPLETED);
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
            {
                "name": "Compras de Outubro"
            }
            """;

        // Act & Assert
        mockMvc.perform(patch("/api/v1/shopping-lists/{listId}", savedList.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("SHOPPING_LIST_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("Only active shopping lists can be modified"));

        // Confirmar que o nome original permanece inalterado
        ShoppingList unchangedList = shoppingListRepository
                .findById(savedList.getId())
                .orElseThrow();

        assertThat(unchangedList.getName())
                .isEqualTo("Compras de Setembro");

        assertThat(unchangedList.getStatus())
                .isEqualTo(ShoppingListStatus.COMPLETED);
    }
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void shouldRejectRenamingShoppingListWithInvalidName(String name) throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping List Validation User");
        user.setEmail("shopping.list.validation." + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Setembro");
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
            {
                "name": "%s"
            }
            """.formatted(name);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/shopping-lists/{listId}", savedList.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.name")
                        .value("Shopping list name is required"));

        // Confirmar que o nome original não foi alterado
        ShoppingList unchangedList = shoppingListRepository
                .findById(savedList.getId())
                .orElseThrow();

        assertThat(unchangedList.getName())
                .isEqualTo("Compras de Setembro");
    }

    @Test
    void shouldRejectRenamingAnotherUsersShoppingList() throws Exception {

        // Arrange: proprietário da lista
        User owner = new User();
        owner.setName("Shopping List Owner");
        owner.setEmail("shopping.list.rename.owner@test.com");
        owner.setPasswordHash("hashed-password");

        User savedOwner = userRepository.saveAndFlush(owner);

        // Outro utilizador
        User otherUser = new User();
        otherUser.setName("Shopping List Other User");
        otherUser.setEmail("shopping.list.rename.other@test.com");
        otherUser.setPasswordHash("hashed-password");

        User savedOtherUser = userRepository.saveAndFlush(otherUser);

        // Lista pertencente ao proprietário
        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras Privadas");
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);
        shoppingList.setUser(savedOwner);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        String token = jwtService.generateAccessToken(savedOtherUser.getId());

        String requestBody = """
            {
                "name": "Compras Alteradas"
            }
            """;

        // Act & Assert
        mockMvc.perform(patch("/api/v1/shopping-lists/{listId}", savedList.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("SHOPPING_LIST_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("Shopping list not found"));

        // Confirmar que os dados permanecem inalterados
        ShoppingList unchangedList = shoppingListRepository
                .findById(savedList.getId())
                .orElseThrow();

        assertThat(unchangedList.getName())
                .isEqualTo("Compras Privadas");

        assertThat(unchangedList.getUser().getId())
                .isEqualTo(savedOwner.getId());
    }

    // DELETE

    @Test
    void shouldDeleteOwnShoppingList() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping List Delete User");
        user.setEmail("shopping.list.delete@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Outubro");
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act & Assert
        mockMvc.perform(delete("/api/v1/shopping-lists/{listId}", savedList.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Confirmar que a lista foi eliminada da base de dados
        assertThat(shoppingListRepository.existsById(savedList.getId()))
                .isFalse();
    }
    @Test
    void shouldReturnNotFoundWhenDeletingNonexistentShoppingList() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping List Delete Not Found User");
        user.setEmail("shopping.list.delete.notfound." + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act & Assert
        mockMvc.perform(delete("/api/v1/shopping-lists/{listId}", -1L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("SHOPPING_LIST_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("Shopping list not found"));
    }

    @Test
    void shouldRejectDeletingAnotherUsersShoppingList() throws Exception {

        // Arrange: proprietário da lista
        User owner = new User();
        owner.setName("Shopping List Delete Owner");
        owner.setEmail("shopping.list.delete.owner." + UUID.randomUUID() + "@test.com");
        owner.setPasswordHash("hashed-password");

        User savedOwner = userRepository.saveAndFlush(owner);

        // Outro utilizador
        User otherUser = new User();
        otherUser.setName("Shopping List Delete Other User");
        otherUser.setEmail("shopping.list.delete.other." + UUID.randomUUID() + "@test.com");
        otherUser.setPasswordHash("hashed-password");

        User savedOtherUser = userRepository.saveAndFlush(otherUser);

        // Criar uma lista pertencente ao primeiro utilizador
        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras Privadas");
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);
        shoppingList.setUser(savedOwner);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        // Autenticar o segundo utilizador
        String token = jwtService.generateAccessToken(savedOtherUser.getId());

        // Act & Assert
        mockMvc.perform(delete("/api/v1/shopping-lists/{listId}", savedList.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("SHOPPING_LIST_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("Shopping list not found"));

        // Confirmar que a lista não foi eliminada
        ShoppingList unchangedList = shoppingListRepository
                .findById(savedList.getId())
                .orElseThrow();

        assertThat(unchangedList.getName())
                .isEqualTo("Compras Privadas");

        assertThat(unchangedList.getUser().getId())
                .isEqualTo(savedOwner.getId());
    }

    @Test
    void shouldDeleteCompletedShoppingList() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping List Completed Delete User");
        user.setEmail("shopping.list.completed.delete."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras Concluídas");
        shoppingList.setStatus(ShoppingListStatus.COMPLETED);
        shoppingList.setCompletedAt(LocalDateTime.now());
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act & Assert
        mockMvc.perform(delete(
                        "/api/v1/shopping-lists/{listId}",
                        savedList.getId()
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Confirmar a eliminação na base de dados
        assertThat(shoppingListRepository.existsById(savedList.getId()))
                .isFalse();
    }

    @Test
    void shouldCompleteShoppingListWithUncheckedItems() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping List Pending Items User");
        user.setEmail("shopping.list.pending."
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

        // Item ainda não comprado
        ShoppingItem item = new ShoppingItem();
        item.setShoppingList(savedList);
        item.setProduct(savedProduct);
        item.setQuantity(BigDecimal.ONE);
        item.setUnit(Unit.KG);
        item.setChecked(false);

        ShoppingItem savedItem = shoppingItemRepository.saveAndFlush(item);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act & Assert
        mockMvc.perform(patch(
                        "/api/v1/shopping-lists/{listId}/complete",
                        savedList.getId()
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Confirmar o estado persistido
        ShoppingList completedList = shoppingListRepository
                .findById(savedList.getId())
                .orElseThrow();

        assertThat(completedList.getStatus())
                .isEqualTo(ShoppingListStatus.COMPLETED);

        assertThat(completedList.getCompletedAt())
                .isNotNull();

        ShoppingItem unchangedItem = shoppingItemRepository
                .findById(savedItem.getId())
                .orElseThrow();

        assertThat(unchangedItem.isChecked()).isFalse();
    }

    @Test
    void shouldRejectCompletingEmptyShoppingList() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Empty Shopping List User");
        user.setEmail("shopping.list.empty."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Lista Vazia");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act & Assert
        mockMvc.perform(patch(
                        "/api/v1/shopping-lists/{listId}/complete",
                        savedList.getId()
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code")
                        .value("EMPTY_LIST_CANNOT_BE_COMPLETED"))
                .andExpect(jsonPath("$.message")
                        .value("Cannot complete an empty shopping list"));

        // A lista permanece ACTIVE
        ShoppingList unchangedList = shoppingListRepository
                .findById(savedList.getId())
                .orElseThrow();

        assertThat(unchangedList.getStatus())
                .isEqualTo(ShoppingListStatus.ACTIVE);

        assertThat(unchangedList.getCompletedAt())
                .isNull();
    }

    @Test
    void shouldReopenCompletedShoppingList() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping List Reopen User");
        user.setEmail("shopping.list.reopen."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras Concluídas");
        shoppingList.setUser(savedUser);
        shoppingList.setStatus(ShoppingListStatus.COMPLETED);
        shoppingList.setCompletedAt(LocalDateTime.now().minusDays(1));

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act & Assert
        mockMvc.perform(patch(
                        "/api/v1/shopping-lists/{listId}/reopen",
                        savedList.getId()
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Confirmar que a lista foi reaberta
        ShoppingList reopenedList = shoppingListRepository
                .findById(savedList.getId())
                .orElseThrow();

        assertThat(reopenedList.getStatus())
                .isEqualTo(ShoppingListStatus.ACTIVE);

        assertThat(reopenedList.getCompletedAt())
                .isNull();
    }

    @Test
    void shouldRejectReopeningActiveShoppingList() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping List Already Active User");
        user.setEmail("shopping.list.active."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras de Outubro");
        shoppingList.setUser(savedUser);
        shoppingList.setStatus(ShoppingListStatus.ACTIVE);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act & Assert
        mockMvc.perform(patch(
                        "/api/v1/shopping-lists/{listId}/reopen",
                        savedList.getId()
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("SHOPPING_LIST_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("Only completed shopping lists can be reopened"));

        // A lista permanece ACTIVE
        ShoppingList unchangedList = shoppingListRepository
                .findById(savedList.getId())
                .orElseThrow();

        assertThat(unchangedList.getStatus())
                .isEqualTo(ShoppingListStatus.ACTIVE);

        assertThat(unchangedList.getCompletedAt())
                .isNull();
    }

    @Test
    void shouldRejectReopeningAnotherUsersShoppingList() throws Exception {

        // Arrange: proprietário da lista
        User owner = new User();
        owner.setName("Shopping List Owner");
        owner.setEmail("shopping.list.owner."
                + UUID.randomUUID() + "@test.com");
        owner.setPasswordHash("hashed-password");

        User savedOwner = userRepository.saveAndFlush(owner);

        // Outro utilizador
        User otherUser = new User();
        otherUser.setName("Other Shopping List User");
        otherUser.setEmail("shopping.list.other."
                + UUID.randomUUID() + "@test.com");
        otherUser.setPasswordHash("hashed-password");

        User savedOtherUser = userRepository.saveAndFlush(otherUser);

        // Lista concluída pertencente ao primeiro utilizador
        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Compras Privadas");
        shoppingList.setUser(savedOwner);
        shoppingList.setStatus(ShoppingListStatus.COMPLETED);
        shoppingList.setCompletedAt(LocalDateTime.now().minusDays(1));

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        // Autenticação do segundo utilizador
        String token = jwtService.generateAccessToken(
                savedOtherUser.getId()
        );

        // Act & Assert
        mockMvc.perform(patch(
                        "/api/v1/shopping-lists/{listId}/reopen",
                        savedList.getId()
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("SHOPPING_LIST_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("Shopping list not found"));

        // A lista original deve permanecer concluída
        ShoppingList unchangedList = shoppingListRepository
                .findById(savedList.getId())
                .orElseThrow();

        assertThat(unchangedList.getStatus())
                .isEqualTo(ShoppingListStatus.COMPLETED);

        assertThat(unchangedList.getCompletedAt())
                .isNotNull();
    }

    @Test
    void shouldReturnShoppingListProgress() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping List Progress User");
        user.setEmail("shopping.list.progress."
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

        String[] productNames = {
                "Arroz", "Leite", "Açúcar", "Massa", "Óleo"
        };

        for (int i = 0; i < productNames.length; i++) {

            Product product = new Product();
            product.setName(productNames[i]);
            product.setCategory(category);
            product.setUser(savedUser);

            Product savedProduct = productRepository.saveAndFlush(product);

            ShoppingItem item = new ShoppingItem();
            item.setShoppingList(savedList);
            item.setProduct(savedProduct);
            item.setQuantity(BigDecimal.ONE);
            item.setUnit(Unit.UNIT);

            // Três produtos comprados e dois pendentes
            item.setChecked(i < 3);

            if (item.isChecked()) {
                item.setCheckedAt(LocalDateTime.now());
            }

            shoppingItemRepository.saveAndFlush(item);
        }

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act & Assert
        mockMvc.perform(get(
                        "/api/v1/shopping-lists/{listId}",
                        savedList.getId()
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedList.getId()))
                .andExpect(jsonPath("$.totalItems").value(5))
                .andExpect(jsonPath("$.checkedItems").value(3))
                .andExpect(jsonPath("$.pendingItems").value(2))
                .andExpect(jsonPath("$.progress").value(60))
                .andExpect(jsonPath("$.items.length()").value(5))
                .andExpect(jsonPath("$.items[0].product.id").isNumber())
                .andExpect(jsonPath("$.items[0].product.name").isString())
                .andExpect(jsonPath("$.items[0].category.id").isNumber())
                .andExpect(jsonPath("$.items[0].category.name").isString())
                .andExpect(jsonPath("$.items[0].quantity").isNumber())
                .andExpect(jsonPath("$.items[0].unit").value("UNIT"))
                .andExpect(jsonPath("$.items[0].checked").isBoolean());
    }

    @Test
    void shouldReturnZeroProgressForEmptyShoppingList() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Empty Shopping List User");
        user.setEmail("empty.shopping.list."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Lista vazia");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act & Assert
        mockMvc.perform(get(
                        "/api/v1/shopping-lists/{listId}",
                        savedList.getId()
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.checkedItems").value(0))
                .andExpect(jsonPath("$.pendingItems").value(0))
                .andExpect(jsonPath("$.progress").value(0));
    }


    @Test
    void shouldUseAggregatedQueryWhenListingShoppingLists() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping List Aggregation User");
        user.setEmail("shopping.list.aggregation."
                + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        ShoppingList firstList = new ShoppingList();
        firstList.setName("Lista A");
        firstList.setUser(savedUser);

        ShoppingList savedFirstList =
                shoppingListRepository.saveAndFlush(firstList);

        ShoppingList secondList = new ShoppingList();
        secondList.setName("Lista B");
        secondList.setUser(savedUser);

        ShoppingList savedSecondList =
                shoppingListRepository.saveAndFlush(secondList);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act
        mockMvc.perform(get("/api/v1/shopping-lists")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        // Assert
        verify(shoppingItemRepository, times(1))
                .countItemsByShoppingListIds(argThat(ids ->
                        ids.size() == 2
                                && ids.contains(savedFirstList.getId())
                                && ids.contains(savedSecondList.getId())
                ));

        verify(shoppingItemRepository, never())
                .countByShoppingListId(anyLong());

        verify(shoppingItemRepository, never())
                .countByShoppingListIdAndCheckedTrue(anyLong());
    }

    @Test
    void shouldReturnCorrectProgressWhenListingShoppingLists() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Shopping List Paginated Progress User");
        user.setEmail("shopping.list.paginated.progress."
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
        shoppingList.setName("Lista com produtos");
        shoppingList.setUser(savedUser);

        ShoppingList savedList =
                shoppingListRepository.saveAndFlush(shoppingList);

        ShoppingList emptyList = new ShoppingList();
        emptyList.setName("Lista vazia");
        emptyList.setUser(savedUser);

        shoppingListRepository.saveAndFlush(emptyList);

        String[] productNames = {"Arroz", "Leite", "Açúcar"};

        for (int i = 0; i < productNames.length; i++) {

            Product product = new Product();
            product.setName(productNames[i]);
            product.setCategory(category);
            product.setUser(savedUser);

            Product savedProduct = productRepository.saveAndFlush(product);

            ShoppingItem item = new ShoppingItem();
            item.setShoppingList(savedList);
            item.setProduct(savedProduct);
            item.setQuantity(BigDecimal.ONE);
            item.setUnit(Unit.UNIT);
            item.setChecked(i < 2);

            if (item.isChecked()) {
                item.setCheckedAt(LocalDateTime.now());
            }

            shoppingItemRepository.saveAndFlush(item);
        }

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act & Assert
        mockMvc.perform(get("/api/v1/shopping-lists")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].totalItems").value(3))
                .andExpect(jsonPath("$.content[0].checkedItems").value(2))
                .andExpect(jsonPath("$.content[0].pendingItems").value(1))
                .andExpect(jsonPath("$.content[0].progress").value(66))
                .andExpect(jsonPath("$.content[1].totalItems").value(0))
                .andExpect(jsonPath("$.content[1].checkedItems").value(0))
                .andExpect(jsonPath("$.content[1].pendingItems").value(0))
                .andExpect(jsonPath("$.content[1].progress").value(0));
    }


}
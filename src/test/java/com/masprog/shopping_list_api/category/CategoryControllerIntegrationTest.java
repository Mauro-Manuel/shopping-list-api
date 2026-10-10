package com.masprog.shopping_list_api.category;

import com.masprog.shopping_list_api.auth.jwt.JwtService;
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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CategoryControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void shouldCreateCustomCategoryWhenAuthenticatedUserProvidesValidName()
            throws Exception {

        // Arrange: criar um utilizador
        User user = new User();
        user.setName("Category Test User");
        user.setEmail("category.create@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        // Gerar JWT para o utilizador
        String accessToken =
                jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
        {
            "name": "Produtos para o bebé",
            "icon": "baby"
        }
        """;

        // Act & Assert
        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name")
                        .value("Produtos para o bebé"))
                .andExpect(jsonPath("$.type").value("CUSTOM"))
                .andExpect(jsonPath("$.icon").value("baby"));

        Category savedCategory = categoryRepository.findAll()
                .stream()
                .filter(category -> category.getName().equals("Produtos para o bebé"))
                .findFirst()
                .orElseThrow();

        assertEquals("baby", savedCategory.getIcon());
        assertEquals(CategoryType.CUSTOM, savedCategory.getType());
        assertEquals(savedUser.getId(), savedCategory.getUser().getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void shouldRejectCategoryCreationWhenNameIsBlank(String name) throws Exception {

        User user = new User();
        user.setName("Category Validation");
        user.setEmail("category.validation." + java.util.UUID.randomUUID() + "@test.com");
        user.setPasswordHash("test-password");

        User savedUser = userRepository.saveAndFlush(user);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
            {
                "name": "%s",
                "icon": "basket"
            }
            """.formatted(name);

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectDuplicateCustomCategoryNameIgnoringCase() throws Exception {

        User user = new User();
        user.setName("Category Duplicate");
        user.setEmail("category.duplicate@test.com");
        user.setPasswordHash("test-password");

        User savedUser = userRepository.saveAndFlush(user);

        String token = jwtService.generateAccessToken(savedUser.getId());

        Category existingCategory = new Category();
        existingCategory.setName("Cosméticos");
        existingCategory.setType(CategoryType.CUSTOM);
        existingCategory.setUser(savedUser);

        categoryRepository.saveAndFlush(existingCategory);

        String requestBody = """
            {
                "name": "cosméticos",
                "icon": "sparkles"
            }
            """;

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectCustomCategoryWhenNameMatchesDefaultCategory() throws Exception {

        User user = new User();
        user.setName("Category Default Conflict");
        user.setEmail("category.default.conflict@test.com");
        user.setPasswordHash("test-password");

        User savedUser = userRepository.saveAndFlush(user);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
        {
            "name": "alimentação",
            "icon": "utensils"
        }
        """;

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_ALREADY_EXISTS"));
    }

    @Test
    void shouldRejectDuplicateCategoryNameWithExtraSpaces() throws Exception {

        User user = new User();
        user.setName("Category Trim Test");
        user.setEmail("category.trim@test.com");
        user.setPasswordHash("test-password");

        User savedUser = userRepository.saveAndFlush(user);

        String token = jwtService.generateAccessToken(savedUser.getId());

        Category existingCategory = new Category();
        existingCategory.setName("Cosméticos");
        existingCategory.setType(CategoryType.CUSTOM);
        existingCategory.setUser(savedUser);

        categoryRepository.saveAndFlush(existingCategory);

        String requestBody = """
            {
                "name": " Cosméticos ",
                "icon": "sparkles"
            }
            """;

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_ALREADY_EXISTS"));
    }

    @Test
    void shouldAllowDifferentUsersToCreateCustomCategoriesWithSameName() throws Exception {

        User firstUser = new User();
        firstUser.setName("First Category User");
        firstUser.setEmail("category.first.user@test.com");
        firstUser.setPasswordHash("test-password");

        User savedFirstUser = userRepository.saveAndFlush(firstUser);

        User secondUser = new User();
        secondUser.setName("Second Category User");
        secondUser.setEmail("category.second.user@test.com");
        secondUser.setPasswordHash("test-password");

        User savedSecondUser = userRepository.saveAndFlush(secondUser);

        Category existingCategory = new Category();
        existingCategory.setName("Material Escolar");
        existingCategory.setType(CategoryType.CUSTOM);
        existingCategory.setUser(savedFirstUser);

        categoryRepository.saveAndFlush(existingCategory);

        String token = jwtService.generateAccessToken(savedSecondUser.getId());

        String requestBody = """
            {
                "name": "Material Escolar",
                "icon": "book"
            }
            """;

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Material Escolar"))
                .andExpect(jsonPath("$.type").value("CUSTOM"));
    }

    @Test
    void shouldListDefaultCategoriesAndOnlyAuthenticatedUserCustomCategories() throws Exception {

        User user = new User();
        user.setName("Category List User");
        user.setEmail("category.list.user@test.com");
        user.setPasswordHash("test-password");

        User savedUser = userRepository.saveAndFlush(user);

        User otherUser = new User();
        otherUser.setName("Other Category User");
        otherUser.setEmail("category.list.other@test.com");
        otherUser.setPasswordHash("test-password");

        User savedOtherUser = userRepository.saveAndFlush(otherUser);

        Category ownCategory = new Category();
        ownCategory.setName("Material Escolar");
        ownCategory.setType(CategoryType.CUSTOM);
        ownCategory.setUser(savedUser);
        categoryRepository.saveAndFlush(ownCategory);

        Category otherCategory = new Category();
        otherCategory.setName("Ferramentas");
        otherCategory.setType(CategoryType.CUSTOM);
        otherCategory.setUser(savedOtherUser);
        categoryRepository.saveAndFlush(otherCategory);

        String token = jwtService.generateAccessToken(savedUser.getId());

        mockMvc.perform(get("/api/v1/categories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Outros')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'Material Escolar')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'Ferramentas')]").doesNotExist());
    }

    @Test
    void shouldUpdateOwnCustomCategory() throws Exception {

        User user = new User();
        user.setName("Category Update User");
        user.setEmail("category.update.user@test.com");
        user.setPasswordHash("test-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = new Category();
        category.setName("Material Escolar");
        category.setIcon("book");
        category.setType(CategoryType.CUSTOM);
        category.setUser(savedUser);

        Category savedCategory = categoryRepository.saveAndFlush(category);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
        {
            "name": "Papelaria",
            "icon": "pencil"
        }
        """;

        mockMvc.perform(put("/api/v1/categories/{categoryId}", savedCategory.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedCategory.getId()))
                .andExpect(jsonPath("$.name").value("Papelaria"))
                .andExpect(jsonPath("$.icon").value("pencil"))
                .andExpect(jsonPath("$.type").value("CUSTOM"));
    }

    @Test
    void shouldRejectUpdateOfDefaultCategory() throws Exception {

        User user = new User();
        user.setName("Category Default Update User");
        user.setEmail("category.default.update@test.com");
        user.setPasswordHash("test-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category defaultCategory = categoryRepository
                .findAll()
                .stream()
                .filter(category ->
                        category.getType() == CategoryType.DEFAULT
                                && category.getName().equals("Outros"))
                .findFirst()
                .orElseThrow();

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
        {
            "name": "Outros Actualizados",
            "icon": "edit"
        }
        """;

        mockMvc.perform(put("/api/v1/categories/{categoryId}", defaultCategory.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void shouldRejectUpdateOfAnotherUsersCategory() throws Exception {

        User owner = new User();
        owner.setName("Category Owner");
        owner.setEmail("category.update.owner@test.com");
        owner.setPasswordHash("test-password");

        User savedOwner = userRepository.saveAndFlush(owner);

        User otherUser = new User();
        otherUser.setName("Category Other User");
        otherUser.setEmail("category.update.other@test.com");
        otherUser.setPasswordHash("test-password");

        User savedOtherUser = userRepository.saveAndFlush(otherUser);

        Category category = new Category();
        category.setName("Material Escolar");
        category.setType(CategoryType.CUSTOM);
        category.setUser(savedOwner);

        Category savedCategory = categoryRepository.saveAndFlush(category);

        String token = jwtService.generateAccessToken(savedOtherUser.getId());

        String requestBody = """
        {
            "name": "Papelaria",
            "icon": "pencil"
        }
        """;

        mockMvc.perform(put("/api/v1/categories/{categoryId}", savedCategory.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void shouldRejectUpdateWhenCategoryNameAlreadyExists() throws Exception {

        User user = new User();
        user.setName("Category Duplicate Update User");
        user.setEmail("category.duplicate.update@test.com");
        user.setPasswordHash("test-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category existingCategory = new Category();
        existingCategory.setName("Papelaria");
        existingCategory.setType(CategoryType.CUSTOM);
        existingCategory.setUser(savedUser);
        categoryRepository.saveAndFlush(existingCategory);

        Category categoryToUpdate = new Category();
        categoryToUpdate.setName("Material Escolar");
        categoryToUpdate.setType(CategoryType.CUSTOM);
        categoryToUpdate.setUser(savedUser);

        Category savedCategory = categoryRepository.saveAndFlush(categoryToUpdate);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
        {
            "name": "PAPELARIA",
            "icon": "pencil"
        }
        """;

        mockMvc.perform(put("/api/v1/categories/{categoryId}", savedCategory.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_ALREADY_EXISTS"));
    }

    @Test
    void shouldRejectUpdateWhenNameMatchesDefaultCategory() throws Exception {

        User user = new User();
        user.setName("Category Default Name Update");
        user.setEmail("category.default.name.update@test.com");
        user.setPasswordHash("test-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = new Category();
        category.setName("Material Escolar");
        category.setType(CategoryType.CUSTOM);
        category.setUser(savedUser);

        Category savedCategory = categoryRepository.saveAndFlush(category);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
        {
            "name": "ALIMENTAÇÃO",
            "icon": "utensils"
        }
        """;

        mockMvc.perform(put("/api/v1/categories/{categoryId}", savedCategory.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_ALREADY_EXISTS"));
    }

    @Test
    void shouldDeleteOwnCustomCategory() throws Exception {

        User user = new User();
        user.setName("Category Delete User");
        user.setEmail("category.delete.user@test.com");
        user.setPasswordHash("test-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = new Category();
        category.setName("Material Escolar");
        category.setType(CategoryType.CUSTOM);
        category.setUser(savedUser);

        Category savedCategory = categoryRepository.saveAndFlush(category);

        String token = jwtService.generateAccessToken(savedUser.getId());

        mockMvc.perform(delete("/api/v1/categories/{categoryId}", savedCategory.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(categoryRepository.findById(savedCategory.getId()))
                .isEmpty();
    }

    @Test
    void shouldRejectDeletionOfDefaultCategory() throws Exception {

        User user = new User();
        user.setName("Category Default Delete User");
        user.setEmail("category.default.delete@test.com");
        user.setPasswordHash("test-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category defaultCategory = categoryRepository.findAll()
                .stream()
                .filter(category ->
                        category.getType() == CategoryType.DEFAULT
                                && category.getName().equals("Outros"))
                .findFirst()
                .orElseThrow();

        String token = jwtService.generateAccessToken(savedUser.getId());

        mockMvc.perform(delete("/api/v1/categories/{categoryId}", defaultCategory.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));

        assertThat(categoryRepository.findById(defaultCategory.getId()))
                .isPresent();
    }

    @Test
    void shouldRejectDeletionOfAnotherUsersCategory() throws Exception {

        User owner = new User();
        owner.setName("Category Delete Owner");
        owner.setEmail("category.delete.owner@test.com");
        owner.setPasswordHash("test-password");

        User savedOwner = userRepository.saveAndFlush(owner);

        User otherUser = new User();
        otherUser.setName("Category Delete Other User");
        otherUser.setEmail("category.delete.other@test.com");
        otherUser.setPasswordHash("test-password");

        User savedOtherUser = userRepository.saveAndFlush(otherUser);

        Category category = new Category();
        category.setName("Material Escolar");
        category.setType(CategoryType.CUSTOM);
        category.setUser(savedOwner);

        Category savedCategory = categoryRepository.saveAndFlush(category);

        String token = jwtService.generateAccessToken(savedOtherUser.getId());

        mockMvc.perform(delete("/api/v1/categories/{categoryId}", savedCategory.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));

        assertThat(categoryRepository.findById(savedCategory.getId()))
                .isPresent();
    }

    @Test
    void shouldRejectDeletionOfNonExistingCategory() throws Exception {

        User user = new User();
        user.setName("Category Delete Not Found User");
        user.setEmail("category.delete.notfound@test.com");
        user.setPasswordHash("test-password");

        User savedUser = userRepository.saveAndFlush(user);

        String token = jwtService.generateAccessToken(savedUser.getId());

        mockMvc.perform(delete("/api/v1/categories/{categoryId}", 0L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }
}
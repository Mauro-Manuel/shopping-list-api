package com.masprog.shopping_list_api.product;

import com.masprog.shopping_list_api.auth.jwt.JwtService;
import com.masprog.shopping_list_api.category.Category;
import com.masprog.shopping_list_api.category.CategoryRepository;
import com.masprog.shopping_list_api.category.CategoryType;
import com.masprog.shopping_list_api.user.User;
import com.masprog.shopping_list_api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.springframework.dao.DataIntegrityViolationException;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProductControllerIntegrationTest {

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
    private JwtService jwtService;

    // CREATE

    @Test
    void shouldCreateProductWhenAuthenticatedUserProvidesValidData() throws Exception {

        // Arrange: criar utilizador
        User user = new User();
        user.setName("Product Test User");
        user.setEmail("product.create@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        // Obter uma categoria DEFAULT existente
        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Laticínios"))
                .findFirst()
                .orElseThrow();

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
                {
                    "name": "Leite",
                    "categoryId": %d
                }
                """.formatted(category.getId());

        // Act & Assert
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Leite"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.category.id").value(category.getId()))
                .andExpect(jsonPath("$.category.name").value("Laticínios"));

        // Confirmar persistência na base de dados
        Product savedProduct = productRepository.findAll()
                .stream()
                .filter(p -> p.getName().equals("Leite")
                        && p.getUser().getId().equals(savedUser.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(savedProduct.getActive()).isTrue();
        assertThat(savedProduct.getCategory().getId())
                .isEqualTo(category.getId());
    }

    @Test
    void shouldReturnConflictWhenProductAlreadyExists() throws Exception {

        User user = new User();
        user.setName("Product Duplicate User");
        user.setEmail("product.duplicate@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Laticínios"))
                .findFirst()
                .orElseThrow();

        Product existingProduct = new Product();
        existingProduct.setName("Leite");
        existingProduct.setActive(true);
        existingProduct.setCategory(category);
        existingProduct.setUser(savedUser);

        productRepository.saveAndFlush(existingProduct);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
            {
                "name": "leite",
                "categoryId": %d
            }
            """.formatted(category.getId());

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Product already exists"));
    }

    @Test
    void shouldRejectProductCreationWhenNameIsBlank() throws Exception {

        User user = new User();
        user.setName("Product Validation User");
        user.setEmail("product.validation@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
            {
                "name": "   ",
                "categoryId": 5
            }
            """;

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.name")
                        .value("Product name is required"));
    }

    @Test
    void shouldRejectProductCreationWhenCategoryIdIsMissing() throws Exception {

        User user = new User();
        user.setName("Product Missing Category User");
        user.setEmail("product.missing.category@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
            {
                "name": "Leite"
            }
            """;

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.categoryId")
                        .value("Category is required"));
    }

    @Test
    void shouldRejectProductCreationWhenCategoryDoesNotExist() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Product Invalid Category User");
        user.setEmail("product.invalid.category@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
            {
                "name": "Leite",
                "categoryId": -1
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Category not found"));
    }

    @Test
    void shouldRejectProductCreationWithAnotherUsersCategory() throws Exception {

        // Arrange: proprietário da categoria
        User owner = new User();
        owner.setName("Category Owner");
        owner.setEmail("product.category.owner@test.com");
        owner.setPasswordHash("hashed-password");

        User savedOwner = userRepository.saveAndFlush(owner);

        // Outro utilizador
        User otherUser = new User();
        otherUser.setName("Other Product User");
        otherUser.setEmail("product.category.other@test.com");
        otherUser.setPasswordHash("hashed-password");

        User savedOtherUser = userRepository.saveAndFlush(otherUser);

        // Categoria CUSTOM pertencente ao primeiro utilizador
        Category category = new Category();
        category.setName("Produtos Especiais");
        category.setType(CategoryType.CUSTOM);
        category.setUser(savedOwner);

        Category savedCategory = categoryRepository.saveAndFlush(category);

        // Autenticar o segundo utilizador
        String token = jwtService.generateAccessToken(savedOtherUser.getId());

        String requestBody = """
            {
                "name": "Leite",
                "categoryId": %d
            }
            """.formatted(savedCategory.getId());

        // Act & Assert
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Category not found"));
    }

    @Test
    void shouldRejectProductCreationWithoutAuthentication() throws Exception {

        String requestBody = """
            {
                "name": "Leite",
                "categoryId": 1
            }
            """;

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void shouldRejectProductCreationWithDuplicateNameContainingExtraSpaces() throws Exception {

        // Arrange: criar utilizador
        User user = new User();
        user.setName("Product Normalization User");
        user.setEmail("product.normalization@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Laticínios"))
                .findFirst()
                .orElseThrow();

        // Produto existente
        Product existingProduct = new Product();
        existingProduct.setName("Leite Integral");
        existingProduct.setActive(true);
        existingProduct.setCategory(category);
        existingProduct.setUser(savedUser);

        productRepository.saveAndFlush(existingProduct);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Nome com espaços repetidos e letras maiúsculas
        String requestBody = """
            {
                "name": "  LEITE   INTEGRAL  ",
                "categoryId": %d
            }
            """.formatted(category.getId());

        // Act & Assert
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_ALREADY_EXISTS"));

        // Confirmar que não foi criado outro produto
        assertThat(productRepository.count()).isEqualTo(1);
    }

    // GET ALL

    @Test
    void shouldListOnlyAuthenticatedUsersActiveProducts() throws Exception {

        // Arrange: utilizador autenticado
        User user = new User();
        user.setName("Product List User");
        user.setEmail("product.list.user@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        // Outro utilizador
        User otherUser = new User();
        otherUser.setName("Other Product List User");
        otherUser.setEmail("product.list.other@test.com");
        otherUser.setPasswordHash("hashed-password");

        User savedOtherUser = userRepository.saveAndFlush(otherUser);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Laticínios"))
                .findFirst()
                .orElseThrow();

        // Produto activo do utilizador autenticado
        Product ownProduct = new Product();
        ownProduct.setName("Iogurte Natural");
        ownProduct.setActive(true);
        ownProduct.setCategory(category);
        ownProduct.setUser(savedUser);
        productRepository.saveAndFlush(ownProduct);

        // Produto inactivo do mesmo utilizador
        Product inactiveProduct = new Product();
        inactiveProduct.setName("Queijo Fresco");
        inactiveProduct.setActive(false);
        inactiveProduct.setCategory(category);
        inactiveProduct.setUser(savedUser);
        productRepository.saveAndFlush(inactiveProduct);

        // Produto de outro utilizador
        Product otherProduct = new Product();
        otherProduct.setName("Manteiga Especial");
        otherProduct.setActive(true);
        otherProduct.setCategory(category);
        otherProduct.setUser(savedOtherUser);
        productRepository.saveAndFlush(otherProduct);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act & Assert
        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.name == 'Iogurte Natural')]").exists())
                .andExpect(jsonPath("$.content[?(@.name == 'Queijo Fresco')]").doesNotExist())
                .andExpect(jsonPath("$.content[?(@.name == 'Manteiga Especial')]").doesNotExist());
    }
    @Test
    void shouldSearchProductsByNameIgnoringCase() throws Exception {

        User user = new User();
        user.setName("Product Search User");
        user.setEmail("product.search@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Laticínios"))
                .findFirst()
                .orElseThrow();

        Product milk = new Product();
        milk.setName("Leite Integral");
        milk.setActive(true);
        milk.setCategory(category);
        milk.setUser(savedUser);
        productRepository.saveAndFlush(milk);

        Product yogurt = new Product();
        yogurt.setName("Iogurte Natural");
        yogurt.setActive(true);
        yogurt.setCategory(category);
        yogurt.setUser(savedUser);
        productRepository.saveAndFlush(yogurt);

        String token = jwtService.generateAccessToken(savedUser.getId());

        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .param("search", "LEI")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Leite Integral"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }
    @Test
    void shouldPaginateAuthenticatedUsersProducts() throws Exception {

        User user = new User();
        user.setName("Product Pagination User");
        user.setEmail("product.pagination@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Laticínios"))
                .findFirst()
                .orElseThrow();

        for (int i = 1; i <= 5; i++) {
            Product product = new Product();
            product.setName("Produto Paginação " + i);
            product.setActive(true);
            product.setCategory(category);
            product.setUser(savedUser);

            productRepository.save(product);
        }

        productRepository.flush();

        String token = jwtService.generateAccessToken(savedUser.getId());

        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name")
                        .value("Produto Paginação 3"))
                .andExpect(jsonPath("$.content[1].name")
                        .value("Produto Paginação 4"))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(5))
                .andExpect(jsonPath("$.page.totalPages").value(3));
    }

    // GET BY PRODUCT ID

    @Test
    void shouldFindOwnProductById() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Product Find User");
        user.setEmail("product.find@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Laticínios"))
                .findFirst()
                .orElseThrow();

        Product product = new Product();
        product.setName("Leite Desnatado");
        product.setActive(true);
        product.setCategory(category);
        product.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act & Assert
        mockMvc.perform(get("/api/v1/products/{productId}", savedProduct.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedProduct.getId()))
                .andExpect(jsonPath("$.name").value("Leite Desnatado"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.category.id").value(category.getId()))
                .andExpect(jsonPath("$.category.name").value("Laticínios"));
    }
    @Test
    void shouldRejectAccessToAnotherUsersProduct() throws Exception {

        // Arrange: proprietário do produto
        User owner = new User();
        owner.setName("Product Owner");
        owner.setEmail("product.find.owner@test.com");
        owner.setPasswordHash("hashed-password");

        User savedOwner = userRepository.saveAndFlush(owner);

        // Outro utilizador
        User otherUser = new User();
        otherUser.setName("Other Product User");
        otherUser.setEmail("product.find.other@test.com");
        otherUser.setPasswordHash("hashed-password");

        User savedOtherUser = userRepository.saveAndFlush(otherUser);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Laticínios"))
                .findFirst()
                .orElseThrow();

        // Produto pertencente ao primeiro utilizador
        Product product = new Product();
        product.setName("Leite Especial");
        product.setActive(true);
        product.setCategory(category);
        product.setUser(savedOwner);

        Product savedProduct = productRepository.saveAndFlush(product);

        // Autenticar o segundo utilizador
        String token = jwtService.generateAccessToken(savedOtherUser.getId());

        // Act & Assert
        mockMvc.perform(get("/api/v1/products/{productId}", savedProduct.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Product not found"));

        // Confirmar que o produto continua na base de dados
        assertThat(productRepository.findById(savedProduct.getId()))
                .isPresent();
    }
    @Test
    void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Product Not Found User");
        user.setEmail("product.notfound@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act & Assert
        mockMvc.perform(get("/api/v1/products/{productId}", -1L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Product not found"));
    }

    // PUT PRODUCT BY ID

    @Test
    void shouldUpdateOwnProduct() throws Exception {

        // Arrange: criar utilizador
        User user = new User();
        user.setName("Product Update User");
        user.setEmail("product.update@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        // Obter categorias DEFAULT
        Category originalCategory = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Laticínios"))
                .findFirst()
                .orElseThrow();

        Category newCategory = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Bebidas"))
                .findFirst()
                .orElseThrow();

        // Criar produto
        Product product = new Product();
        product.setName("Leite Integral");
        product.setActive(true);
        product.setCategory(originalCategory);
        product.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
            {
                "name": "Leite de Soja",
                "categoryId": %d
            }
            """.formatted(newCategory.getId());

        // Act & Assert
        mockMvc.perform(put("/api/v1/products/{productId}", savedProduct.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedProduct.getId()))
                .andExpect(jsonPath("$.name").value("Leite de Soja"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.category.id").value(newCategory.getId()))
                .andExpect(jsonPath("$.category.name").value("Bebidas"));

        // Confirmar actualização na base de dados
        Product updatedProduct = productRepository.findById(savedProduct.getId())
                .orElseThrow();

        assertThat(updatedProduct.getName()).isEqualTo("Leite de Soja");
        assertThat(updatedProduct.getCategory().getId())
                .isEqualTo(newCategory.getId());
    }
    @Test
    void shouldRejectUpdatingAnotherUsersProduct() throws Exception {

        // Proprietário do produto
        User owner = new User();
        owner.setName("Product Update Owner");
        owner.setEmail("product.update.owner@test.com");
        owner.setPasswordHash("hashed-password");
        User savedOwner = userRepository.saveAndFlush(owner);

        // Outro utilizador
        User otherUser = new User();
        otherUser.setName("Product Update Other");
        otherUser.setEmail("product.update.other@test.com");
        otherUser.setPasswordHash("hashed-password");
        User savedOtherUser = userRepository.saveAndFlush(otherUser);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Laticínios"))
                .findFirst()
                .orElseThrow();

        // Produto do proprietário
        Product product = new Product();
        product.setName("Leite Integral");
        product.setActive(true);
        product.setCategory(category);
        product.setUser(savedOwner);
        Product savedProduct = productRepository.saveAndFlush(product);

        String token = jwtService.generateAccessToken(savedOtherUser.getId());

        String requestBody = """
            {
                "name": "Produto Alterado",
                "categoryId": %d
            }
            """.formatted(category.getId());

        // Tentar actualizar com outro utilizador
        mockMvc.perform(put("/api/v1/products/{productId}", savedProduct.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));

        // Confirmar que o produto não foi alterado
        Product unchangedProduct = productRepository.findById(savedProduct.getId())
                .orElseThrow();

        assertThat(unchangedProduct.getName()).isEqualTo("Leite Integral");
        assertThat(unchangedProduct.getCategory().getId())
                .isEqualTo(category.getId());
    }
    @Test
    void shouldRejectUpdatingProductWithDuplicateName() throws Exception {

        // Arrange: criar utilizador
        User user = new User();
        user.setName("Product Duplicate Update User");
        user.setEmail("product.duplicate.update@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Laticínios"))
                .findFirst()
                .orElseThrow();

        // Primeiro produto
        Product firstProduct = new Product();
        firstProduct.setName("Leite Integral");
        firstProduct.setActive(true);
        firstProduct.setCategory(category);
        firstProduct.setUser(savedUser);
        productRepository.saveAndFlush(firstProduct);

        // Segundo produto
        Product secondProduct = new Product();
        secondProduct.setName("Iogurte Natural");
        secondProduct.setActive(true);
        secondProduct.setCategory(category);
        secondProduct.setUser(savedUser);

        Product savedSecondProduct = productRepository.saveAndFlush(secondProduct);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
            {
                "name": "LEITE INTEGRAL",
                "categoryId": %d
            }
            """.formatted(category.getId());

        // Act & Assert
        mockMvc.perform(put("/api/v1/products/{productId}", savedSecondProduct.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_ALREADY_EXISTS"));

        // Confirmar que o segundo produto não foi alterado
        Product unchangedProduct = productRepository.findById(savedSecondProduct.getId())
                .orElseThrow();

        assertThat(unchangedProduct.getName()).isEqualTo("Iogurte Natural");
    }
    @Test
    void shouldUpdateProductKeepingSameName() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Product Same Name User");
        user.setEmail("product.same.name@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category originalCategory = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Laticínios"))
                .findFirst()
                .orElseThrow();

        Category newCategory = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Bebidas"))
                .findFirst()
                .orElseThrow();

        Product product = new Product();
        product.setName("Leite Integral");
        product.setActive(true);
        product.setCategory(originalCategory);
        product.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
            {
                "name": "Leite Integral",
                "categoryId": %d
            }
            """.formatted(newCategory.getId());

        // Act & Assert
        mockMvc.perform(put("/api/v1/products/{productId}", savedProduct.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedProduct.getId()))
                .andExpect(jsonPath("$.name").value("Leite Integral"))
                .andExpect(jsonPath("$.category.id").value(newCategory.getId()));

        // Confirmar persistência
        Product updatedProduct = productRepository.findById(savedProduct.getId())
                .orElseThrow();

        assertThat(updatedProduct.getName()).isEqualTo("Leite Integral");
        assertThat(updatedProduct.getCategory().getId())
                .isEqualTo(newCategory.getId());
    }
    @Test
    void shouldRejectUpdatingProductWithAnotherUsersCategory() throws Exception {

        // Proprietário do produto
        User owner = new User();
        owner.setName("Product Category Update Owner");
        owner.setEmail("product.category.update.owner@test.com");
        owner.setPasswordHash("hashed-password");
        User savedOwner = userRepository.saveAndFlush(owner);

        // Outro utilizador
        User otherUser = new User();
        otherUser.setName("Product Category Update Other");
        otherUser.setEmail("product.category.update.other@test.com");
        otherUser.setPasswordHash("hashed-password");
        User savedOtherUser = userRepository.saveAndFlush(otherUser);

        Category defaultCategory = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Laticínios"))
                .findFirst()
                .orElseThrow();

        // Categoria personalizada pertencente ao outro utilizador
        Category foreignCategory = new Category();
        foreignCategory.setName("Categoria Privada");
        foreignCategory.setIcon("lock");
        foreignCategory.setType(CategoryType.CUSTOM);
        foreignCategory.setUser(savedOtherUser);

        Category savedForeignCategory = categoryRepository.saveAndFlush(foreignCategory);

        // Produto do proprietário
        Product product = new Product();
        product.setName("Leite Integral");
        product.setActive(true);
        product.setCategory(defaultCategory);
        product.setUser(savedOwner);

        Product savedProduct = productRepository.saveAndFlush(product);

        String token = jwtService.generateAccessToken(savedOwner.getId());

        String requestBody = """
            {
                "name": "Leite Integral",
                "categoryId": %d
            }
            """.formatted(savedForeignCategory.getId());

        // Tentar associar a categoria de outro utilizador
        mockMvc.perform(put("/api/v1/products/{productId}", savedProduct.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));

        // Confirmar que a categoria original foi preservada
        Product unchangedProduct = productRepository.findById(savedProduct.getId())
                .orElseThrow();

        assertThat(unchangedProduct.getCategory().getId())
                .isEqualTo(defaultCategory.getId());
    }
    @Test
    void shouldRejectUpdatingProductWithNonExistentCategory() throws Exception {

        // Arrange: criar utilizador
        User user = new User();
        user.setName("Product Invalid Category User");
        user.setEmail("product.invalid.category.update@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Laticínios"))
                .findFirst()
                .orElseThrow();

        // Criar produto
        Product product = new Product();
        product.setName("Leite Integral");
        product.setActive(true);
        product.setCategory(category);
        product.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
            {
                "name": "Leite de Soja",
                "categoryId": -1
            }
            """;

        // Act & Assert
        mockMvc.perform(put("/api/v1/products/{productId}", savedProduct.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));

        // Confirmar que o produto não foi alterado
        Product unchangedProduct = productRepository.findById(savedProduct.getId())
                .orElseThrow();

        assertThat(unchangedProduct.getName()).isEqualTo("Leite Integral");
        assertThat(unchangedProduct.getCategory().getId())
                .isEqualTo(category.getId());
    }

    // DELETE PRODUCT

    @Test
    void shouldSoftDeleteOwnProduct() throws Exception {

        // Arrange: criar utilizador
        User user = new User();
        user.setName("Product Delete User");
        user.setEmail("product.delete@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Laticínios"))
                .findFirst()
                .orElseThrow();

        // Criar produto activo
        Product product = new Product();
        product.setName("Leite Integral");
        product.setActive(true);
        product.setCategory(category);
        product.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act: desactivar produto
        mockMvc.perform(delete("/api/v1/products/{productId}", savedProduct.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Assert: produto continua na base de dados
        Product deletedProduct = productRepository.findById(savedProduct.getId())
                .orElseThrow();

        assertThat(deletedProduct.getActive()).isFalse();
        assertThat(deletedProduct.getName()).isEqualTo("Leite Integral");
        assertThat(deletedProduct.getCategory().getId())
                .isEqualTo(category.getId());
    }
    @Test
    void shouldRejectDeletingAnotherUsersProduct() throws Exception {

        // Arrange: proprietário do produto
        User owner = new User();
        owner.setName("Product Delete Owner");
        owner.setEmail("product.delete.owner@test.com");
        owner.setPasswordHash("hashed-password");
        User savedOwner = userRepository.saveAndFlush(owner);

        // Outro utilizador
        User otherUser = new User();
        otherUser.setName("Product Delete Other");
        otherUser.setEmail("product.delete.other@test.com");
        otherUser.setPasswordHash("hashed-password");
        User savedOtherUser = userRepository.saveAndFlush(otherUser);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Laticínios"))
                .findFirst()
                .orElseThrow();

        // Produto pertencente ao primeiro utilizador
        Product product = new Product();
        product.setName("Leite Integral");
        product.setActive(true);
        product.setCategory(category);
        product.setUser(savedOwner);

        Product savedProduct = productRepository.saveAndFlush(product);

        // Autenticar o segundo utilizador
        String token = jwtService.generateAccessToken(savedOtherUser.getId());

        // Act & Assert
        mockMvc.perform(delete("/api/v1/products/{productId}", savedProduct.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));

        // Confirmar que o produto continua activo
        Product unchangedProduct = productRepository.findById(savedProduct.getId())
                .orElseThrow();

        assertThat(unchangedProduct.getActive()).isTrue();
        assertThat(unchangedProduct.getName()).isEqualTo("Leite Integral");
    }
    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentProduct() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Product Delete Not Found User");
        user.setEmail("product.delete.notfound@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act & Assert
        mockMvc.perform(delete("/api/v1/products/{productId}", -1L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Product not found"));
    }

    // OTHER

    @Test
    void shouldNormalizeProductNameWhenCreating() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Product Name Normalization User");
        user.setEmail("product.name.normalize@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Laticínios"))
                .findFirst()
                .orElseThrow();

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
            {
                "name": "  Leite   Integral  ",
                "categoryId": %d
            }
            """.formatted(category.getId());

        // Act & Assert
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Leite Integral"));

        // Confirmar persistência
        Page<Product> products = productRepository.findByUserIdAndActiveTrue(
                savedUser.getId(),
                Pageable.unpaged()
        );

        assertThat(products.getContent()).hasSize(1);
        assertThat(products.getContent().get(0).getName())
                .isEqualTo("Leite Integral");
    }
    @Test
    void shouldNormalizeProductNameWhenUpdating() throws Exception {

        // Arrange
        User user = new User();
        user.setName("Product Update Normalization User");
        user.setEmail("product.update.normalize@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Laticínios"))
                .findFirst()
                .orElseThrow();

        Product product = new Product();
        product.setName("Leite");
        product.setActive(true);
        product.setCategory(category);
        product.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        String token = jwtService.generateAccessToken(savedUser.getId());

        String requestBody = """
            {
                "name": "  Leite   Integral  ",
                "categoryId": %d
            }
            """.formatted(category.getId());

        // Act & Assert
        mockMvc.perform(put("/api/v1/products/{id}", savedProduct.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Leite Integral"));

        // Confirmar persistência na base de dados
        Product updatedProduct = productRepository.findById(savedProduct.getId())
                .orElseThrow();

        assertThat(updatedProduct.getName())
                .isEqualTo("Leite Integral");
    }
    @Test
    void shouldRejectDuplicateNormalizedProductNameAtDatabaseLevel() {

        // Arrange
        User user = new User();
        user.setName("Database Unique Test");
        user.setEmail("product.db.unique@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Laticínios"))
                .findFirst()
                .orElseThrow();

        Product firstProduct = new Product();
        firstProduct.setName("Leite Integral");
        firstProduct.setActive(true);
        firstProduct.setCategory(category);
        firstProduct.setUser(savedUser);

        productRepository.saveAndFlush(firstProduct);

        // Act: tentar guardar directamente outro nome equivalente
        Product duplicateProduct = new Product();
        duplicateProduct.setName("  LEITE   INTEGRAL  ");
        duplicateProduct.setActive(true);
        duplicateProduct.setCategory(category);
        duplicateProduct.setUser(savedUser);

        // Assert
        assertThatThrownBy(() ->
                productRepository.saveAndFlush(duplicateProduct)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldReturnNoContentWhenDeletingAlreadyInactiveProduct() throws Exception {

        // Arrange: criar utilizador
        User user = new User();
        user.setName("Product Repeated Delete User");
        user.setEmail("product.repeated.delete@test.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        Category category = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getType() == CategoryType.DEFAULT
                        && c.getName().equals("Laticínios"))
                .findFirst()
                .orElseThrow();

        // Criar produto activo
        Product product = new Product();
        product.setName("Leite Integral");
        product.setActive(true);
        product.setCategory(category);
        product.setUser(savedUser);

        Product savedProduct = productRepository.saveAndFlush(product);

        String token = jwtService.generateAccessToken(savedUser.getId());

        // Act: primeira eliminação
        mockMvc.perform(delete("/api/v1/products/{productId}", savedProduct.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Act: segunda eliminação do mesmo produto
        mockMvc.perform(delete("/api/v1/products/{productId}", savedProduct.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Assert: produto continua na base de dados e inactivo
        Product deletedProduct = productRepository.findById(savedProduct.getId())
                .orElseThrow();

        assertThat(deletedProduct.getActive()).isFalse();
        assertThat(deletedProduct.getName()).isEqualTo("Leite Integral");
        assertThat(deletedProduct.getCategory().getId())
                .isEqualTo(category.getId());
    }
}
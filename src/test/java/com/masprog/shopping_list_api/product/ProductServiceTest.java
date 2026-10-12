package com.masprog.shopping_list_api.product;

import com.masprog.shopping_list_api.category.Category;
import com.masprog.shopping_list_api.category.CategoryRepository;
import com.masprog.shopping_list_api.category.CategoryType;
import com.masprog.shopping_list_api.category.exception.CategoryNotFoundException;
import com.masprog.shopping_list_api.product.exception.ProductAlreadyExistsException;
import com.masprog.shopping_list_api.user.User;
import com.masprog.shopping_list_api.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.masprog.shopping_list_api.auth.exception.AuthenticatedUserNotFoundException;
import com.masprog.shopping_list_api.product.exception.ProductValidationException;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldCreateProductWhenNameAndCategoryAreValid() {

        User user = new User();
        user.setId(1L);
        user.setEmail("mauro@test.com");

        Category category = new Category();
        category.setId(5L);
        category.setName("Laticínios");
        category.setType(CategoryType.DEFAULT);

        when(userRepository.findByEmail("mauro@test.com"))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findById(5L))
                .thenReturn(Optional.of(category));

        when(productRepository.existsByUserIdAndNameIgnoreCase(1L, "Leite"))
                .thenReturn(false);

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product product = invocation.getArgument(0);
                    product.setId(15L);
                    return product;
                });

        Product result = productService.create(
                "Leite",
                5L,
                "mauro@test.com"
        );

        assertThat(result.getId()).isEqualTo(15L);
        assertThat(result.getName()).isEqualTo("Leite");
        assertThat(result.getActive()).isTrue();
        assertThat(result.getCategory()).isEqualTo(category);
        assertThat(result.getUser()).isEqualTo(user);

        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldRejectDuplicateProductNameIgnoringCase() {

        User user = new User();
        user.setId(1L);
        user.setEmail("mauro@test.com");

        Category category = new Category();
        category.setId(5L);
        category.setName("Laticínios");
        category.setType(CategoryType.DEFAULT);

        when(userRepository.findByEmail("mauro@test.com"))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findById(5L))
                .thenReturn(Optional.of(category));

        when(productRepository.existsByUserIdAndNameIgnoreCase(1L, "leite"))
                .thenReturn(true);

        assertThatThrownBy(() ->
                productService.create("  leite  ", 5L, "mauro@test.com")
        )
                .isInstanceOf(ProductAlreadyExistsException.class)
                .hasMessage("Product already exists");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectProductCreationWithAnotherUsersCategory() {

        User authenticatedUser = new User();
        authenticatedUser.setId(1L);
        authenticatedUser.setEmail("mauro@test.com");

        User categoryOwner = new User();
        categoryOwner.setId(2L);
        categoryOwner.setEmail("outro@test.com");

        Category category = new Category();
        category.setId(10L);
        category.setName("Material Escolar");
        category.setType(CategoryType.CUSTOM);
        category.setUser(categoryOwner);

        when(userRepository.findByEmail("mauro@test.com"))
                .thenReturn(Optional.of(authenticatedUser));

        when(categoryRepository.findById(10L))
                .thenReturn(Optional.of(category));

        assertThatThrownBy(() ->
                productService.create("Caderno", 10L, "mauro@test.com")
        )
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Category not found");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectProductCreationWhenCategoryDoesNotExist() {

        User user = new User();
        user.setId(1L);
        user.setEmail("mauro@test.com");

        when(userRepository.findByEmail("mauro@test.com"))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                productService.create("Leite", 999L, "mauro@test.com")
        )
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Category not found");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectProductCreationWhenNameIsBlank() {

        assertThatThrownBy(() ->
                productService.create("   ", 5L, "mauro@test.com")
        )
                .isInstanceOf(ProductValidationException.class)
                .hasMessage("Product name is required");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectProductCreationWhenNameIsNull() {

        assertThatThrownBy(() ->
                productService.create(null, 5L, "mauro@test.com")
        )
                .isInstanceOf(ProductValidationException.class)
                .hasMessage("Product name is required");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectProductCreationWhenAuthenticatedUserDoesNotExist() {

        when(userRepository.findByEmail("unknown@test.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                productService.create("Leite", 5L, "unknown@test.com")
        )
                .isInstanceOf(AuthenticatedUserNotFoundException.class)
                .hasMessage("Authenticated user not found");

        verifyNoInteractions(categoryRepository, productRepository);
    }


}
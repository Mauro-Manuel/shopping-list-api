package com.masprog.shopping_list_api.product;


import com.masprog.shopping_list_api.product.exception.ProductNotFoundException;
import com.masprog.shopping_list_api.product.dto.ProductResponse;
import com.masprog.shopping_list_api.product.dto.ProductCategoryResponse;

import com.masprog.shopping_list_api.auth.exception.AuthenticatedUserNotFoundException;
import com.masprog.shopping_list_api.category.exception.CategoryNotFoundException;
import com.masprog.shopping_list_api.product.dto.ProductCategoryResponse;
import com.masprog.shopping_list_api.product.exception.ProductAlreadyExistsException;
import com.masprog.shopping_list_api.product.exception.ProductValidationException;
import com.masprog.shopping_list_api.category.Category;
import com.masprog.shopping_list_api.category.CategoryRepository;
import com.masprog.shopping_list_api.category.CategoryType;
import com.masprog.shopping_list_api.user.User;
import com.masprog.shopping_list_api.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;


@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository productRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository
    ) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Product create(String name, Long categoryId, String authenticatedEmail) {

        if (name == null || name.isBlank()) {
            throw new ProductValidationException("Product name is required");
        }

        User user = getAuthenticatedUser(authenticatedEmail);

        Category category = getAccessibleCategory(categoryId, user);

        String normalizedName = ProductNameNormalizer.normalize(name);

        if (productRepository.existsByUserIdAndNameIgnoreCase(
                user.getId(), normalizedName)) {

            throw new ProductAlreadyExistsException("Product already exists");
        }

        Product product = new Product();
        product.setName(normalizedName);
        product.setActive(true);
        product.setCategory(category);
        product.setUser(user);

        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> findAll(
            String authenticatedEmail,
            String search,
            Pageable pageable
    ) {
        User user = getAuthenticatedUser(authenticatedEmail);

        Page<Product> products;

        if (search == null || search.isBlank()) {
            products = productRepository.findByUserIdAndActiveTrue(
                    user.getId(),
                    pageable
            );
        } else {
            products = productRepository
                    .findByUserIdAndActiveTrueAndNameContainingIgnoreCase(
                            user.getId(),
                            search.trim(),
                            pageable
                    );
        }

        return products.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long productId, String authenticatedEmail) {

        User user = getAuthenticatedUser(authenticatedEmail);

        Product product = productRepository.findByIdAndUserId(
                productId,
                user.getId()
        ).orElseThrow(() ->
                new ProductNotFoundException("Product not found")
        );

        return toResponse(product);
    }


    @Transactional
    public ProductResponse update(
            Long productId,
            String name,
            Long categoryId,
            String authenticatedEmail
    ) {
        if (name == null || name.isBlank()) {
            throw new ProductValidationException("Product name is required");
        }

        User user = getAuthenticatedUser(authenticatedEmail);

        Product product = productRepository.findByIdAndUserId(
                productId,
                user.getId()
        ).orElseThrow(() ->
                new ProductNotFoundException("Product not found")
        );

        Category category = getAccessibleCategory(categoryId, user);

        String normalizedName = ProductNameNormalizer.normalize(name);

        if (productRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(
                user.getId(),
                normalizedName,
                productId
        )) {
            throw new ProductAlreadyExistsException("Product already exists");
        }

        product.setName(normalizedName);
        product.setCategory(category);

        return toResponse(product);
    }

    @Transactional
    public void delete(Long productId, String authenticatedEmail) {

        User user = getAuthenticatedUser(authenticatedEmail);

        Product product = productRepository.findByIdAndUserId(
                productId,
                user.getId()
        ).orElseThrow(() ->
                new ProductNotFoundException("Product not found")
        );

        product.setActive(false);
    }

    @Transactional
    public Product findOrCreate(
            String name,
            Long categoryId,
            String authenticatedEmail
    ) {

        if (name == null || name.isBlank()) {
            throw new ProductValidationException("Product name is required");
        }

        User user = getAuthenticatedUser(authenticatedEmail);

        // Validar a categoria antes de procurar o produto
        getAccessibleCategory(categoryId, user);

        String normalizedName = ProductNameNormalizer.normalize(name);

        Optional<Product> existingProduct =
                productRepository.findByUserIdAndNameIgnoreCase(
                        user.getId(),
                        normalizedName
                );

        if (existingProduct.isPresent()) {

            Product product = existingProduct.get();

            // Reactivar o produto caso esteja inactivo
            if (!product.getActive()) {
                product.setActive(true);
            }

            return product;
        }

        return create(normalizedName, categoryId, authenticatedEmail);
    }

    private User getAuthenticatedUser(String authenticatedEmail) {
        return userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new AuthenticatedUserNotFoundException(
                                "Authenticated user not found"
                        ));
    }

    private Category getAccessibleCategory(Long categoryId, User user) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new CategoryNotFoundException("Category not found")
                );

        if (category.getType() == CategoryType.CUSTOM
                && (category.getUser() == null
                || !category.getUser().getId().equals(user.getId()))) {

            throw new CategoryNotFoundException("Category not found");
        }

        return category;
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getActive(),
                new ProductCategoryResponse(
                        product.getCategory().getId(),
                        product.getCategory().getName(),
                        product.getCategory().getIcon()
                )
        );
    }
}
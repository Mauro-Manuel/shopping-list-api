package com.masprog.shopping_list_api.category;

import com.masprog.shopping_list_api.auth.exception.AuthenticatedUserNotFoundException;
import com.masprog.shopping_list_api.category.exception.CategoryNotFoundException;
import com.masprog.shopping_list_api.user.User;
import com.masprog.shopping_list_api.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.masprog.shopping_list_api.category.exception.CategoryAlreadyExistsException;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Category create(String name, String icon, String authenticatedEmail) {

        User user = getAuthenticatedUser(authenticatedEmail);

        String normalizedName = name.trim();

        boolean customCategoryExists = categoryRepository
                .existsByUserIdAndTypeAndNameIgnoreCase(
                        user.getId(),
                        CategoryType.CUSTOM,
                        normalizedName
                );

        boolean defaultCategoryExists = categoryRepository
                .existsByTypeAndNameIgnoreCase(
                        CategoryType.DEFAULT,
                        normalizedName
                );

        if (customCategoryExists || defaultCategoryExists) {
            throw new CategoryAlreadyExistsException(
                    "Category already exists: " + normalizedName
            );
        }

        Category category = new Category();
        category.setName(normalizedName);
        category.setIcon(icon);
        category.setType(CategoryType.CUSTOM);
        category.setUser(user);

        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public List<Category> findAvailableCategories(String authenticatedEmail) {

        User user = getAuthenticatedUser(authenticatedEmail);

        return categoryRepository.findByTypeOrUserId(
                CategoryType.DEFAULT,
                user.getId()
        );
    }

    @Transactional
    public Category update(
            Long categoryId,
            String name,
            String icon,
            String authenticatedEmail) {

        User user = getAuthenticatedUser(authenticatedEmail);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new CategoryNotFoundException("Category not found"));

        if (category.getType() != CategoryType.CUSTOM
                || !category.getUser().getId().equals(user.getId())) {
            throw new CategoryNotFoundException("Category not found");
        }

        String normalizedName = name.trim();

        boolean nameChanged = !category.getName()
                .equalsIgnoreCase(normalizedName);

        if (nameChanged) {

            boolean customCategoryExists =
                    categoryRepository.existsByUserIdAndTypeAndNameIgnoreCase(
                            user.getId(),
                            CategoryType.CUSTOM,
                            normalizedName
                    );

            boolean defaultCategoryExists =
                    categoryRepository.existsByTypeAndNameIgnoreCase(
                            CategoryType.DEFAULT,
                            normalizedName
                    );

            if (customCategoryExists || defaultCategoryExists) {
                throw new CategoryAlreadyExistsException(
                        "Category already exists: " + normalizedName
                );
            }
        }

        category.setName(normalizedName);
        category.setIcon(icon);

        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long categoryId, String authenticatedEmail) {

        User user = getAuthenticatedUser(authenticatedEmail);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new CategoryNotFoundException("Category not found"));

        if (category.getType() != CategoryType.CUSTOM
                || !category.getUser().getId().equals(user.getId())) {
            throw new CategoryNotFoundException("Category not found");
        }

        categoryRepository.delete(category);
    }

    private User getAuthenticatedUser(String authenticatedEmail) {
        return userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new AuthenticatedUserNotFoundException(
                                "Authenticated user not found"
                        ));
    }

}
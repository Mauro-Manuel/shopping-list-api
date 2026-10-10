package com.masprog.shopping_list_api.category;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.masprog.shopping_list_api.category.dto.CreateCategoryRequest;
import com.masprog.shopping_list_api.category.dto.CategoryResponse;
import jakarta.validation.Valid;

import java.util.List;


@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid
            @RequestBody CreateCategoryRequest request,
            Authentication authentication) {

        Category category = categoryService.create(
                request.name(),
                request.icon(),
                authentication.getName()
        );

        CategoryResponse response = new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getIcon(),
                category.getType()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> findAll(
            Authentication authentication) {

        List<CategoryResponse> categories = categoryService
                .findAvailableCategories(authentication.getName())
                .stream()
                .map(category -> new CategoryResponse(
                        category.getId(),
                        category.getName(),
                        category.getIcon(),
                        category.getType()
                ))
                .toList();

        return ResponseEntity.ok(categories);
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable Long categoryId,
            @Valid @RequestBody CreateCategoryRequest request,
            Authentication authentication
    ) {

        Category category = categoryService.update(
                categoryId,
                request.name(),
                request.icon(),
                authentication.getName()
        );

        CategoryResponse response = new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getIcon(),
                category.getType()
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long categoryId,
            Authentication authentication
    ) {

        categoryService.delete(
                categoryId,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }
}
package com.masprog.shopping_list_api.category;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.masprog.shopping_list_api.category.dto.CreateCategoryRequest;
import com.masprog.shopping_list_api.category.dto.CategoryResponse;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Categories", description = "Endpoints for managing product categories"
)
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "Create a category",
            description = "Creates a new custom category for the authenticated user."
    )
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Category created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Category already exists"
            )
    })
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

    @Operation(
            summary = "List available categories",
            description = "Returns the categories available to the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Categories retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            )
    })
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

    @Operation(
            summary = "Update a category",
            description = "Updates an existing category belonging to the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Category updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Category modification is not allowed"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Category not found"
            )
    })
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

    @Operation(
            summary = "Delete a category",
            description = "Deletes a custom category belonging to the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Category deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Category deletion is not allowed"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Category not found"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Category cannot be deleted because it is in use"
            )
    })
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
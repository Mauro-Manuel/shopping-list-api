package com.masprog.shopping_list_api.product;

import com.masprog.shopping_list_api.product.dto.CreateProductRequest;
import com.masprog.shopping_list_api.product.dto.ProductCategoryResponse;
import com.masprog.shopping_list_api.product.dto.ProductResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.web.PagedModel;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Tag(name = "Products", description = "Endpoints for managing products"
)
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(
            summary = "Create a product",
            description = "Creates a new product associated with a category."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Product created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(
            @Valid @RequestBody CreateProductRequest request,
            Authentication authentication
    ) {

        Product product = productService.create(
                request.name(),
                request.categoryId(),
                authentication.getName()
        );

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

    @Operation(
            summary = "List products",
            description = "Returns a paginated list of products available to "
                    + "the authenticated user, optionally filtered by search."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Products retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            )
    })
    @GetMapping
    public PagedModel<ProductResponse> findAll(
            Authentication authentication,
            @Parameter(
                    description = "Optional text used to search products by name",
                    example = "Milk"
            )
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return new PagedModel<>(
                productService.findAll(
                        authentication.getName(),
                        search,
                        pageable
                )
        );
    }



    @Operation(
            summary = "Get product by ID",
            description = "Returns the details of a product available to the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Product retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Product not found"
            )
    })
    @GetMapping("/{productId}")
    public ProductResponse findById(
            @PathVariable Long productId,
            Authentication authentication
    ) {
        return productService.findById(
                productId,
                authentication.getName()
        );
    }

    @Operation(
            summary = "Update a product",
            description = "Updates the name and category of an existing product."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Product updated successfully"
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
                    responseCode = "404",
                    description = "Product not found"
            )
    })
    @PutMapping("/{productId}")
    public ProductResponse update(
            @PathVariable Long productId,
            @Valid @RequestBody CreateProductRequest request,
            Authentication authentication
    ) {
        return productService.update(
                productId,
                request.name(),
                request.categoryId(),
                authentication.getName()
        );
    }

    @Operation(
            summary = "Deactivate a product",
            description = "Deactivates a product belonging to the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Product deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Product not found"
            )
    })
    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long productId,
            Authentication authentication
    ) {
        productService.delete(
                productId,
                authentication.getName()
        );
    }
}
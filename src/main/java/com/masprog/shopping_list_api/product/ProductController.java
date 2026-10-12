package com.masprog.shopping_list_api.product;

import com.masprog.shopping_list_api.product.dto.CreateProductRequest;
import com.masprog.shopping_list_api.product.dto.ProductCategoryResponse;
import com.masprog.shopping_list_api.product.dto.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.web.PagedModel;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

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

    @GetMapping
    public PagedModel<ProductResponse> findAll(
            Authentication authentication,
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
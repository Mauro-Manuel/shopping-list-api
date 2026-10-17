package com.masprog.shopping_list_api.shoppingitem;

import com.masprog.shopping_list_api.shoppingitem.dto.CreateShoppingItemRequest;
import com.masprog.shopping_list_api.shoppingitem.dto.ShoppingItemResponse;
import com.masprog.shopping_list_api.shoppingitem.dto.UpdateShoppingItemRequest;
import com.masprog.shopping_list_api.shoppingitem.exception.ShoppingItemValidationException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.math.BigDecimal;

@Tag(name = "Shopping Items",description = "Endpoints for managing items within shopping lists")
@RestController
@RequestMapping("/api/v1/shopping-lists/{listId}/items")
@RequiredArgsConstructor
public class ShoppingItemController {

    private final ShoppingItemService shoppingItemService;

    @Operation(
            summary = "Add an item to a shopping list",
            description = "Adds an existing product using productId or creates "
                    + "a new product using product. Exactly one of these fields "
                    + "must be provided. Quantity defaults to 1 when omitted."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Shopping item added successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or product selection"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Shopping list or product not found"
            )
    })
    @PostMapping
    public ResponseEntity<ShoppingItemResponse> create(
            @PathVariable Long listId,
            @Valid @RequestBody CreateShoppingItemRequest request,
            Authentication authentication
    ) {

        if ((request.productId() == null) == (request.product() == null)) {
            throw new ShoppingItemValidationException(
                    "Provide either productId or product"
            );
        }

        BigDecimal quantity = request.quantity() == null
                ? BigDecimal.ONE
                : request.quantity();

        ShoppingItem item;

        if (request.productId() != null) {

            item = shoppingItemService.addExistingProduct(
                    listId,
                    request.productId(),
                    quantity,
                    request.unit(),
                    authentication.getName()
            );

        } else {

            item = shoppingItemService.addNewProduct(
                    listId,
                    request.product().name(),
                    request.product().categoryId(),
                    quantity,
                    request.unit(),
                    authentication.getName()
            );
        }

        ShoppingItemResponse response = toResponse(item);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    @Operation(
            summary = "Update a shopping item",
            description = "Updates the quantity and unit of an existing shopping item."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Shopping item updated successfully"
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
                    description = "Shopping list or item not found"
            )
    })
    @PatchMapping("/{itemId}")
    public ResponseEntity<ShoppingItemResponse> update(
            @PathVariable Long listId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateShoppingItemRequest request,
            Authentication authentication
    ) {

        ShoppingItem item = shoppingItemService.update(
                listId,
                itemId,
                request.quantity(),
                request.unit(),
                authentication.getName()
        );

        ShoppingItemResponse response = toResponse(item);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Remove an item from a shopping list",
            description = "Removes an item from the specified shopping list."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Shopping item removed successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Shopping list or item not found"
            )
    })
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long listId,
            @PathVariable Long itemId,
            Authentication authentication
    ) {

        shoppingItemService.delete(
                listId,
                itemId,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Check a shopping item",
            description = "Marks an item as purchased in the specified shopping list."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Shopping item checked successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Shopping list or item not found"
            )
    })
    @PatchMapping("/{itemId}/check")
    public ResponseEntity<ShoppingItemResponse> check(
            @PathVariable Long listId,
            @PathVariable Long itemId,
            Authentication authentication
    ) {

        ShoppingItem item = shoppingItemService.check(
                listId,
                itemId,
                authentication.getName()
        );

        ShoppingItemResponse response = toResponse(item);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Uncheck a shopping item",
            description = "Marks a previously checked item as pending."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Shopping item unchecked successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Shopping list or item not found"
            )
    })
    @PatchMapping("/{itemId}/uncheck")
    public ResponseEntity<ShoppingItemResponse> uncheck(
            @PathVariable Long listId,
            @PathVariable Long itemId,
            Authentication authentication
    ) {

        ShoppingItem item = shoppingItemService.uncheck(
                listId,
                itemId,
                authentication.getName()
        );

        ShoppingItemResponse response = toResponse(item);

        return ResponseEntity.ok(response);
    }



    private ShoppingItemResponse toResponse(ShoppingItem item) {
        return new ShoppingItemResponse(
                item.getId(),
                new ShoppingItemResponse.ProductInfo(
                        item.getProduct().getId(),
                        item.getProduct().getName()
                ),
                item.getQuantity(),
                item.getUnit(),
                item.isChecked()
        );
    }
}
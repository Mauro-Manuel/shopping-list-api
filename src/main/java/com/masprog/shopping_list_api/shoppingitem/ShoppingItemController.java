package com.masprog.shopping_list_api.shoppingitem;

import com.masprog.shopping_list_api.shoppingitem.dto.CreateShoppingItemRequest;
import com.masprog.shopping_list_api.shoppingitem.dto.ShoppingItemResponse;
import com.masprog.shopping_list_api.shoppingitem.dto.UpdateShoppingItemRequest;
import com.masprog.shopping_list_api.shoppingitem.exception.ShoppingItemValidationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/shopping-lists/{listId}/items")
@RequiredArgsConstructor
public class ShoppingItemController {

    private final ShoppingItemService shoppingItemService;

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
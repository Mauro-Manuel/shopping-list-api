package com.masprog.shopping_list_api.shoppinglist;

import com.masprog.shopping_list_api.shoppinglist.dto.CreateShoppingListRequest;
import com.masprog.shopping_list_api.shoppinglist.dto.ShoppingListResponse;
import com.masprog.shopping_list_api.shoppinglist.dto.UpdateShoppingListRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/shopping-lists")
public class ShoppingListController {

    private final ShoppingListService shoppingListService;

    public ShoppingListController(ShoppingListService shoppingListService) {
        this.shoppingListService = shoppingListService;
    }

    @PostMapping
    public ResponseEntity<ShoppingListResponse> create(
            @Valid @RequestBody CreateShoppingListRequest request,
            Authentication authentication
    ) {

        ShoppingList shoppingList = shoppingListService.create(
                request.name(),
                authentication.getName()
        );

        ShoppingListResponse response = toResponse(shoppingList);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<PagedModel<ShoppingListResponse>> findAll(
            Authentication authentication,
            @RequestParam(required = false) ShoppingListStatus status,
            Pageable pageable
    ) {

        Page<ShoppingListResponse> lists = shoppingListService.findAll(
                authentication.getName(),
                status,
                pageable
        ).map(this::toResponse);

        return ResponseEntity.ok(new PagedModel<>(lists));
    }

    @GetMapping("/{listId}")
    public ResponseEntity<ShoppingListResponse> findById(
            @PathVariable Long listId,
            Authentication authentication
    ) {

        ShoppingList shoppingList = shoppingListService.findById(
                listId,
                authentication.getName()
        );

        return ResponseEntity.ok(toResponse(shoppingList));
    }


    @PatchMapping("/{listId}")
    public ResponseEntity<ShoppingListResponse> rename(
            @PathVariable Long listId,
            @Valid @RequestBody UpdateShoppingListRequest request,
            Authentication authentication
    ) {

        ShoppingList shoppingList = shoppingListService.rename(
                listId,
                request.name(),
                authentication.getName()
        );

        return ResponseEntity.ok(toResponse(shoppingList));
    }

    @DeleteMapping("/{listId}")
    public ResponseEntity<Void> delete(@PathVariable Long listId, Authentication authentication) {

        shoppingListService.delete(listId, authentication.getName());

        return ResponseEntity.noContent().build();
    }


    private ShoppingListResponse toResponse(ShoppingList shoppingList) {
        return new ShoppingListResponse(
                shoppingList.getId(),
                shoppingList.getName(),
                shoppingList.getStatus(),
                0,
                0,
                0,
                shoppingList.getCreatedAt()
        );
    }

}
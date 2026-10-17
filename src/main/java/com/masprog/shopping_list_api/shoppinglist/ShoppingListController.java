package com.masprog.shopping_list_api.shoppinglist;

import com.masprog.shopping_list_api.shoppingitem.ShoppingItemRepository;
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

import com.masprog.shopping_list_api.shoppingitem.ShoppingListItemCount;

import com.masprog.shopping_list_api.shoppinglist.dto.ShoppingListDetailResponse;
import com.masprog.shopping_list_api.shoppingitem.ShoppingItem;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/shopping-lists")
public class ShoppingListController {

    private final ShoppingListService shoppingListService;
    private final ShoppingItemRepository shoppingItemRepository;

    public ShoppingListController(
            ShoppingListService shoppingListService,
            ShoppingItemRepository shoppingItemRepository
    ) {
        this.shoppingListService = shoppingListService;
        this.shoppingItemRepository = shoppingItemRepository;
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

        Page<ShoppingList> lists = shoppingListService.findAll(
                authentication.getName(),
                status,
                pageable
        );

        List<Long> listIds = lists.getContent()
                .stream()
                .map(ShoppingList::getId)
                .toList();

        Map<Long, ShoppingListItemCount> countsByListId;

        if (listIds.isEmpty()) {
            countsByListId = Map.of();
        } else {
            countsByListId = shoppingItemRepository
                    .countItemsByShoppingListIds(listIds)
                    .stream()
                    .collect(Collectors.toMap(
                            ShoppingListItemCount::getShoppingListId,
                            Function.identity()
                    ));
        }

        Page<ShoppingListResponse> responses = lists.map(
                list -> toResponse(list, countsByListId.get(list.getId()))
        );

        return ResponseEntity.ok(new PagedModel<>(responses));
    }

    @GetMapping("/{listId}")
    public ResponseEntity<ShoppingListDetailResponse> findById(
            @PathVariable Long listId,
            Authentication authentication
    ) {

        ShoppingList shoppingList = shoppingListService.findById(
                listId,
                authentication.getName()
        );

        ShoppingListResponse summary = toResponse(shoppingList);

        List<ShoppingListDetailResponse.ItemInfo> items =
                shoppingItemRepository
                        .findItemsForList(listId)
                        .stream()
                        .map(this::toItemInfo)
                        .toList();

        ShoppingListDetailResponse response =
                new ShoppingListDetailResponse(
                        shoppingList.getId(),
                        shoppingList.getName(),
                        shoppingList.getStatus(),
                        summary.totalItems(),
                        summary.checkedItems(),
                        summary.pendingItems(),
                        summary.progress(),
                        shoppingList.getCreatedAt(),
                        shoppingList.getCompletedAt(),
                        items
                );

        return ResponseEntity.ok(response);
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

    @PatchMapping("/{listId}/complete")
    public ResponseEntity<Void> complete(
            @PathVariable Long listId,
            Authentication authentication
    ) {

        shoppingListService.complete(
                listId,
                authentication.getName()
        );

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{listId}/reopen")
    public ResponseEntity<Void> reopen(
            @PathVariable Long listId,
            Authentication authentication
    ) {

        shoppingListService.reopen(
                listId,
                authentication.getName()
        );

        return ResponseEntity.ok().build();
    }


    private ShoppingListResponse toResponse(ShoppingList shoppingList) {

        Long listId = shoppingList.getId();

        long totalItems = shoppingItemRepository
                .countByShoppingListId(listId);

        long checkedItems = shoppingItemRepository
                .countByShoppingListIdAndCheckedTrue(listId);

        long pendingItems = totalItems - checkedItems;

        int progress = totalItems == 0
                ? 0
                : (int) (checkedItems * 100 / totalItems);

        return new ShoppingListResponse(
                shoppingList.getId(),
                shoppingList.getName(),
                shoppingList.getStatus(),
                totalItems,
                checkedItems,
                pendingItems,
                progress,
                shoppingList.getCreatedAt()
        );
    }

    private ShoppingListResponse toResponse(
            ShoppingList shoppingList,
            ShoppingListItemCount counts
    ) {

        long totalItems = counts == null
                ? 0L
                : counts.getTotalItems();

        long checkedItems = counts == null
                ? 0L
                : counts.getCheckedItems();

        long pendingItems = totalItems - checkedItems;

        int progress = totalItems == 0
                ? 0
                : (int) (checkedItems * 100 / totalItems);

        return new ShoppingListResponse(
                shoppingList.getId(),
                shoppingList.getName(),
                shoppingList.getStatus(),
                totalItems,
                checkedItems,
                pendingItems,
                progress,
                shoppingList.getCreatedAt()
        );
    }

    private ShoppingListDetailResponse.ItemInfo toItemInfo(
            ShoppingItem item
    ) {

        var product = item.getProduct();
        var category = product.getCategory();

        return new ShoppingListDetailResponse.ItemInfo(
                item.getId(),
                new ShoppingListDetailResponse.ProductInfo(
                        product.getId(),
                        product.getName()
                ),
                new ShoppingListDetailResponse.CategoryInfo(
                        category.getId(),
                        category.getName(),
                        category.getIcon()
                ),
                item.getQuantity(),
                item.getUnit(),
                item.isChecked()
        );
    }

}
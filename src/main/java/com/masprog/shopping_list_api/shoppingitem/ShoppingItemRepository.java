package com.masprog.shopping_list_api.shoppingitem;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ShoppingItemRepository extends JpaRepository<ShoppingItem, Long> {

    boolean existsByShoppingListIdAndProductId(Long listId, Long productId);

   // Optional<ShoppingItem> findByIdAndShoppingListId(Long itemId, Long listId);

    @EntityGraph(attributePaths = "product")
    Optional<ShoppingItem> findByIdAndShoppingListId(
            Long itemId,
            Long listId
    );

    boolean existsByShoppingListId(Long listId);

    long countByShoppingListId(Long listId);

    long countByShoppingListIdAndCheckedTrue(Long listId);

    @Query("""
        SELECT
            si.shoppingList.id AS shoppingListId,
            COUNT(si.id) AS totalItems,
            SUM(CASE WHEN si.checked = true THEN 1L ELSE 0L END) AS checkedItems
        FROM ShoppingItem si
        WHERE si.shoppingList.id IN :listIds
        GROUP BY si.shoppingList.id
        """)
    List<ShoppingListItemCount> countItemsByShoppingListIds(
            @Param("listIds") Collection<Long> listIds
    );

    @EntityGraph(attributePaths = {"product", "product.category"})
    @Query("""
        SELECT si
        FROM ShoppingItem si
        WHERE si.shoppingList.id = :listId
        ORDER BY si.id ASC
        """)
    List<ShoppingItem> findItemsForList(@Param("listId") Long listId);
}
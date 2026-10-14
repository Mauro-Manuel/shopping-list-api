package com.masprog.shopping_list_api.shoppinglist;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {

    Page<ShoppingList> findByUserId(Long userId, Pageable pageable);

    Page<ShoppingList> findByUserIdAndStatus(
            Long userId,
            ShoppingListStatus status,
            Pageable pageable
    );

    Optional<ShoppingList> findByIdAndUserId(Long listId, Long userId);
}
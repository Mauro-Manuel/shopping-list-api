package com.masprog.shopping_list_api.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByUserIdAndTypeAndNameIgnoreCase(
            Long userId,
            CategoryType type,
            String name
    );

    boolean existsByTypeAndNameIgnoreCase(
            CategoryType type,
            String name
    );

    List<Category> findByTypeOrUserId(
            CategoryType type,
            Long userId
    );

    Optional<Category> findByTypeAndNameIgnoreCase(
            CategoryType type,
            String name
    );
}
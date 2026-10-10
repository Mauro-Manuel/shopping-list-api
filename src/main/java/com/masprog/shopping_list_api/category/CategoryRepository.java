package com.masprog.shopping_list_api.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

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
}
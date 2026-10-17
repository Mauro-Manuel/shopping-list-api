package com.masprog.shopping_list_api.product;

import com.masprog.shopping_list_api.category.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    Page<Product> findByUserIdAndActiveTrue(
            Long userId,
            Pageable pageable
    );

    Page<Product> findByUserIdAndActiveTrueAndNameContainingIgnoreCase(
            Long userId,
            String name,
            Pageable pageable
    );

    Optional<Product> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(
            Long userId,
            String name,
            Long productId
    );

    @Modifying
    @Query("""
    UPDATE Product p
       SET p.category = :newCategory
     WHERE p.category.id = :oldCategoryId
       AND p.user.id = :userId
    """)
    int reassignCategory(
            @Param("oldCategoryId") Long oldCategoryId,
            @Param("newCategory") Category newCategory,
            @Param("userId") Long userId
    );

    Optional<Product> findByUserIdAndNameIgnoreCase(Long userId, String name);

}
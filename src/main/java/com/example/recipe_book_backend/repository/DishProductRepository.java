package com.example.recipe_book_backend.repository;

import com.example.recipe_book_backend.entity.DishProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DishProductRepository extends JpaRepository<DishProduct, Long> {
    @Query("SELECT dp.product.id FROM DishProduct dp WHERE dp.dish.id = :dishId")
    List<Long> findProductIdsByDishId(@Param("dishId") Long dishId);

    @Query("SELECT DISTINCT dp.dish.id FROM DishProduct dp WHERE dp.product.id = :productId")
    List<Long> findDishIdsByProductId(@Param("productId") Long productId);
}
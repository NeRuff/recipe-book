package com.example.recipe_book_backend.repository;

import com.example.recipe_book_backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Product> searchByName(@Param("name") String name);

    List<Product> findByCategory(String category);
    List<Product> findByCookingRequirement(String cookingRequirement);

    @Query("SELECT p FROM Product p WHERE :flag MEMBER OF p.flags")
    List<Product> findByFlag(@Param("flag") String flag);
}
package com.example.recipe_book_backend.repository;

import com.example.recipe_book_backend.entity.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DishRepository extends JpaRepository<Dish, Long> {
    @Query("SELECT d FROM Dish d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Dish> searchByName(@Param("name") String name);

    List<Dish> findByCategory(String category);

    @Query("SELECT d FROM Dish d WHERE :flag MEMBER OF d.flags")
    List<Dish> findByFlag(@Param("flag") String flag);
}
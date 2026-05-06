package com.example.recipe_book_backend.service;

import com.example.recipe_book_backend.dto.DishDTO;
import com.example.recipe_book_backend.entity.Dish;
import com.example.recipe_book_backend.entity.DishProduct;
import com.example.recipe_book_backend.entity.Product;
import com.example.recipe_book_backend.repository.DishRepository;
import com.example.recipe_book_backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DishService {
    @Autowired
    private DishRepository dishRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<Dish> getAllDishes() {
        return dishRepository.findAll();
    }

    public Optional<Dish> getDishById(Long id) {
        return dishRepository.findById(id);
    }

    @Transactional
    public Dish createDish(@Valid DishDTO dto) {
        Dish dish = new Dish();
        dish.setName(extractNameWithoutMacro(dto.getName()));
        dish.setPhotos(dto.getPhotos() != null ? dto.getPhotos() : new ArrayList<>());
        dish.setPortionSize(dto.getPortionSize());

        String categoryFromMacro = extractCategoryFromMacro(dto.getName());
        if (dto.getCategory() != null && !dto.getCategory().isEmpty()) {
            dish.setCategory(dto.getCategory());
        } else if (categoryFromMacro != null) {
            dish.setCategory(categoryFromMacro);
        } else {
            dish.setCategory(dto.getCategory());
        }

        List<DishProduct> components = buildComponents(dish, dto.getComponents());
        dish.setComponents(components);

        calculateNutrition(dish, components);

        if (dto.getCalories() != null) dish.setCalories(dto.getCalories());
        if (dto.getProteins() != null) dish.setProteins(dto.getProteins());
        if (dto.getFats() != null) dish.setFats(dto.getFats());
        if (dto.getCarbs() != null) dish.setCarbs(dto.getCarbs());

        updateAvailableFlags(dish);

        List<String> finalFlags = new ArrayList<>(dish.getFlags());
        if (dto.getFlags() != null && !dto.getFlags().isEmpty()) {
            finalFlags = dto.getFlags().stream()
                    .filter(flag -> dish.getFlags().contains(flag))
                    .collect(Collectors.toList());
        }
        dish.setFlags(finalFlags);

        return dishRepository.save(dish);
    }

    @Transactional
    public Dish updateDish(Long id, @Valid DishDTO dto) {
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Блюдо не найдено"));

        dish.setName(extractNameWithoutMacro(dto.getName()));
        dish.setPhotos(dto.getPhotos() != null ? dto.getPhotos() : new ArrayList<>());
        dish.setPortionSize(dto.getPortionSize());

        String categoryFromMacro = extractCategoryFromMacro(dto.getName());
        if (dto.getCategory() != null && !dto.getCategory().isEmpty()) {
            dish.setCategory(dto.getCategory());
        } else if (categoryFromMacro != null) {
            dish.setCategory(categoryFromMacro);
        }

        dish.getComponents().clear();
        List<DishProduct> newComponents = buildComponents(dish, dto.getComponents());
        dish.getComponents().addAll(newComponents);

        calculateNutrition(dish, newComponents);

        if (dto.getCalories() != null) dish.setCalories(dto.getCalories());
        if (dto.getProteins() != null) dish.setProteins(dto.getProteins());
        if (dto.getFats() != null) dish.setFats(dto.getFats());
        if (dto.getCarbs() != null) dish.setCarbs(dto.getCarbs());

        updateAvailableFlags(dish);

        List<String> finalFlags = new ArrayList<>(dish.getFlags());
        if (dto.getFlags() != null && !dto.getFlags().isEmpty()) {
            finalFlags = dto.getFlags().stream()
                    .filter(flag -> dish.getFlags().contains(flag))
                    .collect(Collectors.toList());
        }
        dish.setFlags(finalFlags);

        return dishRepository.save(dish);
    }

    @Transactional
    public void deleteDish(Long id) {
        dishRepository.deleteById(id);
    }

    public List<Dish> searchDishes(String name, String category, List<String> flags) {
        List<Dish> dishes = dishRepository.findAll();

        if (name != null && !name.isEmpty()) {
            dishes = dishes.stream()
                    .filter(d -> d.getName().toLowerCase().contains(name.toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (category != null && !category.isEmpty()) {
            dishes = dishes.stream()
                    .filter(d -> d.getCategory().equals(category))
                    .collect(Collectors.toList());
        }
        if (flags != null && !flags.isEmpty()) {
            dishes = dishes.stream()
                    .filter(d -> d.getFlags().containsAll(flags))
                    .collect(Collectors.toList());
        }
        return dishes;
    }

    private String extractNameWithoutMacro(String name) {
        if (name == null) return null;
        String[] macros = {"!десерт", "!первое", "!второе", "!напиток", "!салат", "!суп", "!перекус"};
        for (String macro : macros) {
            name = name.replace(macro, "").trim();
        }
        return name;
    }

    private String extractCategoryFromMacro(String name) {
        if (name == null) return null;
        Map<String, String> macroMap = new HashMap<>();
        macroMap.put("!десерт", "Десерт");
        macroMap.put("!первое", "Первое");
        macroMap.put("!второе", "Второе");
        macroMap.put("!напиток", "Напиток");
        macroMap.put("!салат", "Салат");
        macroMap.put("!суп", "Суп");
        macroMap.put("!перекус", "Перекус");

        for (Map.Entry<String, String> entry : macroMap.entrySet()) {
            if (name.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private List<DishProduct> buildComponents(Dish dish, List<DishDTO.ComponentDTO> componentDTOs) {
        List<DishProduct> components = new ArrayList<>();
        for (DishDTO.ComponentDTO compDto : componentDTOs) {
            Product product = productRepository.findById(compDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Продукт не найден: " + compDto.getProductId()));
            DishProduct dp = new DishProduct();
            dp.setDish(dish);
            dp.setProduct(product);
            dp.setQuantity(compDto.getQuantity());
            components.add(dp);
        }
        return components;
    }

    private void calculateNutrition(Dish dish, List<DishProduct> components) {
        double totalCalories = 0;
        double totalProteins = 0;
        double totalFats = 0;
        double totalCarbs = 0;

        for (DishProduct dp : components) {
            Product p = dp.getProduct();
            double ratio = dp.getQuantity() / 100.0;
            totalCalories += p.getCalories() * ratio;
            totalProteins += p.getProteins() * ratio;
            totalFats += p.getFats() * ratio;
            totalCarbs += p.getCarbs() * ratio;
        }

        dish.setCalories(totalCalories);
        dish.setProteins(totalProteins);
        dish.setFats(totalFats);
        dish.setCarbs(totalCarbs);
    }

    private void updateAvailableFlags(Dish dish) {
        List<String> availableFlags = new ArrayList<>(Arrays.asList("Веган", "Без глютена", "Без сахара"));

        for (DishProduct dp : dish.getComponents()) {
            Product p = dp.getProduct();
            if (!p.getFlags().contains("Веган")) {
                availableFlags.remove("Веган");
            }
            if (!p.getFlags().contains("Без глютена")) {
                availableFlags.remove("Без глютена");
            }
            if (!p.getFlags().contains("Без сахара")) {
                availableFlags.remove("Без сахара");
            }
        }

        dish.setFlags(availableFlags);
    }
}
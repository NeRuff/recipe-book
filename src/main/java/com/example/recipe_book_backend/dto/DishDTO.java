package com.example.recipe_book_backend.dto;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;

public class DishDTO {
    private Long id;

    @NotBlank(message = "Название обязательно")
    @Size(min = 2, message = "Название должно содержать минимум 2 символа")
    private String name;

    private List<String> photos;

    private Double calories;
    private Double proteins;
    private Double fats;
    private Double carbs;

    @NotNull(message = "Состав обязателен")
    @Size(min = 1, message = "Блюдо должно содержать хотя бы один продукт")
    private List<ComponentDTO> components;

    @NotNull(message = "Размер порции обязателен")
    @Positive(message = "Размер порции должен быть больше 0")
    private Double portionSize;

    @NotBlank(message = "Категория обязательна")
    private String category;

    private List<String> flags;

    public static class ComponentDTO {
        @NotNull(message = "ID продукта обязателен")
        private Long productId;

        @NotNull(message = "Количество обязательно")
        @Positive(message = "Количество должно быть больше 0")
        private Double quantity;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Double getQuantity() { return quantity; }
        public void setQuantity(Double quantity) { this.quantity = quantity; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getPhotos() { return photos; }
    public void setPhotos(List<String> photos) { this.photos = photos; }
    public Double getCalories() { return calories; }
    public void setCalories(Double calories) { this.calories = calories; }
    public Double getProteins() { return proteins; }
    public void setProteins(Double proteins) { this.proteins = proteins; }
    public Double getFats() { return fats; }
    public void setFats(Double fats) { this.fats = fats; }
    public Double getCarbs() { return carbs; }
    public void setCarbs(Double carbs) { this.carbs = carbs; }
    public List<ComponentDTO> getComponents() { return components; }
    public void setComponents(List<ComponentDTO> components) { this.components = components; }
    public Double getPortionSize() { return portionSize; }
    public void setPortionSize(Double portionSize) { this.portionSize = portionSize; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public List<String> getFlags() { return flags; }
    public void setFlags(List<String> flags) { this.flags = flags; }
}
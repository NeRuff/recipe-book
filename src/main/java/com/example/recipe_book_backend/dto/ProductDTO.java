package com.example.recipe_book_backend.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public class ProductDTO {
    private Long id;

    @NotBlank(message = "Название обязательно")
    @Size(min = 2, message = "Название должно содержать минимум 2 символа")
    private String name;

    private List<String> photos;

    @NotNull(message = "Калорийность обязательна")
    @Min(value = 0, message = "Калорийность не может быть отрицательной")
    private Double calories;

    @NotNull(message = "Белки обязательны")
    @Min(value = 0, message = "Белки не могут быть отрицательными")
    @Max(value = 100, message = "Белки не могут превышать 100")
    private Double proteins;

    @NotNull(message = "Жиры обязательны")
    @Min(value = 0, message = "Жиры не могут быть отрицательными")
    @Max(value = 100, message = "Жиры не могут превышать 100")
    private Double fats;

    @NotNull(message = "Углеводы обязательны")
    @Min(value = 0, message = "Углеводы не могут быть отрицательными")
    @Max(value = 100, message = "Углеводы не могут превышать 100")
    private Double carbs;

    private String composition;

    @NotBlank(message = "Категория обязательна")
    private String category;

    @NotBlank(message = "Необходимость готовки обязательна")
    private String cookingRequirement;

    private List<String> flags;

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
    public String getComposition() { return composition; }
    public void setComposition(String composition) { this.composition = composition; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getCookingRequirement() { return cookingRequirement; }
    public void setCookingRequirement(String cookingRequirement) { this.cookingRequirement = cookingRequirement; }
    public List<String> getFlags() { return flags; }
    public void setFlags(List<String> flags) { this.flags = flags; }
}
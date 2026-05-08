package com.example.recipe_book_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ElementCollection
    @CollectionTable(name = "product_photos", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "photo_url", length = 1000)
    @Size(max = 5, message = "Максимум 5 фото")
    private List<String> photos = new ArrayList<>();

    @Column(nullable = false)
    private Double calories;

    @Column(nullable = false)
    private Double proteins;

    @Column(nullable = false)
    private Double fats;

    @Column(nullable = false)
    private Double carbs;

    @Column(columnDefinition = "TEXT")
    private String composition;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String cookingRequirement;

    @ElementCollection
    @CollectionTable(name = "product_flags", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "flag")
    private List<String> flags = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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
    public String getComposition() { return composition; }
    public void setComposition(String composition) { this.composition = composition; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getCookingRequirement() { return cookingRequirement; }
    public void setCookingRequirement(String cookingRequirement) { this.cookingRequirement = cookingRequirement; }
    public List<String> getFlags() { return flags; }
    public void setFlags(List<String> flags) { this.flags = flags; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
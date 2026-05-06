package com.example.recipe_book_backend.service;

import com.example.recipe_book_backend.dto.ProductDTO;
import com.example.recipe_book_backend.entity.Product;
import com.example.recipe_book_backend.entity.DishProduct;
import com.example.recipe_book_backend.repository.ProductRepository;
import com.example.recipe_book_backend.repository.DishProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DishProductRepository dishProductRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    @Transactional
    public Product createProduct(@Valid ProductDTO dto) {
        validateBjuSum(dto.getProteins(), dto.getFats(), dto.getCarbs());
        Product product = convertToEntity(dto);
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, @Valid ProductDTO dto) {
        validateBjuSum(dto.getProteins(), dto.getFats(), dto.getCarbs());
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Продукт не найден"));
        updateEntity(product, dto);
        return productRepository.save(product);
    }

    public Map<String, Object> checkDeleteAvailability(Long id) {
        List<Long> dishIds = dishProductRepository.findDishIdsByProductId(id);
        boolean canDelete = dishIds.isEmpty();
        Map<String, Object> result = new HashMap<>();
        result.put("canDelete", canDelete);
        if (!canDelete) {
            result.put("usedInDishes", dishIds);
        }
        return result;
    }

    @Transactional
    public void deleteProduct(Long id) {
        Map<String, Object> check = checkDeleteAvailability(id);
        if (!(Boolean) check.get("canDelete")) {
            throw new RuntimeException("Невозможно удалить продукт: он используется в блюдах");
        }
        productRepository.deleteById(id);
    }

    public List<Product> searchProducts(String name, String category, String cookingRequirement, List<String> flags) {
        List<Product> products = productRepository.findAll();

        if (name != null && !name.isEmpty()) {
            products = products.stream()
                    .filter(p -> p.getName().toLowerCase().contains(name.toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (category != null && !category.isEmpty()) {
            products = products.stream()
                    .filter(p -> p.getCategory().equals(category))
                    .collect(Collectors.toList());
        }
        if (cookingRequirement != null && !cookingRequirement.isEmpty()) {
            products = products.stream()
                    .filter(p -> p.getCookingRequirement().equals(cookingRequirement))
                    .collect(Collectors.toList());
        }
        if (flags != null && !flags.isEmpty()) {
            products = products.stream()
                    .filter(p -> p.getFlags().containsAll(flags))
                    .collect(Collectors.toList());
        }
        return products;
    }

    private void validateBjuSum(Double proteins, Double fats, Double carbs) {
        if (proteins + fats + carbs > 100) {
            throw new RuntimeException("Сумма БЖУ не может превышать 100 грамм");
        }
    }

    private Product convertToEntity(ProductDTO dto) {
        Product product = new Product();
        product.setName(dto.getName());
        product.setPhotos(dto.getPhotos() != null ? dto.getPhotos() : new ArrayList<>());
        product.setCalories(dto.getCalories());
        product.setProteins(dto.getProteins());
        product.setFats(dto.getFats());
        product.setCarbs(dto.getCarbs());
        product.setComposition(dto.getComposition());
        product.setCategory(dto.getCategory());
        product.setCookingRequirement(dto.getCookingRequirement());
        product.setFlags(dto.getFlags() != null ? dto.getFlags() : new ArrayList<>());
        return product;
    }

    private void updateEntity(Product product, ProductDTO dto) {
        product.setName(dto.getName());
        product.setPhotos(dto.getPhotos() != null ? dto.getPhotos() : new ArrayList<>());
        product.setCalories(dto.getCalories());
        product.setProteins(dto.getProteins());
        product.setFats(dto.getFats());
        product.setCarbs(dto.getCarbs());
        product.setComposition(dto.getComposition());
        product.setCategory(dto.getCategory());
        product.setCookingRequirement(dto.getCookingRequirement());
        product.setFlags(dto.getFlags() != null ? dto.getFlags() : new ArrayList<>());
    }
}
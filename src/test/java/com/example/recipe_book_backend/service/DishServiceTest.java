package com.example.recipe_book_backend.service;

import com.example.recipe_book_backend.dto.DishDTO;
import com.example.recipe_book_backend.dto.ProductDTO;
import com.example.recipe_book_backend.entity.Dish;
import com.example.recipe_book_backend.entity.DishProduct;
import com.example.recipe_book_backend.entity.Product;
import org.junit.jupiter.api.*;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DishServiceTest {

    private Method calculateNutritionMethod;
    private Dish dish;
    private ProductService productService;
    private List<DishProduct> components;

    @BeforeEach
    void setUp() throws Exception {
        DishService dishService = new DishService();
        productService = new ProductService();
        calculateNutritionMethod = DishService.class.getDeclaredMethod("calculateNutrition", Dish.class, List.class);
        calculateNutritionMethod.setAccessible(true);
        dish = new Dish();
        components = new ArrayList<>();
    }

    @Test
    @DisplayName("Расчёт КБЖУ для блюда из одного продукта с нормальными значениями")
    void shouldCalculateCorrectNutritionForSingleProductWithNormalValues() throws Exception {
        Product product = createProduct("Картофель", 77.0, 2.0, 0.4, 16.3);
        components.add(createDishProduct(product, 200.0));

        calculateNutritionMethod.invoke(dishService, dish, components);

        assertEquals(154.0, dish.getCalories(), 0.01);
        assertEquals(4.0, dish.getProteins(), 0.01);
        assertEquals(0.8, dish.getFats(), 0.01);
        assertEquals(32.6, dish.getCarbs(), 0.01);
    }

    @Test
    @DisplayName("Расчёт КБЖУ для блюда из нескольких продуктов с нормальными значениями")
    void shouldCalculateCorrectNutritionForMultipleProductsWithNormalValues() throws Exception {
        Product potato = createProduct("Картофель", 77.0, 2.0, 0.4, 16.3);
        Product meat = createProduct("Мясо", 187.2, 18.9, 12.4, 0.0);
        Product water = createProduct("Вода", 0.0, 0.0, 0.0, 0.0);

        components.add(createDishProduct(potato, 150.0));
        components.add(createDishProduct(meat, 100.0));
        components.add(createDishProduct(water, 500.0));

        calculateNutritionMethod.invoke(dishService, dish, components);

        double expectedCalories = (77.0 * 1.5) + (187.2 * 1.0);
        double expectedProteins = (2.0 * 1.5) + (18.9 * 1.0);
        double expectedFats = (0.4 * 1.5) + (12.4 * 1.0);
        double expectedCarbs = (16.3 * 1.5) + (0.0 * 1.0);

        assertEquals(expectedCalories, dish.getCalories(), 0.01);
        assertEquals(expectedProteins, dish.getProteins(), 0.01);
        assertEquals(expectedFats, dish.getFats(), 0.01);
        assertEquals(expectedCarbs, dish.getCarbs(), 0.01);
    }

    @Test
    @DisplayName("Граничное значение: расчёт КБЖУ для продукта с минимальным количеством 0.1 грамма")
    void shouldCalculateCorrectNutritionForMinimumQuantityAtBoundaryValue() throws Exception {
        Product product = createProduct("Специя", 300.0, 10.0, 5.0, 20.0);
        components.add(createDishProduct(product, 0.1));

        calculateNutritionMethod.invoke(dishService, dish, components);

        assertEquals(0.3, dish.getCalories(), 0.01);
        assertEquals(0.01, dish.getProteins(), 0.01);
        assertEquals(0.005, dish.getFats(), 0.01);
        assertEquals(0.02, dish.getCarbs(), 0.01);
    }

    @Test
    @DisplayName("Граничное значение: расчёт КБЖУ для продукта с большим количеством 10000 грамм")
    void shouldCalculateCorrectNutritionForLargeQuantityAtBoundaryValue() throws Exception {
        Product product = createProduct("Вода", 0.0, 0.0, 0.0, 0.0);
        components.add(createDishProduct(product, 10000.0));

        calculateNutritionMethod.invoke(dishService, dish, components);

        assertEquals(0.0, dish.getCalories(), 0.01);
        assertEquals(0.0, dish.getProteins(), 0.01);
        assertEquals(0.0, dish.getFats(), 0.01);
        assertEquals(0.0, dish.getCarbs(), 0.01);
    }

    @Test
    @DisplayName("Эквивалентный класс: расчёт КБЖУ для продукта с нулевой калорийностью")
    void shouldCalculateZeroNutritionForZeroCalorieProduct() throws Exception {
        Product product = createProduct("Вода", 0.0, 0.0, 0.0, 0.0);
        components.add(createDishProduct(product, 500.0));

        calculateNutritionMethod.invoke(dishService, dish, components);

        assertEquals(0.0, dish.getCalories(), 0.01);
        assertEquals(0.0, dish.getProteins(), 0.01);
        assertEquals(0.0, dish.getFats(), 0.01);
        assertEquals(0.0, dish.getCarbs(), 0.01);
    }

    @Test
    @DisplayName("Граничное значение: расчёт КБЖУ для продукта с максимальными значениями 100 грамм БЖУ")
    void shouldCalculateCorrectNutritionForMaximumBjuValuesAtBoundaryValue() throws Exception {
        Product product = createProduct("Максимальный продукт", 900.0, 100.0, 100.0, 100.0);
        components.add(createDishProduct(product, 100.0));

        calculateNutritionMethod.invoke(dishService, dish, components);

        assertEquals(900.0, dish.getCalories(), 0.01);
        assertEquals(100.0, dish.getProteins(), 0.01);
        assertEquals(100.0, dish.getFats(), 0.01);
        assertEquals(100.0, dish.getCarbs(), 0.01);
    }

    @Test
    @DisplayName("Эквивалентный класс: расчёт КБЖУ для пустого состава блюда")
    void shouldReturnZeroNutritionForEmptyComposition() throws Exception {
        calculateNutritionMethod.invoke(dishService, dish, components);
        assertEquals(0.0, dish.getCalories(), 0.01);
    }

    @Test
    @DisplayName("Граничное значение: расчёт КБЖУ для продукта весом ровно 100 грамм")
    void shouldCalculateCorrectNutritionForExactlyOneHundredGrams() throws Exception {
        Product product = createProduct("Тестовый продукт", 200.0, 10.0, 5.0, 15.0);
        components.add(createDishProduct(product, 100.0));

        calculateNutritionMethod.invoke(dishService, dish, components);

        assertEquals(200.0, dish.getCalories(), 0.01);
        assertEquals(10.0, dish.getProteins(), 0.01);
        assertEquals(5.0, dish.getFats(), 0.01);
        assertEquals(15.0, dish.getCarbs(), 0.01);
    }

    @Test
    @DisplayName("Эквивалентный класс: расчёт КБЖУ для продуктов с дробными значениями")
    void shouldCalculateCorrectNutritionForFractionalBjuValues() throws Exception {
        Product product = createProduct("Сложный продукт", 123.45, 6.78, 9.01, 2.34);
        components.add(createDishProduct(product, 250.0));

        calculateNutritionMethod.invoke(dishService, dish, components);

        assertEquals(308.625, dish.getCalories(), 0.01);
        assertEquals(16.95, dish.getProteins(), 0.01);
        assertEquals(22.525, dish.getFats(), 0.01);
        assertEquals(5.85, dish.getCarbs(), 0.01);
    }

    @Test
    @DisplayName("Эквивалентный класс: расчёт КБЖУ для веганского блюда без мяса")
    void shouldCalculateCorrectNutritionForVeganDishWithoutMeat() throws Exception {
        Product potato = createProduct("Картофель", 77.0, 2.0, 0.4, 16.3);
        Product water = createProduct("Вода", 0.0, 0.0, 0.0, 0.0);
        Product beet = createProduct("Свёкла", 43.0, 1.6, 0.1, 9.6);

        components.add(createDishProduct(potato, 150.0));
        components.add(createDishProduct(water, 500.0));
        components.add(createDishProduct(beet, 100.0));

        calculateNutritionMethod.invoke(dishService, dish, components);

        double expectedCalories = (77.0 * 1.5) + (43.0 * 1.0);
        double expectedProteins = (2.0 * 1.5) + (1.6 * 1.0);
        double expectedFats = (0.4 * 1.5) + (0.1 * 1.0);
        double expectedCarbs = (16.3 * 1.5) + (9.6 * 1.0);

        assertEquals(expectedCalories, dish.getCalories(), 0.01);
        assertEquals(expectedProteins, dish.getProteins(), 0.01);
        assertEquals(expectedFats, dish.getFats(), 0.01);
        assertEquals(expectedCarbs, dish.getCarbs(), 0.01);
    }
    @Test
    @DisplayName("Негативный сценарий: отрицательное количество продукта - исключение")
    void shouldNotCreateDishWithNegativeQuantity() {
        DishDTO dto = new DishDTO();
        dto.setName("Блюдо");
        dto.setPortionSize(200.0);

        DishDTO.ComponentDTO component = new DishDTO.ComponentDTO();
        component.setProductId(1L);
        component.setQuantity(-100.0);
        dto.setComponents(List.of(component));

        assertThrows(Exception.class, () -> {
            dishService.createDish(dto);
        });
    }

    @Test
    @DisplayName("Негативный сценарий: продукт с отрицательными белками - исключение")
    void shouldNotCreateProductWithNegativeProteins() {
        ProductDTO dto = new ProductDTO();
        dto.setName("Тест");
        dto.setCalories(100.0);
        dto.setProteins(-5.0);
        dto.setFats(10.0);
        dto.setCarbs(10.0);
        dto.setCategory("Овощи");
        dto.setCookingRequirement("Готовый к употреблению");

        assertThrows(Exception.class, () -> {
            productService.createProduct(dto);
        });
    }

    private Product createProduct(String name, double calories, double proteins, double fats, double carbs) {
        Product product = new Product();
        product.setName(name);
        product.setCalories(calories);
        product.setProteins(proteins);
        product.setFats(fats);
        product.setCarbs(carbs);
        product.setFlags(new ArrayList<>());
        return product;
    }

    private DishProduct createDishProduct(Product product, double quantity) {
        DishProduct dp = new DishProduct();
        dp.setProduct(product);
        dp.setQuantity(quantity);
        return dp;
    }

    private final DishService dishService = new DishService();
}
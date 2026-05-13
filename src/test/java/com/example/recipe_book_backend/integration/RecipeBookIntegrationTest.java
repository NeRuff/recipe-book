package com.example.recipe_book_backend.integration;

import com.example.recipe_book_backend.dto.DishDTO;
import com.example.recipe_book_backend.dto.ProductDTO;
import com.example.recipe_book_backend.entity.Dish;
import com.example.recipe_book_backend.entity.Product;
import com.example.recipe_book_backend.repository.DishProductRepository;
import com.example.recipe_book_backend.repository.DishRepository;
import com.example.recipe_book_backend.repository.ProductRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RecipeBookIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DishRepository dishRepository;

    @Autowired
    private DishProductRepository dishProductRepository;

    private Long createdProductId;
    private Long createdDishId;

    @BeforeEach
    void setUp() {
        dishRepository.deleteAll();
        dishProductRepository.deleteAll();
        productRepository.deleteAll();
        createdProductId = null;
        createdDishId = null;
    }

    @Nested
    @DisplayName("Тесты управления продуктами")
    class ProductTests {

        @Test
        @Order(1)
        @DisplayName("Создание корректного продукта с нормальными значениями")
        void testCreateProductWithNormalValues() throws Exception {
            ProductDTO dto = createProductDto(
                    "Картофель", 77.0, 2.0, 0.4, 16.3,
                    "Овощи", "Требует приготовления",
                    List.of("Веган", "Без глютена", "Без сахара"),
                    List.of("https://downloader.disk.yandex.ru/preview/56682f0ccaff37d521e1345bbab851fb55833b1f3309778d1b5884222eeb23b1/6a04a23c/knMVnybnxiATWES34Ov9M1-08ACFMZo9E4eabBzoK3a1xM7IN3bLHAaZtBsPVWQFewcKqH-zwa2e-6ktg-iiEg%3D%3D?uid=0&filename=kartoshqa.png&disposition=inline&hash=&limit=0&content_type=image%2Fpng&owner_uid=0&tknv=v3&size=1920x918")
            );

            MvcResult result = mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn();

            Product product = objectMapper.readValue(result.getResponse().getContentAsString(), Product.class);
            createdProductId = product.getId();

            assertNotNull(product.getId());
            assertTrue(product.getName().contains("Картофель"));
            assertNotNull(product.getName());
        }

        @Test
        @Order(2)
        @DisplayName("Создание продукта с нулевой калорийностью")
        void testCreateProductWithZeroCalories() throws Exception {
            ProductDTO dto = createProductDto(
                    "Вода", 0.0, 0.0, 0.0, 0.0,
                    "Жидкость", "Готовый к употреблению",
                    List.of("Веган", "Без глютена", "Без сахара"),
                    List.of()
            );

            MvcResult result = mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn();

            Product product = objectMapper.readValue(result.getResponse().getContentAsString(), Product.class);
            createdProductId = product.getId();

            assertEquals(0.0, product.getCalories());
        }

        @Test
        @Order(3)
        @DisplayName("Поиск продуктов по названию")
        void testSearchProductsByName() throws Exception {
            createProductAndSave("Картофель", 77.0, 2.0, 0.4, 16.3, "Овощи", "Требует приготовления");
            createProductAndSave("Картошка фри", 312.0, 3.4, 15.5, 41.0, "Овощи", "Требует приготовления");
            createProductAndSave("Мясо", 187.2, 18.9, 12.4, 0.0, "Мясной", "Требует приготовления");

            MvcResult result = mockMvc.perform(get("/api/products/search")
                            .param("name", "карто"))
                    .andExpect(status().isOk())
                    .andReturn();

            List<Product> products = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
            assertEquals(2, products.size());
        }

        @Test
        @Order(4)
        @DisplayName("Создание продукта с суммой БЖУ > 100 - ошибка")
        void testCreateProductWithBjuSumExceeds100() throws Exception {
            ProductDTO dto = createProductDto(
                    "Невалидный продукт", 500.0, 60.0, 30.0, 20.0,
                    "Овощи", "Готовый к употреблению",
                    List.of(), List.of()
            );

            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @Order(5)
        @DisplayName("Фильтрация продуктов по категории")
        void testFilterProductsByCategory() throws Exception {
            createProductAndSave("Картофель", 77.0, 2.0, 0.4, 16.3, "Овощи", "Требует приготовления");
            createProductAndSave("Мясо", 187.2, 18.9, 12.4, 0.0, "Мясной", "Требует приготовления");

            MvcResult result = mockMvc.perform(get("/api/products/search")
                            .param("category", "Овощи"))
                    .andExpect(status().isOk())
                    .andReturn();

            List<Product> products = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
            assertEquals(1, products.size());
            assertEquals("Овощи", products.get(0).getCategory());
        }

        @Test
        @Order(6)
        @DisplayName("Фильтрация продуктов по флагам")
        void testFilterProductsByFlags() throws Exception {
            ProductDTO veganProduct = createProductDto(
                    "Тофу", 76.0, 8.0, 4.8, 1.9,
                    "Овощи", "Готовый к употреблению",
                    List.of("Веган"), List.of()
            );
            createProductAndSaveFromDto(veganProduct);

            MvcResult result = mockMvc.perform(get("/api/products/search")
                            .param("flags", "Веган"))
                    .andExpect(status().isOk())
                    .andReturn();

            List<Product> products = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
            assertEquals(1, products.size());
            assertTrue(products.get(0).getFlags().contains("Веган"));
        }

        @Test
        @Order(7)
        @DisplayName("Граничное значение: сумма БЖУ ровно 100")
        void testCreateProductWithBjuSumExactly100() throws Exception {
            ProductDTO dto = createProductDto(
                    "Граничный продукт", 300.0, 50.0, 30.0, 20.0,
                    "Овощи", "Готовый к употреблению",
                    List.of(), List.of()
            );

            MvcResult result = mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn();

            Product product = objectMapper.readValue(result.getResponse().getContentAsString(), Product.class);
            assertEquals(100.0, product.getProteins() + product.getFats() + product.getCarbs(), 0.01);
        }

        @Test
        @Order(8)
        @DisplayName("Негативный сценарий: создание продукта с отрицательными белками")
        void testCreateProductWithNegativeProteins() throws Exception {
            ProductDTO dto = createProductDto(
                    "Невалидный продукт", 100.0, -5.0, 10.0, 10.0,
                    "Овощи", "Готовый к употреблению",
                    List.of(), List.of()
            );

            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @Order(9)
        @DisplayName("Негативный сценарий: создание продукта без названия")
        void testCreateProductWithoutName() throws Exception {
            ProductDTO dto = createProductDto(
                    "", 100.0, 10.0, 5.0, 10.0,
                    "Овощи", "Готовый к употреблению",
                    List.of(), List.of()
            );

            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @Order(11)
        @DisplayName("Редактирование продукта")
        void testUpdateProduct() throws Exception {
            ProductDTO createDto = createProductDto(
                    "Продукт для редактирования", 100.0, 10.0, 5.0, 10.0,
                    "Овощи", "Готовый к употреблению",
                    List.of("Веган"), List.of()
            );

            MvcResult createResult = mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isOk())
                    .andReturn();

            Product product = objectMapper.readValue(createResult.getResponse().getContentAsString(), Product.class);
            Long productId = product.getId();

            ProductDTO updateDto = createProductDto(
                    "Обновлённый продукт", 150.0, 15.0, 7.0, 12.0,
                    "Мясной", "Требует приготовления",
                    List.of("Без глютена"), List.of("https://example.com/new.jpg")
            );

            MvcResult updateResult = mockMvc.perform(put("/api/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andReturn();

            Product updatedProduct = objectMapper.readValue(updateResult.getResponse().getContentAsString(), Product.class);

            assertEquals("Обновлённый продукт", updatedProduct.getName());
            assertEquals(150.0, updatedProduct.getCalories());
            assertEquals("Мясной", updatedProduct.getCategory());
        }

        @Test
        @Order(12)
        @DisplayName("Фильтрация продуктов по необходимости готовки")
        void testFilterProductsByCookingRequirement() throws Exception {
            createProductAndSave("Салат", 50.0, 1.0, 0.5, 10.0, "Овощи", "Готовый к употреблению");
            createProductAndSave("Картофель", 77.0, 2.0, 0.4, 16.3, "Овощи", "Требует приготовления");

            MvcResult result = mockMvc.perform(get("/api/products/search")
                            .param("cookingRequirement", "Готовый к употреблению"))
                    .andExpect(status().isOk())
                    .andReturn();

            List<Product> products = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
            assertEquals(1, products.size());
            assertEquals("Готовый к употреблению", products.get(0).getCookingRequirement());
        }

        @Test
        @Order(13)
        @DisplayName("Комбинация фильтров (категория + флаги)")
        void testFilterProductsByCategoryAndFlags() throws Exception {
            ProductDTO veganProduct = createProductDto(
                    "Тофу", 76.0, 8.0, 4.8, 1.9,
                    "Овощи", "Готовый к употреблению",
                    List.of("Веган", "Без глютена"), List.of()
            );
            createProductAndSaveFromDto(veganProduct);

            MvcResult result = mockMvc.perform(get("/api/products/search")
                            .param("category", "Овощи")
                            .param("flags", "Веган")
                            .param("flags", "Без глютена"))
                    .andExpect(status().isOk())
                    .andReturn();

            List<Product> products = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
            assertEquals(1, products.size());
            assertTrue(products.get(0).getFlags().containsAll(List.of("Веган", "Без глютена")));
        }

        @Test
        @Order(14)
        @DisplayName("Поиск продукта по несуществующему имени - пустой результат")
        void testSearchProductByNonExistentName() throws Exception {
            createProductAndSave("Картофель", 77.0, 2.0, 0.4, 16.3, "Овощи", "Требует приготовления");

            MvcResult result = mockMvc.perform(get("/api/products/search")
                            .param("name", "несуществующий продукт"))
                    .andExpect(status().isOk())
                    .andReturn();

            List<Product> products = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
            assertEquals(0, products.size());
        }

        @Test
        @Order(15)
        @DisplayName("Создание продукта с максимальными значениями БЖУ (100) - НЕ создаётся")
        void testCreateProductWithMaxBjuValues() throws Exception {
            ProductDTO dto = createProductDto(
                    "Максимальный продукт", 900.0, 100.0, 100.0, 100.0,
                    "Овощи", "Готовый к употреблению",
                    List.of(), List.of()
            );

            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @Order(16)
        @DisplayName("Негативный сценарий: добавление 6 фото - ошибка валидации")
        void testCreateProductWithSixPhotos() throws Exception {
            ProductDTO dto = createProductDto(
                    "Продукт с 6 фото", 100.0, 10.0, 5.0, 10.0,
                    "Овощи", "Готовый к употреблению",
                    List.of(),
                    List.of("url1", "url2", "url3", "url4", "url5", "url6")
            );

            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @Order(17)
        @DisplayName("Позитивный сценарий: добавление 5 фото - успешно")
        void testCreateProductWithFivePhotos() throws Exception {
            ProductDTO dto = createProductDto(
                    "Продукт с 5 фото", 100.0, 10.0, 5.0, 10.0,
                    "Овощи", "Готовый к употреблению",
                    List.of(),
                    List.of("url1", "url2", "url3", "url4", "url5")
            );

            MvcResult result = mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn();

            Product product = objectMapper.readValue(result.getResponse().getContentAsString(), Product.class);
            assertEquals(5, product.getPhotos().size());
        }

        @Test
        @Order(18)
        @DisplayName("Граничное значение: название продукта из 2 символов (минимум)")
        void testCreateProductWithMinNameLength() throws Exception {
            ProductDTO dto = createProductDto(
                    "аб", 100.0, 10.0, 5.0, 10.0,
                    "Овощи", "Готовый к употреблению",
                    List.of(), List.of()
            );

            MvcResult result = mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn();

            Product product = objectMapper.readValue(result.getResponse().getContentAsString(), Product.class);
            assertEquals("аб", product.getName());
        }

        @Test
        @Order(19)
        @DisplayName("Граничное значение: название продукта из 1 символа (меньше минимума - ошибка)")
        void testCreateProductWithOneCharName() throws Exception {
            ProductDTO dto = createProductDto(
                    "а", 100.0, 10.0, 5.0, 10.0,
                    "Овощи", "Готовый к употреблению",
                    List.of(), List.of()
            );

            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @Order(20)
        @DisplayName("Граничное значение: белки = 0 (минимум)")
        void testCreateProductWithZeroProteins() throws Exception {
            ProductDTO dto = createProductDto(
                    "Нулевые белки", 100.0, 0.0, 10.0, 10.0,
                    "Овощи", "Готовый к употреблению",
                    List.of(), List.of()
            );

            MvcResult result = mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn();

            Product product = objectMapper.readValue(result.getResponse().getContentAsString(), Product.class);
            assertEquals(0.0, product.getProteins());
        }

        @Test
        @Order(21)
        @DisplayName("Граничное значение: белки = 100 (максимум)")
        void testCreateProductWithMaxProteins() throws Exception {
            ProductDTO dto = createProductDto(
                    "Максимум белков", 500.0, 100.0, 0.0, 0.0,
                    "Овощи", "Готовый к употреблению",
                    List.of(), List.of()
            );

            MvcResult result = mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn();

            Product product = objectMapper.readValue(result.getResponse().getContentAsString(), Product.class);
            assertEquals(100.0, product.getProteins());
        }

        @Test
        @Order(22)
        @DisplayName("Граничное значение: белки = 101 (больше максимума - ошибка)")
        void testCreateProductWithTooHighProteins() throws Exception {
            ProductDTO dto = createProductDto(
                    "Слишком много белков", 500.0, 101.0, 0.0, 0.0,
                    "Овощи", "Готовый к употреблению",
                    List.of(), List.of()
            );

            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Тесты управления блюдами")
    class DishTests {

        private Long potatoId;
        private Long meatId;
        private Long waterId;

        @BeforeEach
        void setUpDishData() throws Exception {
            potatoId = createProductAndGetId("Картофель", 77.0, 2.0, 0.4, 16.3, "Овощи", "Требует приготовления");
            meatId = createProductAndGetId("Мясо", 187.2, 18.9, 12.4, 0.0, "Мясной", "Требует приготовления");
            waterId = createProductAndGetId("Вода", 0.0, 0.0, 0.0, 0.0, "Жидкость", "Готовый к употреблению");
        }

        @Test
        @Order(1)
        @DisplayName("Создание блюда с явной категорией")
        void testCreateDishWithExplicitCategory() throws Exception {
            DishDTO dto = createDishDto(
                    "Борщ", List.of(
                            createComponent(potatoId, 150.0),
                            createComponent(meatId, 100.0),
                            createComponent(waterId, 500.0)
                    ),
                    200.0, "Суп", List.of()
            );

            MvcResult result = mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn();

            Dish dish = objectMapper.readValue(result.getResponse().getContentAsString(), Dish.class);
            createdDishId = dish.getId();

            assertEquals("Борщ", dish.getName());
            assertEquals("Суп", dish.getCategory());
        }

        @Test
        @Order(2)
        @DisplayName("Создание блюда с макросом !суп в названии")
        void testCreateDishWithMacroInName() throws Exception {
            DishDTO dto = createDishDto(
                    "Борщ !суп", List.of(
                            createComponent(potatoId, 150.0),
                            createComponent(meatId, 100.0),
                            createComponent(waterId, 500.0)
                    ),
                    200.0, "", List.of()
            );

            MvcResult result = mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn();

            Dish dish = objectMapper.readValue(result.getResponse().getContentAsString(), Dish.class);
            createdDishId = dish.getId();

            assertEquals("Борщ", dish.getName());
            assertEquals("Суп", dish.getCategory());
        }

        @Test
        @Order(3)
        @DisplayName("Автоматический расчёт КБЖУ блюда")
        void testCalculateNutritionForDish() throws Exception {
            DishDTO dto = createDishDto(
                    "Картофель с мясом", List.of(
                            createComponent(potatoId, 200.0),
                            createComponent(meatId, 150.0)
                    ),
                    350.0, "Второе", List.of()
            );

            MvcResult result = mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn();

            Dish dish = objectMapper.readValue(result.getResponse().getContentAsString(), Dish.class);
            createdDishId = dish.getId();

            double expectedCalories = (77.0 * 2.0) + (187.2 * 1.5);
            assertEquals(expectedCalories, dish.getCalories(), 0.01);
        }

        @Test
        @Order(4)
        @DisplayName("Поиск блюда по названию")
        void testSearchDishByName() throws Exception {
            DishDTO dto = createDishDto(
                    "Борщ сибирский", List.of(createComponent(potatoId, 100.0)),
                    200.0, "Суп", List.of()
            );
            createDishFromDto(dto);

            MvcResult result = mockMvc.perform(get("/api/dishes/search")
                            .param("name", "борщ"))
                    .andExpect(status().isOk())
                    .andReturn();

            List<Dish> dishes = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
            assertTrue(dishes.stream().anyMatch(d -> d.getName().toLowerCase().contains("борщ")));
        }

        @Test
        @Order(5)
        @DisplayName("Создание блюда без продуктов - ошибка")
        void testCreateDishWithoutComponents() throws Exception {
            DishDTO dto = createDishDto("Пустое блюдо", List.of(), 200.0, "Суп", List.of());

            mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @Order(6)
        @DisplayName("Фильтрация блюд по категории")
        void testFilterDishesByCategory() throws Exception {
            DishDTO soupDto = createDishDto("Уха", List.of(createComponent(waterId, 500.0)), 250.0, "Суп", List.of());
            DishDTO mainDto = createDishDto("Стейк", List.of(createComponent(meatId, 200.0)), 200.0, "Второе", List.of());
            createDishFromDto(soupDto);
            createDishFromDto(mainDto);

            MvcResult result = mockMvc.perform(get("/api/dishes/search")
                            .param("category", "Суп"))
                    .andExpect(status().isOk())
                    .andReturn();

            List<Dish> dishes = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
            assertEquals(1, dishes.size());
            assertEquals("Суп", dishes.get(0).getCategory());
        }

        @Test
        @Order(7)
        @DisplayName("Граничное значение: создание блюда с порцией 1 грамм")
        void testCreateDishWithMinPortionSize() throws Exception {
            DishDTO dto = createDishDto(
                    "Микропорция", List.of(createComponent(potatoId, 100.0)),
                    1.0, "Второе", List.of()
            );

            MvcResult result = mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn();

            Dish dish = objectMapper.readValue(result.getResponse().getContentAsString(), Dish.class);
            assertEquals(1.0, dish.getPortionSize());
        }

        @Test
        @Order(8)
        @DisplayName("Негативный сценарий: создание блюда с нулевым размером порции")
        void testCreateDishWithZeroPortion() throws Exception {
            DishDTO dto = createDishDto(
                    "Нулевая порция", List.of(createComponent(potatoId, 100.0)),
                    0.0, "Второе", List.of()
            );

            mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @Order(9)
        @DisplayName("Негативный сценарий: создание блюда с отрицательным размером порции")
        void testCreateDishWithNegativePortion() throws Exception {
            DishDTO dto = createDishDto(
                    "Отрицательная порция", List.of(createComponent(potatoId, 100.0)),
                    -100.0, "Второе", List.of()
            );

            mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @Order(10)
        @DisplayName("Редактирование блюда")
        void testUpdateDish() throws Exception {
            DishDTO createDto = createDishDto(
                    "Блюдо для обновления", List.of(createComponent(potatoId, 100.0)),
                    200.0, "Второе", List.of()
            );

            MvcResult createResult = mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isOk())
                    .andReturn();

            Dish dish = objectMapper.readValue(createResult.getResponse().getContentAsString(), Dish.class);
            Long dishId = dish.getId();

            DishDTO updateDto = createDishDto(
                    "Обновлённое блюдо", List.of(createComponent(potatoId, 200.0)),
                    300.0, "Первое", List.of()
            );

            MvcResult updateResult = mockMvc.perform(put("/api/dishes/{id}", dishId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andReturn();

            Dish updatedDish = objectMapper.readValue(updateResult.getResponse().getContentAsString(), Dish.class);
            assertEquals("Обновлённое блюдо", updatedDish.getName());
            assertEquals(300.0, updatedDish.getPortionSize());
            assertEquals("Первое", updatedDish.getCategory());
        }

        @Test
        @Order(11)
        @DisplayName("Фильтрация блюд по флагам")
        void testFilterDishesByFlags() throws Exception {
            DishDTO veganDto = createDishDto(
                    "Овощной салат", List.of(createComponent(potatoId, 100.0)),
                    150.0, "Салат", List.of("Веган")
            );
            createDishFromDto(veganDto);

            MvcResult result = mockMvc.perform(get("/api/dishes/search")
                            .param("flags", "Веган"))
                    .andExpect(status().isOk())
                    .andReturn();

            List<Dish> dishes = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
            assertTrue(dishes.stream().allMatch(d -> d.getFlags().contains("Веган")));
        }

        @Test
        @Order(12)
        @DisplayName("Поиск блюда по несуществующему названию - пустой результат")
        void testSearchDishByNonExistentName() throws Exception {
            DishDTO dto = createDishDto(
                    "Борщ", List.of(createComponent(potatoId, 100.0)),
                    200.0, "Суп", List.of()
            );
            createDishFromDto(dto);

            MvcResult result = mockMvc.perform(get("/api/dishes/search")
                            .param("name", "несуществующее блюдо"))
                    .andExpect(status().isOk())
                    .andReturn();

            List<Dish> dishes = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
            assertEquals(0, dishes.size());
        }

        @Test
        @Order(13)
        @DisplayName("Граничное значение: порция 0.1 грамма")
        void testCreateDishWithMinimalPortion() throws Exception {
            DishDTO dto = createDishDto(
                    "Микро блюдо", List.of(createComponent(potatoId, 100.0)),
                    0.1, "Второе", List.of()
            );

            MvcResult result = mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn();

            Dish dish = objectMapper.readValue(result.getResponse().getContentAsString(), Dish.class);
            assertEquals(0.1, dish.getPortionSize());
        }

        @Test
        @Order(14)
        @DisplayName("Создание блюда с макросом в середине названия")
        void testCreateDishWithMacroInMiddleOfName() throws Exception {
            DishDTO dto = createDishDto(
                    "Борщ !суп вкусный", List.of(
                            createComponent(potatoId, 150.0),
                            createComponent(meatId, 100.0),
                            createComponent(waterId, 500.0)
                    ),
                    200.0, "", List.of()
            );

            MvcResult result = mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn();

            Dish dish = objectMapper.readValue(result.getResponse().getContentAsString(), Dish.class);
            assertEquals("Борщ вкусный", dish.getName().replaceAll("\\s+", " ").trim());
            assertEquals("Суп", dish.getCategory());
        }

        @Test
        @Order(15)
        @DisplayName("Создание блюда с несколькими макросами - применяется только первый")
        void testCreateDishWithMultipleMacros() throws Exception {
            DishDTO dto = createDishDto(
                    "!суп !первое Борщ", List.of(
                            createComponent(potatoId, 150.0),
                            createComponent(meatId, 100.0),
                            createComponent(waterId, 500.0)
                    ),
                    200.0, "", List.of()
            );

            MvcResult result = mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn();

            Dish dish = objectMapper.readValue(result.getResponse().getContentAsString(), Dish.class);
            assertEquals("Борщ", dish.getName());
            assertEquals("Суп", dish.getCategory());
        }

        @Test
        @Order(16)
        @DisplayName("Пользовательская категория важнее макроса")
        void testExplicitCategoryOverridesMacro() throws Exception {
            DishDTO dto = createDishDto(
                    "Борщ !суп", List.of(
                            createComponent(potatoId, 150.0),
                            createComponent(meatId, 100.0),
                            createComponent(waterId, 500.0)
                    ),
                    200.0, "Десерт", List.of()
            );

            MvcResult result = mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn();

            Dish dish = objectMapper.readValue(result.getResponse().getContentAsString(), Dish.class);
            assertEquals("Десерт", dish.getCategory());
        }

        @Test
        @Order(17)
        @DisplayName("Позитивный сценарий: добавление 5 фото для блюда - успешно")
        void testCreateDishWithFivePhotos() throws Exception {
            DishDTO dto = createDishDto(
                    "Блюдо с 5 фото", List.of(createComponent(potatoId, 100.0)),
                    200.0, "Второе", List.of()
            );
            dto.setPhotos(List.of("url1", "url2", "url3", "url4", "url5"));

            MvcResult result = mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn();

            Dish dish = objectMapper.readValue(result.getResponse().getContentAsString(), Dish.class);
            assertEquals(5, dish.getPhotos().size());
        }
    }

    @Nested
    @DisplayName("Тесты удаления")
    class DeleteTests {

        private Long productToDeleteId;

        @BeforeEach
        void setUpDeleteData() throws Exception {
            productToDeleteId = createProductAndGetId("Продукт для удаления", 100.0, 10.0, 5.0, 10.0, "Овощи", "Готовый к употреблению");
        }

        @Test
        @Order(1)
        @DisplayName("Удаление продукта, не используемого в блюдах")
        void testDeleteProductNotUsedInDishes() throws Exception {
            MvcResult checkResult = mockMvc.perform(get("/api/products/{id}/check-delete", productToDeleteId))
                    .andExpect(status().isOk())
                    .andReturn();

            Map<String, Object> result = objectMapper.readValue(checkResult.getResponse().getContentAsString(), new TypeReference<>() {});
            assertTrue((Boolean) result.get("canDelete"));

            mockMvc.perform(delete("/api/products/{id}", productToDeleteId))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/products/{id}", productToDeleteId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @Order(2)
        @DisplayName("Невозможно удалить продукт, используемый в блюде")
        void testDeleteProductUsedInDish() throws Exception {
            Long potatoId = createProductAndGetId("Картофель", 77.0, 2.0, 0.4, 16.3, "Овощи", "Требует приготовления");

            DishDTO dishDto = createDishDto(
                    "Блюдо с картофелем", List.of(createComponent(potatoId, 100.0)),
                    200.0, "Второе", List.of()
            );
            createDishFromDto(dishDto);

            MvcResult checkResult = mockMvc.perform(get("/api/products/{id}/check-delete", potatoId))
                    .andExpect(status().isOk())
                    .andReturn();

            Map<String, Object> result = objectMapper.readValue(checkResult.getResponse().getContentAsString(), new TypeReference<>() {});
            assertFalse((Boolean) result.get("canDelete"));
        }

        @Test
        @Order(3)
        @DisplayName("Удаление блюда")
        void testDeleteDish() throws Exception {
            DishDTO dto = createDishDto(
                    "Блюдо для удаления", List.of(createComponent(productToDeleteId, 100.0)),
                    200.0, "Второе", List.of()
            );

            MvcResult createResult = mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn();

            Dish dish = objectMapper.readValue(createResult.getResponse().getContentAsString(), Dish.class);
            Long dishId = dish.getId();

            mockMvc.perform(delete("/api/dishes/{id}", dishId))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/dishes/{id}", dishId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Тесты редактирования")
    class UpdateTests {

        private Long productToUpdateId;
        private Long dishToUpdateId;

        @BeforeEach
        void setUpUpdateData() throws Exception {
            productToUpdateId = createProductAndGetId("Продукт для редактирования", 100.0, 10.0, 5.0, 10.0, "Овощи", "Готовый к употреблению");

            DishDTO dishDto = createDishDto(
                    "Блюдо для редактирования", List.of(createComponent(productToUpdateId, 100.0)),
                    200.0, "Второе", List.of()
            );
            MvcResult result = mockMvc.perform(post("/api/dishes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dishDto)))
                    .andExpect(status().isOk())
                    .andReturn();
            Dish dish = objectMapper.readValue(result.getResponse().getContentAsString(), Dish.class);
            dishToUpdateId = dish.getId();
        }

        @Test
        @Order(1)
        @DisplayName("Редактирование продукта - изменение всех полей")
        void testUpdateProductAllFields() throws Exception {
            ProductDTO updateDto = createProductDto(
                    "Обновлённый продукт", 200.0, 20.0, 10.0, 15.0,
                    "Мясной", "Требует приготовления",
                    List.of("Без глютена"), List.of("https://example.com/photo.jpg")
            );

            MvcResult result = mockMvc.perform(put("/api/products/{id}", productToUpdateId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andReturn();

            Product product = objectMapper.readValue(result.getResponse().getContentAsString(), Product.class);

            assertEquals("Обновлённый продукт", product.getName());
            assertEquals(200.0, product.getCalories());
            assertEquals("Мясной", product.getCategory());
            assertEquals("Требует приготовления", product.getCookingRequirement());
            assertTrue(product.getFlags().contains("Без глютена"));
        }

        @Test
        @Order(2)
        @DisplayName("Редактирование блюда - изменение состава")
        void testUpdateDishComponents() throws Exception {
            Long newProductId = createProductAndGetId("Новый продукт", 50.0, 5.0, 2.0, 8.0, "Овощи", "Готовый к употреблению");

            DishDTO updateDto = createDishDto(
                    "Обновлённое блюдо", List.of(createComponent(newProductId, 200.0)),
                    300.0, "Первое", List.of()
            );

            MvcResult result = mockMvc.perform(put("/api/dishes/{id}", dishToUpdateId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andReturn();

            Dish dish = objectMapper.readValue(result.getResponse().getContentAsString(), Dish.class);

            assertEquals("Обновлённое блюдо", dish.getName());
            assertEquals(300.0, dish.getPortionSize());
            assertEquals("Первое", dish.getCategory());
            assertEquals(1, dish.getComponents().size());
            assertEquals("Новый продукт", dish.getComponents().get(0).getProduct().getName());
        }

        @Test
        @Order(3)
        @DisplayName("Редактирование блюда - удаление всех продуктов (ошибка)")
        void testUpdateDishRemoveAllComponents() throws Exception {
            DishDTO updateDto = createDishDto(
                    "Пустое блюдо", List.of(),
                    200.0, "Второе", List.of()
            );

            mockMvc.perform(put("/api/dishes/{id}", dishToUpdateId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isBadRequest());
        }
    }


    private ProductDTO createProductDto(String name, double calories, double proteins, double fats, double carbs,
                                        String category, String cookingRequirement, List<String> flags, List<String> photos) {
        ProductDTO dto = new ProductDTO();
        dto.setName(name);
        dto.setCalories(calories);
        dto.setProteins(proteins);
        dto.setFats(fats);
        dto.setCarbs(carbs);
        dto.setCategory(category);
        dto.setCookingRequirement(cookingRequirement);
        dto.setFlags(flags != null ? flags : new ArrayList<>());
        dto.setPhotos(photos != null ? photos : new ArrayList<>());
        return dto;
    }

    private Product createProductAndSave(String name, double calories, double proteins, double fats, double carbs,
                                         String category, String cookingRequirement) throws Exception {
        ProductDTO dto = createProductDto(name, calories, proteins, fats, carbs, category, cookingRequirement, List.of(), List.of());
        return createProductAndSaveFromDto(dto);
    }

    private Long createProductAndGetId(String name, double calories, double proteins, double fats, double carbs,
                                       String category, String cookingRequirement) throws Exception {
        Product product = createProductAndSave(name, calories, proteins, fats, carbs, category, cookingRequirement);
        return product.getId();
    }

    private Product createProductAndSaveFromDto(ProductDTO dto) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), Product.class);
    }

    private DishDTO.ComponentDTO createComponent(Long productId, double quantity) {
        DishDTO.ComponentDTO component = new DishDTO.ComponentDTO();
        component.setProductId(productId);
        component.setQuantity(quantity);
        return component;
    }

    private DishDTO createDishDto(String name, List<DishDTO.ComponentDTO> components, double portionSize, String category, List<String> flags) {
        DishDTO dto = new DishDTO();
        dto.setName(name);
        dto.setComponents(components);
        dto.setPortionSize(portionSize);
        dto.setCategory(category);
        dto.setFlags(flags != null ? flags : new ArrayList<>());
        return dto;
    }

    private Dish createDishFromDto(DishDTO dto) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/dishes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), Dish.class);
    }
}
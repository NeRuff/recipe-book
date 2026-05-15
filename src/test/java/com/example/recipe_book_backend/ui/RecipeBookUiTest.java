package com.example.recipe_book_backend.ui;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RecipeBookUiTest {

    private static WebDriver driver;
    private static WebDriverWait wait;

    // Списки для очистки
    private static List<String> createdProducts = new ArrayList<>();
    private static List<String> createdDishes = new ArrayList<>();

    // ID созданных продуктов для тестов блюд (общие для всех тестов)
    private static String sharedPotatoId;
    private static String sharedMeatId;
    private static String sharedWaterId;
    private static String sharedPotatoName;
    private static String sharedMeatName;
    private static String sharedWaterName;

    @BeforeAll
    static void setUp() {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();
        options.setBinary("C:\\Users\\Роман\\AppData\\Local\\Mozilla Firefox\\firefox.exe");
        driver = new FirefoxDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(6));
        driver.manage().window().maximize();

        // Создаем общие продукты для тестов блюд ДО всех тестов
        driver.get("http://localhost:8080");
        wait.until(webDriver -> ((JavascriptExecutor) webDriver)
                .executeScript("return document.readyState").equals("complete"));

        clickTab("products");

        sharedPotatoName = "SharedКартофель";
        sharedMeatName = "SharedМясо";
        sharedWaterName = "SharedВода";

        createProductViaUiStatic(sharedPotatoName, 77, 2, 0.4, 16.3, "Овощи", "Требует приготовления");
        createProductViaUiStatic(sharedMeatName, 187.2, 18.9, 12.4, 0, "Мясной", "Требует приготовления");
        createProductViaUiStatic(sharedWaterName, 0, 0, 0, 0, "Жидкость", "Готовый к употреблению");

        sharedPotatoId = getProductId(sharedPotatoName);
        sharedMeatId = getProductId(sharedMeatName);
        sharedWaterId = getProductId(sharedWaterName);

        createdProducts.add(sharedPotatoName);
        createdProducts.add(sharedMeatName);
        createdProducts.add(sharedWaterName);
    }

    @BeforeEach
    void navigateToHome() {
        driver.get("http://localhost:8080");
        wait.until(webDriver -> ((JavascriptExecutor) webDriver)
                .executeScript("return document.readyState").equals("complete"));

        // Закрываем любые открытые модальные окна
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("document.getElementById('dishModal').style.display='none'");
            js.executeScript("document.getElementById('productModal').style.display='none'");
        } catch (Exception e) {
        }
    }

    @AfterEach
    void cleanUp() {
        // Сначала закрываем любые открытые модальные окна
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("document.getElementById('dishModal').style.display='none'");
            js.executeScript("document.getElementById('productModal').style.display='none'");
        } catch (Exception e) {
        }

        // Удаляем только временные блюда
        for (String dishName : createdDishes) {
            try {
                deleteDish(dishName);
            } catch (Exception e) {
                System.out.println("Не удалось удалить блюдо: " + dishName);
            }
        }
        createdDishes.clear();

        // Возвращаемся на вкладку продуктов для следующих тестов
        try {
            clickTab("products");
        } catch (Exception e) {
        }
    }

    @AfterAll
    static void tearDown() {
        // Удаляем общие продукты после всех тестов
        if (driver != null) {
            try {
                clickTab("products");
                for (String productName : createdProducts) {
                    try {
                        deleteProductStatic(productName);
                    } catch (Exception e) {
                        System.out.println("Не удалось удалить продукт: " + productName);
                    }
                }
            } catch (Exception e) {
                System.out.println("Ошибка при очистке: " + e.getMessage());
            }
            driver.quit();
        }
    }

    @Nested
    @DisplayName("UI тесты управления продуктами")
    class ProductUiTests {

        @Test
        @Order(1)
        @DisplayName("EP1: Создание продукта с нормальными значениями")
        void testCreateProductWithNormalValues() throws InterruptedException {
            String productName = "ТестПродукт" + System.currentTimeMillis();

            // Открываем форму
            clickTab("products");
            clickAddProductButton();

            // Заполняем только обязательные поля
            fillProductField("prodName", productName);
            fillProductField("prodCalories", "77");
            fillProductField("prodProteins", "2");
            fillProductField("prodFats", "0.4");
            fillProductField("prodCarbs", "16.3");
            selectProductCategory("Овощи");
            selectProductCooking("Требует приготовления");

            // Сохраняем
            WebElement saveButton = driver.findElement(By.xpath("//form[@id='productForm']//button[contains(text(), 'Сохранить')]"));
            saveButton.click();

            // Ждем закрытия модального окна
            Thread.sleep(1000);

            // Обновляем страницу
            driver.navigate().refresh();
            Thread.sleep(1000);

            // Проверяем наличие продукта
            assertTrue(driver.getPageSource().contains(productName), "Продукт не найден: " + productName);

            createdProducts.add(productName);
        }

        @Test
        @Order(2)
        @DisplayName("BVA: Создание продукта с нулевой калорийностью")
        void testCreateProductWithZeroCalories() {
            String productName = "ВременнаяВода" + System.currentTimeMillis();
            boolean success = createProductAndCheckSuccess(productName, 0, 0, 0, 0, "Жидкость", "Готовый к употреблению");

            assertTrue(success, "Продукт с нулевой калорийностью должен создаться: " + productName);
            createdProducts.add(productName);
        }


        @Test
        @Order(3)
        @DisplayName("Негативный сценарий: сумма БЖУ > 100 - ошибка")
        void testCreateProductWithBjuSumExceeds100() {
            String productName = "Невалидный продукт";
            clickAddProductButton();

            fillProductField("prodName", productName);
            fillProductField("prodCalories", "500");
            fillProductField("prodProteins", "60");
            fillProductField("prodFats", "30");
            fillProductField("prodCarbs", "20");
            selectProductCategory("Овощи");
            selectProductCooking("Готовый к употреблению");

            clickSaveProduct();

            // Проверяем, что продукт НЕ создался
            boolean notCreated = !isProductDisplayed(productName);

            // Принудительно закрываем модальное окно
            try {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("document.getElementById('productModal').style.display='none'");
            } catch (Exception e) {
            }

            assertTrue(notCreated, "Продукт с суммой БЖУ > 100 не должен создаваться");
        }

        @Test
        @Order(4)
        @DisplayName("EP2: Поиск продуктов по названию")
        void testSearchProductsByName() {
            clickTab("products");

            // Очищаем список перед тестом
            WebElement searchInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("productSearch")));
            searchInput.clear();
            searchInput.sendKeys("");
            sleep(1000);

            // Закрываем любое открытое модальное окно
            try {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("document.getElementById('productModal').style.display='none'");
            } catch (Exception e) {
            }

            // Создаем уникальные продукты с уникальными именами
            String uniqueId = String.valueOf(System.currentTimeMillis());
            String product1 = "ПоискКартофель" + uniqueId;
            String product2 = "ПоискКартошка" + uniqueId;
            String product3 = "ПоискМясо" + uniqueId;

            createProductAndCheckSuccess(product1, 77, 2, 0.4, 16.3, "Овощи", "Требует приготовления");
            createProductAndCheckSuccess(product2, 312, 3.4, 15.5, 41, "Овощи", "Требует приготовления");
            createProductAndCheckSuccess(product3, 187.2, 18.9, 12.4, 0, "Мясной", "Требует приготовления");

            createdProducts.add(product1);
            createdProducts.add(product2);
            createdProducts.add(product3);

            sleep(1000);

            // Обновляем список после создания
            clickTab("products");
            sleep(500);

            searchInput.clear();
            searchInput.sendKeys("ПоискКарто");
            sleep(1000);

            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("productsList")));
            List<WebElement> cards = driver.findElements(By.cssSelector("#productsList .card"));

            // Подсчитываем только наши продукты
            long ourProductsCount = cards.stream()
                    .filter(card -> card.getText().contains("ПоискКарто"))
                    .count();

            assertEquals(2, ourProductsCount,
                    "Должно найтись 2 продукта с 'ПоискКарто' в названии. Найдено: " + ourProductsCount);
        }

        @Test
        @Order(5)
        @DisplayName("EP3: Фильтрация продуктов по категории")
        void testFilterProductsByCategory() {
            clickTab("products");

            // Закрываем любое открытое модальное окно
            try {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("document.getElementById('productModal').style.display='none'");
            } catch (Exception e) {
            }

            String uniqueId = String.valueOf(System.currentTimeMillis());
            String uniqueProduct = "ФильтрОвощ" + uniqueId;
            String uniqueMeat = "ФильтрМясо" + uniqueId;

            // Создаем продукты с уникальными именами
            createProductAndCheckSuccess(uniqueProduct, 77, 2, 0.4, 16.3, "Овощи", "Требует приготовления");
            createProductAndCheckSuccess(uniqueMeat, 187.2, 18.9, 12.4, 0, "Мясной", "Требует приготовления");

            createdProducts.add(uniqueProduct);
            createdProducts.add(uniqueMeat);

            sleep(500);

            // Обновляем страницу
            clickTab("products");
            sleep(500);

            // Применяем фильтр
            WebElement categorySelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("productCategory")));
            categorySelect.sendKeys("Овощи");
            sleep(1000);

            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("productsList")));
            List<WebElement> cards = driver.findElements(By.cssSelector("#productsList .card"));

            // Считаем только созданные продукты с категорией "Овощи"
            long овощиCount = cards.stream()
                    .filter(card -> card.getText().contains(uniqueProduct) && card.getText().contains("Овощи"))
                    .count();

            assertEquals(1, овощиCount,
                    "Должен найтись 1 продукт категории 'Овощи'. Найдено: " + овощиCount);
        }

        @Test
        @Order(6)
        @DisplayName("Негативный сценарий: создание продукта с отрицательными белками")
        void testCreateProductWithNegativeProteins() {
            String productName = "Невалидный продукт" + System.currentTimeMillis();
            clickAddProductButton();

            fillProductField("prodName", productName);
            fillProductField("prodCalories", "100");
            fillProductField("prodProteins", "-5");
            fillProductField("prodFats", "10");
            fillProductField("prodCarbs", "10");
            selectProductCategory("Овощи");
            selectProductCooking("Готовый к употреблению");

            clickSaveProduct();

            // Проверяем, что продукт НЕ создался
            boolean notCreated = !isProductDisplayed(productName);

            // Принудительно закрываем модальное окно
            try {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("document.getElementById('productModal').style.display='none'");
            } catch (Exception e) {
            }

            assertTrue(notCreated, "Продукт с отрицательными белками не должен создаваться");
        }

        @Test
        @Order(7)
        @DisplayName("Граничное значение: сумма БЖУ ровно 100")
        void testCreateProductWithBjuSumExactly100() {
            String productName = "ГраничныйПродукт" + System.currentTimeMillis();
            clickTab("products");
            clickAddProductButton();

            fillProductField("prodName", productName);
            fillProductField("prodCalories", "300");
            fillProductField("prodProteins", "50");
            fillProductField("prodFats", "30");
            fillProductField("prodCarbs", "20");
            selectProductCategory("Овощи");
            selectProductCooking("Готовый к употреблению");

            clickSaveProduct();

            assertTrue(isProductDisplayed(productName));
            createdProducts.add(productName);
        }

        @Test
        @Order(8)
        @DisplayName("Граничное значение: максимальное количество фото (5)")
        void testCreateProductWithMaxPhotos() {
            String productName = "Продукт5Фото" + System.currentTimeMillis();
            clickAddProductButton();

            fillProductField("prodName", productName);
            fillProductField("prodCalories", "100");
            fillProductField("prodProteins", "10");
            fillProductField("prodFats", "5");
            fillProductField("prodCarbs", "10");
            selectProductCategory("Овощи");
            selectProductCooking("Готовый к употреблению");
            fillProductField("prodPhotos", "url1, url2, url3, url4, url5");

            clickSaveProduct();

            assertTrue(isProductDisplayed(productName), "Продукт с 5 фото должен создаться");
            createdProducts.add(productName);
        }

        @Test
        @Order(9)
        @DisplayName("Негативный сценарий: 6 фото - ошибка валидации")
        void testCreateProductWithSixPhotos() {
            String productName = "Продукт6Фото" + System.currentTimeMillis();
            clickAddProductButton();

            fillProductField("prodName", productName);
            fillProductField("prodCalories", "100");
            fillProductField("prodProteins", "10");
            fillProductField("prodFats", "5");
            fillProductField("prodCarbs", "10");
            selectProductCategory("Овощи");
            selectProductCooking("Готовый к употреблению");
            fillProductField("prodPhotos", "url1, url2, url3, url4, url5, url6");

            clickSaveProduct();

            // Проверяем, что продукт НЕ создался
            boolean notCreated = !isProductDisplayed(productName);

            // Принудительно закрываем модальное окно
            try {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("document.getElementById('productModal').style.display='none'");
            } catch (Exception e) {
            }

            assertTrue(notCreated, "Продукт с 6 фото не должен создаваться");
        }
        @Test
        @Order(10)
        @DisplayName("Граничное значение: калории = 0.01 (минимальное положительное)")
        void testCreateProductWithMinPositiveCalories() throws InterruptedException {
            String productName = "МинКалории" + System.currentTimeMillis();

            clickTab("products");
            clickAddProductButton();

            fillProductField("prodName", productName);
            fillProductField("prodCalories", "0.01");
            fillProductField("prodProteins", "0");
            fillProductField("prodFats", "0");
            fillProductField("prodCarbs", "0");
            selectProductCategory("Овощи");
            selectProductCooking("Готовый к употреблению");

            clickSave();
            Thread.sleep(1000);
            driver.navigate().refresh();
            Thread.sleep(1000);

            assertTrue(driver.getPageSource().contains(productName), "Продукт с 0.01 калорий должен создаться");
            createdProducts.add(productName);
        }

        @Test
        @Order(11)
        @DisplayName("Граничное значение: калории = 0 (минимум)")
        void testCreateProductWithZeroCaloriesBoundary() throws InterruptedException {
            String productName = "НольКалорий" + System.currentTimeMillis();

            clickTab("products");
            clickAddProductButton();

            fillProductField("prodName", productName);
            fillProductField("prodCalories", "0");
            fillProductField("prodProteins", "0");
            fillProductField("prodFats", "0");
            fillProductField("prodCarbs", "0");
            selectProductCategory("Овощи");
            selectProductCooking("Готовый к употреблению");

            clickSave();
            Thread.sleep(1000);
            driver.navigate().refresh();
            Thread.sleep(1000);

            assertTrue(driver.getPageSource().contains(productName), "Продукт с 0 калорий должен создаться");
            createdProducts.add(productName);
        }

        @Test
        @Order(12)
        @DisplayName("Граничное значение: отрицательные калории - ошибка")
        void testCreateProductWithNegativeCalories() throws InterruptedException {
            String productName = "ОтрицКалории" + System.currentTimeMillis();

            clickTab("products");
            clickAddProductButton();

            fillProductField("prodName", productName);
            fillProductField("prodCalories", "-1");
            fillProductField("prodProteins", "10");
            fillProductField("prodFats", "5");
            fillProductField("prodCarbs", "10");
            selectProductCategory("Овощи");
            selectProductCooking("Готовый к употреблению");

            clickSave();
            Thread.sleep(1000);

            assertFalse(driver.getPageSource().contains(productName), "Продукт с отрицательными калориями не должен создаваться");
        }

        @Test
        @Order(13)
        @DisplayName("Граничное значение: название из 50 символов (максимум)")
        void testCreateProductWithMaxNameLength() throws InterruptedException {
            String productName = "А".repeat(50) + System.currentTimeMillis();
            if (productName.length() > 50) {
                productName = productName.substring(0, 50);
            }

            clickTab("products");
            clickAddProductButton();

            fillProductField("prodName", productName);
            fillProductField("prodCalories", "100");
            fillProductField("prodProteins", "10");
            fillProductField("prodFats", "5");
            fillProductField("prodCarbs", "10");
            selectProductCategory("Овощи");
            selectProductCooking("Готовый к употреблению");

            clickSave();
            Thread.sleep(1000);
            driver.navigate().refresh();
            Thread.sleep(1000);

            assertTrue(driver.getPageSource().contains(productName), "Продукт с названием из 50 символов должен создаться");
            createdProducts.add(productName);
        }
    }

    @Nested
    @DisplayName("UI тесты управления блюдами")
    class DishUiTests {

        @BeforeEach
        void setUp() {
            clickTab("dishes");
        }

        @Test
        @Order(1)
        @DisplayName("EP1: Создание блюда с явной категорией")
        void testCreateDishWithExplicitCategory() {
            clickAddDishButton();

            String dishName = "ТестБорщ" + System.currentTimeMillis();
            fillDishField("dishName", dishName);
            fillDishField("dishPortion", "200");
            selectDishCategory("Суп");

            addDishComponent(sharedPotatoId, "150");
            addDishComponent(sharedMeatId, "100");
            addDishComponent(sharedWaterId, "500");

            clickSaveDish();

            assertTrue(isDishDisplayed(dishName));
            createdDishes.add(dishName);
        }

        @Test
        @Order(2)
        @DisplayName("EP2: Создание блюда с макросом !суп в названии")
        void testCreateDishWithMacroInName() {
            clickAddDishButton();

            String dishName = "ТестБорщСуп" + System.currentTimeMillis();
            fillDishField("dishName", dishName + " !суп");
            fillDishField("dishPortion", "200");

            addDishComponent(sharedPotatoId, "150");
            addDishComponent(sharedMeatId, "100");
            addDishComponent(sharedWaterId, "500");

            clickSaveDish();

            assertTrue(isDishDisplayed(dishName));
            createdDishes.add(dishName);
        }

        @Test
        @Order(3)
        @DisplayName("BVA: Автоматический расчёт КБЖУ блюда")
        void testCalculateNutritionForDish() {
            clickAddDishButton();

            fillDishField("dishName", "ТестРасчет" + System.currentTimeMillis());
            fillDishField("dishPortion", "350");

            addDishComponent(sharedPotatoId, "200");
            addDishComponent(sharedMeatId, "150");

            clickCalculateNutrition();

            WebElement caloriesField = driver.findElement(By.id("dishCalories"));
            String caloriesValue = caloriesField.getAttribute("value");

            assertNotNull(caloriesValue);
            assertFalse(caloriesValue.isEmpty());
            createdDishes.add("ТестРасчет" + System.currentTimeMillis());
        }

        @Test
        @Order(4)
        @DisplayName("EP3: Поиск блюда по названию")
        void testSearchDishByName() {
            String dishName = "БорщСибирский" + System.currentTimeMillis();
            createDishViaUi(dishName, List.of(
                    new ComponentDto(sharedPotatoId, "100")
            ), "200", "Суп");
            createdDishes.add(dishName);

            WebElement searchInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("dishSearch")));
            searchInput.clear();
            searchInput.sendKeys(dishName);

            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("dishesList")));
            List<WebElement> cards = driver.findElements(By.cssSelector("#dishesList .card"));
            assertTrue(cards.size() > 0);
        }

        @Test
        @Order(5)
        @DisplayName("Граничное значение: создание блюда с порцией 1 грамм")
        void testCreateDishWithMinPortionSize() {
            clickAddDishButton();

            String dishName = "Микропорция" + System.currentTimeMillis();
            fillDishField("dishName", dishName);
            fillDishField("dishPortion", "1");
            selectDishCategory("Второе");
            addDishComponent(sharedPotatoId, "100");

            clickSaveDish();

            assertTrue(isDishDisplayed(dishName));
            createdDishes.add(dishName);
        }

        @Test
        @Order(6)
        @DisplayName("Негативный сценарий: создание блюда без продуктов")
        void testCreateDishWithoutComponents() {
            clickAddDishButton();

            String dishName = "Пустое блюдо";
            fillDishField("dishName", dishName);
            fillDishField("dishPortion", "200");

            clickSaveDish();

            // Проверяем, что блюдо не создалось
            boolean notCreated = isDishNotCreated(dishName);

            // Проверяем, что модальное окно все еще открыто (ошибка)
            boolean modalStillOpen = isValidationErrorDetected();

            // Принудительно закрываем модальное окно
            try {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("document.getElementById('dishModal').style.display='none'");
            } catch (Exception e) {
            }

            assertTrue(notCreated || modalStillOpen, "Блюдо без продуктов не должно создаваться");
        }

        @Test
        @Order(7)
        @DisplayName("Негативный сценарий: создание блюда с нулевой порцией")
        void testCreateDishWithZeroPortion() {
            clickAddDishButton();

            String dishName = "Нулевая порция";
            fillDishField("dishName", dishName);
            fillDishField("dishPortion", "0");
            addDishComponent(sharedPotatoId, "100");

            clickSaveDish();

            // Проверяем, что блюдо не создалось
            boolean notCreated = isDishNotCreated(dishName);

            // Проверяем, что модальное окно все еще открыто (ошибка)
            boolean modalStillOpen = isValidationErrorDetected();

            // Принудительно закрываем модальное окно
            try {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("document.getElementById('dishModal').style.display='none'");
            } catch (Exception e) {
            }

            assertTrue(notCreated || modalStillOpen,
                    "Блюдо с нулевой порцией не должно создаваться. " +
                            "Создалось: " + !notCreated + ", Модальное окно открыто: " + modalStillOpen);
        }

        @Test
        @Order(8)
        @DisplayName("Фильтрация блюд по категории")
        void testFilterDishesByCategory() {
            String dishName1 = "Уха" + System.currentTimeMillis();
            String dishName2 = "Стейк" + System.currentTimeMillis();

            createDishViaUi(dishName1, List.of(new ComponentDto(sharedWaterId, "500")), "250", "Суп");
            createDishViaUi(dishName2, List.of(new ComponentDto(sharedMeatId, "200")), "200", "Второе");

            createdDishes.add(dishName1);
            createdDishes.add(dishName2);

            WebElement categorySelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("dishCategory")));
            categorySelect.sendKeys("Суп");
            sleep(500);

            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("dishesList")));
            List<WebElement> cards = driver.findElements(By.cssSelector("#dishesList .card"));
            assertEquals(1, cards.size());
        }

        @Test
        @Order(9)
        @DisplayName("Негативный сценарий: добавление 6 фото для блюда - ошибка")
        void testCreateDishWithSixPhotos() {
            clickAddDishButton();

            String dishName = "Блюдо с 6 фото";
            fillDishField("dishName", dishName);
            fillDishField("dishPortion", "200");
            addDishComponent(sharedPotatoId, "100");
            fillDishField("dishPhotos", "url1, url2, url3, url4, url5, url6");

            clickSaveDish();

            // Проверяем, что блюдо не создалось
            boolean notCreated = isDishNotCreated(dishName);

            // Проверяем, что модальное окно все еще открыто (ошибка)
            boolean modalStillOpen = isValidationErrorDetected();

            // Принудительно закрываем модальное окно
            try {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("document.getElementById('dishModal').style.display='none'");
            } catch (Exception e) {
            }

            assertTrue(notCreated || modalStillOpen,
                    "Блюдо с 6 фото не должно создаваться");
        }

        @Test
        @Order(10)
        @DisplayName("Позитивный сценарий: 5 фото для блюда - успешно")
        void testCreateDishWithFivePhotos() {
            String dishName = "Блюдо5Фото" + System.currentTimeMillis();
            clickAddDishButton();

            fillDishField("dishName", dishName);
            fillDishField("dishPortion", "200");
            addDishComponent(sharedPotatoId, "100");
            fillDishField("dishPhotos", "url1, url2, url3, url4, url5");

            clickSaveDish();

            assertTrue(isDishDisplayed(dishName));
            createdDishes.add(dishName);
        }
    }

    @Nested
    @DisplayName("UI тесты удаления")
    class DeleteUiTests {

        @Test
        @Order(1)
        @DisplayName("Удаление продукта, не используемого в блюдах")
        void testDeleteProductNotUsedInDishes() {
            clickTab("products");
            String uniqueProduct = "УдаляемыйПродукт" + System.currentTimeMillis();
            createProductViaUi(uniqueProduct, 100, 10, 5, 10, "Овощи", "Готовый к употреблению");

            WebElement deleteButton = driver.findElement(By.xpath("//h3[text()='" + uniqueProduct + "']/ancestor::div[@class='card']//button[text()='🗑️']"));
            deleteButton.click();

            acceptAlert();
            assertFalse(isProductDisplayed(uniqueProduct));
            // Не добавляем в список, так как продукт уже удален
        }
        @Test
        @Order(2)
        @DisplayName("Невозможно удалить продукт, используемый в блюде")
        void testCannotDeleteProductUsedInDish() {
            // Создаем уникальный продукт
            String uniqueProduct = "ПродуктДляБлюда" + System.currentTimeMillis();
            createProductViaUi(uniqueProduct, 100, 10, 5, 10, "Овощи", "Готовый к употреблению");

            // Обновляем страницу и проверяем, что продукт создался
            driver.navigate().refresh();
            sleep(500);
            clickTab("products");
            sleep(500);

            assertTrue(isProductDisplayed(uniqueProduct), "Продукт должен создаться: " + uniqueProduct);

            // Создаем блюдо с этим продуктом
            clickTab("dishes");
            clickAddDishButton();

            String dishName = "ТестовоеБлюдо" + System.currentTimeMillis();
            fillDishField("dishName", dishName);
            fillDishField("dishPortion", "200");

            // ИСПРАВЛЕНО: Добавляем правильный продукт, а не shared
            // Сначала находим продукт в выпадающем списке по имени
            addDishComponentByName(uniqueProduct, "100");

            clickSaveDish();

            assertTrue(isDishDisplayed(dishName), "Блюдо должно создаться: " + dishName);
            createdDishes.add(dishName);

            // Пытаемся удалить продукт
            clickTab("products");
            sleep(500);

            // Находим кнопку удаления для нашего продукта
            WebElement deleteButton = driver.findElement(By.xpath("//h3[text()='" + uniqueProduct + "']/ancestor::div[@class='card']//button[text()='🗑️']"));
            deleteButton.click();
            sleep(500);

            // Обрабатываем алерт
            try {
                org.openqa.selenium.Alert alert = driver.switchTo().alert();
                String alertText = alert.getText();
                System.out.println("Alert text: " + alertText);

                // Принимаем алерт
                alert.accept();
                sleep(500);

                // Обновляем страницу и проверяем, удалился ли продукт
                driver.navigate().refresh();
                sleep(500);
                clickTab("products");
                sleep(500);

                boolean productStillExists = isProductDisplayed(uniqueProduct);

                // ПРОВЕРКА: Продукт НЕ должен удаляться! Если удалился - тест должен ПАДАТЬ
                assertTrue(productStillExists,
                        "БАГ: Продукт, используемый в блюде, НЕ ДОЛЖЕН удаляться, но он был удален!");

            } catch (Exception e) {
                // Если алерта нет - тоже проблема
                fail("Должен появиться алерт при попытке удалить продукт, используемый в блюде");
            }
        }

        @Test
        @Order(3)
        @DisplayName("Удаление блюда")
        void testDeleteDish() {
            clickTab("dishes");

            // Создаем блюдо для удаления
            String dishName = "УдаляемоеБлюдо" + System.currentTimeMillis();
            clickAddDishButton();
            fillDishField("dishName", dishName);
            fillDishField("dishPortion", "200");
            addDishComponent(sharedPotatoId, "100");
            clickSaveDish();

            assertTrue(isDishDisplayed(dishName), "Блюдо должно создаться");

            // Удаляем блюдо
            WebElement deleteButton = driver.findElement(By.xpath("//h3[text()='" + dishName + "']/ancestor::div[@class='card']//button[text()='🗑️']"));
            deleteButton.click();
            acceptAlert();

            assertFalse(isDishDisplayed(dishName), "Блюдо должно быть удалено");
        }
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private static void clickTab(String tabName) {
        WebElement tab = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[@data-tab='" + tabName + "']")));
        tab.click();
        sleep(500);
        // Ждем, пока контент вкладки станет видимым
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(tabName + "-tab")));
    }

    private static void clickAddProductButton() {
        WebElement addButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(), 'Добавить продукт')]")));
        addButton.click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("productModal")));
        sleep(500);
    }

    private void clickAddDishButton() {
        WebElement addButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@id='dishes-tab']//button[contains(text(), 'Добавить блюдо')]")));
        addButton.click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("dishForm")));
        sleep(500);
    }

    private void refreshDishesList() {
        try {
            // Переключаемся на другую вкладку и обратно, чтобы обновить список
            clickTab("products");
            Thread.sleep(500);
            clickTab("dishes");
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("Error refreshing dishes list: " + e.getMessage());
        }
    }
    private void fillProductField(String fieldId, String value) {
        WebElement field = wait.until(ExpectedConditions.elementToBeClickable(By.id(fieldId)));
        field.clear();
        field.sendKeys(value);
    }

    private void fillDishField(String fieldId, String value) {
        WebElement field = wait.until(ExpectedConditions.elementToBeClickable(
                By.id(fieldId)));
        field.clear();
        field.sendKeys(value);
        sleep(200);
    }

    private void selectProductCategory(String category) {
        WebElement select = wait.until(ExpectedConditions.elementToBeClickable(By.id("prodCategory")));
        select.sendKeys(category);
    }

    private void selectProductCooking(String cooking) {
        WebElement select = wait.until(ExpectedConditions.elementToBeClickable(By.id("prodCooking")));
        select.sendKeys(cooking);
    }

    private void selectDishCategory(String category) {
        WebElement select = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("dishCategorySelect")));
        select.sendKeys(category);
        sleep(300);
    }

    private void checkProductFlag(String flag) {
        WebElement checkbox = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//label[contains(text(), '" + flag + "')]/input")));
        if (!checkbox.isSelected()) {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].click();", checkbox);
        }
    }

    private void clickSaveProduct() {
        try {
            Thread.sleep(500);
            WebElement saveButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//form[@id='productForm']//button[contains(text(), 'Сохранить')]")));
            saveButton.click();
            Thread.sleep(1000);
            try {
                driver.switchTo().alert().accept();
            } catch (Exception e) {
            }
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("productModal")));
        } catch (Exception e) {
        }
        sleep(500);
    }

    private void clickSaveDish() {
        try {
            WebElement saveButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//form[@id='dishForm']//button[contains(text(), 'Сохранить')]")));
            saveButton.click();

            // Для отладки
            Thread.sleep(1000);
            debugModalContent();
            try {
                Thread.sleep(1500);
                org.openqa.selenium.Alert alert = driver.switchTo().alert();
                String alertText = alert.getText();
                System.out.println("Alert in save dish: " + alertText);
                alert.accept();
                Thread.sleep(500);
            } catch (Exception e) {
                System.out.println("No alert present");
            }

            // Проверяем, закрылось ли модальное окно
            try {
                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("dishModal")));
            } catch (Exception e) {
                // Если модальное окно не закрылось, закрываем его принудительно через JS
                try {
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("document.getElementById('dishModal').style.display='none'");
                    System.out.println("Closed modal via JS");
                } catch (Exception ex) {
                    System.out.println("Could not close modal");
                }
            }
        } catch (Exception e) {
            System.out.println("Error in clickSaveDish: " + e.getMessage());
        }
        sleep(500);
    }

    private void clickCalculateNutrition() {
        WebElement calcButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//form[@id='dishForm']//button[contains(text(), 'Рассчитать КБЖУ')]")));
        calcButton.click();
        sleep(500);
    }

    private void addDishComponent(String productId, String quantity) {
        // Ищем кнопку "+ Добавить продукт" ТОЛЬКО внутри формы блюда
        WebElement addButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@id='componentsList']//button[contains(text(), 'Добавить продукт')]")));
        addButton.click();
        sleep(500);

        List<WebElement> selects = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("#componentsContainer select")));
        WebElement lastSelect = selects.get(selects.size() - 1);
        lastSelect.sendKeys(productId);

        List<WebElement> inputs = driver.findElements(By.cssSelector("#componentsContainer input[type='number']"));
        WebElement lastInput = inputs.get(inputs.size() - 1);
        lastInput.sendKeys(quantity);
    }

    private static void deleteProductStatic(String productName) {
        try {
            clickTab("products");
            WebElement deleteButton = driver.findElement(By.xpath("//h3[text()='" + productName + "']/ancestor::div[@class='card']//button[text()='🗑️']"));
            deleteButton.click();
            acceptAlertStatic();
        } catch (Exception e) {
        }
    }
    private boolean isValidationErrorDetected() {
        try {
            // Проверяем, открыто ли модальное окно (ошибка не закрывает его)
            WebElement modal = driver.findElement(By.id("dishModal"));
            if (modal.isDisplayed()) {
                // Модальное окно все еще открыто - значит была ошибка
                System.out.println("Modal still open - validation error occurred");
                return true;
            }
        } catch (Exception e) {
            // Модальное окно закрыто - возможно, успех
        }
        return false;
    }
    private void clickSave() {
        WebElement saveButton = driver.findElement(By.xpath("//form[@id='productForm']//button[contains(text(), 'Сохранить')]"));
        saveButton.click();
    }

    private void deleteProduct(String productName) {
        try {
            clickTab("products");
            WebElement deleteButton = driver.findElement(By.xpath("//h3[text()='" + productName + "']/ancestor::div[@class='card']//button[text()='🗑️']"));
            deleteButton.click();
            acceptAlert();
        } catch (Exception e) {
        }
    }

    private void deleteDish(String dishName) {
        try {
            // Сначала закрываем любые модальные окна
            try {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("document.getElementById('dishModal').style.display='none'");
            } catch (Exception e) {
            }

            clickTab("dishes");
            Thread.sleep(500);

            WebElement deleteButton = driver.findElement(By.xpath("//h3[text()='" + dishName + "']/ancestor::div[@class='card']//button[text()='🗑️']"));
            deleteButton.click();
            Thread.sleep(500);

            // Обрабатываем алерт подтверждения удаления
            try {
                org.openqa.selenium.Alert alert = driver.switchTo().alert();
                alert.accept();
                Thread.sleep(500);
            } catch (Exception e) {
                System.out.println("No confirmation alert for dish deletion");
            }
        } catch (Exception e) {
            System.out.println("Failed to delete dish: " + dishName);
        }
    }

    private boolean isProductDisplayed(String productName) {
        try {
            WebElement element = driver.findElement(By.xpath("//h3[text()='" + productName + "']"));
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isDishDisplayed(String dishName) {
        try {
            WebElement element = driver.findElement(By.xpath("//h3[text()='" + dishName + "']"));
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAlertPresent(String expectedMessage) {
        try {
            wait.until(ExpectedConditions.alertIsPresent());
            String alertText = driver.switchTo().alert().getText();
            driver.switchTo().alert().accept();
            return alertText.contains(expectedMessage);
        } catch (Exception e) {
            return false;
        }
    }
    private boolean isAlertPresentForNegativeScenario(String expectedMessage) {
        try {
            // Ждем появления алерта дольше
            for (int i = 0; i < 10; i++) {
                Thread.sleep(500);
                try {
                    org.openqa.selenium.Alert alert = driver.switchTo().alert();
                    String alertText = alert.getText();
                    System.out.println("Alert detected: " + alertText);
                    alert.accept();

                    // Закрываем модальное окно если оно открыто
                    try {
                        JavascriptExecutor js = (JavascriptExecutor) driver;
                        js.executeScript("document.getElementById('dishModal').style.display='none'");
                    } catch (Exception e) {
                    }

                    return alertText.contains(expectedMessage);
                } catch (Exception e) {
                    // Алерта пока нет
                }
            }
            return false;
        } catch (Exception e) {
            System.out.println("Error checking alert: " + e.getMessage());
            return false;
        }
    }
    private boolean createProductAndCheckSuccess(String name, double calories, double proteins, double fats, double carbs,
                                                 String category, String cooking) {
        clickAddProductButton();
        fillProductField("prodName", name);
        fillProductField("prodCalories", String.valueOf(calories));
        fillProductField("prodProteins", String.valueOf(proteins));
        fillProductField("prodFats", String.valueOf(fats));
        fillProductField("prodCarbs", String.valueOf(carbs));
        selectProductCategory(category);
        selectProductCooking(cooking);

        // Сохраняем
        WebElement saveButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//form[@id='productForm']//button[contains(text(), 'Сохранить')]")));
        saveButton.click();
        sleep(1000);

        // Закрываем модальное окно
        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("productModal")));
        } catch (Exception e) {
            try {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("document.getElementById('productModal').style.display='none'");
            } catch (Exception ex) {
            }
        }

        // Обновляем список продуктов
        refreshProductsList();

        // Проверяем, создался ли продукт
        return isProductDisplayed(name);
    }
    private void refreshProductsList() {
        try {
            // Переключаемся на другую вкладку и обратно, чтобы обновить список
            clickTab("dishes");
            Thread.sleep(500);
            clickTab("products");
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("Error refreshing products list: " + e.getMessage());
        }
    }

    private boolean isErrorMessageDisplayed(String expectedMessage) {
        try {
            // Проверяем наличие сообщения об ошибке в модальном окне
            Thread.sleep(1000);

            // Ищем элемент с ошибкой в модальном окне
            List<WebElement> errorElements = driver.findElements(By.cssSelector(".error-message, .alert, .alert-danger, .toast-error, div[role='alert']"));

            for (WebElement element : errorElements) {
                String errorText = element.getText();
                System.out.println("Found error text: " + errorText);
                if (errorText.contains(expectedMessage)) {
                    return true;
                }
            }

            // Проверяем тело модального окна на наличие ошибки
            WebElement modalBody = driver.findElement(By.cssSelector("#dishModal .modal-content, #dishModal"));
            String modalText = modalBody.getText();
            System.out.println("Modal text: " + modalText);

            return modalText.contains(expectedMessage) || modalText.contains("400") || modalText.contains("Bad Request");
        } catch (Exception e) {
            System.out.println("Error checking error message: " + e.getMessage());
            return false;
        }
    }
    private void addDishComponentByName(String productName, String quantity) {
        // Нажимаем кнопку "Добавить продукт"
        WebElement addButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@id='componentsList']//button[contains(text(), 'Добавить продукт')]")));
        addButton.click();
        sleep(500);

        // Находим последний select и выбираем продукт по имени
        List<WebElement> selects = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("#componentsContainer select")));
        WebElement lastSelect = selects.get(selects.size() - 1);

        // Выбираем продукт по видимому тексту
        for (WebElement option : lastSelect.findElements(By.tagName("option"))) {
            if (option.getText().contains(productName)) {
                option.click();
                break;
            }
        }

        // Заполняем количество
        List<WebElement> inputs = driver.findElements(By.cssSelector("#componentsContainer input[type='number']"));
        WebElement lastInput = inputs.get(inputs.size() - 1);
        lastInput.sendKeys(quantity);
    }
    // Исправленный метод для негативных сценариев - проверяет и алерт, и текст ошибки
    private boolean isNegativeScenarioDetected(String expectedMessage) {
        // Сначала проверяем алерт
        try {
            for (int i = 0; i < 5; i++) {
                Thread.sleep(500);
                try {
                    org.openqa.selenium.Alert alert = driver.switchTo().alert();
                    String alertText = alert.getText();
                    System.out.println("Alert detected: " + alertText);
                    alert.accept();
                    return alertText.contains(expectedMessage);
                } catch (Exception e) {
                    // Алерта нет
                }
            }
        } catch (Exception e) {
        }

        // Если алерта нет, проверяем наличие сообщения об ошибке на странице
        return isErrorMessageDisplayed(expectedMessage);
    }
    private boolean isAlertPresentForDelete(String expectedMessage) {
        try {
            Thread.sleep(500);
            org.openqa.selenium.Alert alert = driver.switchTo().alert();
            String alertText = alert.getText();
            System.out.println("Delete alert text: " + alertText);
            alert.accept();
            return alertText.contains(expectedMessage);
        } catch (Exception e) {
            return false;
        }
    }
    private boolean hasErrorMessageInModal(String... expectedMessages) {
        try {
            WebElement modalContent = driver.findElement(By.cssSelector("#dishModal .modal-content"));
            String modalText = modalContent.getText();
            System.out.println("Modal content text: " + modalText);

            for (String expectedMessage : expectedMessages) {
                if (modalText.toLowerCase().contains(expectedMessage.toLowerCase())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    private boolean isDishNotCreated(String dishName) {
        try {
            // Обновляем список блюд
            refreshDishesList();

            WebElement searchInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("dishSearch")));
            searchInput.clear();
            searchInput.sendKeys(dishName);
            Thread.sleep(1000);

            List<WebElement> cards = driver.findElements(By.cssSelector("#dishesList .card"));

            // Проверяем, есть ли блюдо с таким именем
            for (WebElement card : cards) {
                if (card.getText().contains(dishName)) {
                    System.out.println("Dish found: " + dishName);
                    return false;
                }
            }
            System.out.println("Dish not found: " + dishName);
            return true;
        } catch (Exception e) {
            System.out.println("Error checking dish: " + e.getMessage());
            return true;
        }
    }
    private boolean hasProductFlag(String productName, String flag) {
        try {
            // Находим карточку продукта
            WebElement card = driver.findElement(By.xpath("//h3[text()='" + productName + "']/ancestor::div[@class='card']"));
            String cardText = card.getText();
            System.out.println("Card text for " + productName + ": " + cardText);
            return cardText.contains(flag);
        } catch (Exception e) {
            System.out.println("Error checking flag: " + e.getMessage());
            return false;
        }
    }
    private void acceptAlert() {
        try {
            wait.until(ExpectedConditions.alertIsPresent());
            driver.switchTo().alert().accept();
            sleep(500);
        } catch (Exception e) {
        }
    }

    private static void acceptAlertStatic() {
        try {
            wait.until(ExpectedConditions.alertIsPresent());
            driver.switchTo().alert().accept();
            sleepStatic(500);
        } catch (Exception e) {
        }
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    // Добавьте этот метод для работы с флагами (аналогично fillProductField)
    private void setProductFlag(String flagName, boolean check) {
        try {
            // Находим чекбокс по label
            WebElement checkbox = driver.findElement(By.xpath("//label[contains(text(), '" + flagName + "')]/input"));
            boolean isChecked = checkbox.isSelected();

            if (check && !isChecked) {
                // Используем JavaScript для клика (обходит любые перекрытия)
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("arguments[0].click();", checkbox);
                System.out.println("Флаг '" + flagName + "' установлен");
            } else if (!check && isChecked) {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("arguments[0].click();", checkbox);
                System.out.println("Флаг '" + flagName + "' снят");
            }
        } catch (Exception e) {
            System.out.println("Error setting flag '" + flagName + "': " + e.getMessage());
        }
    }

    private static void sleepStatic(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void createProductViaUi(String name, double calories, double proteins, double fats, double carbs,
                                    String category, String cooking) {
        clickAddProductButton();
        fillProductField("prodName", name);
        fillProductField("prodCalories", String.valueOf(calories));
        fillProductField("prodProteins", String.valueOf(proteins));
        fillProductField("prodFats", String.valueOf(fats));
        fillProductField("prodCarbs", String.valueOf(carbs));
        selectProductCategory(category);
        selectProductCooking(cooking);
        clickSaveProduct();
        sleep(500);
    }

    private static void createProductViaUiStatic(String name, double calories, double proteins, double fats, double carbs,
                                                 String category, String cooking) {
        clickAddProductButton();
        WebElement nameField = wait.until(ExpectedConditions.elementToBeClickable(By.id("prodName")));
        nameField.clear();
        nameField.sendKeys(name);

        WebElement caloriesField = wait.until(ExpectedConditions.elementToBeClickable(By.id("prodCalories")));
        caloriesField.clear();
        caloriesField.sendKeys(String.valueOf(calories));

        WebElement proteinsField = wait.until(ExpectedConditions.elementToBeClickable(By.id("prodProteins")));
        proteinsField.clear();
        proteinsField.sendKeys(String.valueOf(proteins));

        WebElement fatsField = wait.until(ExpectedConditions.elementToBeClickable(By.id("prodFats")));
        fatsField.clear();
        fatsField.sendKeys(String.valueOf(fats));

        WebElement carbsField = wait.until(ExpectedConditions.elementToBeClickable(By.id("prodCarbs")));
        carbsField.clear();
        carbsField.sendKeys(String.valueOf(carbs));

        WebElement categorySelect = wait.until(ExpectedConditions.elementToBeClickable(By.id("prodCategory")));
        categorySelect.sendKeys(category);

        WebElement cookingSelect = wait.until(ExpectedConditions.elementToBeClickable(By.id("prodCooking")));
        cookingSelect.sendKeys(cooking);

        WebElement saveButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//form[@id='productForm']//button[contains(text(), 'Сохранить')]")));
        saveButton.click();
        sleepStatic(500);

        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("productModal")));
        } catch (Exception e) {
        }
        sleepStatic(500);
    }

    private static String getProductId(String productName) {
        try {
            WebElement card = driver.findElement(By.xpath("//h3[text()='" + productName + "']/ancestor::div[@class='card']"));
            String id = card.getAttribute("id");
            System.out.println("Found product ID for " + productName + ": " + id);
            return id;
        } catch (Exception e) {
            System.out.println("Could not find product ID for " + productName + ": " + e.getMessage());
            return "";
        }
    }

    private void createDishViaUi(String name, List<ComponentDto> components, String portionSize, String category) {
        clickAddDishButton();
        fillDishField("dishName", name);
        fillDishField("dishPortion", portionSize);
        selectDishCategory(category);

        for (ComponentDto comp : components) {
            addDishComponent(comp.productId, comp.quantity);
        }

        clickSaveDish();
        sleep(500);
    }
    // Вспомогательный метод для отладки - выводит текст модального окна
    private void debugModalContent() {
        try {
            WebElement modal = driver.findElement(By.id("dishModal"));
            System.out.println("Modal display: " + modal.getCssValue("display"));
            System.out.println("Modal text: " + modal.getText());
        } catch (Exception e) {
            System.out.println("Modal not found");
        }
    }

    private static class ComponentDto {
        String productId;
        String quantity;
        ComponentDto(String productId, String quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
    }
}
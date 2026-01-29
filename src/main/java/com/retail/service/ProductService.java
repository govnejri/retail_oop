package com.retail.service;

import com.retail.dao.*;
import com.retail.db.DatabaseManager;
import com.retail.exception.DatabaseException;
import com.retail.exception.ValidationException;
import com.retail.model.Category;
import com.retail.model.Product;
import com.retail.model.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.retail.util.Validator;


public class ProductService {
    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    
    private final DatabaseManager dbManager;
    private final ProductDao productDao;
    private final CategoryDao categoryDao;
    private final UnitDao unitDao;
    private final InventoryDao inventoryDao;

    public ProductService() {
        this.dbManager = DatabaseManager.getInstance();
        this.productDao = new ProductDao();
        this.categoryDao = new CategoryDao();
        this.unitDao = new UnitDao();
        this.inventoryDao = new InventoryDao();
    }

    

    
    public Product createProduct(Product product) {
        try {
            return dbManager.executeInTransaction(conn -> {
                
                validateProduct(product);
                
                
                if (productDao.skuExists(product.getSku())) {
                    throw new ValidationException("Товар с таким артикулом уже существует");
                }
                
                
                Product savedProduct = productDao.save(conn, product);
                
                
                inventoryDao.save(conn, new com.retail.model.Inventory(savedProduct.getId(), 0));
                
                logger.info("Создан товар: {} ({})", savedProduct.getName(), savedProduct.getSku());
                
                return savedProduct;
            });
            
        } catch (SQLException e) {
            logger.error("Ошибка создания товара", e);
            throw new DatabaseException("Ошибка при создании товара", e);
        }
    }

    
    public void updateProduct(Product product) {
        try {
            validateProduct(product);
            productDao.update(product);
            logger.info("Обновлен товар: {} ({})", product.getName(), product.getSku());
        } catch (SQLException e) {
            logger.error("Ошибка обновления товара", e);
            throw new DatabaseException("Ошибка при обновлении товара", e);
        }
    }

    
    public void updatePrice(Integer productId, BigDecimal newPrice) {
        try {
            if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Цена должна быть положительной");
            }
            
            productDao.updateSellingPrice(productId, newPrice);
            logger.info("Обновлена цена товара ID {}: {}", productId, newPrice);
        } catch (SQLException e) {
            logger.error("Ошибка обновления цены", e);
            throw new DatabaseException("Ошибка при обновлении цены", e);
        }
    }

    
    public void deactivateProduct(Integer productId) {
        try {
            productDao.delete(productId);
            logger.info("Деактивирован товар ID {}", productId);
        } catch (SQLException e) {
            logger.error("Ошибка деактивации товара", e);
            throw new DatabaseException("Ошибка при деактивации товара", e);
        }
    }

    
    public Optional<Product> findById(Integer id) {
        try {
            return productDao.findByIdWithStock(id);
        } catch (SQLException e) {
            logger.error("Ошибка поиска товара", e);
            throw new DatabaseException("Ошибка при поиске товара", e);
        }
    }

    
    public Optional<Product> findBySku(String sku) {
        try {
            return productDao.findBySku(sku);
        } catch (SQLException e) {
            logger.error("Ошибка поиска товара", e);
            throw new DatabaseException("Ошибка при поиске товара", e);
        }
    }

    
    public List<Product> searchByName(String namePattern) {
        try {
            return productDao.searchByName(namePattern);
        } catch (SQLException e) {
            logger.error("Ошибка поиска товаров", e);
            throw new DatabaseException("Ошибка при поиске товаров", e);
        }
    }

    
    public List<Product> findAllActive() {
        try {
            return productDao.findAllWithStock();
        } catch (SQLException e) {
            logger.error("Ошибка получения списка товаров", e);
            throw new DatabaseException("Ошибка при получении списка товаров", e);
        }
    }

    
    public List<Product> findByCategory(Integer categoryId) {
        try {
            return productDao.findByCategory(categoryId);
        } catch (SQLException e) {
            logger.error("Ошибка получения товаров по категории", e);
            throw new DatabaseException("Ошибка при получении товаров", e);
        }
    }

    private void validateProduct(Product product) {
        // Использование Lambda-валидаторов из класса Validator
        Validator.requireNotEmpty(product.getSku(), "Артикул");
        Validator.requireNotEmpty(product.getName(), "Название товара");
        Validator.validatePrice(product.getSellingPrice(), "Цена продажи");

        if (product.getPurchasePrice() != null) {
            Validator.validate(
                    product.getPurchasePrice(),
                    Validator.NON_NEGATIVE_AMOUNT,
                    "Закупочная цена не может быть отрицательной"
            );
        }
    }


    public List<Product> filterProducts(BigDecimal minPrice, BigDecimal maxPrice, Integer categoryId) {
        List<Product> products = findAllActive();

        return products.stream()
                .filter(p -> minPrice == null || p.getSellingPrice().compareTo(minPrice) >= 0)
                .filter(p -> maxPrice == null || p.getSellingPrice().compareTo(maxPrice) <= 0)
                .filter(p -> categoryId == null || categoryId.equals(p.getCategoryId()))
                .toList();
    }


    public List<Product> findLowStockProducts() {
        return findAllActive().stream()
                .filter(p -> p.getStockQuantity() != null && p.getMinStockLevel() != null)
                .filter(p -> p.getStockQuantity() <= p.getMinStockLevel())
                .toList();
    }




    public Category createCategory(String name, String description) {
        try {
            if (name == null || name.trim().isEmpty()) {
                throw new ValidationException("Название категории не может быть пустым");
            }
            
            if (categoryDao.nameExists(name)) {
                throw new ValidationException("Категория с таким названием уже существует");
            }
            
            Category category = new Category(name.trim(), description);
            category = categoryDao.save(category);
            
            logger.info("Создана категория: {}", category.getName());
            return category;
            
        } catch (SQLException e) {
            logger.error("Ошибка создания категории", e);
            throw new DatabaseException("Ошибка при создании категории", e);
        }
    }

    
    public void updateCategory(Category category) {
        try {
            categoryDao.update(category);
            logger.info("Обновлена категория: {}", category.getName());
        } catch (SQLException e) {
            logger.error("Ошибка обновления категории", e);
            throw new DatabaseException("Ошибка при обновлении категории", e);
        }
    }

    
    public void deleteCategory(Integer categoryId) {
        try {
            categoryDao.delete(categoryId);
            logger.info("Удалена категория ID {}", categoryId);
        } catch (SQLException e) {
            logger.error("Ошибка удаления категории", e);
            throw new DatabaseException("Ошибка при удалении категории", e);
        }
    }

    
    public List<Category> findAllCategories() {
        try {
            return categoryDao.findAll();
        } catch (SQLException e) {
            logger.error("Ошибка получения списка категорий", e);
            throw new DatabaseException("Ошибка при получении категорий", e);
        }
    }

    
    public Optional<Category> findCategoryById(Integer id) {
        try {
            return categoryDao.findById(id);
        } catch (SQLException e) {
            logger.error("Ошибка поиска категории", e);
            throw new DatabaseException("Ошибка при поиске категории", e);
        }
    }

    

    
    public Unit createUnit(String name, String shortName) {
        try {
            if (name == null || name.trim().isEmpty()) {
                throw new ValidationException("Название единицы измерения не может быть пустым");
            }
            
            if (shortName == null || shortName.trim().isEmpty()) {
                throw new ValidationException("Сокращение не может быть пустым");
            }
            
            Unit unit = new Unit(name.trim(), shortName.trim());
            unit = unitDao.save(unit);
            
            logger.info("Создана единица измерения: {} ({})", unit.getName(), unit.getShortName());
            return unit;
            
        } catch (SQLException e) {
            logger.error("Ошибка создания единицы измерения", e);
            throw new DatabaseException("Ошибка при создании единицы измерения", e);
        }
    }

    
    public List<Unit> findAllUnits() {
        try {
            return unitDao.findAll();
        } catch (SQLException e) {
            logger.error("Ошибка получения списка единиц измерения", e);
            throw new DatabaseException("Ошибка при получении единиц измерения", e);
        }
    }

    
    public Optional<Unit> findUnitById(Integer id) {
        try {
            return unitDao.findById(id);
        } catch (SQLException e) {
            logger.error("Ошибка поиска единицы измерения", e);
            throw new DatabaseException("Ошибка при поиске единицы измерения", e);
        }
    }
}

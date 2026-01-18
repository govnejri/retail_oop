package com.retail.service;

import com.retail.dao.*;
import com.retail.db.DatabaseManager;
import com.retail.exception.DatabaseException;
import com.retail.exception.InsufficientStockException;
import com.retail.exception.ValidationException;
import com.retail.model.*;
import com.retail.model.enums.StockOperationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public class SaleService {
    private static final Logger logger = LoggerFactory.getLogger(SaleService.class);
    
    private final DatabaseManager dbManager;
    private final SaleDao saleDao;
    private final SaleItemDao saleItemDao;
    private final ProductDao productDao;
    private final InventoryDao inventoryDao;
    private final StockLogDao stockLogDao;

    public SaleService() {
        this.dbManager = DatabaseManager.getInstance();
        this.saleDao = new SaleDao();
        this.saleItemDao = new SaleItemDao();
        this.productDao = new ProductDao();
        this.inventoryDao = new InventoryDao();
        this.stockLogDao = new StockLogDao();
    }

    
    public Sale createSale(Sale sale, Integer employeeId) {
        try {
            return dbManager.executeInTransaction(conn -> {
                
                sale.setEmployeeId(employeeId);
                sale.setSaleDate(LocalDateTime.now());
                
                BigDecimal totalAmount = BigDecimal.ZERO;
                
                
                for (SaleItem item : sale.getItems()) {
                    
                    Optional<Product> productOpt = productDao.findById(item.getProductId());
                    if (productOpt.isEmpty()) {
                        throw new ValidationException("Товар не найден: ID " + item.getProductId());
                    }
                    
                    Product product = productOpt.get();
                    
                    
                    Inventory inventory = inventoryDao.findByProductIdForUpdate(conn, item.getProductId());
                    
                    int currentStock = inventory != null ? inventory.getQuantity() : 0;
                    
                    
                    if (currentStock < item.getQuantity()) {
                        throw new InsufficientStockException(
                                item.getProductId(), 
                                item.getQuantity(), 
                                currentStock);
                    }
                    
                    
                    item.setPriceAtSale(product.getSellingPrice());
                    item.setLineTotal(product.getSellingPrice().multiply(
                            BigDecimal.valueOf(item.getQuantity())));
                    
                    totalAmount = totalAmount.add(item.getLineTotal());
                }
                
                
                sale.setTotalAmount(totalAmount);
                sale.setFinalAmount(totalAmount.subtract(
                        sale.getDiscount() != null ? sale.getDiscount() : BigDecimal.ZERO));
                
                Sale savedSale = saleDao.save(conn, sale);
                
                
                for (SaleItem item : sale.getItems()) {
                    item.setSaleId(savedSale.getId());
                    saleItemDao.save(conn, item);
                    
                    
                    Inventory inventory = inventoryDao.findByProductIdForUpdate(conn, item.getProductId());
                    int stockBefore = inventory.getQuantity();
                    
                    
                    inventoryDao.decreaseQuantity(conn, item.getProductId(), item.getQuantity());
                    
                    
                    StockLog stockLog = new StockLog(
                            item.getProductId(),
                            StockOperationType.SALE,
                            -item.getQuantity(),
                            stockBefore,
                            stockBefore - item.getQuantity(),
                            employeeId);
                    stockLog.setReferenceId(savedSale.getId());
                    stockLog.setReferenceType("SALE");
                    stockLogDao.save(conn, stockLog);
                }
                
                logger.info("Создана продажа: {} на сумму {}", 
                        savedSale.getSaleNumber(), savedSale.getFinalAmount());
                
                return savedSale;
            });
            
        } catch (SQLException e) {
            
            if (e.getMessage() != null && e.getMessage().contains("chk_quantity_non_negative")) {
                throw new InsufficientStockException(0, 0, 0);
            }
            logger.error("Ошибка создания продажи", e);
            throw new DatabaseException("Ошибка при создании продажи", e);
        }
    }

    
    public void processReturn(Integer saleId, Integer itemId, int returnQuantity, Integer employeeId) {
        try {
            dbManager.executeInTransaction(conn -> {
                
                Optional<Sale> saleOpt = saleDao.findById(saleId);
                if (saleOpt.isEmpty()) {
                    throw new ValidationException("Продажа не найдена");
                }
                
                
                List<SaleItem> items = saleItemDao.findBySaleId(saleId);
                SaleItem item = items.stream()
                        .filter(i -> i.getId().equals(itemId))
                        .findFirst()
                        .orElseThrow(() -> new ValidationException("Позиция не найдена"));
                
                
                int returnable = item.getReturnableQty();
                if (returnQuantity > returnable) {
                    throw new ValidationException(
                            String.format("Невозможно вернуть %d ед. Доступно для возврата: %d", 
                                    returnQuantity, returnable));
                }
                
                
                Inventory inventory = inventoryDao.findByProductIdForUpdate(conn, item.getProductId());
                int stockBefore = inventory != null ? inventory.getQuantity() : 0;
                
                
                inventoryDao.increaseQuantity(conn, item.getProductId(), returnQuantity);
                
                
                saleItemDao.updateReturnedQty(conn, itemId, item.getReturnedQty() + returnQuantity);
                
                
                StockLog stockLog = new StockLog(
                        item.getProductId(),
                        StockOperationType.RETURN,
                        returnQuantity,
                        stockBefore,
                        stockBefore + returnQuantity,
                        employeeId);
                stockLog.setReferenceId(saleId);
                stockLog.setReferenceType("SALE_RETURN");
                stockLog.setNotes("Возврат по чеку " + saleOpt.get().getSaleNumber());
                stockLogDao.save(conn, stockLog);
                
                
                List<SaleItem> updatedItems = saleItemDao.findBySaleId(saleId);
                boolean fullyReturned = updatedItems.stream()
                        .allMatch(i -> i.getQuantity().equals(i.getReturnedQty()));
                
                if (fullyReturned) {
                    saleDao.markAsReturned(conn, saleId);
                }
                
                logger.info("Выполнен возврат: товар ID {}, кол-во {}, чек ID {}", 
                        item.getProductId(), returnQuantity, saleId);
                
                return null;
            });
            
        } catch (SQLException e) {
            logger.error("Ошибка возврата товара", e);
            throw new DatabaseException("Ошибка при возврате товара", e);
        }
    }

    
    public Optional<Sale> findBySaleNumber(String saleNumber) {
        try {
            Optional<Sale> saleOpt = saleDao.findBySaleNumber(saleNumber);
            if (saleOpt.isPresent()) {
                Sale sale = saleOpt.get();
                sale.setItems(saleItemDao.findBySaleId(sale.getId()));
            }
            return saleOpt;
        } catch (SQLException e) {
            logger.error("Ошибка поиска продажи", e);
            throw new DatabaseException("Ошибка при поиске продажи", e);
        }
    }

    
    public Optional<Sale> findById(Integer id) {
        try {
            Optional<Sale> saleOpt = saleDao.findById(id);
            if (saleOpt.isPresent()) {
                Sale sale = saleOpt.get();
                sale.setItems(saleItemDao.findBySaleId(sale.getId()));
            }
            return saleOpt;
        } catch (SQLException e) {
            logger.error("Ошибка поиска продажи", e);
            throw new DatabaseException("Ошибка при поиске продажи", e);
        }
    }

    
    public List<Sale> findTodaySales() {
        try {
            return saleDao.findToday();
        } catch (SQLException e) {
            logger.error("Ошибка получения продаж за сегодня", e);
            throw new DatabaseException("Ошибка при получении продаж", e);
        }
    }

    
    public List<Sale> findSalesByPeriod(LocalDateTime start, LocalDateTime end) {
        try {
            return saleDao.findByPeriod(start, end);
        } catch (SQLException e) {
            logger.error("Ошибка получения продаж за период", e);
            throw new DatabaseException("Ошибка при получении продаж", e);
        }
    }

    
    public List<SaleItem> getReturnableItems(Integer saleId) {
        try {
            return saleItemDao.findReturnableItems(saleId);
        } catch (SQLException e) {
            logger.error("Ошибка получения позиций для возврата", e);
            throw new DatabaseException("Ошибка при получении позиций", e);
        }
    }
}

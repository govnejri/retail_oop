package com.retail.service;

import com.retail.dao.*;
import com.retail.db.DatabaseManager;
import com.retail.exception.DatabaseException;
import com.retail.exception.ValidationException;
import com.retail.model.*;
import com.retail.model.enums.StockOperationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public class InventoryService {
    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);
    
    private final DatabaseManager dbManager;
    private final ProductDao productDao;
    private final InventoryDao inventoryDao;
    private final ReceiptDao receiptDao;
    private final ReceiptItemDao receiptItemDao;
    private final StockLogDao stockLogDao;

    public InventoryService() {
        this.dbManager = DatabaseManager.getInstance();
        this.productDao = new ProductDao();
        this.inventoryDao = new InventoryDao();
        this.receiptDao = new ReceiptDao();
        this.receiptItemDao = new ReceiptItemDao();
        this.stockLogDao = new StockLogDao();
    }

    
    public Receipt createReceipt(Receipt receipt, Integer managerId) {
        try {
            return dbManager.executeInTransaction(conn -> {
                receipt.setManagerId(managerId);
                receipt.setReceiptDate(LocalDateTime.now());
                
                BigDecimal totalAmount = BigDecimal.ZERO;
                
                
                for (ReceiptItem item : receipt.getItems()) {
                    Optional<Product> productOpt = productDao.findById(item.getProductId());
                    if (productOpt.isEmpty()) {
                        throw new ValidationException("Товар не найден: ID " + item.getProductId());
                    }
                    
                    if (item.getQuantity() <= 0) {
                        throw new ValidationException("Количество должно быть положительным");
                    }
                    
                    item.setLineTotal(item.getPurchasePrice().multiply(
                            BigDecimal.valueOf(item.getQuantity())));
                    totalAmount = totalAmount.add(item.getLineTotal());
                }
                
                receipt.setTotalAmount(totalAmount);
                
                
                Receipt savedReceipt = receiptDao.save(conn, receipt);
                
                
                for (ReceiptItem item : receipt.getItems()) {
                    item.setReceiptId(savedReceipt.getId());
                    receiptItemDao.save(conn, item);
                    
                    
                    int stockBefore = inventoryDao.getQuantity(item.getProductId());
                    
                    
                    inventoryDao.increaseQuantity(conn, item.getProductId(), item.getQuantity());
                    
                    
                    StockLog stockLog = new StockLog(
                            item.getProductId(),
                            StockOperationType.RECEIPT,
                            item.getQuantity(),
                            stockBefore,
                            stockBefore + item.getQuantity(),
                            managerId);
                    stockLog.setReferenceId(savedReceipt.getId());
                    stockLog.setReferenceType("RECEIPT");
                    stockLogDao.save(conn, stockLog);
                }
                
                logger.info("Оформлена поставка: {} на сумму {}", 
                        savedReceipt.getReceiptNumber(), savedReceipt.getTotalAmount());
                
                return savedReceipt;
            });
            
        } catch (SQLException e) {
            logger.error("Ошибка оформления поставки", e);
            throw new DatabaseException("Ошибка при оформлении поставки", e);
        }
    }

    
    public void adjustStock(Integer productId, int newQuantity, String reason, Integer managerId) {
        try {
            dbManager.executeInTransaction(conn -> {
                Optional<Product> productOpt = productDao.findById(productId);
                if (productOpt.isEmpty()) {
                    throw new ValidationException("Товар не найден");
                }
                
                if (newQuantity < 0) {
                    throw new ValidationException("Остаток не может быть отрицательным");
                }
                
                int currentStock = inventoryDao.getQuantity(productId);
                int change = newQuantity - currentStock;
                
                if (change == 0) {
                    return null; 
                }
                
                
                inventoryDao.setQuantity(conn, productId, newQuantity);
                
                
                StockOperationType opType = change > 0 
                        ? StockOperationType.ADJUSTMENT 
                        : StockOperationType.WRITE_OFF;
                
                
                StockLog stockLog = new StockLog(
                        productId,
                        opType,
                        change,
                        currentStock,
                        newQuantity,
                        managerId);
                stockLog.setNotes(reason != null ? reason : "Корректировка по инвентаризации");
                stockLogDao.save(conn, stockLog);
                
                logger.info("Корректировка остатка: товар ID {}, {} -> {} (изменение: {})", 
                        productId, currentStock, newQuantity, change);
                
                return null;
            });
            
        } catch (SQLException e) {
            logger.error("Ошибка корректировки остатка", e);
            throw new DatabaseException("Ошибка при корректировке остатка", e);
        }
    }

    
    public int getStock(Integer productId) {
        try {
            return inventoryDao.getQuantity(productId);
        } catch (SQLException e) {
            logger.error("Ошибка получения остатка", e);
            throw new DatabaseException("Ошибка при получении остатка", e);
        }
    }

    
    public boolean checkStock(Integer productId, int requiredQuantity) {
        try {
            return inventoryDao.hasEnoughStock(productId, requiredQuantity);
        } catch (SQLException e) {
            logger.error("Ошибка проверки остатка", e);
            throw new DatabaseException("Ошибка при проверке остатка", e);
        }
    }

    
    public List<Product> getAllProductsWithStock() {
        try {
            return productDao.findAllWithStock();
        } catch (SQLException e) {
            logger.error("Ошибка получения товаров с остатками", e);
            throw new DatabaseException("Ошибка при получении товаров", e);
        }
    }

    
    public List<Product> getLowStockProducts() {
        try {
            return productDao.findLowStock();
        } catch (SQLException e) {
            logger.error("Ошибка получения товаров с низким остатком", e);
            throw new DatabaseException("Ошибка при получении товаров", e);
        }
    }

    
    public List<StockLog> getProductHistory(Integer productId) {
        try {
            return stockLogDao.findByProductId(productId);
        } catch (SQLException e) {
            logger.error("Ошибка получения истории движения", e);
            throw new DatabaseException("Ошибка при получении истории", e);
        }
    }

    
    public List<StockLog> getAdjustmentLogs() {
        try {
            return stockLogDao.findAdjustments();
        } catch (SQLException e) {
            logger.error("Ошибка получения логов корректировок", e);
            throw new DatabaseException("Ошибка при получении логов", e);
        }
    }

    
    public Optional<Receipt> findReceiptByNumber(String receiptNumber) {
        try {
            Optional<Receipt> receiptOpt = receiptDao.findByReceiptNumber(receiptNumber);
            if (receiptOpt.isPresent()) {
                Receipt receipt = receiptOpt.get();
                receipt.setItems(receiptItemDao.findByReceiptId(receipt.getId()));
            }
            return receiptOpt;
        } catch (SQLException e) {
            logger.error("Ошибка поиска поставки", e);
            throw new DatabaseException("Ошибка при поиске поставки", e);
        }
    }

    
    public List<Receipt> getReceiptsByPeriod(LocalDateTime start, LocalDateTime end) {
        try {
            return receiptDao.findByPeriod(start, end);
        } catch (SQLException e) {
            logger.error("Ошибка получения поставок за период", e);
            throw new DatabaseException("Ошибка при получении поставок", e);
        }
    }
}

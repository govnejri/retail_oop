package com.retail.service;

import com.retail.dao.SaleDao;
import com.retail.dao.SaleItemDao;
import com.retail.dao.SecurityLogDao;
import com.retail.dao.StockLogDao;
import com.retail.exception.DatabaseException;
import com.retail.model.Product;
import com.retail.model.SecurityLog;
import com.retail.model.StockLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


public class ReportService {
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    
    private final SaleDao saleDao;
    private final SaleItemDao saleItemDao;
    private final StockLogDao stockLogDao;
    private final SecurityLogDao securityLogDao;
    private final InventoryService inventoryService;

    public ReportService() {
        this.saleDao = new SaleDao();
        this.saleItemDao = new SaleItemDao();
        this.stockLogDao = new StockLogDao();
        this.securityLogDao = new SecurityLogDao();
        this.inventoryService = new InventoryService();
    }

    

    
    public BigDecimal getTodayRevenue() {
        try {
            LocalDate today = LocalDate.now();
            return saleDao.getTotalRevenue(
                    today.atStartOfDay(),
                    today.plusDays(1).atStartOfDay());
        } catch (SQLException e) {
            logger.error("Ошибка получения выручки за сегодня", e);
            throw new DatabaseException("Ошибка при получении отчета", e);
        }
    }

    
    public BigDecimal getMonthRevenue() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate firstDayOfMonth = today.withDayOfMonth(1);
            return saleDao.getTotalRevenue(
                    firstDayOfMonth.atStartOfDay(),
                    today.plusDays(1).atStartOfDay());
        } catch (SQLException e) {
            logger.error("Ошибка получения выручки за месяц", e);
            throw new DatabaseException("Ошибка при получении отчета", e);
        }
    }

    
    public BigDecimal getRevenueByPeriod(LocalDateTime start, LocalDateTime end) {
        try {
            return saleDao.getTotalRevenue(start, end);
        } catch (SQLException e) {
            logger.error("Ошибка получения выручки за период", e);
            throw new DatabaseException("Ошибка при получении отчета", e);
        }
    }

    
    public List<Object[]> getTopSellingProducts(int limit) {
        try {
            return saleItemDao.getTopSellingProducts(limit);
        } catch (SQLException e) {
            logger.error("Ошибка получения топ товаров", e);
            throw new DatabaseException("Ошибка при получении отчета", e);
        }
    }

    

    
    public List<Product> getStockReport() {
        return inventoryService.getAllProductsWithStock();
    }

    
    public List<Product> getLowStockReport() {
        return inventoryService.getLowStockProducts();
    }

    
    public List<StockLog> getProductMovementHistory(Integer productId) {
        return inventoryService.getProductHistory(productId);
    }

    

    
    public List<StockLog> getAdjustmentLog() {
        return inventoryService.getAdjustmentLogs();
    }

    
    public List<SecurityLog> getSecurityLog(int limit) {
        try {
            return securityLogDao.findRecent(limit);
        } catch (SQLException e) {
            logger.error("Ошибка получения журнала безопасности", e);
            throw new DatabaseException("Ошибка при получении журнала", e);
        }
    }

    
    public List<SecurityLog> getFailedLogins(int limit) {
        try {
            return securityLogDao.findFailedLogins(limit);
        } catch (SQLException e) {
            logger.error("Ошибка получения неудачных попыток входа", e);
            throw new DatabaseException("Ошибка при получении журнала", e);
        }
    }

    
    public List<SecurityLog> getSecurityLogByPeriod(LocalDateTime start, LocalDateTime end) {
        try {
            return securityLogDao.findByPeriod(start, end);
        } catch (SQLException e) {
            logger.error("Ошибка получения журнала безопасности за период", e);
            throw new DatabaseException("Ошибка при получении журнала", e);
        }
    }

    

    
    public static class DashboardStats {
        private BigDecimal todayRevenue;
        private BigDecimal monthRevenue;
        private int totalProducts;
        private int lowStockProducts;
        private int todaySales;

        
        public BigDecimal getTodayRevenue() { return todayRevenue; }
        public void setTodayRevenue(BigDecimal todayRevenue) { this.todayRevenue = todayRevenue; }
        
        public BigDecimal getMonthRevenue() { return monthRevenue; }
        public void setMonthRevenue(BigDecimal monthRevenue) { this.monthRevenue = monthRevenue; }
        
        public int getTotalProducts() { return totalProducts; }
        public void setTotalProducts(int totalProducts) { this.totalProducts = totalProducts; }
        
        public int getLowStockProducts() { return lowStockProducts; }
        public void setLowStockProducts(int lowStockProducts) { this.lowStockProducts = lowStockProducts; }
        
        public int getTodaySales() { return todaySales; }
        public void setTodaySales(int todaySales) { this.todaySales = todaySales; }
    }

    
    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();
        
        stats.setTodayRevenue(getTodayRevenue());
        stats.setMonthRevenue(getMonthRevenue());
        stats.setTotalProducts(getStockReport().size());
        stats.setLowStockProducts(getLowStockReport().size());
        
        try {
            stats.setTodaySales(saleDao.findToday().size());
        } catch (SQLException e) {
            logger.error("Ошибка получения количества продаж за сегодня", e);
            stats.setTodaySales(0);
        }
        
        return stats;
    }
}

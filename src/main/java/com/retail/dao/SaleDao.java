package com.retail.dao;

import com.retail.model.Sale;
import com.retail.model.SaleItem;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class SaleDao extends AbstractDao<Sale, Integer> {

    @Override
    protected String getTableName() {
        return "sales";
    }

    @Override
    protected Sale mapRow(ResultSet rs) throws SQLException {
        Sale sale = new Sale();
        sale.setId(rs.getInt("id"));
        sale.setSaleNumber(rs.getString("sale_number"));
        sale.setEmployeeId(rs.getInt("employee_id"));
        sale.setSaleDate(rs.getTimestamp("sale_date").toLocalDateTime());
        sale.setTotalAmount(rs.getBigDecimal("total_amount"));
        sale.setDiscount(rs.getBigDecimal("discount"));
        sale.setFinalAmount(rs.getBigDecimal("final_amount"));
        sale.setReturned(rs.getBoolean("is_returned"));
        sale.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        
        
        try {
            sale.setEmployeeName(rs.getString("employee_name"));
        } catch (SQLException ignored) {}
        
        return sale;
    }

    @Override
    public Sale save(Sale sale) throws SQLException {
        throw new UnsupportedOperationException("Используйте save(Connection, Sale) для транзакции");
    }

    
    public Sale save(Connection conn, Sale sale) throws SQLException {
        String sql = """
            INSERT INTO sales (sale_number, employee_id, sale_date, total_amount, discount, final_amount)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        
        String saleNumber = generateSaleNumber(conn);
        sale.setSaleNumber(saleNumber);
        
        Integer id = executeInsertAndGetKey(conn, sql,
                sale.getSaleNumber(),
                sale.getEmployeeId(),
                Timestamp.valueOf(sale.getSaleDate()),
                sale.getTotalAmount(),
                sale.getDiscount(),
                sale.getFinalAmount());
        
        sale.setId(id);
        return sale;
    }

    private String generateSaleNumber(Connection conn) throws SQLException {
        String sql = "SELECT generate_sale_number()";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getString(1);
            }
        }
        
        return "S" + System.currentTimeMillis();
    }

    @Override
    public void update(Sale sale) throws SQLException {
        String sql = """
            UPDATE sales SET
                total_amount = ?,
                discount = ?,
                final_amount = ?,
                is_returned = ?
            WHERE id = ?
            """;
        
        executeUpdate(sql,
                sale.getTotalAmount(),
                sale.getDiscount(),
                sale.getFinalAmount(),
                sale.isReturned(),
                sale.getId());
    }

    @Override
    public void delete(Integer id) throws SQLException {
        
        throw new UnsupportedOperationException("Удаление продаж запрещено");
    }

    
    public Optional<Sale> findBySaleNumber(String saleNumber) throws SQLException {
        String sql = """
            SELECT s.*, u.full_name as employee_name
            FROM sales s
            JOIN users u ON s.employee_id = u.id
            WHERE s.sale_number = ?
            """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, saleNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    
    public List<Sale> findByPeriod(LocalDateTime startDate, LocalDateTime endDate) throws SQLException {
        String sql = """
            SELECT s.*, u.full_name as employee_name
            FROM sales s
            JOIN users u ON s.employee_id = u.id
            WHERE s.sale_date BETWEEN ? AND ?
            ORDER BY s.sale_date DESC
            """;
        
        List<Sale> result = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(startDate));
            stmt.setTimestamp(2, Timestamp.valueOf(endDate));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    
    public List<Sale> findToday() throws SQLException {
        LocalDate today = LocalDate.now();
        return findByPeriod(
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );
    }

    
    public List<Sale> findByEmployee(Integer employeeId) throws SQLException {
        String sql = """
            SELECT s.*, u.full_name as employee_name
            FROM sales s
            JOIN users u ON s.employee_id = u.id
            WHERE s.employee_id = ?
            ORDER BY s.sale_date DESC
            """;
        
        List<Sale> result = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    
    public BigDecimal getTotalRevenue(LocalDateTime startDate, LocalDateTime endDate) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(final_amount), 0) as total
            FROM sales
            WHERE sale_date BETWEEN ? AND ? AND is_returned = FALSE
            """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(startDate));
            stmt.setTimestamp(2, Timestamp.valueOf(endDate));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("total");
                }
            }
        }
        return BigDecimal.ZERO;
    }

    
    public void markAsReturned(Connection conn, Integer saleId) throws SQLException {
        String sql = "UPDATE sales SET is_returned = TRUE WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, saleId);
            stmt.executeUpdate();
        }
    }

    
    @Override
    public Optional<Sale> findById(Integer id) throws SQLException {
        String sql = """
            SELECT s.*, u.full_name as employee_name
            FROM sales s
            JOIN users u ON s.employee_id = u.id
            WHERE s.id = ?
            """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<SaleFullDescription> getFullSaleDescription(Integer saleId) throws SQLException {
    // JOIN нескольких таблиц для получения полной информации
    String sql = """
        SELECT 
            s.id as sale_id,
            s.sale_number,
            s.sale_date,
            s.total_amount,
            s.discount,
            s.final_amount,
            s.is_returned,
            u.id as employee_id,
            u.full_name as employee_name,
            u.login as employee_login,
            si.id as item_id,
            si.quantity,
            si.price_at_sale,
            si.line_total,
            si.returned_qty,
            p.id as product_id,
            p.sku as product_sku,
            p.name as product_name,
            c.name as category_name
        FROM sales s
        JOIN users u ON s.employee_id = u.id
        LEFT JOIN sale_items si ON s.id = si.sale_id
        LEFT JOIN products p ON si.product_id = p.id
        LEFT JOIN categories c ON p.category_id = c.id
        WHERE s.id = ?
        ORDER BY si.id
        """;
    
    try (Connection conn = dbManager.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setInt(1, saleId);
        
        try (ResultSet rs = stmt.executeQuery()) {
            SaleFullDescription description = null;
            
            while (rs.next()) {
                if (description == null) {
                    description = new SaleFullDescription();
                    description.setSaleId(rs.getInt("sale_id"));
                    description.setSaleNumber(rs.getString("sale_number"));
                    description.setSaleDate(rs.getTimestamp("sale_date").toLocalDateTime());
                    description.setTotalAmount(rs.getBigDecimal("total_amount"));
                    description.setDiscount(rs.getBigDecimal("discount"));
                    description.setFinalAmount(rs.getBigDecimal("final_amount"));
                    description.setReturned(rs.getBoolean("is_returned"));
                    description.setEmployeeId(rs.getInt("employee_id"));
                    description.setEmployeeName(rs.getString("employee_name"));
                    description.setEmployeeLogin(rs.getString("employee_login"));
                }
                
                int itemId = rs.getInt("item_id");
                if (!rs.wasNull()) {
                    SaleFullDescription.SaleItemDetail item = new SaleFullDescription.SaleItemDetail();
                    item.setItemId(itemId);
                    item.setProductId(rs.getInt("product_id"));
                    item.setProductSku(rs.getString("product_sku"));
                    item.setProductName(rs.getString("product_name"));
                    item.setCategoryName(rs.getString("category_name"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setPriceAtSale(rs.getBigDecimal("price_at_sale"));
                    item.setLineTotal(rs.getBigDecimal("line_total"));
                    item.setReturnedQty(rs.getInt("returned_qty"));
                    description.addItem(item);
                }
            }
            
            return Optional.ofNullable(description);
        }
    }
}

    // DTO класс для полного описания продажи
    public static class SaleFullDescription {
        private Integer saleId;
        private String saleNumber;
        private LocalDateTime saleDate;
        private BigDecimal totalAmount;
        private BigDecimal discount;
        private BigDecimal finalAmount;
        private boolean returned;
        private Integer employeeId;
        private String employeeName;
        private String employeeLogin;
        private List<SaleItemDetail> items = new ArrayList<>();

        public Integer getSaleId() { return saleId; }
        public void setSaleId(Integer saleId) { this.saleId = saleId; }
        public String getSaleNumber() { return saleNumber; }
        public void setSaleNumber(String saleNumber) { this.saleNumber = saleNumber; }
        public LocalDateTime getSaleDate() { return saleDate; }
        public void setSaleDate(LocalDateTime saleDate) { this.saleDate = saleDate; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public BigDecimal getDiscount() { return discount; }
        public void setDiscount(BigDecimal discount) { this.discount = discount; }
        public BigDecimal getFinalAmount() { return finalAmount; }
        public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
        public boolean isReturned() { return returned; }
        public void setReturned(boolean returned) { this.returned = returned; }
        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public String getEmployeeLogin() { return employeeLogin; }
        public void setEmployeeLogin(String employeeLogin) { this.employeeLogin = employeeLogin; }
        public List<SaleItemDetail> getItems() { return items; }
        public void addItem(SaleItemDetail item) { this.items.add(item); }

        public static class SaleItemDetail {
            private Integer itemId;
            private Integer productId;
            private String productSku;
            private String productName;
            private String categoryName;
            private Integer quantity;
            private BigDecimal priceAtSale;
            private BigDecimal lineTotal;
            private Integer returnedQty;

            public Integer getItemId() { return itemId; }
            public void setItemId(Integer itemId) { this.itemId = itemId; }
            public Integer getProductId() { return productId; }
            public void setProductId(Integer productId) { this.productId = productId; }
            public String getProductSku() { return productSku; }
            public void setProductSku(String productSku) { this.productSku = productSku; }
            public String getProductName() { return productName; }
            public void setProductName(String productName) { this.productName = productName; }
            public String getCategoryName() { return categoryName; }
            public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
            public Integer getQuantity() { return quantity; }
            public void setQuantity(Integer quantity) { this.quantity = quantity; }
            public BigDecimal getPriceAtSale() { return priceAtSale; }
            public void setPriceAtSale(BigDecimal priceAtSale) { this.priceAtSale = priceAtSale; }
            public BigDecimal getLineTotal() { return lineTotal; }
            public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
            public Integer getReturnedQty() { return returnedQty; }
            public void setReturnedQty(Integer returnedQty) { this.returnedQty = returnedQty; }
        }
    }
}

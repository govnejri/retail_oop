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
}

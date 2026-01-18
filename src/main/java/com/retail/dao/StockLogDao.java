package com.retail.dao;

import com.retail.model.StockLog;
import com.retail.model.enums.StockOperationType;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class StockLogDao extends AbstractDao<StockLog, Integer> {

    @Override
    protected String getTableName() {
        return "stock_log";
    }

    @Override
    protected StockLog mapRow(ResultSet rs) throws SQLException {
        StockLog log = new StockLog();
        log.setId(rs.getInt("id"));
        log.setProductId(rs.getInt("product_id"));
        log.setOperationType(StockOperationType.fromString(rs.getString("operation_type")));
        log.setQuantityChange(rs.getInt("quantity_change"));
        log.setQuantityBefore(rs.getInt("quantity_before"));
        log.setQuantityAfter(rs.getInt("quantity_after"));
        
        int refId = rs.getInt("reference_id");
        if (!rs.wasNull()) {
            log.setReferenceId(refId);
        }
        
        log.setReferenceType(rs.getString("reference_type"));
        log.setUserId(rs.getInt("user_id"));
        log.setNotes(rs.getString("notes"));
        log.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        
        
        try {
            log.setProductSku(rs.getString("product_sku"));
        } catch (SQLException ignored) {}
        
        try {
            log.setProductName(rs.getString("product_name"));
        } catch (SQLException ignored) {}
        
        try {
            log.setUserName(rs.getString("user_name"));
        } catch (SQLException ignored) {}
        
        return log;
    }

    @Override
    public StockLog save(StockLog log) throws SQLException {
        String sql = """
            INSERT INTO stock_log (product_id, operation_type, quantity_change, 
                                   quantity_before, quantity_after, reference_id, 
                                   reference_type, user_id, notes)
            VALUES (?, ?::stock_operation_type, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;
        
        Integer id = executeInsertAndGetKey(sql,
                log.getProductId(),
                log.getOperationType().name(),
                log.getQuantityChange(),
                log.getQuantityBefore(),
                log.getQuantityAfter(),
                log.getReferenceId(),
                log.getReferenceType(),
                log.getUserId(),
                log.getNotes());
        
        log.setId(id);
        return log;
    }

    
    public StockLog save(Connection conn, StockLog log) throws SQLException {
        String sql = """
            INSERT INTO stock_log (product_id, operation_type, quantity_change, 
                                   quantity_before, quantity_after, reference_id, 
                                   reference_type, user_id, notes)
            VALUES (?, ?::stock_operation_type, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        Integer id = executeInsertAndGetKey(conn, sql,
                log.getProductId(),
                log.getOperationType().name(),
                log.getQuantityChange(),
                log.getQuantityBefore(),
                log.getQuantityAfter(),
                log.getReferenceId(),
                log.getReferenceType(),
                log.getUserId(),
                log.getNotes());
        
        log.setId(id);
        return log;
    }

    @Override
    public void update(StockLog log) throws SQLException {
        throw new UnsupportedOperationException("Изменение логов запрещено");
    }

    @Override
    public void delete(Integer id) throws SQLException {
        throw new UnsupportedOperationException("Удаление логов запрещено");
    }

    
    public List<StockLog> findByProductId(Integer productId) throws SQLException {
        String sql = """
            SELECT sl.*, p.sku as product_sku, p.name as product_name, u.full_name as user_name
            FROM stock_log sl
            JOIN products p ON sl.product_id = p.id
            JOIN users u ON sl.user_id = u.id
            WHERE sl.product_id = ?
            ORDER BY sl.created_at DESC
            """;
        
        List<StockLog> result = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    
    public List<StockLog> findByTypeAndPeriod(StockOperationType type, 
                                               LocalDateTime startDate, 
                                               LocalDateTime endDate) throws SQLException {
        String sql = """
            SELECT sl.*, p.sku as product_sku, p.name as product_name, u.full_name as user_name
            FROM stock_log sl
            JOIN products p ON sl.product_id = p.id
            JOIN users u ON sl.user_id = u.id
            WHERE sl.operation_type = ?::stock_operation_type 
                  AND sl.created_at BETWEEN ? AND ?
            ORDER BY sl.created_at DESC
            """;
        
        List<StockLog> result = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type.name());
            stmt.setTimestamp(2, Timestamp.valueOf(startDate));
            stmt.setTimestamp(3, Timestamp.valueOf(endDate));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    
    public List<StockLog> findAdjustments() throws SQLException {
        String sql = """
            SELECT sl.*, p.sku as product_sku, p.name as product_name, u.full_name as user_name
            FROM stock_log sl
            JOIN products p ON sl.product_id = p.id
            JOIN users u ON sl.user_id = u.id
            WHERE sl.operation_type IN ('ADJUSTMENT', 'WRITE_OFF')
            ORDER BY sl.created_at DESC
            LIMIT 100
            """;
        
        List<StockLog> result = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    
    public List<StockLog> findByUserId(Integer userId, int limit) throws SQLException {
        String sql = """
            SELECT sl.*, p.sku as product_sku, p.name as product_name, u.full_name as user_name
            FROM stock_log sl
            JOIN products p ON sl.product_id = p.id
            JOIN users u ON sl.user_id = u.id
            WHERE sl.user_id = ?
            ORDER BY sl.created_at DESC
            LIMIT ?
            """;
        
        List<StockLog> result = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }
}

package com.retail.dao;

import com.retail.model.Receipt;
import com.retail.model.ReceiptItem;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class ReceiptDao extends AbstractDao<Receipt, Integer> {

    @Override
    protected String getTableName() {
        return "receipts";
    }

    @Override
    protected Receipt mapRow(ResultSet rs) throws SQLException {
        Receipt receipt = new Receipt();
        receipt.setId(rs.getInt("id"));
        receipt.setReceiptNumber(rs.getString("receipt_number"));
        receipt.setSupplierInfo(rs.getString("supplier_info"));
        receipt.setManagerId(rs.getInt("manager_id"));
        receipt.setReceiptDate(rs.getTimestamp("receipt_date").toLocalDateTime());
        receipt.setTotalAmount(rs.getBigDecimal("total_amount"));
        receipt.setNotes(rs.getString("notes"));
        receipt.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        
        try {
            receipt.setManagerName(rs.getString("manager_name"));
        } catch (SQLException ignored) {}
        
        return receipt;
    }

    @Override
    public Receipt save(Receipt receipt) throws SQLException {
        throw new UnsupportedOperationException("Используйте save(Connection, Receipt) для транзакции");
    }

    
    public Receipt save(Connection conn, Receipt receipt) throws SQLException {
        String sql = """
            INSERT INTO receipts (receipt_number, supplier_info, manager_id, receipt_date, total_amount, notes)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        
        String receiptNumber = generateReceiptNumber(conn);
        receipt.setReceiptNumber(receiptNumber);
        
        Integer id = executeInsertAndGetKey(conn, sql,
                receipt.getReceiptNumber(),
                receipt.getSupplierInfo(),
                receipt.getManagerId(),
                Timestamp.valueOf(receipt.getReceiptDate()),
                receipt.getTotalAmount(),
                receipt.getNotes());
        
        receipt.setId(id);
        return receipt;
    }

    private String generateReceiptNumber(Connection conn) throws SQLException {
        String sql = "SELECT generate_receipt_number()";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getString(1);
            }
        }
        return "R" + System.currentTimeMillis();
    }

    @Override
    public void update(Receipt receipt) throws SQLException {
        String sql = """
            UPDATE receipts SET
                supplier_info = ?,
                total_amount = ?,
                notes = ?
            WHERE id = ?
            """;
        
        executeUpdate(sql,
                receipt.getSupplierInfo(),
                receipt.getTotalAmount(),
                receipt.getNotes(),
                receipt.getId());
    }

    @Override
    public void delete(Integer id) throws SQLException {
        throw new UnsupportedOperationException("Удаление поставок запрещено");
    }

    
    public Optional<Receipt> findByReceiptNumber(String receiptNumber) throws SQLException {
        String sql = """
            SELECT r.*, u.full_name as manager_name
            FROM receipts r
            JOIN users u ON r.manager_id = u.id
            WHERE r.receipt_number = ?
            """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, receiptNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    
    public List<Receipt> findByPeriod(LocalDateTime startDate, LocalDateTime endDate) throws SQLException {
        String sql = """
            SELECT r.*, u.full_name as manager_name
            FROM receipts r
            JOIN users u ON r.manager_id = u.id
            WHERE r.receipt_date BETWEEN ? AND ?
            ORDER BY r.receipt_date DESC
            """;
        
        List<Receipt> result = new ArrayList<>();
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

    @Override
    public Optional<Receipt> findById(Integer id) throws SQLException {
        String sql = """
            SELECT r.*, u.full_name as manager_name
            FROM receipts r
            JOIN users u ON r.manager_id = u.id
            WHERE r.id = ?
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

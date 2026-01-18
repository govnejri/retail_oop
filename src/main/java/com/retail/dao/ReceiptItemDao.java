package com.retail.dao;

import com.retail.model.ReceiptItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class ReceiptItemDao extends AbstractDao<ReceiptItem, Integer> {

    @Override
    protected String getTableName() {
        return "receipt_items";
    }

    @Override
    protected ReceiptItem mapRow(ResultSet rs) throws SQLException {
        ReceiptItem item = new ReceiptItem();
        item.setId(rs.getInt("id"));
        item.setReceiptId(rs.getInt("receipt_id"));
        item.setProductId(rs.getInt("product_id"));
        item.setQuantity(rs.getInt("quantity"));
        item.setPurchasePrice(rs.getBigDecimal("purchase_price"));
        item.setLineTotal(rs.getBigDecimal("line_total"));
        
        try {
            item.setProductSku(rs.getString("product_sku"));
        } catch (SQLException ignored) {}
        
        try {
            item.setProductName(rs.getString("product_name"));
        } catch (SQLException ignored) {}
        
        return item;
    }

    @Override
    public ReceiptItem save(ReceiptItem item) throws SQLException {
        throw new UnsupportedOperationException("Используйте save(Connection, ReceiptItem) для транзакции");
    }

    
    public ReceiptItem save(Connection conn, ReceiptItem item) throws SQLException {
        String sql = """
            INSERT INTO receipt_items (receipt_id, product_id, quantity, purchase_price, line_total)
            VALUES (?, ?, ?, ?, ?)
            """;
        
        Integer id = executeInsertAndGetKey(conn, sql,
                item.getReceiptId(),
                item.getProductId(),
                item.getQuantity(),
                item.getPurchasePrice(),
                item.getLineTotal());
        
        item.setId(id);
        return item;
    }

    @Override
    public void update(ReceiptItem item) throws SQLException {
        String sql = """
            UPDATE receipt_items SET
                quantity = ?,
                purchase_price = ?,
                line_total = ?
            WHERE id = ?
            """;
        
        executeUpdate(sql,
                item.getQuantity(),
                item.getPurchasePrice(),
                item.getLineTotal(),
                item.getId());
    }

    @Override
    public void delete(Integer id) throws SQLException {
        throw new UnsupportedOperationException("Удаление позиций поставок запрещено");
    }

    
    public List<ReceiptItem> findByReceiptId(Integer receiptId) throws SQLException {
        String sql = """
            SELECT ri.*, p.sku as product_sku, p.name as product_name
            FROM receipt_items ri
            JOIN products p ON ri.product_id = p.id
            WHERE ri.receipt_id = ?
            ORDER BY ri.id
            """;
        
        List<ReceiptItem> result = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, receiptId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }
}

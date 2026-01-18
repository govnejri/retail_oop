package com.retail.dao;

import com.retail.model.Inventory;
import com.retail.model.enums.StockOperationType;

import java.sql.*;


public class InventoryDao extends AbstractDao<Inventory, Integer> {

    @Override
    protected String getTableName() {
        return "inventory";
    }

    @Override
    protected Inventory mapRow(ResultSet rs) throws SQLException {
        Inventory inventory = new Inventory();
        inventory.setId(rs.getInt("id"));
        inventory.setProductId(rs.getInt("product_id"));
        inventory.setQuantity(rs.getInt("quantity"));
        inventory.setReserved(rs.getInt("reserved"));
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            inventory.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return inventory;
    }

    @Override
    public Inventory save(Inventory inventory) throws SQLException {
        String sql = """
            INSERT INTO inventory (product_id, quantity, reserved)
            VALUES (?, ?, ?)
            RETURNING id
            """;
        
        Integer id = executeInsertAndGetKey(sql,
                inventory.getProductId(),
                inventory.getQuantity(),
                inventory.getReserved());
        
        inventory.setId(id);
        return inventory;
    }

    
    public Inventory save(Connection conn, Inventory inventory) throws SQLException {
        String sql = """
            INSERT INTO inventory (product_id, quantity, reserved)
            VALUES (?, ?, ?)
            """;
        
        Integer id = executeInsertAndGetKey(conn, sql,
                inventory.getProductId(),
                inventory.getQuantity(),
                inventory.getReserved());
        
        inventory.setId(id);
        return inventory;
    }

    @Override
    public void update(Inventory inventory) throws SQLException {
        String sql = """
            UPDATE inventory SET
                quantity = ?,
                reserved = ?
            WHERE id = ?
            """;
        
        executeUpdate(sql,
                inventory.getQuantity(),
                inventory.getReserved(),
                inventory.getId());
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM inventory WHERE id = ?";
        executeUpdate(sql, id);
    }

    
    public Inventory findByProductId(Integer productId) throws SQLException {
        String sql = "SELECT * FROM inventory WHERE product_id = ?";
        return executeQuerySingle(sql, productId).orElse(null);
    }

    
    public Inventory findByProductIdForUpdate(Connection conn, Integer productId) throws SQLException {
        String sql = "SELECT * FROM inventory WHERE product_id = ? FOR UPDATE";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    
    public void decreaseQuantity(Connection conn, Integer productId, int amount) throws SQLException {
        String sql = "UPDATE inventory SET quantity = quantity - ? WHERE product_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, amount);
            stmt.setInt(2, productId);
            
            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Запись инвентаря не найдена для товара: " + productId);
            }
        }
    }

    
    public void increaseQuantity(Connection conn, Integer productId, int amount) throws SQLException {
        
        String updateSql = "UPDATE inventory SET quantity = quantity + ? WHERE product_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setInt(1, amount);
            stmt.setInt(2, productId);
            
            int affected = stmt.executeUpdate();
            if (affected == 0) {
                
                String insertSql = "INSERT INTO inventory (product_id, quantity, reserved) VALUES (?, ?, 0)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, productId);
                    insertStmt.setInt(2, amount);
                    insertStmt.executeUpdate();
                }
            }
        }
    }

    
    public void setQuantity(Connection conn, Integer productId, int newQuantity) throws SQLException {
        String sql = """
            INSERT INTO inventory (product_id, quantity, reserved)
            VALUES (?, ?, 0)
            ON CONFLICT (product_id) DO UPDATE SET quantity = ?
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            stmt.setInt(2, newQuantity);
            stmt.setInt(3, newQuantity);
            stmt.executeUpdate();
        }
    }

    
    public int getQuantity(Integer productId) throws SQLException {
        String sql = "SELECT quantity FROM inventory WHERE product_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("quantity");
                }
            }
        }
        return 0;
    }

    
    public boolean hasEnoughStock(Integer productId, int requiredQuantity) throws SQLException {
        return getQuantity(productId) >= requiredQuantity;
    }

    
    public void upsert(Connection conn, Integer productId, int quantity) throws SQLException {
        String sql = """
            INSERT INTO inventory (product_id, quantity, reserved)
            VALUES (?, ?, 0)
            ON CONFLICT (product_id) DO UPDATE SET quantity = inventory.quantity + EXCLUDED.quantity
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            stmt.setInt(2, quantity);
            stmt.executeUpdate();
        }
    }
}

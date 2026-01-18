package com.retail.dao;

import com.retail.model.Product;

import java.sql.*;
import java.util.List;
import java.util.Optional;


public class ProductDao extends AbstractDao<Product, Integer> {

    @Override
    protected String getTableName() {
        return "products";
    }

    @Override
    protected Product mapRow(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setId(rs.getInt("id"));
        product.setSku(rs.getString("sku"));
        product.setName(rs.getString("name"));
        product.setDescription(rs.getString("description"));
        
        int categoryId = rs.getInt("category_id");
        if (!rs.wasNull()) {
            product.setCategoryId(categoryId);
        }
        
        int unitId = rs.getInt("unit_id");
        if (!rs.wasNull()) {
            product.setUnitId(unitId);
        }
        
        product.setPurchasePrice(rs.getBigDecimal("purchase_price"));
        product.setSellingPrice(rs.getBigDecimal("selling_price"));
        product.setMinStockLevel(rs.getInt("min_stock_level"));
        product.setActive(rs.getBoolean("is_active"));
        product.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            product.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return product;
    }

    
    private Product mapRowWithExtras(ResultSet rs) throws SQLException {
        Product product = mapRow(rs);
        
        
        try {
            product.setCategoryName(rs.getString("category_name"));
        } catch (SQLException ignored) {}
        
        try {
            product.setUnitName(rs.getString("unit_name"));
        } catch (SQLException ignored) {}
        
        try {
            product.setStockQuantity(rs.getInt("stock_quantity"));
        } catch (SQLException ignored) {}
        
        return product;
    }

    @Override
    public Product save(Product product) throws SQLException {
        String sql = """
            INSERT INTO products (sku, name, description, category_id, unit_id, 
                                  purchase_price, selling_price, min_stock_level, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;
        
        Integer id = executeInsertAndGetKey(sql,
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getCategoryId(),
                product.getUnitId(),
                product.getPurchasePrice(),
                product.getSellingPrice(),
                product.getMinStockLevel(),
                product.isActive());
        
        product.setId(id);
        return product;
    }

    
    public Product save(Connection conn, Product product) throws SQLException {
        String sql = """
            INSERT INTO products (sku, name, description, category_id, unit_id, 
                                  purchase_price, selling_price, min_stock_level, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        Integer id = executeInsertAndGetKey(conn, sql,
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getCategoryId(),
                product.getUnitId(),
                product.getPurchasePrice(),
                product.getSellingPrice(),
                product.getMinStockLevel(),
                product.isActive());
        
        product.setId(id);
        return product;
    }

    @Override
    public void update(Product product) throws SQLException {
        String sql = """
            UPDATE products SET
                sku = ?,
                name = ?,
                description = ?,
                category_id = ?,
                unit_id = ?,
                purchase_price = ?,
                selling_price = ?,
                min_stock_level = ?,
                is_active = ?
            WHERE id = ?
            """;
        
        executeUpdate(sql,
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getCategoryId(),
                product.getUnitId(),
                product.getPurchasePrice(),
                product.getSellingPrice(),
                product.getMinStockLevel(),
                product.isActive(),
                product.getId());
    }

    @Override
    public void delete(Integer id) throws SQLException {
        
        String sql = "UPDATE products SET is_active = FALSE WHERE id = ?";
        executeUpdate(sql, id);
    }

    
    public Optional<Product> findBySku(String sku) throws SQLException {
        String sql = "SELECT * FROM products WHERE sku = ?";
        return executeQuerySingle(sql, sku);
    }

    
    public List<Product> searchByName(String namePattern) throws SQLException {
        String sql = """
            SELECT p.*, c.name as category_name, u.short_name as unit_name,
                   COALESCE(i.quantity, 0) as stock_quantity
            FROM products p
            LEFT JOIN categories c ON p.category_id = c.id
            LEFT JOIN units u ON p.unit_id = u.id
            LEFT JOIN inventory i ON p.id = i.product_id
            WHERE p.is_active = TRUE AND LOWER(p.name) LIKE LOWER(?)
            ORDER BY p.name
            """;
        
        List<Product> result = new java.util.ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + namePattern + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRowWithExtras(rs));
                }
            }
        }
        return result;
    }

    
    public List<Product> findAllWithStock() throws SQLException {
        String sql = """
            SELECT p.*, c.name as category_name, u.short_name as unit_name,
                   COALESCE(i.quantity, 0) as stock_quantity
            FROM products p
            LEFT JOIN categories c ON p.category_id = c.id
            LEFT JOIN units u ON p.unit_id = u.id
            LEFT JOIN inventory i ON p.id = i.product_id
            WHERE p.is_active = TRUE
            ORDER BY p.name
            """;
        
        List<Product> result = new java.util.ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(mapRowWithExtras(rs));
            }
        }
        return result;
    }

    
    public List<Product> findLowStock() throws SQLException {
        String sql = """
            SELECT p.*, c.name as category_name, u.short_name as unit_name,
                   COALESCE(i.quantity, 0) as stock_quantity
            FROM products p
            LEFT JOIN categories c ON p.category_id = c.id
            LEFT JOIN units u ON p.unit_id = u.id
            LEFT JOIN inventory i ON p.id = i.product_id
            WHERE p.is_active = TRUE AND COALESCE(i.quantity, 0) <= p.min_stock_level
            ORDER BY i.quantity ASC
            """;
        
        List<Product> result = new java.util.ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(mapRowWithExtras(rs));
            }
        }
        return result;
    }

    
    public List<Product> findByCategory(Integer categoryId) throws SQLException {
        String sql = """
            SELECT p.*, c.name as category_name, u.short_name as unit_name,
                   COALESCE(i.quantity, 0) as stock_quantity
            FROM products p
            LEFT JOIN categories c ON p.category_id = c.id
            LEFT JOIN units u ON p.unit_id = u.id
            LEFT JOIN inventory i ON p.id = i.product_id
            WHERE p.is_active = TRUE AND p.category_id = ?
            ORDER BY p.name
            """;
        
        List<Product> result = new java.util.ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, categoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRowWithExtras(rs));
                }
            }
        }
        return result;
    }

    
    public void updateSellingPrice(Integer productId, java.math.BigDecimal newPrice) throws SQLException {
        String sql = "UPDATE products SET selling_price = ? WHERE id = ?";
        executeUpdate(sql, newPrice, productId);
    }

    
    public boolean skuExists(String sku) throws SQLException {
        String sql = "SELECT 1 FROM products WHERE sku = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sku);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    
    public Optional<Product> findByIdWithStock(Integer id) throws SQLException {
        String sql = """
            SELECT p.*, c.name as category_name, u.short_name as unit_name,
                   COALESCE(i.quantity, 0) as stock_quantity
            FROM products p
            LEFT JOIN categories c ON p.category_id = c.id
            LEFT JOIN units u ON p.unit_id = u.id
            LEFT JOIN inventory i ON p.id = i.product_id
            WHERE p.id = ?
            """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowWithExtras(rs));
                }
            }
        }
        return Optional.empty();
    }
}

package com.retail.dao;

import com.retail.model.SaleItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class SaleItemDao extends AbstractDao<SaleItem, Integer> {

    @Override
    protected String getTableName() {
        return "sale_items";
    }

    @Override
    protected SaleItem mapRow(ResultSet rs) throws SQLException {
        SaleItem item = new SaleItem();
        item.setId(rs.getInt("id"));
        item.setSaleId(rs.getInt("sale_id"));
        item.setProductId(rs.getInt("product_id"));
        item.setQuantity(rs.getInt("quantity"));
        item.setPriceAtSale(rs.getBigDecimal("price_at_sale"));
        item.setLineTotal(rs.getBigDecimal("line_total"));
        item.setReturnedQty(rs.getInt("returned_qty"));
        
        
        try {
            item.setProductSku(rs.getString("product_sku"));
        } catch (SQLException ignored) {}
        
        try {
            item.setProductName(rs.getString("product_name"));
        } catch (SQLException ignored) {}
        
        return item;
    }

    @Override
    public SaleItem save(SaleItem item) throws SQLException {
        throw new UnsupportedOperationException("Используйте save(Connection, SaleItem) для транзакции");
    }

    
    public SaleItem save(Connection conn, SaleItem item) throws SQLException {
        String sql = """
            INSERT INTO sale_items (sale_id, product_id, quantity, price_at_sale, line_total)
            VALUES (?, ?, ?, ?, ?)
            """;
        
        Integer id = executeInsertAndGetKey(conn, sql,
                item.getSaleId(),
                item.getProductId(),
                item.getQuantity(),
                item.getPriceAtSale(),
                item.getLineTotal());
        
        item.setId(id);
        return item;
    }

    @Override
    public void update(SaleItem item) throws SQLException {
        String sql = """
            UPDATE sale_items SET
                quantity = ?,
                price_at_sale = ?,
                line_total = ?,
                returned_qty = ?
            WHERE id = ?
            """;
        
        executeUpdate(sql,
                item.getQuantity(),
                item.getPriceAtSale(),
                item.getLineTotal(),
                item.getReturnedQty(),
                item.getId());
    }

    
    public void updateReturnedQty(Connection conn, Integer itemId, int returnedQty) throws SQLException {
        String sql = "UPDATE sale_items SET returned_qty = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, returnedQty);
            stmt.setInt(2, itemId);
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        throw new UnsupportedOperationException("Удаление позиций продаж запрещено");
    }

    
    public List<SaleItem> findBySaleId(Integer saleId) throws SQLException {
        String sql = """
            SELECT si.*, p.sku as product_sku, p.name as product_name
            FROM sale_items si
            JOIN products p ON si.product_id = p.id
            WHERE si.sale_id = ?
            ORDER BY si.id
            """;
        
        List<SaleItem> result = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, saleId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    
    public List<SaleItem> findReturnableItems(Integer saleId) throws SQLException {
        String sql = """
            SELECT si.*, p.sku as product_sku, p.name as product_name
            FROM sale_items si
            JOIN products p ON si.product_id = p.id
            WHERE si.sale_id = ? AND si.quantity > si.returned_qty
            ORDER BY si.id
            """;
        
        List<SaleItem> result = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, saleId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    
    public List<Object[]> getTopSellingProducts(int limit) throws SQLException {
        String sql = """
            SELECT p.id, p.sku, p.name, 
                   SUM(si.quantity - si.returned_qty) as total_sold,
                   SUM(si.line_total) as total_revenue
            FROM products p
            JOIN sale_items si ON p.id = si.product_id
            JOIN sales s ON si.sale_id = s.id
            WHERE s.is_returned = FALSE
            GROUP BY p.id, p.sku, p.name
            HAVING SUM(si.quantity - si.returned_qty) > 0
            ORDER BY total_sold DESC
            LIMIT ?
            """;
        
        List<Object[]> result = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new Object[]{
                            rs.getInt("id"),
                            rs.getString("sku"),
                            rs.getString("name"),
                            rs.getLong("total_sold"),
                            rs.getBigDecimal("total_revenue")
                    });
                }
            }
        }
        return result;
    }
}

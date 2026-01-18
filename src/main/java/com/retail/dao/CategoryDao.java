package com.retail.dao;

import com.retail.model.Category;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class CategoryDao extends AbstractDao<Category, Integer> {

    @Override
    protected String getTableName() {
        return "categories";
    }

    @Override
    protected Category mapRow(ResultSet rs) throws SQLException {
        Category category = new Category();
        category.setId(rs.getInt("id"));
        category.setName(rs.getString("name"));
        category.setDescription(rs.getString("description"));
        category.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            category.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return category;
    }

    @Override
    public Category save(Category category) throws SQLException {
        String sql = """
            INSERT INTO categories (name, description)
            VALUES (?, ?)
            RETURNING id
            """;
        
        Integer id = executeInsertAndGetKey(sql,
                category.getName(),
                category.getDescription());
        
        category.setId(id);
        return category;
    }

    @Override
    public void update(Category category) throws SQLException {
        String sql = """
            UPDATE categories SET
                name = ?,
                description = ?
            WHERE id = ?
            """;
        
        executeUpdate(sql,
                category.getName(),
                category.getDescription(),
                category.getId());
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM categories WHERE id = ?";
        executeUpdate(sql, id);
    }

    
    public Optional<Category> findByName(String name) throws SQLException {
        String sql = "SELECT * FROM categories WHERE name = ?";
        return executeQuerySingle(sql, name);
    }

    
    public boolean nameExists(String name) throws SQLException {
        String sql = "SELECT 1 FROM categories WHERE name = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public List<Category> findAll() throws SQLException {
        String sql = "SELECT * FROM categories ORDER BY name";
        return executeQuery(sql);
    }
}

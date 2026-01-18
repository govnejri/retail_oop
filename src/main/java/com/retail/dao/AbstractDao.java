package com.retail.dao;

import com.retail.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AbstractDao<T, ID> implements BaseDao<T, ID> {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final DatabaseManager dbManager;

    protected AbstractDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    
    protected abstract String getTableName();

    
    protected abstract T mapRow(ResultSet rs) throws SQLException;

    
    protected String getIdColumnName() {
        return "id";
    }

    @Override
    public Optional<T> findById(ID id) throws SQLException {
        String sql = "SELECT * FROM " + getTableName() + " WHERE " + getIdColumnName() + " = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<T> findAll() throws SQLException {
        String sql = "SELECT * FROM " + getTableName();
        List<T> result = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    @Override
    public boolean exists(ID id) throws SQLException {
        String sql = "SELECT 1 FROM " + getTableName() + " WHERE " + getIdColumnName() + " = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public long count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + getTableName();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
    }

    
    protected List<T> executeQuery(String sql, Object... params) throws SQLException {
        List<T> result = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setParameters(stmt, params);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    
    protected Optional<T> executeQuerySingle(String sql, Object... params) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setParameters(stmt, params);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    
    protected int executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setParameters(stmt, params);
            return stmt.executeUpdate();
        }
    }

    
    protected Integer executeInsertAndGetKey(String sql, Object... params) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            setParameters(stmt, params);
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Не удалось получить сгенерированный ключ");
    }

    
    protected Integer executeInsertAndGetKey(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParameters(stmt, params);
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Не удалось получить сгенерированный ключ");
    }

    
    protected void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                stmt.setNull(i + 1, Types.NULL);
            } else if (params[i] instanceof Enum) {
                stmt.setString(i + 1, ((Enum<?>) params[i]).name());
            } else {
                stmt.setObject(i + 1, params[i]);
            }
        }
    }
}

package com.retail.dao;

import com.retail.model.SecurityLog;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class SecurityLogDao extends AbstractDao<SecurityLog, Integer> {

    @Override
    protected String getTableName() {
        return "security_log";
    }

    @Override
    protected SecurityLog mapRow(ResultSet rs) throws SQLException {
        SecurityLog log = new SecurityLog();
        log.setId(rs.getInt("id"));
        
        int userId = rs.getInt("user_id");
        if (!rs.wasNull()) {
            log.setUserId(userId);
        }
        
        log.setAction(rs.getString("action"));
        log.setDetails(rs.getString("details"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setSuccess(rs.getBoolean("success"));
        log.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        
        
        try {
            log.setUserLogin(rs.getString("user_login"));
        } catch (SQLException ignored) {}
        
        try {
            log.setUserName(rs.getString("user_name"));
        } catch (SQLException ignored) {}
        
        return log;
    }

    @Override
    public SecurityLog save(SecurityLog log) throws SQLException {
        String sql = """
            INSERT INTO security_log (user_id, action, details, ip_address, success)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id
            """;
        
        Integer id = executeInsertAndGetKey(sql,
                log.getUserId(),
                log.getAction(),
                log.getDetails(),
                log.getIpAddress(),
                log.isSuccess());
        
        log.setId(id);
        return log;
    }

    @Override
    public void update(SecurityLog log) throws SQLException {
        throw new UnsupportedOperationException("Изменение логов безопасности запрещено");
    }

    @Override
    public void delete(Integer id) throws SQLException {
        throw new UnsupportedOperationException("Удаление логов безопасности запрещено");
    }

    
    public List<SecurityLog> findByPeriod(LocalDateTime startDate, LocalDateTime endDate) throws SQLException {
        String sql = """
            SELECT sl.*, u.login as user_login, u.full_name as user_name
            FROM security_log sl
            LEFT JOIN users u ON sl.user_id = u.id
            WHERE sl.created_at BETWEEN ? AND ?
            ORDER BY sl.created_at DESC
            """;
        
        List<SecurityLog> result = new ArrayList<>();
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

    
    public List<SecurityLog> findFailedLogins(int limit) throws SQLException {
        String sql = """
            SELECT sl.*, u.login as user_login, u.full_name as user_name
            FROM security_log sl
            LEFT JOIN users u ON sl.user_id = u.id
            WHERE sl.action = 'LOGIN' AND sl.success = FALSE
            ORDER BY sl.created_at DESC
            LIMIT ?
            """;
        
        List<SecurityLog> result = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    
    public List<SecurityLog> findRecent(int limit) throws SQLException {
        String sql = """
            SELECT sl.*, u.login as user_login, u.full_name as user_name
            FROM security_log sl
            LEFT JOIN users u ON sl.user_id = u.id
            ORDER BY sl.created_at DESC
            LIMIT ?
            """;
        
        List<SecurityLog> result = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    
    public List<SecurityLog> findByUserId(Integer userId, int limit) throws SQLException {
        String sql = """
            SELECT sl.*, u.login as user_login, u.full_name as user_name
            FROM security_log sl
            LEFT JOIN users u ON sl.user_id = u.id
            WHERE sl.user_id = ?
            ORDER BY sl.created_at DESC
            LIMIT ?
            """;
        
        List<SecurityLog> result = new ArrayList<>();
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

    
    public void logLogin(Integer userId, String login, boolean success) throws SQLException {
        SecurityLog log = new SecurityLog();
        log.setUserId(userId);
        log.setAction("LOGIN");
        log.setDetails("Попытка входа: " + login);
        log.setSuccess(success);
        save(log);
    }

    
    public void logLogout(Integer userId) throws SQLException {
        SecurityLog log = new SecurityLog();
        log.setUserId(userId);
        log.setAction("LOGOUT");
        log.setDetails("Выход из системы");
        log.setSuccess(true);
        save(log);
    }

    
    public void logUserChange(Integer adminId, String action, String details) throws SQLException {
        SecurityLog log = new SecurityLog();
        log.setUserId(adminId);
        log.setAction(action);
        log.setDetails(details);
        log.setSuccess(true);
        save(log);
    }
}

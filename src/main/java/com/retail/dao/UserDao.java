package com.retail.dao;

import com.retail.model.User;
import com.retail.model.enums.UserRole;
import com.retail.model.enums.UserStatus;

import java.sql.*;
import java.util.List;
import java.util.Optional;


public class UserDao extends AbstractDao<User, Integer> {

    @Override
    protected String getTableName() {
        return "users";
    }

    @Override
    protected User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setLogin(rs.getString("login"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(UserRole.fromString(rs.getString("role")));
        user.setStatus(UserStatus.fromString(rs.getString("status")));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) {
            user.setLastLogin(lastLogin.toLocalDateTime());
        }
        
        return user;
    }

    @Override
    public User save(User user) throws SQLException {
        String sql = """
            INSERT INTO users (login, password_hash, role, status, full_name, email)
            VALUES (?, ?, ?::user_role, ?::user_status, ?, ?)
            RETURNING id
            """;
        
        Integer id = executeInsertAndGetKey(sql,
                user.getLogin(),
                user.getPasswordHash(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getFullName(),
                user.getEmail());
        
        user.setId(id);
        return user;
    }

    @Override
    public void update(User user) throws SQLException {
        String sql = """
            UPDATE users SET
                login = ?,
                role = ?::user_role,
                status = ?::user_status,
                full_name = ?,
                email = ?
            WHERE id = ?
            """;
        
        executeUpdate(sql,
                user.getLogin(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getFullName(),
                user.getEmail(),
                user.getId());
    }

    @Override
    public void delete(Integer id) throws SQLException {
        
        String sql = "UPDATE users SET status = 'DELETED'::user_status WHERE id = ?";
        executeUpdate(sql, id);
    }

    
    public Optional<User> findByLogin(String login) throws SQLException {
        String sql = "SELECT * FROM users WHERE login = ?";
        return executeQuerySingle(sql, login);
    }

    
    public List<User> findActive() throws SQLException {
        String sql = "SELECT * FROM users WHERE status = 'ACTIVE' ORDER BY full_name";
        return executeQuery(sql);
    }

    
    public List<User> findByRole(UserRole role) throws SQLException {
        String sql = "SELECT * FROM users WHERE role = ?::user_role AND status != 'DELETED' ORDER BY full_name";
        return executeQuery(sql, role.name());
    }

    
    public void updatePassword(Integer userId, String newPasswordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        executeUpdate(sql, newPasswordHash, userId);
    }

    
    public void updateStatus(Integer userId, UserStatus status) throws SQLException {
        String sql = "UPDATE users SET status = ?::user_status WHERE id = ?";
        executeUpdate(sql, status.name(), userId);
    }

    
    public void updateLastLogin(Integer userId) throws SQLException {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?";
        executeUpdate(sql, userId);
    }

    
    public boolean loginExists(String login) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE login = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, login);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
}

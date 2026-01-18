package com.retail.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;


public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static DatabaseManager instance;
    private HikariDataSource dataSource;

    private DatabaseManager() {
        initializeDataSource();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializeDataSource() {
        try {
            Properties props = loadProperties();
            
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(props.getProperty("db.url"));
            config.setUsername(props.getProperty("db.username"));
            config.setPassword(props.getProperty("db.password"));
            config.setDriverClassName(props.getProperty("db.driver"));
            
            
            config.setMaximumPoolSize(Integer.parseInt(
                    props.getProperty("db.pool.size", "10")));
            config.setMinimumIdle(Integer.parseInt(
                    props.getProperty("db.pool.min.idle", "2")));
            config.setConnectionTimeout(Long.parseLong(
                    props.getProperty("db.connection.timeout", "30000")));
            
            
            config.setPoolName("RetailPool");
            config.setAutoCommit(true);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            
            dataSource = new HikariDataSource(config);
            logger.info("Пул соединений с базой данных инициализирован");
            
        } catch (Exception e) {
            logger.error("Ошибка инициализации пула соединений", e);
            throw new RuntimeException("Не удалось инициализировать соединение с БД", e);
        }
    }

    private Properties loadProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IOException("Файл application.properties не найден");
            }
            props.load(input);
        }
        return props;
    }

    
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Пул соединений не инициализирован");
        }
        return dataSource.getConnection();
    }

    
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            logger.error("Ошибка проверки соединения с БД", e);
            return false;
        }
    }

    
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Пул соединений закрыт");
        }
    }

    
    public <T> T executeInTransaction(TransactionCallback<T> callback) throws SQLException {
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            
            T result = callback.execute(conn);
            
            conn.commit();
            return result;
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.warn("Транзакция откачена из-за ошибки: {}", e.getMessage());
                } catch (SQLException rollbackEx) {
                    logger.error("Ошибка отката транзакции", rollbackEx);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("Ошибка закрытия соединения", e);
                }
            }
        }
    }

    
    @FunctionalInterface
    public interface TransactionCallback<T> {
        T execute(Connection connection) throws SQLException;
    }
}

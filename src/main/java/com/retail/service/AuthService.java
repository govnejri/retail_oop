package com.retail.service;

import com.retail.dao.SecurityLogDao;
import com.retail.dao.UserDao;
import com.retail.exception.AuthenticationException;
import com.retail.exception.DatabaseException;
import com.retail.model.User;
import com.retail.model.enums.UserStatus;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Optional;


public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("com.retail.security");
    
    private final UserDao userDao;
    private final SecurityLogDao securityLogDao;
    
    
    private User currentUser;

    public AuthService() {
        this.userDao = new UserDao();
        this.securityLogDao = new SecurityLogDao();
    }

    
    public User login(String login, String password) {
        try {
            Optional<User> userOpt = userDao.findByLogin(login);
            
            if (userOpt.isEmpty()) {
                logFailedLogin(null, login);
                throw new AuthenticationException("Неверный логин или пароль");
            }
            
            User user = userOpt.get();
            
            
            if (user.getStatus() == UserStatus.BLOCKED) {
                logFailedLogin(user.getId(), login);
                throw new AuthenticationException("Учетная запись заблокирована");
            }
            
            if (user.getStatus() == UserStatus.DELETED) {
                logFailedLogin(null, login);
                throw new AuthenticationException("Неверный логин или пароль");
            }
            
            
            if (!BCrypt.checkpw(password, user.getPasswordHash())) {
                logFailedLogin(user.getId(), login);
                throw new AuthenticationException("Неверный логин или пароль");
            }
            
            
            userDao.updateLastLogin(user.getId());
            logSuccessfulLogin(user.getId(), login);
            
            currentUser = user;
            logger.info("Пользователь {} вошел в систему", login);
            
            return user;
            
        } catch (SQLException e) {
            logger.error("Ошибка БД при аутентификации", e);
            throw new DatabaseException("Ошибка при входе в систему", e);
        }
    }

    
    public void logout() {
        if (currentUser != null) {
            try {
                securityLogDao.logLogout(currentUser.getId());
                logger.info("Пользователь {} вышел из системы", currentUser.getLogin());
            } catch (SQLException e) {
                logger.error("Ошибка записи лога выхода", e);
            }
            currentUser = null;
        }
    }

    
    public User getCurrentUser() {
        return currentUser;
    }

    
    public boolean isAuthenticated() {
        return currentUser != null;
    }

    
    public String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(10));
    }

    
    public boolean verifyPassword(String password, String hash) {
        return BCrypt.checkpw(password, hash);
    }

    private void logFailedLogin(Integer userId, String login) {
        try {
            securityLogDao.logLogin(userId, login, false);
            securityLogger.warn("Неудачная попытка входа: {}", login);
        } catch (SQLException e) {
            logger.error("Ошибка записи лога неудачного входа", e);
        }
    }

    private void logSuccessfulLogin(Integer userId, String login) {
        try {
            securityLogDao.logLogin(userId, login, true);
            securityLogger.info("Успешный вход: {}", login);
        } catch (SQLException e) {
            logger.error("Ошибка записи лога успешного входа", e);
        }
    }
}

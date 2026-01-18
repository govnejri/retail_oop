package com.retail.service;

import com.retail.dao.SecurityLogDao;
import com.retail.dao.UserDao;
import com.retail.exception.DatabaseException;
import com.retail.exception.ValidationException;
import com.retail.model.User;
import com.retail.model.enums.UserRole;
import com.retail.model.enums.UserStatus;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;


public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final UserDao userDao;
    private final SecurityLogDao securityLogDao;

    public UserService() {
        this.userDao = new UserDao();
        this.securityLogDao = new SecurityLogDao();
    }

    
    public User createUser(String login, String password, UserRole role, 
                           String fullName, String email, Integer adminId) {
        try {
            
            if (login == null || login.trim().isEmpty()) {
                throw new ValidationException("Логин не может быть пустым");
            }
            
            if (password == null || password.length() < 6) {
                throw new ValidationException("Пароль должен содержать минимум 6 символов");
            }
            
            if (fullName == null || fullName.trim().isEmpty()) {
                throw new ValidationException("ФИО не может быть пустым");
            }
            
            
            if (userDao.loginExists(login)) {
                throw new ValidationException("Пользователь с таким логином уже существует");
            }
            
            
            User user = new User();
            user.setLogin(login.trim().toLowerCase());
            user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt(10)));
            user.setRole(role);
            user.setStatus(UserStatus.ACTIVE);
            user.setFullName(fullName.trim());
            user.setEmail(email != null ? email.trim() : null);
            
            user = userDao.save(user);
            
            
            securityLogDao.logUserChange(adminId, "USER_CREATE", 
                    String.format("Создан пользователь: %s (роль: %s)", login, role));
            
            logger.info("Создан пользователь: {} (роль: {})", login, role);
            return user;
            
        } catch (SQLException e) {
            logger.error("Ошибка создания пользователя", e);
            throw new DatabaseException("Ошибка при создании пользователя", e);
        }
    }

    
    public void updateUser(User user, Integer adminId) {
        try {
            userDao.update(user);
            
            securityLogDao.logUserChange(adminId, "USER_UPDATE",
                    String.format("Обновлен пользователь: %s", user.getLogin()));
            
            logger.info("Обновлен пользователь: {}", user.getLogin());
            
        } catch (SQLException e) {
            logger.error("Ошибка обновления пользователя", e);
            throw new DatabaseException("Ошибка при обновлении пользователя", e);
        }
    }

    
    public void blockUser(Integer userId, Integer adminId) {
        try {
            Optional<User> userOpt = userDao.findById(userId);
            if (userOpt.isEmpty()) {
                throw new ValidationException("Пользователь не найден");
            }
            
            User user = userOpt.get();
            
            
            if (user.getRole() == UserRole.ADMIN) {
                throw new ValidationException("Невозможно заблокировать администратора");
            }
            
            userDao.updateStatus(userId, UserStatus.BLOCKED);
            
            securityLogDao.logUserChange(adminId, "USER_BLOCK",
                    String.format("Заблокирован пользователь: %s", user.getLogin()));
            
            logger.info("Заблокирован пользователь: {}", user.getLogin());
            
        } catch (SQLException e) {
            logger.error("Ошибка блокировки пользователя", e);
            throw new DatabaseException("Ошибка при блокировке пользователя", e);
        }
    }

    
    public void unblockUser(Integer userId, Integer adminId) {
        try {
            Optional<User> userOpt = userDao.findById(userId);
            if (userOpt.isEmpty()) {
                throw new ValidationException("Пользователь не найден");
            }
            
            userDao.updateStatus(userId, UserStatus.ACTIVE);
            
            securityLogDao.logUserChange(adminId, "USER_UNBLOCK",
                    String.format("Разблокирован пользователь: %s", userOpt.get().getLogin()));
            
            logger.info("Разблокирован пользователь: {}", userOpt.get().getLogin());
            
        } catch (SQLException e) {
            logger.error("Ошибка разблокировки пользователя", e);
            throw new DatabaseException("Ошибка при разблокировке пользователя", e);
        }
    }

    
    public void deleteUser(Integer userId, Integer adminId) {
        try {
            Optional<User> userOpt = userDao.findById(userId);
            if (userOpt.isEmpty()) {
                throw new ValidationException("Пользователь не найден");
            }
            
            User user = userOpt.get();
            
            
            if (user.getRole() == UserRole.ADMIN) {
                throw new ValidationException("Невозможно удалить администратора");
            }
            
            userDao.delete(userId);
            
            securityLogDao.logUserChange(adminId, "USER_DELETE",
                    String.format("Удален пользователь: %s", user.getLogin()));
            
            logger.info("Удален пользователь: {}", user.getLogin());
            
        } catch (SQLException e) {
            logger.error("Ошибка удаления пользователя", e);
            throw new DatabaseException("Ошибка при удалении пользователя", e);
        }
    }

    
    public void resetPassword(Integer userId, String newPassword, Integer adminId) {
        try {
            if (newPassword == null || newPassword.length() < 6) {
                throw new ValidationException("Пароль должен содержать минимум 6 символов");
            }
            
            Optional<User> userOpt = userDao.findById(userId);
            if (userOpt.isEmpty()) {
                throw new ValidationException("Пользователь не найден");
            }
            
            String passwordHash = BCrypt.hashpw(newPassword, BCrypt.gensalt(10));
            userDao.updatePassword(userId, passwordHash);
            
            securityLogDao.logUserChange(adminId, "PASSWORD_RESET",
                    String.format("Сброшен пароль пользователя: %s", userOpt.get().getLogin()));
            
            logger.info("Сброшен пароль пользователя: {}", userOpt.get().getLogin());
            
        } catch (SQLException e) {
            logger.error("Ошибка сброса пароля", e);
            throw new DatabaseException("Ошибка при сбросе пароля", e);
        }
    }

    
    public Optional<User> findById(Integer id) {
        try {
            return userDao.findById(id);
        } catch (SQLException e) {
            logger.error("Ошибка поиска пользователя", e);
            throw new DatabaseException("Ошибка при поиске пользователя", e);
        }
    }

    
    public Optional<User> findByLogin(String login) {
        try {
            return userDao.findByLogin(login);
        } catch (SQLException e) {
            logger.error("Ошибка поиска пользователя", e);
            throw new DatabaseException("Ошибка при поиске пользователя", e);
        }
    }

    
    public List<User> findAllActive() {
        try {
            return userDao.findActive();
        } catch (SQLException e) {
            logger.error("Ошибка получения списка пользователей", e);
            throw new DatabaseException("Ошибка при получении списка пользователей", e);
        }
    }

    
    public List<User> findByRole(UserRole role) {
        try {
            return userDao.findByRole(role);
        } catch (SQLException e) {
            logger.error("Ошибка получения списка пользователей по роли", e);
            throw new DatabaseException("Ошибка при получении списка пользователей", e);
        }
    }
}

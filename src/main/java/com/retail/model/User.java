package com.retail.model;

import com.retail.model.enums.UserRole;
import com.retail.model.enums.UserStatus;

import java.time.LocalDateTime;


public class User {
    private Integer id;
    private String login;
    private String passwordHash;
    private UserRole role;
    private UserStatus status;
    private String fullName;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;

    public User() {
    }

    public User(String login, String passwordHash, UserRole role, String fullName) {
        this.login = login;
        this.passwordHash = passwordHash;
        this.role = role;
        this.fullName = fullName;
        this.status = UserStatus.ACTIVE;
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isManager() {
        return role == UserRole.MANAGER;
    }

    public boolean isEmployee() {
        return role == UserRole.EMPLOYEE;
    }

    @Override
    public String toString() {
        return String.format("User{id=%d, login='%s', role=%s, fullName='%s', status=%s}",
                id, login, role, fullName, status);
    }
}

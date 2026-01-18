package com.retail.model;

import java.time.LocalDateTime;


public class SecurityLog {
    private Integer id;
    private Integer userId;
    private String action;
    private String details;
    private String ipAddress;
    private boolean success;
    private LocalDateTime createdAt;


    private String userLogin;
    private String userName;

    public SecurityLog() {
    }

    public SecurityLog(Integer userId, String action, String details, boolean success) {
        this.userId = userId;
        this.action = action;
        this.details = details;
        this.success = success;
        this.createdAt = LocalDateTime.now();
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String toString() {
        return String.format("SecurityLog{userId=%d, action='%s', success=%b, date=%s}",
                userId, action, success, createdAt);
    }
}

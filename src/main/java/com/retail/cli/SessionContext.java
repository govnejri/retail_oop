package com.retail.cli;

import com.retail.model.User;
import com.retail.service.*;


public class SessionContext {
    
    private User currentUser;
    private final AuthService authService;
    private final UserService userService;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final SaleService saleService;
    private final ReportService reportService;

    public SessionContext() {
        this.authService = new AuthService();
        this.userService = new UserService();
        this.productService = new ProductService();
        this.inventoryService = new InventoryService();
        this.saleService = new SaleService();
        this.reportService = new ReportService();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public UserService getUserService() {
        return userService;
    }

    public ProductService getProductService() {
        return productService;
    }

    public InventoryService getInventoryService() {
        return inventoryService;
    }

    public SaleService getSaleService() {
        return saleService;
    }

    public ReportService getReportService() {
        return reportService;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public Integer getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : null;
    }

    public void logout() {
        if (currentUser != null) {
            authService.logout();
            currentUser = null;
        }
    }
}

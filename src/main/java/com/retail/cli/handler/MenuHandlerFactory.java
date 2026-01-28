package com.retail.cli.handler;

import com.retail.cli.ConsoleInput;
import com.retail.cli.SessionContext;
import com.retail.model.enums.UserRole;

public class MenuHandlerFactory {

    private MenuHandlerFactory() {
    }

    public static BaseMenuHandler createHandler(UserRole role, SessionContext context, ConsoleInput input) {
        return switch (role) {
            case ADMIN -> new AdminMenuHandler(context, input);
            case MANAGER -> new ManagerMenuHandler(context, input);
            case EMPLOYEE -> new EmployeeMenuHandler(context, input);
        };
    }
}
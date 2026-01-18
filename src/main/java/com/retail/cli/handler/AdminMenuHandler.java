package com.retail.cli.handler;

import com.retail.cli.ConsoleFormatter;
import com.retail.cli.ConsoleInput;
import com.retail.cli.SessionContext;
import com.retail.model.Category;
import com.retail.model.SecurityLog;
import com.retail.model.User;
import com.retail.model.enums.UserRole;
import com.retail.model.enums.UserStatus;

import java.util.ArrayList;
import java.util.List;


public class AdminMenuHandler extends BaseMenuHandler {

    public AdminMenuHandler(SessionContext context, ConsoleInput input) {
        super(context, input);
    }

    @Override
    public boolean handle() {
        while (true) {
            int choice = showMenu();
            if (processChoice(choice)) {
                return true;
            }
        }
    }

    @Override
    protected String getMenuTitle() {
        return "МЕНЮ АДМИНИСТРАТОРА";
    }

    @Override
    protected String[] getMenuItems() {
        return new String[]{
                "Управление пользователями",
                "Категории товаров",
                "Единицы измерения",
                "Журнал безопасности",
                "Выйти в главное меню"
        };
    }

    @Override
    protected boolean processChoice(int choice) {
        switch (choice) {
            case 1 -> handleUserManagement();
            case 2 -> handleCategoryManagement();
            case 3 -> handleUnitManagement();
            case 4 -> handleSecurityLog();
            case 5 -> { return true; }
        }
        return false;
    }

    

    private void handleUserManagement() {
        while (true) {
            ConsoleFormatter.printHeader("УПРАВЛЕНИЕ ПОЛЬЗОВАТЕЛЯМИ");
            System.out.println("  1. Список пользователей");
            System.out.println("  2. Добавить пользователя");
            System.out.println("  3. Заблокировать пользователя");
            System.out.println("  4. Разблокировать пользователя");
            System.out.println("  5. Сбросить пароль");
            System.out.println("  6. Удалить пользователя");
            System.out.println("  7. Назад");
            System.out.println();

            int choice = input.readIntInRange("Выберите пункт", 1, 7);

            switch (choice) {
                case 1 -> showUserList();
                case 2 -> addUser();
                case 3 -> blockUser();
                case 4 -> unblockUser();
                case 5 -> resetPassword();
                case 6 -> deleteUser();
                case 7 -> { return; }
            }
        }
    }

    private void showUserList() {
        try {
            List<User> users = context.getUserService().findAllActive();
            
            if (users.isEmpty()) {
                ConsoleFormatter.printInfo("Нет активных пользователей");
                pressEnterToContinue();
                return;
            }

            String[] headers = {"ID", "Логин", "ФИО", "Роль", "Статус", "Последний вход"};
            List<String[]> rows = new ArrayList<>();

            for (User user : users) {
                rows.add(new String[]{
                        String.valueOf(user.getId()),
                        user.getLogin(),
                        user.getFullName(),
                        user.getRole().getDisplayName(),
                        user.getStatus().getDisplayName(),
                        ConsoleFormatter.formatDateTime(user.getLastLogin())
                });
            }

            ConsoleFormatter.printTable(headers, rows);
            pressEnterToContinue();

        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    private void addUser() {
        try {
            ConsoleFormatter.printHeader("ДОБАВЛЕНИЕ ПОЛЬЗОВАТЕЛЯ");

            String login = input.readNonEmptyString("Логин");
            String password = input.readNonEmptyString("Пароль");
            String fullName = input.readNonEmptyString("ФИО");
            String email = input.readOptionalString("Email");

            System.out.println("\nРоли:");
            System.out.println("  1. Главный менеджер");
            System.out.println("  2. Сотрудник");
            int roleChoice = input.readIntInRange("Выберите роль", 1, 2);

            UserRole role = roleChoice == 1 ? UserRole.MANAGER : UserRole.EMPLOYEE;

            User user = context.getUserService().createUser(
                    login, password, role, fullName,
                    email.isEmpty() ? null : email,
                    context.getCurrentUserId());

            showSuccessAndWait("Пользователь создан: " + user.getLogin());

        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    private void blockUser() {
        try {
            showUserList();
            int userId = input.readPositiveInt("ID пользователя для блокировки");
            
            if (input.readYesNo("Вы уверены?")) {
                context.getUserService().blockUser(userId, context.getCurrentUserId());
                showSuccessAndWait("Пользователь заблокирован");
            }

        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    private void unblockUser() {
        try {
            int userId = input.readPositiveInt("ID пользователя для разблокировки");
            context.getUserService().unblockUser(userId, context.getCurrentUserId());
            showSuccessAndWait("Пользователь разблокирован");

        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    private void resetPassword() {
        try {
            int userId = input.readPositiveInt("ID пользователя");
            String newPassword = input.readNonEmptyString("Новый пароль");
            
            context.getUserService().resetPassword(userId, newPassword, context.getCurrentUserId());
            showSuccessAndWait("Пароль сброшен");

        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    private void deleteUser() {
        try {
            showUserList();
            int userId = input.readPositiveInt("ID пользователя для удаления");
            
            if (input.readYesNo("Вы уверены? Это действие необратимо")) {
                context.getUserService().deleteUser(userId, context.getCurrentUserId());
                showSuccessAndWait("Пользователь удален");
            }

        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    

    private void handleCategoryManagement() {
        while (true) {
            ConsoleFormatter.printHeader("КАТЕГОРИИ ТОВАРОВ");
            System.out.println("  1. Список категорий");
            System.out.println("  2. Добавить категорию");
            System.out.println("  3. Удалить категорию");
            System.out.println("  4. Назад");
            System.out.println();

            int choice = input.readIntInRange("Выберите пункт", 1, 4);

            switch (choice) {
                case 1 -> showCategoryList();
                case 2 -> addCategory();
                case 3 -> deleteCategory();
                case 4 -> { return; }
            }
        }
    }

    private void showCategoryList() {
        try {
            List<Category> categories = context.getProductService().findAllCategories();

            if (categories.isEmpty()) {
                ConsoleFormatter.printInfo("Нет категорий");
                pressEnterToContinue();
                return;
            }

            String[] headers = {"ID", "Название", "Описание"};
            List<String[]> rows = new ArrayList<>();

            for (Category cat : categories) {
                rows.add(new String[]{
                        String.valueOf(cat.getId()),
                        cat.getName(),
                        cat.getDescription() != null ? cat.getDescription() : "-"
                });
            }

            ConsoleFormatter.printTable(headers, rows);
            pressEnterToContinue();

        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    private void addCategory() {
        try {
            String name = input.readNonEmptyString("Название категории");
            String description = input.readOptionalString("Описание");

            Category category = context.getProductService().createCategory(
                    name, description.isEmpty() ? null : description);

            showSuccessAndWait("Категория создана: " + category.getName());

        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    private void deleteCategory() {
        try {
            showCategoryList();
            int categoryId = input.readPositiveInt("ID категории для удаления");
            
            if (input.readYesNo("Вы уверены?")) {
                context.getProductService().deleteCategory(categoryId);
                showSuccessAndWait("Категория удалена");
            }

        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    

    private void handleUnitManagement() {
        while (true) {
            ConsoleFormatter.printHeader("ЕДИНИЦЫ ИЗМЕРЕНИЯ");
            System.out.println("  1. Список единиц");
            System.out.println("  2. Добавить единицу");
            System.out.println("  3. Назад");
            System.out.println();

            int choice = input.readIntInRange("Выберите пункт", 1, 3);

            switch (choice) {
                case 1 -> showUnitList();
                case 2 -> addUnit();
                case 3 -> { return; }
            }
        }
    }

    private void showUnitList() {
        try {
            var units = context.getProductService().findAllUnits();

            if (units.isEmpty()) {
                ConsoleFormatter.printInfo("Нет единиц измерения");
                pressEnterToContinue();
                return;
            }

            String[] headers = {"ID", "Название", "Сокращение"};
            List<String[]> rows = new ArrayList<>();

            for (var unit : units) {
                rows.add(new String[]{
                        String.valueOf(unit.getId()),
                        unit.getName(),
                        unit.getShortName()
                });
            }

            ConsoleFormatter.printTable(headers, rows);
            pressEnterToContinue();

        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    private void addUnit() {
        try {
            String name = input.readNonEmptyString("Название");
            String shortName = input.readNonEmptyString("Сокращение");

            var unit = context.getProductService().createUnit(name, shortName);
            showSuccessAndWait("Единица измерения создана: " + unit.getName());

        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    

    private void handleSecurityLog() {
        try {
            ConsoleFormatter.printHeader("ЖУРНАЛ БЕЗОПАСНОСТИ");

            List<SecurityLog> logs = context.getReportService().getSecurityLog(50);

            if (logs.isEmpty()) {
                ConsoleFormatter.printInfo("Журнал пуст");
                pressEnterToContinue();
                return;
            }

            String[] headers = {"Дата", "Пользователь", "Действие", "Детали", "Успех"};
            List<String[]> rows = new ArrayList<>();

            for (SecurityLog log : logs) {
                rows.add(new String[]{
                        ConsoleFormatter.formatDateTime(log.getCreatedAt()),
                        log.getUserLogin() != null ? log.getUserLogin() : "-",
                        log.getAction(),
                        truncate(log.getDetails(), 30),
                        log.isSuccess() ? "Да" : "Нет"
                });
            }

            ConsoleFormatter.printTable(headers, rows);
            pressEnterToContinue();

        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "-";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 3) + "...";
    }
}

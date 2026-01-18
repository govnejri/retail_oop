package com.retail;

import com.retail.cli.ConsoleFormatter;
import com.retail.cli.ConsoleInput;
import com.retail.cli.SessionContext;
import com.retail.cli.handler.AdminMenuHandler;
import com.retail.cli.handler.EmployeeMenuHandler;
import com.retail.cli.handler.ManagerMenuHandler;
import com.retail.db.DatabaseManager;
import com.retail.exception.AuthenticationException;
import com.retail.model.User;
import com.retail.model.enums.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;


public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private final Scanner scanner;
    private final ConsoleInput input;
    private final SessionContext context;

    public Application() {
        this.scanner = new Scanner(System.in);
        this.input = new ConsoleInput(scanner);
        this.context = new SessionContext();
    }

    public static void main(String[] args) {
        Application app = new Application();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ConsoleFormatter.printInfo("Завершение работы...");
            DatabaseManager.getInstance().shutdown();
        }));

        app.run();
    }

    public void run() {
        showWelcome();

        try {
            
            if (!testDatabaseConnection()) {
                ConsoleFormatter.printError("Не удалось подключиться к базе данных!");
                ConsoleFormatter.printInfo("Проверьте настройки в application.properties");
                return;
            }

            mainLoop();

        } catch (Exception e) {
            logger.error("Критическая ошибка приложения", e);
            ConsoleFormatter.printError("Критическая ошибка: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void mainLoop() {
        while (true) {
            try {
                if (!context.isAuthenticated()) {
                    if (!showLoginMenu()) {
                        break;
                    }
                } else {
                    showRoleBasedMenu();
                }
            } catch (Exception e) {
                logger.error("Ошибка в главном цикле", e);
                ConsoleFormatter.printError("Ошибка: " + e.getMessage());
                input.waitForEnter();
            }
        }
    }

    private boolean showLoginMenu() {
        ConsoleFormatter.printHeader("ГЛАВНОЕ МЕНЮ");
        System.out.println("1. Вход в систему");
        System.out.println("2. Выход из программы");
        System.out.println();

        int choice = input.readIntInRange("Выберите действие", 1, 2);

        if (choice == 2) {
            return false;
        }

        handleLogin();
        return true;
    }

    private void handleLogin() {
        ConsoleFormatter.printHeader("ВХОД В СИСТЕМУ");

        String username = input.readNonEmptyString("Логин");
        String password = input.readPassword("Пароль");

        try {
            User user = context.getAuthService().login(username, password);
            context.setCurrentUser(user);

            ConsoleFormatter.printSuccess("Добро пожаловать, " + user.getFullName() + "!");
            logger.info("Пользователь {} вошел в систему", username);

        } catch (AuthenticationException e) {
            ConsoleFormatter.printError(e.getMessage());
            logger.warn("Ошибка аутентификации для {}: {}", username, e.getMessage());
        } catch (Exception e) {
            ConsoleFormatter.printError("Ошибка входа: " + e.getMessage());
            logger.error("Ошибка входа", e);
        }

        input.waitForEnter();
    }

    private void showRoleBasedMenu() {
        User user = context.getCurrentUser();
        
        if (user == null) {
            context.logout();
            return;
        }

        UserRole role = user.getRole();
        boolean shouldLogout;

        switch (role) {
            case ADMIN -> {
                AdminMenuHandler handler = new AdminMenuHandler(context, input);
                shouldLogout = handler.handle();
            }
            case MANAGER -> {
                ManagerMenuHandler handler = new ManagerMenuHandler(context, input);
                shouldLogout = handler.handle();
            }
            case EMPLOYEE -> {
                EmployeeMenuHandler handler = new EmployeeMenuHandler(context, input);
                shouldLogout = handler.handle();
            }
            default -> {
                ConsoleFormatter.printError("Неизвестная роль пользователя");
                shouldLogout = true;
            }
        }

        if (shouldLogout) {
            String username = user.getLogin();
            context.logout();
            logger.info("Пользователь {} вышел из системы", username);
            ConsoleFormatter.printInfo("Вы вышли из системы");
            input.waitForEnter();
        }
    }

    private void showWelcome() {
        clearScreen();
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                              ║");
        System.out.println("║     СИСТЕМА УПРАВЛЕНИЯ РОЗНИЧНОЙ ТОРГОВЛЕЙ И СКЛАДСКИМ      ║");
        System.out.println("║                         УЧЕТОМ                               ║");
        System.out.println("║                                                              ║");
        System.out.println("║                       Версия 1.0.0                           ║");
        System.out.println("║                                                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private boolean testDatabaseConnection() {
        try {
            ConsoleFormatter.printInfo("Проверка подключения к базе данных...");
            DatabaseManager.getInstance().testConnection();
            ConsoleFormatter.printSuccess("Подключение установлено");
            return true;
        } catch (Exception e) {
            logger.error("Ошибка подключения к БД", e);
            return false;
        }
    }

    private void cleanup() {
        if (context.isAuthenticated()) {
            context.logout();
        }
        DatabaseManager.getInstance().shutdown();
        ConsoleFormatter.printInfo("Приложение завершено. До свидания!");
    }

    private void clearScreen() {
        
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}

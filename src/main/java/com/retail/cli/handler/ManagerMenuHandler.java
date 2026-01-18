package com.retail.cli.handler;

import com.retail.cli.ConsoleFormatter;
import com.retail.cli.ConsoleInput;
import com.retail.cli.SessionContext;
import com.retail.model.*;
import com.retail.service.ReportService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class ManagerMenuHandler extends BaseMenuHandler {

    public ManagerMenuHandler(SessionContext context, ConsoleInput input) {
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
        return "МЕНЮ ГЛАВНОГО МЕНЕДЖЕРА";
    }

    @Override
    protected String[] getMenuItems() {
        return new String[]{
                "Склад (просмотр остатков)",
                "Товары (создать/изменить)",
                "Приемка товара",
                "Инвентаризация",
                "Отчеты",
                "Выйти в главное меню"
        };
    }

    @Override
    protected boolean processChoice(int choice) {
        switch (choice) {
            case 1 -> handleStockView();
            case 2 -> handleProductManagement();
            case 3 -> handleReceiptGoods();
            case 4 -> handleInventoryAdjustment();
            case 5 -> handleReports();
            case 6 -> { return true; }
        }
        return false;
    }

    

    private void handleStockView() {
        while (true) {
            ConsoleFormatter.printHeader("СКЛАД");
            System.out.println("  1. Все товары с остатками");
            System.out.println("  2. Заканчивающиеся товары");
            System.out.println("  3. Поиск товара");
            System.out.println("  4. Назад");
            System.out.println();

            int choice = input.readIntInRange("Выберите пункт", 1, 4);

            switch (choice) {
                case 1 -> showAllStock();
                case 2 -> showLowStock();
                case 3 -> searchProduct();
                case 4 -> { return; }
            }
        }
    }

    private void showAllStock() {
        try {
            List<Product> products = context.getInventoryService().getAllProductsWithStock();
            displayProductList(products, "ВСЕ ТОВАРЫ С ОСТАТКАМИ");
        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    private void showLowStock() {
        try {
            List<Product> products = context.getInventoryService().getLowStockProducts();
            
            if (products.isEmpty()) {
                ConsoleFormatter.printSuccess("Нет товаров с низким остатком!");
                pressEnterToContinue();
                return;
            }
            
            displayProductList(products, "ЗАКАНЧИВАЮЩИЕСЯ ТОВАРЫ");
        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    private void searchProduct() {
        try {
            String query = input.readNonEmptyString("Поиск по названию");
            List<Product> products = context.getProductService().searchByName(query);
            
            if (products.isEmpty()) {
                ConsoleFormatter.printInfo("Товары не найдены");
                pressEnterToContinue();
                return;
            }
            
            displayProductList(products, "РЕЗУЛЬТАТЫ ПОИСКА");
        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    private void displayProductList(List<Product> products, String title) {
        ConsoleFormatter.printHeader(title);

        if (products.isEmpty()) {
            ConsoleFormatter.printInfo("Нет данных");
            pressEnterToContinue();
            return;
        }

        String[] headers = {"ID", "Артикул", "Название", "Цена", "Остаток", "Мин.ост."};
        List<String[]> rows = new ArrayList<>();

        for (Product p : products) {
            String stockStr = p.getStockQuantity() != null ? String.valueOf(p.getStockQuantity()) : "0";
            if (p.isLowStock()) {
                stockStr += " ⚠";
            }
            
            rows.add(new String[]{
                    String.valueOf(p.getId()),
                    p.getSku(),
                    truncate(p.getName(), 25),
                    ConsoleFormatter.formatMoney(p.getSellingPrice()),
                    stockStr,
                    String.valueOf(p.getMinStockLevel())
            });
        }

        ConsoleFormatter.printTable(headers, rows);
        pressEnterToContinue();
    }

    

    private void handleProductManagement() {
        while (true) {
            ConsoleFormatter.printHeader("УПРАВЛЕНИЕ ТОВАРАМИ");
            System.out.println("  1. Создать новый товар");
            System.out.println("  2. Изменить цену товара");
            System.out.println("  3. Просмотр товара");
            System.out.println("  4. Назад");
            System.out.println();

            int choice = input.readIntInRange("Выберите пункт", 1, 4);

            switch (choice) {
                case 1 -> createProduct();
                case 2 -> changePrice();
                case 3 -> viewProduct();
                case 4 -> { return; }
            }
        }
    }

    private void createProduct() {
        try {
            ConsoleFormatter.printHeader("СОЗДАНИЕ ТОВАРА");

            String sku = input.readNonEmptyString("Артикул (SKU)");
            String name = input.readNonEmptyString("Название");
            String description = input.readOptionalString("Описание");
            BigDecimal purchasePrice = input.readDecimal("Цена закупки");
            BigDecimal sellingPrice = input.readPositiveDecimal("Цена продажи");
            int minStock = input.readInt("Минимальный остаток (для уведомлений)");

            
            List<Category> categories = context.getProductService().findAllCategories();
            Integer categoryId = null;
            if (!categories.isEmpty()) {
                System.out.println("\nКатегории:");
                for (Category c : categories) {
                    System.out.println("  " + c.getId() + ". " + c.getName());
                }
                System.out.println("  0. Без категории");
                int catChoice = input.readIntInRange("Категория", 0, categories.size());
                if (catChoice > 0) {
                    categoryId = categories.get(catChoice - 1).getId();
                }
            }

            
            var units = context.getProductService().findAllUnits();
            Integer unitId = null;
            if (!units.isEmpty()) {
                System.out.println("\nЕдиницы измерения:");
                for (var u : units) {
                    System.out.println("  " + u.getId() + ". " + u.getName() + " (" + u.getShortName() + ")");
                }
                System.out.println("  0. Не указывать");
                int unitChoice = input.readIntInRange("Единица", 0, units.size());
                if (unitChoice > 0) {
                    unitId = units.get(unitChoice - 1).getId();
                }
            }

            Product product = new Product(sku, name, sellingPrice);
            product.setDescription(description.isEmpty() ? null : description);
            product.setPurchasePrice(purchasePrice);
            product.setMinStockLevel(minStock);
            product.setCategoryId(categoryId);
            product.setUnitId(unitId);

            product = context.getProductService().createProduct(product);
            showSuccessAndWait("Товар создан: " + product.getName() + " (ID: " + product.getId() + ")");

        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    private void changePrice() {
        try {
            int productId = input.readPositiveInt("ID товара");
            
            var productOpt = context.getProductService().findById(productId);
            if (productOpt.isEmpty()) {
                showErrorAndWait("Товар не найден");
                return;
            }
            
            Product product = productOpt.get();
            System.out.println("Товар: " + product.getName());
            System.out.println("Текущая цена: " + ConsoleFormatter.formatMoney(product.getSellingPrice()));
            
            BigDecimal newPrice = input.readPositiveDecimal("Новая цена");
            
            if (input.readYesNo("Изменить цену?")) {
                context.getProductService().updatePrice(productId, newPrice);
                showSuccessAndWait("Цена изменена");
            }

        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    private void viewProduct() {
        try {
            int productId = input.readPositiveInt("ID товара");
            
            var productOpt = context.getProductService().findById(productId);
            if (productOpt.isEmpty()) {
                showErrorAndWait("Товар не найден");
                return;
            }
            
            Product p = productOpt.get();
            
            ConsoleFormatter.printHeader("ИНФОРМАЦИЯ О ТОВАРЕ");
            System.out.println("ID:           " + p.getId());
            System.out.println("Артикул:      " + p.getSku());
            System.out.println("Название:     " + p.getName());
            System.out.println("Описание:     " + (p.getDescription() != null ? p.getDescription() : "-"));
            System.out.println("Категория:    " + (p.getCategoryName() != null ? p.getCategoryName() : "-"));
            System.out.println("Ед. изм.:     " + (p.getUnitName() != null ? p.getUnitName() : "-"));
            System.out.println("Цена закупки: " + ConsoleFormatter.formatMoney(p.getPurchasePrice()));
            System.out.println("Цена продажи: " + ConsoleFormatter.formatMoney(p.getSellingPrice()));
            System.out.println("Остаток:      " + (p.getStockQuantity() != null ? p.getStockQuantity() : 0));
            System.out.println("Мин. остаток: " + p.getMinStockLevel());
            
            pressEnterToContinue();

        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    

    private void handleReceiptGoods() {
        try {
            ConsoleFormatter.printHeader("ПРИЕМКА ТОВАРА");

            String supplier = input.readOptionalString("Поставщик");
            String notes = input.readOptionalString("Примечания");

            Receipt receipt = new Receipt(context.getCurrentUserId());
            receipt.setSupplierInfo(supplier.isEmpty() ? null : supplier);
            receipt.setNotes(notes.isEmpty() ? null : notes);

            System.out.println("\nДобавление позиций (введите 0 для завершения):");

            while (true) {
                int productId = input.readInt("ID товара (0 - завершить)");
                if (productId == 0) break;

                var productOpt = context.getProductService().findById(productId);
                if (productOpt.isEmpty()) {
                    ConsoleFormatter.printError("Товар не найден");
                    continue;
                }

                Product product = productOpt.get();
                System.out.println("  Товар: " + product.getName());

                int quantity = input.readPositiveInt("Количество");
                BigDecimal price = input.readDecimal("Цена закупки");

                ReceiptItem item = new ReceiptItem(productId, quantity, price);
                receipt.addItem(item);

                ConsoleFormatter.printSuccess("Добавлено: " + product.getName() + " x " + quantity);
            }

            if (receipt.getItems().isEmpty()) {
                ConsoleFormatter.printInfo("Приемка отменена - нет позиций");
                pressEnterToContinue();
                return;
            }

            System.out.println("\n--- Итого позиций: " + receipt.getItems().size());
            System.out.println("--- Сумма: " + ConsoleFormatter.formatMoney(receipt.getTotalAmount()));

            if (input.readYesNo("Провести приемку?")) {
                receipt = context.getInventoryService().createReceipt(receipt, context.getCurrentUserId());
                showSuccessAndWait("Приемка проведена! Номер: " + receipt.getReceiptNumber());
            }

        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    

    private void handleInventoryAdjustment() {
        try {
            ConsoleFormatter.printHeader("КОРРЕКТИРОВКА ОСТАТКА (ИНВЕНТАРИЗАЦИЯ)");

            int productId = input.readPositiveInt("ID товара");

            var productOpt = context.getProductService().findById(productId);
            if (productOpt.isEmpty()) {
                showErrorAndWait("Товар не найден");
                return;
            }

            Product product = productOpt.get();
            int currentStock = context.getInventoryService().getStock(productId);

            System.out.println("Товар: " + product.getName());
            System.out.println("Текущий остаток: " + currentStock);

            int newQuantity = input.readInt("Фактический остаток");
            String reason = input.readNonEmptyString("Причина корректировки");

            int diff = newQuantity - currentStock;
            System.out.println("Изменение: " + (diff >= 0 ? "+" : "") + diff);

            if (input.readYesNo("Подтвердить корректировку?")) {
                context.getInventoryService().adjustStock(productId, newQuantity, reason, context.getCurrentUserId());
                showSuccessAndWait("Остаток скорректирован");
            }

        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    

    private void handleReports() {
        while (true) {
            ConsoleFormatter.printHeader("ОТЧЕТЫ");
            System.out.println("  1. Выручка за сегодня");
            System.out.println("  2. Выручка за месяц");
            System.out.println("  3. Выручка за период");
            System.out.println("  4. Топ продаваемых товаров");
            System.out.println("  5. Отчет по остаткам");
            System.out.println("  6. Журнал корректировок");
            System.out.println("  7. Назад");
            System.out.println();

            int choice = input.readIntInRange("Выберите пункт", 1, 7);

            switch (choice) {
                case 1 -> showTodayRevenue();
                case 2 -> showMonthRevenue();
                case 3 -> showPeriodRevenue();
                case 4 -> showTopProducts();
                case 5 -> showStockReport();
                case 6 -> showAdjustmentLog();
                case 7 -> { return; }
            }
        }
    }

    private void showTodayRevenue() {
        try {
            BigDecimal revenue = context.getReportService().getTodayRevenue();
            ConsoleFormatter.printHeader("ВЫРУЧКА ЗА СЕГОДНЯ");
            System.out.println("Дата: " + ConsoleFormatter.formatDate(LocalDate.now()));
            System.out.println("Выручка: " + ConsoleFormatter.formatMoney(revenue));
            pressEnterToContinue();
        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    private void showMonthRevenue() {
        try {
            BigDecimal revenue = context.getReportService().getMonthRevenue();
            ConsoleFormatter.printHeader("ВЫРУЧКА ЗА МЕСЯЦ");
            System.out.println("Месяц: " + LocalDate.now().getMonth().name());
            System.out.println("Выручка: " + ConsoleFormatter.formatMoney(revenue));
            pressEnterToContinue();
        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    private void showPeriodRevenue() {
        try {
            LocalDate startDate = input.readDate("Дата начала");
            LocalDate endDate = input.readDate("Дата окончания");

            BigDecimal revenue = context.getReportService().getRevenueByPeriod(
                    startDate.atStartOfDay(),
                    endDate.plusDays(1).atStartOfDay());

            ConsoleFormatter.printHeader("ВЫРУЧКА ЗА ПЕРИОД");
            System.out.println("Период: " + ConsoleFormatter.formatDate(startDate) +
                    " - " + ConsoleFormatter.formatDate(endDate));
            System.out.println("Выручка: " + ConsoleFormatter.formatMoney(revenue));
            pressEnterToContinue();

        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    private void showTopProducts() {
        try {
            int limit = input.readIntInRange("Количество товаров в топе", 1, 50);
            List<Object[]> topProducts = context.getReportService().getTopSellingProducts(limit);

            if (topProducts.isEmpty()) {
                ConsoleFormatter.printInfo("Нет данных о продажах");
                pressEnterToContinue();
                return;
            }

            ConsoleFormatter.printHeader("ТОП ПРОДАВАЕМЫХ ТОВАРОВ");

            String[] headers = {"#", "Артикул", "Название", "Продано", "Выручка"};
            List<String[]> rows = new ArrayList<>();

            int rank = 1;
            for (Object[] row : topProducts) {
                rows.add(new String[]{
                        String.valueOf(rank++),
                        (String) row[1],
                        truncate((String) row[2], 25),
                        String.valueOf(row[3]),
                        ConsoleFormatter.formatMoney((BigDecimal) row[4])
                });
            }

            ConsoleFormatter.printTable(headers, rows);
            pressEnterToContinue();

        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    private void showStockReport() {
        showAllStock();
    }

    private void showAdjustmentLog() {
        try {
            List<StockLog> logs = context.getReportService().getAdjustmentLog();

            if (logs.isEmpty()) {
                ConsoleFormatter.printInfo("Нет записей корректировок");
                pressEnterToContinue();
                return;
            }

            ConsoleFormatter.printHeader("ЖУРНАЛ КОРРЕКТИРОВОК");

            String[] headers = {"Дата", "Товар", "Тип", "Изм.", "Было", "Стало", "Кто"};
            List<String[]> rows = new ArrayList<>();

            for (StockLog log : logs) {
                rows.add(new String[]{
                        ConsoleFormatter.formatDateTime(log.getCreatedAt()),
                        truncate(log.getProductName(), 20),
                        log.getOperationType().getDisplayName(),
                        String.format("%+d", log.getQuantityChange()),
                        String.valueOf(log.getQuantityBefore()),
                        String.valueOf(log.getQuantityAfter()),
                        truncate(log.getUserName(), 15)
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

package com.retail.cli.handler;

import com.retail.cli.ConsoleFormatter;
import com.retail.cli.ConsoleInput;
import com.retail.cli.SessionContext;
import com.retail.exception.InsufficientStockException;
import com.retail.model.Product;
import com.retail.model.Sale;
import com.retail.model.SaleItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class EmployeeMenuHandler extends BaseMenuHandler {

    public EmployeeMenuHandler(SessionContext context, ConsoleInput input) {
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
        return "МЕНЮ СОТРУДНИКА (КАССА)";
    }

    @Override
    protected String[] getMenuItems() {
        return new String[]{
                "Новая продажа",
                "Поиск товара (цена/наличие)",
                "Оформить возврат",
                "Продажи за сегодня",
                "Выйти в главное меню"
        };
    }

    @Override
    protected boolean processChoice(int choice) {
        switch (choice) {
            case 1 -> handleNewSale();
            case 2 -> handleProductSearch();
            case 3 -> handleReturn();
            case 4 -> showTodaySales();
            case 5 -> { return true; }
        }
        return false;
    }

    

    private void handleNewSale() {
        try {
            ConsoleFormatter.printHeader("НОВАЯ ПРОДАЖА");
            System.out.println("Добавляйте товары в чек. Введите 0 для завершения и оплаты.");
            System.out.println();

            Sale sale = new Sale(context.getCurrentUserId());
            List<CartItem> cart = new ArrayList<>();

            while (true) {
                
                if (!cart.isEmpty()) {
                    showCart(cart);
                }

                System.out.println("\nКоманды: [ID товара] - добавить, [0] - оплата, [У] - удалить последний");
                String input_str = input.readString("Действие");

                if (input_str.equalsIgnoreCase("У") || input_str.equalsIgnoreCase("D")) {
                    if (!cart.isEmpty()) {
                        CartItem removed = cart.remove(cart.size() - 1);
                        ConsoleFormatter.printInfo("Удалено: " + removed.productName);
                    }
                    continue;
                }

                int productId;
                try {
                    productId = Integer.parseInt(input_str);
                } catch (NumberFormatException e) {
                    ConsoleFormatter.printError("Введите ID товара или команду");
                    continue;
                }

                if (productId == 0) {
                    break;
                }

                
                Optional<Product> productOpt = context.getProductService().findById(productId);
                if (productOpt.isEmpty()) {
                    ConsoleFormatter.printError("Товар не найден");
                    continue;
                }

                Product product = productOpt.get();
                int stock = context.getInventoryService().getStock(productId);

                System.out.println("  " + product.getName() + " | Цена: " + 
                        ConsoleFormatter.formatMoney(product.getSellingPrice()) +
                        " | Остаток: " + stock);

                if (stock == 0) {
                    ConsoleFormatter.printError("Товар отсутствует на складе!");
                    continue;
                }

                int quantity = input.readPositiveInt("Количество");

                if (quantity > stock) {
                    ConsoleFormatter.printError("Недостаточно товара! Доступно: " + stock);
                    continue;
                }

                
                CartItem cartItem = new CartItem();
                cartItem.productId = product.getId();
                cartItem.productName = product.getName();
                cartItem.price = product.getSellingPrice();
                cartItem.quantity = quantity;
                cart.add(cartItem);

                ConsoleFormatter.printSuccess("Добавлено: " + product.getName() + " x " + quantity);
            }

            
            if (cart.isEmpty()) {
                ConsoleFormatter.printInfo("Чек пуст. Продажа отменена.");
                pressEnterToContinue();
                return;
            }

            
            showCart(cart);

            BigDecimal total = cart.stream()
                    .map(i -> i.price.multiply(BigDecimal.valueOf(i.quantity)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            System.out.println("\n══════════════════════════════════════");
            System.out.println("ИТОГО К ОПЛАТЕ: " + ConsoleFormatter.formatMoney(total));
            System.out.println("══════════════════════════════════════");

            if (!input.readYesNo("Провести продажу?")) {
                ConsoleFormatter.printInfo("Продажа отменена");
                pressEnterToContinue();
                return;
            }

            
            for (CartItem item : cart) {
                sale.addItem(new SaleItem(item.productId, item.quantity, item.price));
            }

            try {
                Sale completedSale = context.getSaleService().createSale(sale, context.getCurrentUserId());
                
                System.out.println();
                ConsoleFormatter.printSuccess("ПРОДАЖА ПРОВЕДЕНА!");
                System.out.println("Чек №: " + completedSale.getSaleNumber());
                System.out.println("Сумма: " + ConsoleFormatter.formatMoney(completedSale.getFinalAmount()));
                
            } catch (InsufficientStockException e) {
                ConsoleFormatter.printError("Недостаточно товара на складе. Возможно, его уже продали.");
            }

            pressEnterToContinue();

        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    private void showCart(List<CartItem> cart) {
        System.out.println("\n--- ТЕКУЩИЙ ЧЕК ---");
        
        String[] headers = {"#", "Товар", "Цена", "Кол-во", "Сумма"};
        List<String[]> rows = new ArrayList<>();

        int num = 1;
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : cart) {
            BigDecimal lineTotal = item.price.multiply(BigDecimal.valueOf(item.quantity));
            total = total.add(lineTotal);

            rows.add(new String[]{
                    String.valueOf(num++),
                    truncate(item.productName, 25),
                    ConsoleFormatter.formatMoney(item.price),
                    String.valueOf(item.quantity),
                    ConsoleFormatter.formatMoney(lineTotal)
            });
        }

        ConsoleFormatter.printSimpleTable(headers, rows);
        System.out.println("Итого: " + ConsoleFormatter.formatMoney(total));
    }

    

    private void handleProductSearch() {
        try {
            ConsoleFormatter.printHeader("ПОИСК ТОВАРА");
            System.out.println("1. По ID");
            System.out.println("2. По названию");
            
            int choice = input.readIntInRange("Способ поиска", 1, 2);

            List<Product> products;
            
            if (choice == 1) {
                int id = input.readPositiveInt("ID товара");
                Optional<Product> productOpt = context.getProductService().findById(id);
                products = productOpt.map(List::of).orElse(List.of());
            } else {
                String query = input.readNonEmptyString("Название (часть)");
                products = context.getProductService().searchByName(query);
            }

            if (products.isEmpty()) {
                ConsoleFormatter.printInfo("Товар не найден");
                pressEnterToContinue();
                return;
            }

            displayProducts(products);

        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    private void displayProducts(List<Product> products) {
        String[] headers = {"ID", "Артикул", "Название", "Цена", "Остаток"};
        List<String[]> rows = new ArrayList<>();

        for (Product p : products) {
            int stock = p.getStockQuantity() != null ? p.getStockQuantity() : 0;
            String stockStr = stock > 0 ? String.valueOf(stock) : "НЕТ";
            
            rows.add(new String[]{
                    String.valueOf(p.getId()),
                    p.getSku(),
                    truncate(p.getName(), 30),
                    ConsoleFormatter.formatMoney(p.getSellingPrice()),
                    stockStr
            });
        }

        ConsoleFormatter.printTable(headers, rows);
        pressEnterToContinue();
    }

    

    private void handleReturn() {
        try {
            ConsoleFormatter.printHeader("ОФОРМЛЕНИЕ ВОЗВРАТА");

            String saleNumber = input.readNonEmptyString("Номер чека");

            Optional<Sale> saleOpt = context.getSaleService().findBySaleNumber(saleNumber);
            if (saleOpt.isEmpty()) {
                showErrorAndWait("Чек не найден");
                return;
            }

            Sale sale = saleOpt.get();

            if (sale.isReturned()) {
                showErrorAndWait("Этот чек уже полностью возвращен");
                return;
            }

            
            List<SaleItem> returnableItems = context.getSaleService().getReturnableItems(sale.getId());

            if (returnableItems.isEmpty()) {
                showErrorAndWait("Нет позиций для возврата");
                return;
            }

            
            System.out.println("\nЧек: " + sale.getSaleNumber());
            System.out.println("Дата: " + ConsoleFormatter.formatDateTime(sale.getSaleDate()));
            System.out.println("Сумма: " + ConsoleFormatter.formatMoney(sale.getFinalAmount()));
            System.out.println("\nПозиции, доступные для возврата:");

            String[] headers = {"#", "ID", "Товар", "Куплено", "Возвращено", "Можно вернуть"};
            List<String[]> rows = new ArrayList<>();

            int num = 1;
            for (SaleItem item : returnableItems) {
                rows.add(new String[]{
                        String.valueOf(num++),
                        String.valueOf(item.getId()),
                        item.getProductName(),
                        String.valueOf(item.getQuantity()),
                        String.valueOf(item.getReturnedQty()),
                        String.valueOf(item.getReturnableQty())
                });
            }

            ConsoleFormatter.printTable(headers, rows);

            
            int itemId = input.readPositiveInt("ID позиции для возврата");
            
            SaleItem itemToReturn = returnableItems.stream()
                    .filter(i -> i.getId().equals(itemId))
                    .findFirst()
                    .orElse(null);

            if (itemToReturn == null) {
                showErrorAndWait("Позиция не найдена");
                return;
            }

            int maxReturn = itemToReturn.getReturnableQty();
            int returnQty = input.readIntInRange("Количество для возврата (макс: " + maxReturn + ")", 1, maxReturn);

            BigDecimal refundAmount = itemToReturn.getPriceAtSale().multiply(BigDecimal.valueOf(returnQty));
            System.out.println("Сумма возврата: " + ConsoleFormatter.formatMoney(refundAmount));

            if (input.readYesNo("Подтвердить возврат?")) {
                context.getSaleService().processReturn(
                        sale.getId(), itemId, returnQty, context.getCurrentUserId());
                
                showSuccessAndWait("Возврат оформлен. Сумма к выдаче: " + 
                        ConsoleFormatter.formatMoney(refundAmount));
            }

        } catch (Exception e) {
            showErrorAndWait(e.getMessage());
        }
    }

    

    private void showTodaySales() {
        try {
            List<Sale> sales = context.getSaleService().findTodaySales();

            if (sales.isEmpty()) {
                ConsoleFormatter.printInfo("Сегодня продаж не было");
                pressEnterToContinue();
                return;
            }

            ConsoleFormatter.printHeader("ПРОДАЖИ ЗА СЕГОДНЯ");

            String[] headers = {"Чек", "Время", "Сотрудник", "Сумма", "Возврат"};
            List<String[]> rows = new ArrayList<>();

            BigDecimal total = BigDecimal.ZERO;

            for (Sale sale : sales) {
                if (!sale.isReturned()) {
                    total = total.add(sale.getFinalAmount());
                }
                
                rows.add(new String[]{
                        sale.getSaleNumber(),
                        ConsoleFormatter.formatDateTime(sale.getSaleDate()),
                        truncate(sale.getEmployeeName(), 15),
                        ConsoleFormatter.formatMoney(sale.getFinalAmount()),
                        sale.isReturned() ? "Да" : "-"
                });
            }

            ConsoleFormatter.printTable(headers, rows);
            System.out.println("\nВсего продаж: " + sales.size());
            System.out.println("Общая выручка: " + ConsoleFormatter.formatMoney(total));
            
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

    
    private static class CartItem {
        int productId;
        String productName;
        BigDecimal price;
        int quantity;
    }
}

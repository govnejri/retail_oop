package com.retail.cli;

import java.util.List;


public class ConsoleFormatter {
    
    private static final String HORIZONTAL_LINE = "─";
    private static final String VERTICAL_LINE = "│";
    private static final String CORNER_TL = "┌";
    private static final String CORNER_TR = "┐";
    private static final String CORNER_BL = "└";
    private static final String CORNER_BR = "┘";
    private static final String T_DOWN = "┬";
    private static final String T_UP = "┴";
    private static final String T_RIGHT = "├";
    private static final String T_LEFT = "┤";
    private static final String CROSS = "┼";

    
    public static void printTable(String[] headers, List<String[]> rows) {
        if (headers == null || headers.length == 0) {
            return;
        }

        int[] widths = calculateColumnWidths(headers, rows);
        
        
        System.out.println(buildHorizontalLine(widths, CORNER_TL, T_DOWN, CORNER_TR));
        
        
        System.out.println(buildRow(headers, widths));
        
        
        System.out.println(buildHorizontalLine(widths, T_RIGHT, CROSS, T_LEFT));
        
        
        for (String[] row : rows) {
            System.out.println(buildRow(row, widths));
        }
        
        
        System.out.println(buildHorizontalLine(widths, CORNER_BL, T_UP, CORNER_BR));
    }

    
    public static void printSimpleTable(String[] headers, List<String[]> rows) {
        if (headers == null || headers.length == 0) {
            return;
        }

        int[] widths = calculateColumnWidths(headers, rows);
        
        
        StringBuilder headerLine = new StringBuilder();
        for (int i = 0; i < headers.length; i++) {
            headerLine.append(padRight(headers[i], widths[i]));
            if (i < headers.length - 1) {
                headerLine.append("  ");
            }
        }
        System.out.println(headerLine);
        
        
        StringBuilder separator = new StringBuilder();
        for (int i = 0; i < headers.length; i++) {
            separator.append("-".repeat(widths[i]));
            if (i < headers.length - 1) {
                separator.append("  ");
            }
        }
        System.out.println(separator);
        
        
        for (String[] row : rows) {
            StringBuilder rowLine = new StringBuilder();
            for (int i = 0; i < row.length && i < widths.length; i++) {
                rowLine.append(padRight(row[i] != null ? row[i] : "", widths[i]));
                if (i < row.length - 1) {
                    rowLine.append("  ");
                }
            }
            System.out.println(rowLine);
        }
    }

    private static int[] calculateColumnWidths(String[] headers, List<String[]> rows) {
        int[] widths = new int[headers.length];
        
        
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }
        
        
        for (String[] row : rows) {
            for (int i = 0; i < row.length && i < widths.length; i++) {
                if (row[i] != null && row[i].length() > widths[i]) {
                    widths[i] = row[i].length();
                }
            }
        }
        
        return widths;
    }

    private static String buildHorizontalLine(int[] widths, String left, String middle, String right) {
        StringBuilder sb = new StringBuilder(left);
        for (int i = 0; i < widths.length; i++) {
            sb.append(HORIZONTAL_LINE.repeat(widths[i] + 2));
            if (i < widths.length - 1) {
                sb.append(middle);
            }
        }
        sb.append(right);
        return sb.toString();
    }

    private static String buildRow(String[] data, int[] widths) {
        StringBuilder sb = new StringBuilder(VERTICAL_LINE);
        for (int i = 0; i < widths.length; i++) {
            String value = (i < data.length && data[i] != null) ? data[i] : "";
            sb.append(" ").append(padRight(value, widths[i])).append(" ");
            sb.append(VERTICAL_LINE);
        }
        return sb.toString();
    }

    private static String padRight(String s, int width) {
        if (s.length() >= width) {
            return s.substring(0, width);
        }
        return s + " ".repeat(width - s.length());
    }

    
    public static void printSeparator() {
        System.out.println("═".repeat(60));
    }

    
    public static void printHeader(String title) {
        System.out.println();
        System.out.println("╔" + "═".repeat(title.length() + 2) + "╗");
        System.out.println("║ " + title + " ║");
        System.out.println("╚" + "═".repeat(title.length() + 2) + "╝");
        System.out.println();
    }

    
    public static void printSuccess(String message) {
        System.out.println("✓ " + message);
    }

    
    public static void printError(String message) {
        System.out.println("✗ Ошибка: " + message);
    }

    
    public static void printWarning(String message) {
        System.out.println("⚠ " + message);
    }

    
    public static void printInfo(String message) {
        System.out.println("ℹ " + message);
    }

    
    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    
    public static String formatMoney(java.math.BigDecimal amount) {
        if (amount == null) {
            return "0.00 ₽";
        }
        return String.format("%,.2f ₽", amount);
    }

    
    public static String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    
    public static String formatDate(java.time.LocalDate date) {
        if (date == null) {
            return "-";
        }
        return date.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }
}

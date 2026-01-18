package com.retail.cli;

import java.util.Scanner;


public class ConsoleInput {
    
    private final Scanner scanner;

    public ConsoleInput(Scanner scanner) {
        this.scanner = scanner;
    }

    
    public String readString(String prompt) {
        System.out.print(prompt + ": ");
        return scanner.nextLine().trim();
    }

    
    public String readNonEmptyString(String prompt) {
        while (true) {
            String input = readString(prompt);
            if (!input.isEmpty()) {
                return input;
            }
            ConsoleFormatter.printError("Значение не может быть пустым");
        }
    }

    
    public int readInt(String prompt) {
        while (true) {
            String input = readString(prompt);
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                ConsoleFormatter.printError("Введите целое число");
            }
        }
    }

    
    public int readPositiveInt(String prompt) {
        while (true) {
            int value = readInt(prompt);
            if (value > 0) {
                return value;
            }
            ConsoleFormatter.printError("Число должно быть положительным");
        }
    }

    
    public int readIntInRange(String prompt, int min, int max) {
        while (true) {
            int value = readInt(prompt);
            if (value >= min && value <= max) {
                return value;
            }
            ConsoleFormatter.printError("Число должно быть от " + min + " до " + max);
        }
    }

    
    public java.math.BigDecimal readDecimal(String prompt) {
        while (true) {
            String input = readString(prompt);
            try {
                java.math.BigDecimal value = new java.math.BigDecimal(input);
                if (value.compareTo(java.math.BigDecimal.ZERO) >= 0) {
                    return value;
                }
                ConsoleFormatter.printError("Число не может быть отрицательным");
            } catch (NumberFormatException e) {
                ConsoleFormatter.printError("Введите число (например: 199.99)");
            }
        }
    }

    
    public java.math.BigDecimal readPositiveDecimal(String prompt) {
        while (true) {
            java.math.BigDecimal value = readDecimal(prompt);
            if (value.compareTo(java.math.BigDecimal.ZERO) > 0) {
                return value;
            }
            ConsoleFormatter.printError("Число должно быть положительным");
        }
    }

    
    public boolean readYesNo(String prompt) {
        while (true) {
            String input = readString(prompt + " (да/нет)").toLowerCase();
            if (input.equals("да") || input.equals("д") || input.equals("yes") || input.equals("y")) {
                return true;
            }
            if (input.equals("нет") || input.equals("н") || input.equals("no") || input.equals("n")) {
                return false;
            }
            ConsoleFormatter.printError("Введите 'да' или 'нет'");
        }
    }

    
    public String readPassword(String prompt) {
        System.out.print(prompt + ": ");
        
        
        return scanner.nextLine();
    }

    
    public String readOptionalString(String prompt) {
        return readString(prompt + " (необязательно)");
    }

    
    public java.time.LocalDate readDate(String prompt) {
        while (true) {
            String input = readString(prompt + " (ДД.ММ.ГГГГ)");
            try {
                return java.time.LocalDate.parse(input, 
                        java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            } catch (java.time.format.DateTimeParseException e) {
                ConsoleFormatter.printError("Неверный формат даты. Используйте ДД.ММ.ГГГГ");
            }
        }
    }

    
    public void waitForEnter() {
        System.out.print("\nНажмите Enter для продолжения...");
        scanner.nextLine();
    }

    
    public void waitForEnter(String message) {
        System.out.print("\n" + message);
        scanner.nextLine();
    }
}

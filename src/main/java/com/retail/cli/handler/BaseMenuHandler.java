package com.retail.cli.handler;

import com.retail.cli.ConsoleFormatter;
import com.retail.cli.ConsoleInput;
import com.retail.cli.SessionContext;


public abstract class BaseMenuHandler {
    
    protected final SessionContext context;
    protected final ConsoleInput input;

    protected BaseMenuHandler(SessionContext context, ConsoleInput input) {
        this.context = context;
        this.input = input;
    }

    
    public abstract boolean handle();

    
    protected abstract String getMenuTitle();

    
    protected abstract String[] getMenuItems();

    
    protected abstract boolean processChoice(int choice);

    
    protected int showMenu() {
        ConsoleFormatter.printHeader(getMenuTitle());
        
        String[] items = getMenuItems();
        for (int i = 0; i < items.length; i++) {
            System.out.println("  " + (i + 1) + ". " + items[i]);
        }
        System.out.println();
        
        return input.readIntInRange("Выберите пункт", 1, items.length);
    }

    
    protected void pressEnterToContinue() {
        input.waitForEnter();
    }

    
    protected void showErrorAndWait(String message) {
        ConsoleFormatter.printError(message);
        input.waitForEnter();
    }

    
    protected void showSuccessAndWait(String message) {
        ConsoleFormatter.printSuccess(message);
        input.waitForEnter();
    }
}

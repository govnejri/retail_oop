package com.retail.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        String[] passwords = {"admin123", "manager123", "cashier123"};
        for (String pwd : passwords) {
            String hash = BCrypt.hashpw(pwd, BCrypt.gensalt(10));
            System.out.println(pwd + " -> " + hash);
        }
    }
}

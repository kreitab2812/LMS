package com.lms.quanlythuvien.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {

    // Tạo "salt" ngẫu nhiên và băm mật khẩu
    public static String hashPassword(String plainTextPassword) {
        if (plainTextPassword == null || plainTextPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
    }

    // Kiểm tra mật khẩu gốc với mật khẩu đã được băm
    public static boolean verifyPassword(String plainTextPassword, String hashedPassword) {
        if (plainTextPassword == null || plainTextPassword.isEmpty() ||
                hashedPassword == null || hashedPassword.isEmpty()) {
            return false; // Hoặc ném exception tùy theo logic bạn muốn
        }
        try {
            return BCrypt.checkpw(plainTextPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Xảy ra nếu hashedPassword không phải là định dạng BCrypt hợp lệ
            System.err.println("Error verifying password: " + e.getMessage());
            return false;
        }
    }
}
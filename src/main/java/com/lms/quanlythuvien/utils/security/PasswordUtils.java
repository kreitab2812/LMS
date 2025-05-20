package com.lms.quanlythuvien.utils.security;

import org.mindrot.jbcrypt.BCrypt; // Đảm bảo thư viện jBCrypt có trong dependencies

public class PasswordUtils {

    // Số vòng lặp (log rounds) cho BCrypt. Giá trị càng cao, càng an toàn nhưng càng chậm.
    // 10-12 là giá trị phổ biến hiện nay.
    private static final int LOG_ROUNDS = 12;

    /**
     * Băm mật khẩu sử dụng BCrypt.
     * @param plainTextPassword Mật khẩu dạng text thuần.
     * @return Chuỗi mật khẩu đã được băm.
     * @throws IllegalArgumentException nếu mật khẩu rỗng hoặc null.
     */
    public static String hashPassword(String plainTextPassword) {
        if (plainTextPassword == null || plainTextPassword.isEmpty()) {
            // Ném Exception rõ ràng hơn thay vì dựa vào BCrypt tự ném
            throw new IllegalArgumentException("Password to hash cannot be null or empty.");
        }
        try {
            // BCrypt.gensalt() mặc định dùng 10 vòng. Có thể truyền tham số để tăng độ mạnh.
            return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(LOG_ROUNDS));
        } catch (Exception e) {
            // Xử lý các lỗi không mong muốn từ thư viện BCrypt (dù hiếm)
            System.err.println("CRITICAL_PWD_UTILS: Unexpected error during password hashing: " + e.getMessage());
            // e.printStackTrace();
            // Trong trường hợp này, việc ném một RuntimeException có thể là phù hợp
            // vì không thể tiếp tục một cách an toàn nếu việc băm mật khẩu thất bại.
            throw new RuntimeException("Failed to hash password due to an internal error.", e);
        }
    }

    /**
     * Xác minh mật khẩu text thuần với một chuỗi đã được băm bằng BCrypt.
     * @param plainTextPassword Mật khẩu dạng text thuần cần kiểm tra.
     * @param hashedPassword Chuỗi mật khẩu đã được băm từ trước.
     * @return true nếu mật khẩu khớp, false nếu không khớp hoặc có lỗi.
     */
    public static boolean verifyPassword(String plainTextPassword, String hashedPassword) {
        if (plainTextPassword == null || plainTextPassword.isEmpty()) {
            System.err.println("WARN_PWD_UTILS_VERIFY: Plain text password for verification is null or empty.");
            return false;
        }
        if (hashedPassword == null || hashedPassword.isEmpty()) {
            System.err.println("WARN_PWD_UTILS_VERIFY: Hashed password for verification is null or empty.");
            return false;
        }

        try {
            return BCrypt.checkpw(plainTextPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Lỗi này thường xảy ra nếu hashedPassword không phải là một chuỗi hash BCrypt hợp lệ.
            System.err.println("ERROR_PWD_UTILS_VERIFY: Invalid hashed password format or an error during verification. " + e.getMessage());
            // e.printStackTrace(); // Có thể bật để xem chi tiết lỗi từ BCrypt
            return false;
        } catch (Exception e) {
            // Bắt các lỗi không mong muốn khác
            System.err.println("CRITICAL_PWD_UTILS_VERIFY: Unexpected error during password verification: " + e.getMessage());
            // e.printStackTrace();
            return false; // An toàn nhất là trả về false nếu có lỗi không xác định
        }
    }
}
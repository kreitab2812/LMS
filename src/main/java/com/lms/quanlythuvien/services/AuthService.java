package com.lms.quanlythuvien.services;

import com.lms.quanlythuvien.models.User;
import com.lms.quanlythuvien.models.User.Role;
import com.lms.quanlythuvien.utils.PasswordUtils; // IMPORT LỚP TIỆN ÍCH MỚI

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuthService {

    private final List<User> userList;

    private static final String ADMIN_EMAIL = "24022274@vnu.edu.vn";
    private static final String ADMIN_USERNAME = "admin_vnu";
    private static final String ADMIN_RAW_PASSWORD = "LGTV2006"; // Mật khẩu gốc chỉ dùng để khởi tạo lần đầu

    public AuthService() {
        this.userList = new ArrayList<>();
        initializeAdminAccount();
    }

    private void initializeAdminAccount() {
        // Băm mật khẩu Admin trước khi lưu
        String adminPasswordHash = PasswordUtils.hashPassword(ADMIN_RAW_PASSWORD); // SỬ DỤNG BCRYPT

        if (findUserByEmailInternal(ADMIN_EMAIL).isEmpty()) {
            User adminUser = new User(
                    ADMIN_USERNAME,
                    ADMIN_EMAIL,
                    adminPasswordHash, // LƯU MẬT KHẨU ĐÃ BĂM
                    Role.ADMIN
            );
            this.userList.add(adminUser);
            System.out.println("Admin account initialized (with hashed password): " + ADMIN_EMAIL);
        }
    }

    public AuthResult login(String email, String password) {
        Optional<User> userOptional = findUserByEmailInternal(email);

        if (userOptional.isEmpty()) {
            return AuthResult.failure("Incorrect email or password. Please try again.");
        }

        User user = userOptional.get();

        // Xác minh mật khẩu đã băm
        if (PasswordUtils.verifyPassword(password, user.getPasswordHash())) { // SỬ DỤNG BCRYPT ĐỂ XÁC MINH
            return AuthResult.success(user);
        } else {
            return AuthResult.failure("Incorrect email or password. Please try again.");
        }
    }

    private Optional<User> findUserByEmailInternal(String email) {
        return userList.stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    public boolean isEmailTaken(String email) {
        return findUserByEmailInternal(email).isPresent();
    }

    public boolean isUsernameTaken(String username) {
        return userList.stream()
                .anyMatch(user -> user.getUsername().equalsIgnoreCase(username));
    }

    public AuthResult register(String username, String email, String rawPassword, User.Role role) {
        if (isUsernameTaken(username)) {
            return AuthResult.failure("Username already exists. Please choose another one.");
        }
        if (isEmailTaken(email)) {
            return AuthResult.failure("Email already registered. Please use a different email or login.");
        }

        // Băm mật khẩu trước khi lưu
        String hashedPassword = PasswordUtils.hashPassword(rawPassword); // SỬ DỤNG BCRYPT

        User newUser = new User(username, email, hashedPassword, role); // LƯU MẬT KHẨU ĐÃ BĂM
        userList.add(newUser);
        System.out.println("New user registered: " + newUser.getEmail() + " with role " + role + " (password hashed)");
        return AuthResult.success(newUser);
    }

    // Xóa bỏ lớp PasswordUtilsPlaceholder tĩnh nội bộ không còn cần thiết nữa
}
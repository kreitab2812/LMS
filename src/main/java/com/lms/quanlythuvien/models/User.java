package com.lms.quanlythuvien.models;

import java.util.UUID; // Để tạo ID người dùng duy nhất

public class User {

    public enum Role {
        ADMIN,
        USER // Người dùng thông thường
    }

    private String userId;
    private String username; // Tên người dùng (thay cho Reg No.)
    private String email;
    private String passwordHash; // Sẽ lưu trữ mật khẩu đã được băm
    private Role role;

    // Constructor cho việc tạo User mới (ví dụ khi đăng ký)
    public User(String username, String email, String passwordHash, Role role) {
        this.userId = UUID.randomUUID().toString(); // Tự động tạo ID duy nhất
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash; // Mật khẩu nên được băm trước khi truyền vào đây
        this.role = role;
    }

    // Constructor có thể dùng khi tải User từ database (đã có sẵn userId)
    public User(String userId, String username, String email, String passwordHash, Role role) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    // Setters (chỉ cung cấp setters cho những trường có thể thay đổi sau khi tạo)
    // Ví dụ: username, email, password có thể cho phép thay đổi. Role có thể bị hạn chế.
    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPasswordHash(String passwordHash) {
        // Nên có logic băm mật khẩu ở service trước khi gọi setter này
        this.passwordHash = passwordHash;
    }

    // Không nên cho phép thay đổi UserId sau khi đã tạo
    // public void setUserId(String userId) { this.userId = userId; }

    // Việc thay đổi Role nên được kiểm soát chặt chẽ, có thể cần một phương thức riêng với quyền hạn
    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                // Không nên in passwordHash trong toString thực tế
                ", role=" + role +
                '}';
    }
}
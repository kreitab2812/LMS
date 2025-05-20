package com.lms.quanlythuvien.models.user; // Hoặc package models của cậu

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class User {

    public enum Role { ADMIN, USER }

    private String userId;
    private String username;
    private String email;
    private String passwordHash;
    private Role role;
    private List<String> activeLoanRecordIds; // THAY ĐỔI: từ borrowedBookIds thành activeLoanRecordIds

    // Constructor cho việc tạo User mới
    public User(String username, String email, String passwordHash, Role role) {
        this.userId = UUID.randomUUID().toString();
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash; // Mật khẩu nên được băm trước khi truyền vào đây
        this.role = role;
        this.activeLoanRecordIds = new ArrayList<>(); // Khởi tạo danh sách rỗng
    }

    // Constructor có thể dùng khi tải User từ database (đã có sẵn userId)
    public User(String userId, String username, String email, String passwordHash, Role role) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.activeLoanRecordIds = new ArrayList<>(); // Sẽ cần tải danh sách này từ DB nếu có
    }

    // Getters
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public List<String> getActiveLoanRecordIds() { return activeLoanRecordIds; } // GETTER MỚI

    // Setters (chỉ cung cấp setters cho những trường có thể thay đổi sau khi tạo)
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setRole(Role role) { this.role = role; }

    // (Tùy chọn) Setter này có thể hữu ích khi tải dữ liệu người dùng cùng các active loans từ DB
    public void setActiveLoanRecordIds(List<String> activeLoanRecordIds) {
        this.activeLoanRecordIds = activeLoanRecordIds != null ? new ArrayList<>(activeLoanRecordIds) : new ArrayList<>();
    }


    // --- Phương thức hỗ trợ quản lý các bản ghi mượn sách ---
    /**
     * Thêm ID của một bản ghi mượn (loan record) vào danh sách đang hoạt động của người dùng.
     * @param loanRecordId ID của BorrowingRecord.
     */
    public void addActiveLoanRecord(String loanRecordId) { // THAY ĐỔI: từ borrowBook(bookId)
        if (loanRecordId != null && !this.activeLoanRecordIds.contains(loanRecordId)) {
            // Có thể kiểm tra borrowLimit của user ở đây trước khi thêm (nếu có)
            this.activeLoanRecordIds.add(loanRecordId);
        }
    }

    /**
     * Xóa ID của một bản ghi mượn (loan record) khỏi danh sách đang hoạt động của người dùng (khi sách được trả).
     * @param loanRecordId ID của BorrowingRecord.
     */
    public void removeActiveLoanRecord(String loanRecordId) { // THAY ĐỔI: từ returnBook(bookId)
        if (loanRecordId != null) {
            this.activeLoanRecordIds.remove(loanRecordId);
        }
    }

    // (Tùy chọn) Kiểm tra xem người dùng có đang mượn dựa trên một loanRecordId cụ thể không
    public boolean hasActiveLoanRecord(String loanRecordId) {
        return this.activeLoanRecordIds.contains(loanRecordId);
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", activeLoansCount=" + (activeLoanRecordIds != null ? activeLoanRecordIds.size() : 0) +
                '}';
    }
}
package com.lms.quanlythuvien.models.user;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; // Thêm để dùng trong get...Formatted
import java.util.ArrayList;
import java.util.List;
import java.util.Objects; // Thêm cho equals và hashCode
import java.util.UUID;   // Vẫn dùng trong constructor tối thiểu

public class User {

    public enum Role { ADMIN, USER }

    // Thông tin cơ bản
    private String userId;
    private String username;
    private String email;
    private String passwordHash;
    private Role role;

    // Thông tin cá nhân mở rộng
    private String fullName;
    private LocalDate dateOfBirth;
    private String address;
    private String phoneNumber;
    private String avatarUrl;
    private String introduction;

    // Thông tin liên quan đến thư viện
    private List<String> activeLoanRecordIds;
    private boolean isAccountLocked;
    private double currentFineAmount;
    private int reputationScore;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    // Constructor tối thiểu khi tạo User mới (ví dụ: từ RegistrationScreenController)
    // Constructor này tự tạo UUID và thời gian. UserService.addUser sẽ ghi đè userId.
    public User(String username, String email, String passwordHash, Role role) {
        this.userId = UUID.randomUUID().toString(); // ID tạm thời, sẽ được UserService ghi đè nếu cần
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.activeLoanRecordIds = new ArrayList<>();
        this.isAccountLocked = false;
        this.currentFineAmount = 0.0;
        this.reputationScore = 80; // Điểm mặc định
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Constructor đầy đủ (ví dụ: khi tải từ DB hoặc khi UserService tạo User mới)
    public User(String userId, String username, String email, String passwordHash, Role role,
                String fullName, LocalDate dateOfBirth, String address, String phoneNumber,
                String avatarUrl, String introduction,
                boolean isAccountLocked, double currentFineAmount, int reputationScore,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId; // ID này từ DB hoặc do UserService.generateNewUserId() tạo
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.activeLoanRecordIds = new ArrayList<>();

        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
        this.introduction = introduction;

        this.isAccountLocked = isAccountLocked;
        this.currentFineAmount = currentFineAmount;
        this.reputationScore = reputationScore;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters (Giữ nguyên các getter cậu đã có)
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public String getFullName() { return fullName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getAddress() { return address; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getIntroduction() { return introduction; }
    public List<String> getActiveLoanRecordIds() { return activeLoanRecordIds; }
    public boolean isAccountLocked() { return isAccountLocked; }
    public double getCurrentFineAmount() { return currentFineAmount; }
    public int getReputationScore() { return reputationScore; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters (Giữ nguyên các setter cậu đã có, chúng tự động cập nhật `updatedAt`)
    public void setUsername(String username) { this.username = username; this.updatedAt = LocalDateTime.now(); }
    public void setEmail(String email) { this.email = email; this.updatedAt = LocalDateTime.now(); }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; this.updatedAt = LocalDateTime.now(); }
    public void setRole(Role role) { this.role = role; this.updatedAt = LocalDateTime.now(); }
    public void setFullName(String fullName) { this.fullName = fullName; this.updatedAt = LocalDateTime.now(); }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; this.updatedAt = LocalDateTime.now(); }
    public void setAddress(String address) { this.address = address; this.updatedAt = LocalDateTime.now(); }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; this.updatedAt = LocalDateTime.now(); }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; this.updatedAt = LocalDateTime.now(); }
    public void setIntroduction(String introduction) { this.introduction = introduction; this.updatedAt = LocalDateTime.now(); }
    public void setActiveLoanRecordIds(List<String> activeLoanRecordIds) { this.activeLoanRecordIds = activeLoanRecordIds != null ? new ArrayList<>(activeLoanRecordIds) : new ArrayList<>(); }
    public void setAccountLocked(boolean accountLocked) { isAccountLocked = accountLocked; this.updatedAt = LocalDateTime.now(); }
    public void setCurrentFineAmount(double currentFineAmount) { this.currentFineAmount = currentFineAmount; this.updatedAt = LocalDateTime.now(); }
    public void setReputationScore(int reputationScore) { this.reputationScore = Math.max(0, Math.min(100, reputationScore)); this.updatedAt = LocalDateTime.now(); } // Đảm bảo trong khoảng 0-100
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // --- SETTER ĐẶC BIỆT (FORCED) CHỈ DÙNG BỞI SERVICE KHI KHỞI TẠO ---
    // Cần thiết nếu constructor tối thiểu tự tạo ID/createdAt và Service muốn ghi đè.
    // Nếu Service đã gọi constructor đầy đủ với ID/createdAt đúng ngay từ đầu thì không cần các hàm "Forced" này.
    /**
     * Chỉ được dùng bởi UserService khi tạo User mới với ID đã được generate.
     * @param userId ID người dùng đã được generate.
     */
    public void forceSetUserId(String userId) {
        this.userId = userId;
    }
    /**
     * Chỉ được dùng bởi UserService khi tạo User mới với thời gian tạo đã được xác định.
     * @param createdAt Thời gian tạo.
     */
    public void forceSetCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    // Không cần forceSetUpdatedAt vì đã có setUpdatedAt() public.

    // --- CÁC PHƯƠNG THỨC TIỆN ÍCH "OrDefault" ---
    public String getUsernameOrDefault(String defaultValue) {
        return (this.username != null && !this.username.trim().isEmpty()) ? this.username.trim() : defaultValue;
    }
    public String getEmailOrDefault(String defaultValue) {
        return (this.email != null && !this.email.trim().isEmpty()) ? this.email.trim() : defaultValue;
    }
    public String getFullNameOrDefault(String defaultValue) {
        return (this.fullName != null && !this.fullName.trim().isEmpty()) ? this.fullName.trim() : defaultValue;
    }
    public String getPhoneNumberOrDefault(String defaultValue) {
        return (this.phoneNumber != null && !this.phoneNumber.trim().isEmpty()) ? this.phoneNumber.trim() : defaultValue;
    }
    public String getIntroductionOrDefault(String defaultValue) {
        return (this.introduction != null && !this.introduction.trim().isEmpty()) ? this.introduction.trim() : defaultValue;
    }
    public String getAvatarUrlOrDefault(String defaultValue) {
        return (this.avatarUrl != null && !this.avatarUrl.trim().isEmpty()) ? this.avatarUrl.trim() : defaultValue;
    }
    public String getDateOfBirthFormattedOrDefault(String defaultValue, DateTimeFormatter formatter) {
        return (this.dateOfBirth != null && formatter != null) ? this.dateOfBirth.format(formatter) : defaultValue;
    }


    // --- Các phương thức khác (addActiveLoanRecord, etc. giữ nguyên) ---
    public void addActiveLoanRecord(String loanRecordId) { /* ... */ }
    public void removeActiveLoanRecord(String loanRecordId) { /* ... */ }
    public boolean hasActiveLoanRecord(String loanRecordId) { /* ... */ return false; }
    public void increaseReputation(int points) { /* ... */ }
    public void decreaseReputation(int points) { /* ... */ }


    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", username='" + getUsernameOrDefault("N/A") + '\'' +
                ", email='" + getEmailOrDefault("N/A") + '\'' +
                ", role=" + role +
                ", fullName='" + getFullNameOrDefault("N/A") + '\'' +
                ", reputationScore=" + reputationScore +
                ", isAccountLocked=" + isAccountLocked +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        // ID người dùng là duy nhất và nên được dùng để so sánh nếu đã có
        if (userId != null && !userId.trim().isEmpty()) {
            return userId.equals(user.userId);
        }
        // Nếu không có ID, so sánh bằng username (cũng nên là duy nhất)
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        // Dùng ID nếu có, nếu không dùng username
        if (userId != null && !userId.trim().isEmpty()) {
            return Objects.hash(userId);
        }
        return Objects.hash(username);
    }
}
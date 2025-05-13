package com.lms.quanlythuvien.services;

import com.lms.quanlythuvien.models.User;
import com.lms.quanlythuvien.utils.PasswordUtils; // Cần để hash mật khẩu

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserService {

    private final List<User> users; // Danh sách TẤT CẢ người dùng (bao gồm cả admin)

    // Thông tin admin mặc định - nên được quản lý ở đây
    private static final String ADMIN_EMAIL = "24022274@vnu.edu.vn";
    private static final String ADMIN_USERNAME = "admin_vnu";
    private static final String ADMIN_RAW_PASSWORD = "LGTV2006";

    public UserService() {
        this.users = new ArrayList<>();
        initializeUsers(); // Khởi tạo admin và các user mẫu khác
    }

    private void initializeUsers() {
        System.out.println("DEBUG_US: UserService initializeUsers() started.");
        // 1. Khởi tạo tài khoản Admin mặc định nếu chưa có
        try {
            if (this.users.stream().noneMatch(u -> u.getEmail().equalsIgnoreCase(ADMIN_EMAIL))) {
                String adminPasswordHash = PasswordUtils.hashPassword(ADMIN_RAW_PASSWORD);
                User adminUser = new User(
                        ADMIN_USERNAME, // username
                        ADMIN_EMAIL,    // email
                        adminPasswordHash, // passwordHash
                        User.Role.ADMIN // role
                );
                // Nếu User constructor của cậu tự tạo ID, không cần setId.
                // Nếu cậu muốn ID cố định cho admin, User constructor cần cho phép truyền ID,
                // hoặc có setId (nhưng thường ID không nên set sau khi tạo).
                // Ví dụ, nếu User constructor tự tạo ID:
                this.users.add(adminUser);
                System.out.println("DEBUG_US: Default Admin account CREATED: " + ADMIN_EMAIL);
            } else {
                System.out.println("DEBUG_US: Default Admin account already exists: " + ADMIN_EMAIL);
            }
        } catch (Exception e) {
            System.err.println("CRITICAL_US: Failed to initialize admin user in UserService!");
            e.printStackTrace();
        }

        // 2. (Tùy chọn) Khởi tạo các user mẫu khác với vai trò USER
        try {
            if (this.users.stream().noneMatch(u -> u.getUsername().equalsIgnoreCase("user_test1"))) {
                String user1PasswordHash = PasswordUtils.hashPassword("password123");
                User testUser1 = new User("user_test1", "test1@example.com", user1PasswordHash, User.Role.USER);
                this.users.add(testUser1);
                System.out.println("DEBUG_US: Sample user 'user_test1' created.");
            }

            if (this.users.stream().noneMatch(u -> u.getUsername().equalsIgnoreCase("user_test2"))) {
                String user2PasswordHash = PasswordUtils.hashPassword("password456");
                User testUser2 = new User("user_test2", "test2@example.com", user2PasswordHash, User.Role.USER);
                this.users.add(testUser2);
                System.out.println("DEBUG_US: Sample user 'user_test2' created.");
            }
        } catch (Exception e) {
            System.err.println("CRITICAL_US: Failed to initialize sample users in UserService!");
            e.printStackTrace();
        }
        System.out.println("DEBUG_US: UserService initializeUsers() finished. Total users: " + this.users.size());
    }

    public boolean addUser(User newUser) {
        if (newUser == null || newUser.getUsername() == null || newUser.getEmail() == null || newUser.getPasswordHash() == null) {
            System.err.println("ERROR_US_ADD: User data is invalid (null fields).");
            return false;
        }
        // Kiểm tra username hoặc email đã tồn tại chưa
        if (isUsernameTaken(newUser.getUsername())) {
            System.err.println("ERROR_US_ADD: Username '" + newUser.getUsername() + "' already exists.");
            return false;
        }
        if (isEmailTaken(newUser.getEmail())) {
            System.err.println("ERROR_US_ADD: Email '" + newUser.getEmail() + "' already registered.");
            return false;
        }
        // User ID thường được tự tạo bởi constructor của User (ví dụ bằng UUID)
        users.add(newUser);
        System.out.println("DEBUG_US_ADD: User added successfully: " + newUser.getUsername());
        return true;
    }

    public boolean updateUser(User userToUpdate) {
        if (userToUpdate == null || userToUpdate.getUserId() == null) {
            System.err.println("ERROR_US_UPDATE: User to update or its ID is null.");
            return false;
        }
        Optional<User> existingUserOpt = findUserById(userToUpdate.getUserId());
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            // Kiểm tra nếu username mới khác username cũ VÀ username mới đã tồn tại cho user khác
            if (!existingUser.getUsername().equalsIgnoreCase(userToUpdate.getUsername()) &&
                    isUsernameTaken(userToUpdate.getUsername())) {
                System.err.println("ERROR_US_UPDATE: New username '" + userToUpdate.getUsername() + "' is already taken by another user.");
                return false;
            }
            // Kiểm tra nếu email mới khác email cũ VÀ email mới đã tồn tại cho user khác
            if (!existingUser.getEmail().equalsIgnoreCase(userToUpdate.getEmail()) &&
                    isEmailTaken(userToUpdate.getEmail())) {
                System.err.println("ERROR_US_UPDATE: New email '" + userToUpdate.getEmail() + "' is already registered by another user.");
                return false;
            }

            existingUser.setUsername(userToUpdate.getUsername());
            existingUser.setEmail(userToUpdate.getEmail());
            // Chỉ cập nhật password hash nếu userToUpdate có password hash mới (khác với hash cũ)
            // Hoặc, nếu userToUpdate được truyền vào với một PAsswordHash mới đã được hash từ controller.
            if (userToUpdate.getPasswordHash() != null && !userToUpdate.getPasswordHash().equals(existingUser.getPasswordHash())) {
                // Giả sử userToUpdate.getPasswordHash() là hash mới nếu có thay đổi mật khẩu
                existingUser.setPasswordHash(userToUpdate.getPasswordHash());
                System.out.println("DEBUG_US_UPDATE: Password updated for user: " + existingUser.getUsername());
            }
            // Cẩn thận khi cho phép thay đổi Role
            // existingUser.setRole(userToUpdate.getRole());

            System.out.println("DEBUG_US_UPDATE: User updated successfully: " + existingUser.getUsername());
            return true;
        }
        System.err.println("ERROR_US_UPDATE: User with ID '" + userToUpdate.getUserId() + "' not found for update.");
        return false;
    }

    public boolean deleteUser(String userId) {
        if (userId == null) {
            System.err.println("ERROR_US_DELETE: User ID cannot be null.");
            return false;
        }
        // Không cho xóa tài khoản admin mặc định bằng cách này (nếu muốn)
        Optional<User> userToDelete = findUserById(userId);
        if(userToDelete.isPresent() && userToDelete.get().getEmail().equalsIgnoreCase(ADMIN_EMAIL)){
            System.err.println("ERROR_US_DELETE: Default admin account cannot be deleted through this method.");
            return false;
        }

        boolean removed = users.removeIf(user -> userId.equals(user.getUserId()));
        if (removed) {
            System.out.println("DEBUG_US_DELETE: User deleted with ID: " + userId);
        } else {
            System.err.println("ERROR_US_DELETE: User with ID '" + userId + "' not found for deletion.");
        }
        return removed;
    }


    public Optional<User> findUserById(String userId) {
        if (userId == null) return Optional.empty();
        return users.stream()
                .filter(user -> userId.equals(user.getUserId()))
                .findFirst();
    }

    public Optional<User> findUserByUsername(String username) {
        if (username == null) return Optional.empty();
        return users.stream()
                .filter(user -> username.equalsIgnoreCase(user.getUsername()))
                .findFirst();
    }

    // Thêm phương thức này nếu cần (ví dụ: AuthServive gọi)
    public Optional<User> findUserByEmail(String email) {
        if (email == null) return Optional.empty();
        return users.stream()
                .filter(user -> email.equalsIgnoreCase(user.getEmail()))
                .findFirst();
    }

    public List<User> getAllUsers() {
        System.out.println("DEBUG_US_GET_ALL: Retrieving all users. Count: " + users.size());
        return new ArrayList<>(users); // Trả về bản sao để tránh sửa đổi trực tiếp
    }

    // Lấy user theo Role (hữu ích cho UserManagementController)
    public List<User> getUsersByRole(User.Role role) {
        if (role == null) return getAllUsers(); // Hoặc trả về list rỗng
        return users.stream()
                .filter(user -> user.getRole() == role)
                .collect(Collectors.toList());
    }

    // Kiểm tra username đã tồn tại (không tính chính user đang được sửa nếu có userId)
    public boolean isUsernameTaken(String username) {
        if (username == null) return false;
        return users.stream()
                .anyMatch(user -> user.getUsername().equalsIgnoreCase(username));
    }

    // Kiểm tra email đã tồn tại
    public boolean isEmailTaken(String email) {
        if (email == null) return false;
        return users.stream()
                .anyMatch(user -> user.getEmail().equalsIgnoreCase(email));
    }


    // Các phương thức liên quan đến mượn/trả (giữ nguyên như cũ, đã đúng)
    public boolean recordNewLoanForUser(String userId, String loanRecordId) {
        Optional<User> userOpt = findUserById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.addActiveLoanRecord(loanRecordId);
            System.out.println("DEBUG_US_LOAN: Loan Record ID " + loanRecordId + " added for User ID " + userId);
            return true;
        }
        System.err.println("ERROR_US_LOAN: User ID " + userId + " not found to record new loan.");
        return false;
    }

    public boolean recordLoanEndedForUser(String userId, String loanRecordId) {
        Optional<User> userOpt = findUserById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.removeActiveLoanRecord(loanRecordId);
            System.out.println("DEBUG_US_LOAN: Loan Record ID " + loanRecordId + " removed for User ID " + userId);
            return true;
        }
        System.err.println("ERROR_US_LOAN: User ID " + userId + " not found to record loan ended.");
        return false;
    }
}
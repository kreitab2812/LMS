package com.lms.quanlythuvien.services.user;

import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.models.user.User.Role;
import com.lms.quanlythuvien.utils.database.DatabaseManager;
import com.lms.quanlythuvien.utils.security.PasswordUtils;
import com.lms.quanlythuvien.exceptions.DeletionRestrictedException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class UserService {

    private static UserService instance;

    // Thông tin admin mặc định
    private static final String ADMIN_EMAIL = "24022274@vnu.edu.vn";
    private static final String ADMIN_USERNAME = "admin_vnu";
    private static final String ADMIN_RAW_PASSWORD = "LGTV2006";

    private UserService() {
        System.out.println("DEBUG_US_SINGLETON: UserService Singleton instance created. Initializing default admin check with DB.");
        checkAndCreateDefaultAdmin();
    }

    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    /**
     * Kiểm tra và tạo admin mặc định trong database nếu chưa tồn tại.
     * Phương thức này được gọi một lần khi UserService được khởi tạo.
     */
    private void checkAndCreateDefaultAdmin() {
        if (!isEmailTaken(ADMIN_EMAIL)) { // isEmailTaken sẽ kiểm tra DB
            System.out.println("INFO_US_DB_SETUP: Default admin with email " + ADMIN_EMAIL + " not found in DB. Attempting to create.");
            try {
                String adminPasswordHash = PasswordUtils.hashPassword(ADMIN_RAW_PASSWORD);
                String adminUserId = generateNewUserId(); // Tạo ID cho admin

                String sql = "INSERT INTO Users (id, username, email, passwordHash, role, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?)";
                String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

                try (Connection conn = DatabaseManager.getInstance().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    pstmt.setString(1, adminUserId);
                    pstmt.setString(2, ADMIN_USERNAME);
                    pstmt.setString(3, ADMIN_EMAIL);
                    pstmt.setString(4, adminPasswordHash);
                    pstmt.setString(5, User.Role.ADMIN.name());
                    pstmt.setString(6, currentTime);
                    pstmt.setString(7, currentTime);

                    int affectedRows = pstmt.executeUpdate();
                    if (affectedRows > 0) {
                        System.out.println("INFO_US_DB_SETUP: Default Admin account CREATED in Database. Username: " + ADMIN_USERNAME + ", Email: " + ADMIN_EMAIL + ", ID: " + adminUserId);
                    } else {
                        System.err.println("ERROR_US_DB_SETUP: Failed to create default Admin in Database (no rows affected). Email: " + ADMIN_EMAIL);
                    }
                }
            } catch (Exception e) { // Catch SQLException from DB or other exceptions (e.g., from PasswordUtils)
                System.err.println("CRITICAL_US_DB_SETUP: Failed to create default admin in DB for email " + ADMIN_EMAIL + "!");
                e.printStackTrace();
            }
        } else {
            System.out.println("INFO_US_DB_SETUP: Default Admin account (" + ADMIN_EMAIL + ") already exists in Database.");
        }
    }

    /**
     * Tạo một User ID mới theo định dạng YYYYMM-NNNN.
     */
    private String generateNewUserId() {
        String yearMonthPart = new SimpleDateFormat("yyyyMM").format(new Date());
        String querySql = "SELECT id FROM Users WHERE id LIKE ? ORDER BY id DESC LIMIT 1";
        int nextSequence = 1;

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(querySql)) {

            pstmt.setString(1, yearMonthPart + "-%");
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String lastId = rs.getString("id");
                    String[] parts = lastId.split("-");
                    if (parts.length == 2 && parts[0].equals(yearMonthPart)) {
                        try {
                            nextSequence = Integer.parseInt(parts[1]) + 1;
                        } catch (NumberFormatException e) {
                            System.err.println("ERROR_US_GEN_ID: Could not parse sequence number from ID: " + lastId + ". Defaulting to 1.");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_US_GEN_ID: DB error generating new user ID prefix " + yearMonthPart + ": " + e.getMessage());
            // Fallback to a more unique ID if DB sequence fails, though this deviates from YYYYMM-NNNN
            return yearMonthPart + "-" + System.currentTimeMillis() % 10000; // Simple fallback
        }
        return String.format("%s-%04d", yearMonthPart, nextSequence);
    }

    public boolean addUser(User newUserDetails) {
        if (newUserDetails == null || newUserDetails.getUsername() == null || newUserDetails.getEmail() == null || newUserDetails.getPasswordHash() == null) {
            System.err.println("ERROR_US_ADD: User data is invalid (null fields).");
            return false;
        }
        if (isUsernameTaken(newUserDetails.getUsername())) {
            System.err.println("ERROR_US_ADD: Username '" + newUserDetails.getUsername() + "' already exists.");
            return false;
        }
        if (isEmailTaken(newUserDetails.getEmail())) {
            System.err.println("ERROR_US_ADD: Email '" + newUserDetails.getEmail() + "' already registered.");
            return false;
        }

        String newUserId = generateNewUserId(); // ID này sẽ được dùng thay vì ID tự tạo bằng UUID trong newUserDetails
        String sql = "INSERT INTO Users (id, username, email, passwordHash, role, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newUserId);
            pstmt.setString(2, newUserDetails.getUsername());
            pstmt.setString(3, newUserDetails.getEmail());
            pstmt.setString(4, newUserDetails.getPasswordHash());
            pstmt.setString(5, newUserDetails.getRole().name());
            pstmt.setString(6, currentTime);
            pstmt.setString(7, currentTime);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_US_ADD: User added successfully to DB: " + newUserDetails.getUsername() + " with ID: " + newUserId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("ERROR_US_ADD: DB error adding user '" + newUserDetails.getUsername() + "': " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateUser(User userToUpdate) {
        if (userToUpdate == null || userToUpdate.getUserId() == null) {
            System.err.println("ERROR_US_UPDATE: User to update or its DB ID is null.");
            return false;
        }

        // Kiểm tra username mới (nếu thay đổi) có bị trùng với user khác không
        Optional<User> userByNewUsername = findUserByUsername(userToUpdate.getUsername());
        if (userByNewUsername.isPresent() && !userByNewUsername.get().getUserId().equals(userToUpdate.getUserId())) {
            System.err.println("ERROR_US_UPDATE: New username '" + userToUpdate.getUsername() + "' is already taken by another user.");
            return false;
        }

        // Kiểm tra email mới (nếu thay đổi) có bị trùng với user khác không
        Optional<User> userByNewEmail = findUserByEmail(userToUpdate.getEmail());
        if (userByNewEmail.isPresent() && !userByNewEmail.get().getUserId().equals(userToUpdate.getUserId())) {
            System.err.println("ERROR_US_UPDATE: New email '" + userToUpdate.getEmail() + "' is already registered by another user.");
            return false;
        }

        String sql = "UPDATE Users SET username = ?, email = ?, passwordHash = ?, role = ?, updatedAt = ? WHERE id = ?";
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userToUpdate.getUsername());
            pstmt.setString(2, userToUpdate.getEmail());
            pstmt.setString(3, userToUpdate.getPasswordHash());
            pstmt.setString(4, userToUpdate.getRole().name());
            pstmt.setString(5, currentTime);
            pstmt.setString(6, userToUpdate.getUserId()); // userId này phải là ID từ DB (YYYYMM-NNNN)

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_US_UPDATE: User updated successfully in DB: " + userToUpdate.getUsername());
                return true;
            } else {
                System.err.println("ERROR_US_UPDATE: User with ID '" + userToUpdate.getUserId() + "' not found in DB for update, or no data changed.");
            }
        } catch (SQLException e) {
            System.err.println("ERROR_US_UPDATE: DB error updating user '" + userToUpdate.getUsername() + "': " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteUser(String userId) throws DeletionRestrictedException { // << THÊM "throws DeletionRestrictedException"
        if (userId == null) {
            System.err.println("ERROR_US_DELETE: User ID cannot be null.");
            // Hoặc: throw new IllegalArgumentException("User ID cannot be null.");
            return false;
        }

        // Kiểm tra không cho xóa admin mặc định
        Optional<User> userOpt = findUserById(userId); // findUserById giờ đã dùng DB
        if (userOpt.isPresent() && ADMIN_EMAIL.equalsIgnoreCase(userOpt.get().getEmail()) && userOpt.get().getRole() == User.Role.ADMIN) {
            System.err.println("ERROR_US_DELETE: Default admin account (" + ADMIN_EMAIL + ") cannot be deleted.");
            throw new DeletionRestrictedException("Không thể xóa tài khoản Quản trị viên mặc định.");
        }

        // Bước 1: Kiểm tra xem user có đang mượn sách không (status "BORROWED", "ACTIVE", hoặc "OVERDUE")
        String checkLoansSql = "SELECT COUNT(*) AS loanCount FROM BorrowingRecords WHERE userId = ? AND status IN ('BORROWED', 'ACTIVE', 'OVERDUE')";
        try (Connection conn = DatabaseManager.getInstance().getConnection()) { // Mở connection một lần cho cả kiểm tra và xóa
            try (PreparedStatement checkStmt = conn.prepareStatement(checkLoansSql)) {
                checkStmt.setString(1, userId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt("loanCount") > 0) {
                        String message = "Không thể xóa người dùng này (ID: " + userId + ") vì họ đang có " + rs.getInt("loanCount") + " lượt mượn sách chưa trả.";
                        System.err.println("ERROR_US_DELETE: " + message);
                        throw new DeletionRestrictedException(message);
                    }
                }
            }

            // Bước 2: Nếu không có sách mượn, tiến hành xóa user
            String deleteUserSql = "DELETE FROM Users WHERE id = ?";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteUserSql)) {
                deleteStmt.setString(1, userId);
                int affectedRows = deleteStmt.executeUpdate();
                if (affectedRows > 0) {
                    System.out.println("DEBUG_US_DELETE: User deleted from DB with ID: " + userId);
                    return true;
                } else {
                    System.err.println("ERROR_US_DELETE: User with ID '" + userId + "' not found in DB for deletion (after check).");
                    return false; // Hoặc ném một exception khác nếu không tìm thấy sau khi đã có thể không có active loans
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_US_DELETE: DB error during deletion process for user ID '" + userId + "': " + e.getMessage());
            e.printStackTrace();
            // Nếu lỗi là do vi phạm ràng buộc (dù đã kiểm tra, trường hợp hiếm hoặc race condition)
            if (e.getMessage().toLowerCase().contains("constraint") || e.getMessage().toLowerCase().contains("foreign key")) {
                throw new DeletionRestrictedException("Lỗi ràng buộc dữ liệu khi xóa người dùng. Người dùng có thể vẫn còn dữ liệu liên quan (ví dụ: lượt mượn).");
            }
            // Ném một exception chung hơn hoặc trả về false
            // throw new RuntimeException("Database error during user deletion.", e);
            return false;
        }
    }

    public Optional<User> findUserById(String userId) {
        if (userId == null) return Optional.empty();
        String sql = "SELECT username, passwordHash, email, role FROM Users WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String dbUsername = rs.getString("username");
                    String dbPasswordHash = rs.getString("passwordHash");
                    String dbEmail = rs.getString("email");
                    User.Role dbRole = User.Role.valueOf(rs.getString("role").toUpperCase());
                    // Sử dụng constructor User(userId, username, email, passwordHash, role)
                    User user = new User(userId, dbUsername, dbEmail, dbPasswordHash, dbRole);
                    return Optional.of(user);
                }
            }
        } catch (SQLException | IllegalArgumentException e) { // Catch multiple exceptions
            System.err.println("ERROR_US_FIND_ID: Error finding user by ID '" + userId + "': " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<User> findUserByUsername(String username) {
        if (username == null) return Optional.empty();
        String sql = "SELECT id, passwordHash, email, role FROM Users WHERE username = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String dbUserId = rs.getString("id");
                    String dbPasswordHash = rs.getString("passwordHash");
                    String dbEmail = rs.getString("email");
                    User.Role dbRole = User.Role.valueOf(rs.getString("role").toUpperCase());
                    User user = new User(dbUserId, username, dbEmail, dbPasswordHash, dbRole);
                    return Optional.of(user);
                }
            }
        } catch (SQLException | IllegalArgumentException e) {
            System.err.println("ERROR_US_FIND_USERNAME: Error finding user by username '" + username + "': " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<User> findUserByEmail(String email) {
        if (email == null) return Optional.empty();
        String sql = "SELECT id, username, passwordHash, role FROM Users WHERE email = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String dbUserId = rs.getString("id");
                    String dbUsername = rs.getString("username");
                    String dbPasswordHash = rs.getString("passwordHash");
                    User.Role dbRole = User.Role.valueOf(rs.getString("role").toUpperCase());
                    User user = new User(dbUserId, dbUsername, email, dbPasswordHash, dbRole);
                    return Optional.of(user);
                }
            }
        } catch (SQLException | IllegalArgumentException e) {
            System.err.println("ERROR_US_FIND_EMAIL: Error finding user by email '" + email + "': " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        String sql = "SELECT id, username, passwordHash, email, role FROM Users";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql); // Hoặc Statement nếu không có tham số
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String userId = rs.getString("id");
                String username = rs.getString("username");
                String passwordHash = rs.getString("passwordHash");
                String email = rs.getString("email");
                User.Role role = User.Role.valueOf(rs.getString("role").toUpperCase());
                userList.add(new User(userId, username, email, passwordHash, role));
            }
        } catch (SQLException | IllegalArgumentException e) {
            System.err.println("ERROR_US_GET_ALL: DB error retrieving all users: " + e.getMessage());
            e.printStackTrace();
        }
        return userList;
    }

    public List<User> getUsersByRole(User.Role role) {
        if (role == null) return getAllUsers(); // Hoặc trả về list rỗng tùy logic
        List<User> userList = new ArrayList<>();
        String sql = "SELECT id, username, passwordHash, email, role FROM Users WHERE role = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, role.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String userId = rs.getString("id");
                    String username = rs.getString("username");
                    String passwordHash = rs.getString("passwordHash");
                    String email = rs.getString("email");
                    // role đã biết từ tham số
                    userList.add(new User(userId, username, email, passwordHash, role));
                }
            }
        } catch (SQLException | IllegalArgumentException e) {
            System.err.println("ERROR_US_GET_BY_ROLE: DB error retrieving users by role '" + role.name() + "': " + e.getMessage());
            e.printStackTrace();
        }
        return userList;
    }

    public boolean isUsernameTaken(String username) {
        if (username == null) return false;
        String sql = "SELECT COUNT(*) AS count FROM Users WHERE username = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_US_USERNAME_TAKEN: DB error checking username '" + username + "': " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean isEmailTaken(String email) {
        if (email == null) return false;
        String sql = "SELECT COUNT(*) AS count FROM Users WHERE email = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_US_EMAIL_TAKEN: DB error checking email '" + email + "': " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // Các phương thức recordNewLoanForUser và recordLoanEndedForUser
    // Hiện tại, chúng thao tác trên danh sách activeLoanRecordIds của đối tượng User trong bộ nhớ.
    // Khi User object được load từ DB, danh sách này chưa được điền.
    // Danh sách này nên được load bởi BorrowingRecordService khi cần.
    // Do đó, ý nghĩa của các hàm này trong UserService sẽ thay đổi.
    // Chúng chỉ ảnh hưởng đến đối tượng User đang có trong bộ nhớ tại thời điểm đó.
    public boolean recordNewLoanForUser(String userId, String loanRecordId) {
        Optional<User> userOpt = findUserById(userId); // Lấy User từ DB
        if (userOpt.isPresent()) {
            // Thao tác này chỉ cập nhật trên đối tượng User trong bộ nhớ.
            // Không có thay đổi nào được lưu vào bảng Users liên quan đến activeLoanRecordIds.
            userOpt.get().addActiveLoanRecord(loanRecordId);
            System.out.println("DEBUG_US_LOAN: Loan Record ID " + loanRecordId + " added to User object (ID: " + userId + ") in memory. (DB not directly affected here for active loans list)");
            return true;
        }
        System.err.println("ERROR_US_LOAN: User ID " + userId + " not found to record new loan.");
        return false;
    }

    public boolean recordLoanEndedForUser(String userId, String loanRecordId) {
        Optional<User> userOpt = findUserById(userId); // Lấy User từ DB
        if (userOpt.isPresent()) {
            userOpt.get().removeActiveLoanRecord(loanRecordId);
            System.out.println("DEBUG_US_LOAN: Loan Record ID " + loanRecordId + " removed from User object (ID: " + userId + ") in memory. (DB not directly affected here for active loans list)");
            return true;
        }
        System.err.println("ERROR_US_LOAN: User ID " + userId + " not found to record loan ended.");
        return false;
    }
}
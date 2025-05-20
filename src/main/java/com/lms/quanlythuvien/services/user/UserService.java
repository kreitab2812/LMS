package com.lms.quanlythuvien.services.user;

import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.utils.database.DatabaseManager;
import com.lms.quanlythuvien.utils.security.PasswordUtils;
import com.lms.quanlythuvien.exceptions.DeletionRestrictedException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement; // Đã import
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class UserService {

    private static UserService instance;

    private static final String ADMIN_EMAIL = "24022274@vnu.edu.vn";
    private static final String ADMIN_USERNAME = "admin_vnu";
    private static final String ADMIN_RAW_PASSWORD = "LGTV2006";

    private static final DateTimeFormatter DB_DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DB_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private UserService() {
        System.out.println("DEBUG_US_SINGLETON: UserService Singleton instance created. Initializing default admin check.");
        checkAndCreateDefaultAdmin();
    }

    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    private void checkAndCreateDefaultAdmin() {
        if (findUserByEmail(ADMIN_EMAIL).isEmpty()) {
            System.out.println("INFO_US_DB_SETUP: Default admin " + ADMIN_EMAIL + " not found. Attempting to create.");
            try {
                String adminPasswordHash = PasswordUtils.hashPassword(ADMIN_RAW_PASSWORD);
                String adminUserId = generateNewUserId(); // Tạo ID tùy chỉnh
                LocalDateTime now = LocalDateTime.now();

                // Tạo User object cho admin sử dụng constructor đầy đủ.
                // Các trường không có giá trị ban đầu sẽ là null hoặc giá trị mặc định từ constructor User.
                User adminUser = new User(
                        adminUserId, ADMIN_USERNAME, ADMIN_EMAIL, adminPasswordHash, User.Role.ADMIN,
                        "Quản Trị Viên Hệ Thống", // fullName
                        null, // dateOfBirth
                        null, // address
                        null, // phoneNumber
                        null, // avatarUrl
                        "Tài khoản quản trị viên mặc định của hệ thống.", // introduction
                        false, // isAccountLocked
                        0.0,   // currentFineAmount
                        100,   // reputationScore (admin có điểm uy tín cao nhất)
                        now,   // createdAt
                        now    // updatedAt
                );

                // Câu lệnh SQL để insert tất cả các trường
                String sql = "INSERT INTO Users (id, username, email, passwordHash, role, " +
                        "fullName, dateOfBirth, address, phoneNumber, avatarUrl, introduction, " +
                        "isAccountLocked, currentFineAmount, reputationScore, createdAt, updatedAt) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                try (Connection conn = DatabaseManager.getInstance().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    pstmt.setString(1, adminUser.getUserId());
                    pstmt.setString(2, adminUser.getUsername());
                    pstmt.setString(3, adminUser.getEmail());
                    pstmt.setString(4, adminUser.getPasswordHash());
                    pstmt.setString(5, adminUser.getRole().name());
                    pstmt.setString(6, adminUser.getFullName());
                    pstmt.setObject(7, adminUser.getDateOfBirth() != null ? adminUser.getDateOfBirth().format(DB_DATE_FORMATTER) : null, Types.VARCHAR);
                    pstmt.setString(8, adminUser.getAddress());
                    pstmt.setString(9, adminUser.getPhoneNumber());
                    pstmt.setString(10, adminUser.getAvatarUrl());
                    pstmt.setString(11, adminUser.getIntroduction());
                    pstmt.setInt(12, adminUser.isAccountLocked() ? 1 : 0);
                    pstmt.setDouble(13, adminUser.getCurrentFineAmount());
                    pstmt.setInt(14, adminUser.getReputationScore());
                    pstmt.setString(15, adminUser.getCreatedAt().format(DB_DATETIME_FORMATTER));
                    pstmt.setString(16, adminUser.getUpdatedAt().format(DB_DATETIME_FORMATTER));

                    int affectedRows = pstmt.executeUpdate();
                    if (affectedRows > 0) {
                        System.out.println("INFO_US_DB_SETUP: Default Admin account CREATED. Username: " + ADMIN_USERNAME + ", ID: " + adminUserId);
                    } else {
                        System.err.println("ERROR_US_DB_SETUP: Failed to create default Admin (no rows affected). Email: " + ADMIN_EMAIL);
                    }
                }
            } catch (Exception e) {
                System.err.println("CRITICAL_US_DB_SETUP: Exception during default admin creation for " + ADMIN_EMAIL + ": " + e.getMessage());
            }
        } else {
            System.out.println("INFO_US_DB_SETUP: Default Admin account (" + ADMIN_EMAIL + ") already exists.");
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        // Sử dụng constructor đầy đủ của User
        return new User(
                rs.getString("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("passwordHash"),
                User.Role.valueOf(rs.getString("role").toUpperCase()),
                rs.getString("fullName"),
                rs.getString("dateOfBirth") != null ? LocalDate.parse(rs.getString("dateOfBirth"), DB_DATE_FORMATTER) : null,
                rs.getString("address"),
                rs.getString("phoneNumber"),
                rs.getString("avatarUrl"),
                rs.getString("introduction"),
                rs.getInt("isAccountLocked") == 1,
                rs.getDouble("currentFineAmount"),
                rs.getInt("reputationScore"),
                rs.getString("createdAt") != null ? LocalDateTime.parse(rs.getString("createdAt"), DB_DATETIME_FORMATTER) : null,
                rs.getString("updatedAt") != null ? LocalDateTime.parse(rs.getString("updatedAt"), DB_DATETIME_FORMATTER) : null
        );
    }

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
                            System.err.println("WARN_US_GEN_ID: Could not parse sequence from ID: " + lastId + ". Defaulting to 1.");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_US_GEN_ID: DB error generating user ID for " + yearMonthPart + ": " + e.getMessage());
            return yearMonthPart + "-" + String.format("%04d", (int)(Math.random()*10000)); // Fallback
        }
        return String.format("%s-%04d", yearMonthPart, nextSequence);
    }

    /**
     * Thêm một người dùng mới. Thông tin cơ bản (username, email, passwordHash, role)
     * được lấy từ đối tượng User truyền vào (thường là từ màn hình đăng ký).
     * ID và thời gian sẽ được tạo và gán.
     */
    public Optional<User> addUser(User basicUserDetails) {
        if (basicUserDetails == null || basicUserDetails.getUsername() == null || basicUserDetails.getEmail() == null || basicUserDetails.getPasswordHash() == null) {
            System.err.println("ERROR_US_ADD: User data is invalid (username, email, or passwordHash is null).");
            return Optional.empty();
        }
        if (isUsernameTaken(basicUserDetails.getUsername())) {
            System.err.println("ERROR_US_ADD: Username '" + basicUserDetails.getUsername() + "' already exists.");
            return Optional.empty();
        }
        if (isEmailTaken(basicUserDetails.getEmail())) {
            System.err.println("ERROR_US_ADD: Email '" + basicUserDetails.getEmail() + "' already registered.");
            return Optional.empty();
        }

        String newUserId = generateNewUserId();
        LocalDateTime now = LocalDateTime.now();

        // Tạo đối tượng User hoàn chỉnh để insert, sử dụng constructor đầy đủ
        User userToInsert = new User(
                newUserId,
                basicUserDetails.getUsername(),
                basicUserDetails.getEmail(),
                basicUserDetails.getPasswordHash(),
                basicUserDetails.getRole(),
                basicUserDetails.getFullName(),       // Có thể null nếu user chưa nhập
                basicUserDetails.getDateOfBirth(),    // Có thể null
                basicUserDetails.getAddress(),        // Có thể null
                basicUserDetails.getPhoneNumber(),    // Có thể null
                basicUserDetails.getAvatarUrl(),      // Có thể null
                basicUserDetails.getIntroduction(),   // Có thể null
                false,                                // isAccountLocked (mặc định)
                0.0,                                  // currentFineAmount (mặc định)
                80,                                   // reputationScore (mặc định)
                now,                                  // createdAt
                now                                   // updatedAt
        );
        // activeLoanRecordIds được khởi tạo rỗng trong constructor User

        String sql = "INSERT INTO Users (id, username, email, passwordHash, role, " +
                "fullName, dateOfBirth, address, phoneNumber, avatarUrl, introduction, " +
                "isAccountLocked, currentFineAmount, reputationScore, createdAt, updatedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userToInsert.getUserId());
            pstmt.setString(2, userToInsert.getUsername());
            pstmt.setString(3, userToInsert.getEmail());
            pstmt.setString(4, userToInsert.getPasswordHash());
            pstmt.setString(5, userToInsert.getRole().name());
            pstmt.setString(6, userToInsert.getFullName());
            pstmt.setObject(7, userToInsert.getDateOfBirth() != null ? userToInsert.getDateOfBirth().format(DB_DATE_FORMATTER) : null, Types.VARCHAR);
            pstmt.setString(8, userToInsert.getAddress());
            pstmt.setString(9, userToInsert.getPhoneNumber());
            pstmt.setString(10, userToInsert.getAvatarUrl());
            pstmt.setString(11, userToInsert.getIntroduction());
            pstmt.setInt(12, userToInsert.isAccountLocked() ? 1 : 0);
            pstmt.setDouble(13, userToInsert.getCurrentFineAmount());
            pstmt.setInt(14, userToInsert.getReputationScore());
            pstmt.setString(15, userToInsert.getCreatedAt().format(DB_DATETIME_FORMATTER));
            pstmt.setString(16, userToInsert.getUpdatedAt().format(DB_DATETIME_FORMATTER));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_US_ADD: User added: " + userToInsert.getUsername() + " with ID: " + userToInsert.getUserId());
                return Optional.of(userToInsert); // Trả về đối tượng User đã được insert (có ID và timestamps đúng)
            }
        } catch (SQLException e) {
            System.err.println("ERROR_US_ADD: DB error adding user '" + basicUserDetails.getUsername() + "': " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean updateUser(User userToUpdate) {
        if (userToUpdate == null || userToUpdate.getUserId() == null) {
            System.err.println("ERROR_US_UPDATE: User to update or its ID is null.");
            return false;
        }

        Optional<User> userByNewUsername = findUserByUsername(userToUpdate.getUsername());
        if (userByNewUsername.isPresent() && !userByNewUsername.get().getUserId().equals(userToUpdate.getUserId())) {
            System.err.println("ERROR_US_UPDATE: New username '" + userToUpdate.getUsername() + "' is already taken.");
            return false;
        }
        Optional<User> userByNewEmail = findUserByEmail(userToUpdate.getEmail());
        if (userByNewEmail.isPresent() && !userByNewEmail.get().getUserId().equals(userToUpdate.getUserId())) {
            System.err.println("ERROR_US_UPDATE: New email '" + userToUpdate.getEmail() + "' is already registered.");
            return false;
        }

        userToUpdate.setUpdatedAt(LocalDateTime.now()); // Model User có setter cho updatedAt

        String sql = "UPDATE Users SET username = ?, email = ?, passwordHash = ?, role = ?, " +
                "fullName = ?, dateOfBirth = ?, address = ?, phoneNumber = ?, avatarUrl = ?, introduction = ?, " +
                "isAccountLocked = ?, currentFineAmount = ?, reputationScore = ?, updatedAt = ? " +
                "WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userToUpdate.getUsername());
            pstmt.setString(2, userToUpdate.getEmail());
            pstmt.setString(3, userToUpdate.getPasswordHash()); // Password hash chỉ nên cập nhật qua hàm changePassword
            pstmt.setString(4, userToUpdate.getRole().name());
            pstmt.setString(5, userToUpdate.getFullName());
            pstmt.setObject(6, userToUpdate.getDateOfBirth() != null ? userToUpdate.getDateOfBirth().format(DB_DATE_FORMATTER) : null, Types.VARCHAR);
            pstmt.setString(7, userToUpdate.getAddress());
            pstmt.setString(8, userToUpdate.getPhoneNumber());
            pstmt.setString(9, userToUpdate.getAvatarUrl());
            pstmt.setString(10, userToUpdate.getIntroduction());
            pstmt.setInt(11, userToUpdate.isAccountLocked() ? 1 : 0);
            pstmt.setDouble(12, userToUpdate.getCurrentFineAmount());
            pstmt.setInt(13, userToUpdate.getReputationScore());
            pstmt.setString(14, userToUpdate.getUpdatedAt().format(DB_DATETIME_FORMATTER));
            pstmt.setString(15, userToUpdate.getUserId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_US_UPDATE: User updated: " + userToUpdate.getUsername());
                return true;
            } else {
                System.err.println("WARN_US_UPDATE: User with ID '" + userToUpdate.getUserId() + "' not found or no data changed.");
            }
        } catch (SQLException e) {
            System.err.println("ERROR_US_UPDATE: DB error updating user '" + userToUpdate.getUsername() + "': " + e.getMessage());
        }
        return false;
    }

    public boolean changePassword(String userId, String newPasswordHash) {
        if (userId == null || newPasswordHash == null || newPasswordHash.trim().isEmpty()) {
            System.err.println("ERROR_US_CHANGE_PWD: User ID or new password hash is invalid.");
            return false;
        }
        String sql = "UPDATE Users SET passwordHash = ?, updatedAt = ? WHERE id = ?";
        String currentTime = LocalDateTime.now().format(DB_DATETIME_FORMATTER);
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newPasswordHash);
            pstmt.setString(2, currentTime);
            pstmt.setString(3, userId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_US_CHANGE_PWD: Password changed successfully for user ID: " + userId);
                return true;
            } else {
                System.err.println("WARN_US_CHANGE_PWD: User ID " + userId + " not found for password change.");
            }
        } catch (SQLException e) {
            System.err.println("ERROR_US_CHANGE_PWD: DB error changing password for user ID " + userId + ": " + e.getMessage());
        }
        return false;
    }

    public boolean deleteUser(String userId) throws DeletionRestrictedException {
        if (userId == null || userId.trim().isEmpty()) {
            System.err.println("ERROR_US_DELETE: User ID cannot be null or empty.");
            return false;
        }

        Optional<User> userOpt = findUserById(userId);
        if (userOpt.isEmpty()) {
            System.err.println("ERROR_US_DELETE: User with ID '" + userId + "' not found.");
            return false;
        }
        User userToDelete = userOpt.get();

        if (ADMIN_EMAIL.equalsIgnoreCase(userToDelete.getEmail()) && userToDelete.getRole() == User.Role.ADMIN) {
            throw new DeletionRestrictedException("Không thể xóa tài khoản Quản trị viên mặc định (" + ADMIN_EMAIL + ").");
        }

        String checkLoansSql = "SELECT COUNT(*) AS loanCount FROM BorrowingRecords WHERE userId = ? AND status IN ('ACTIVE', 'OVERDUE')";
        Connection conn = null;
        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement checkStmt = conn.prepareStatement(checkLoansSql)) {
                checkStmt.setString(1, userId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        int loanCount = rs.getInt("loanCount");
                        if (loanCount > 0) {
                            throw new DeletionRestrictedException("Không thể xóa người dùng (ID: " + userId + ") vì còn " + loanCount + " lượt mượn sách chưa trả.");
                        }
                    }
                }
            }

            deleteUserRelatedData(conn, userId);

            String deleteUserSql = "DELETE FROM Users WHERE id = ?";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteUserSql)) {
                deleteStmt.setString(1, userId);
                int affectedRows = deleteStmt.executeUpdate();
                if (affectedRows > 0) {
                    conn.commit();
                    System.out.println("DEBUG_US_DELETE: User deleted: " + userId);
                    return true;
                } else {
                    conn.rollback();
                    System.err.println("WARN_US_DELETE: User " + userId + " not found during deletion.");
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_US_DELETE: DB error for user " + userId + ": " + e.getMessage() + ", SQLState: " + e.getSQLState());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { System.err.println("ERROR_US_DELETE_ROLLBACK_SQLEX: " + ex.getMessage()); }
            String sqlState = e.getSQLState();
            if (sqlState != null && sqlState.startsWith("23")) {
                throw new DeletionRestrictedException("Không thể xóa người dùng do ràng buộc dữ liệu (SQLState: " + sqlState + "). Chi tiết: " + e.getMessage());
            } else if (e.getMessage().toLowerCase().contains("constraint")) {
                throw new DeletionRestrictedException("Không thể xóa người dùng do ràng buộc dữ liệu. Chi tiết: " + e.getMessage());
            }
            return false;
        } catch (DeletionRestrictedException dre) {
            System.err.println("INFO_US_DELETE_DRE_CAUGHT: " + dre.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { System.err.println("ERROR_US_DELETE_ROLLBACK_DRE: " + ex.getMessage()); }
            throw dre;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { System.err.println("ERROR_US_DELETE_CLOSE: " + e.getMessage()); }
        }
    }

    private void deleteUserRelatedData(Connection conn, String userId) throws SQLException {
        String[] tablesToClean = {"UserFavoriteBooks", "UserQuestions", "Notifications", "BorrowingRequests", "BookReviews"};
        for (String table : tablesToClean) {
            String sql = "DELETE FROM " + table + " WHERE userId = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                int deletedCount = pstmt.executeUpdate();
                System.out.println("DEBUG_US_CLEANUP: Deleted " + deletedCount + " rows from " + table + " for user " + userId);
            }
        }
    }

    public Optional<User> findUserById(String userId) {
        if (userId == null || userId.trim().isEmpty()) return Optional.empty();
        String sql = "SELECT * FROM Users WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException | IllegalArgumentException e) {
            System.err.println("ERROR_US_FIND_ID: Error finding user by ID '" + userId + "': " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<User> findUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) return Optional.empty();
        String sql = "SELECT * FROM Users WHERE username = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException | IllegalArgumentException e) {
            System.err.println("ERROR_US_FIND_USERNAME: Error finding user by username '" + username + "': " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<User> findUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) return Optional.empty();
        String sql = "SELECT * FROM Users WHERE email = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException | IllegalArgumentException e) {
            System.err.println("ERROR_US_FIND_EMAIL: Error finding user by email '" + email + "': " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        String sql = "SELECT * FROM Users ORDER BY username";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement(); // Đã sửa thành Statement
             ResultSet rs = stmt.executeQuery(sql)) { // Đã sửa để dùng executeQuery(sql)
            while (rs.next()) {
                userList.add(mapResultSetToUser(rs));
            }
        } catch (SQLException | IllegalArgumentException e) {
            System.err.println("ERROR_US_GET_ALL: DB error retrieving all users: " + e.getMessage());
        }
        return userList;
    }

    public List<User> getUsersByRole(User.Role role) {
        if (role == null) return getAllUsers(); // Hoặc new ArrayList<>() nếu muốn trả về rỗng
        List<User> userList = new ArrayList<>();
        String sql = "SELECT * FROM Users WHERE role = ? ORDER BY username";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, role.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    userList.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException | IllegalArgumentException e) {
            System.err.println("ERROR_US_GET_BY_ROLE: DB error retrieving users by role '" + role.name() + "': " + e.getMessage());
        }
        return userList;
    }

    public Map<String, User> getUsersMapByIds(Set<String> userIds) {
        Map<String, User> usersMap = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return usersMap;
        }
        String placeholders = userIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT * FROM Users WHERE id IN (" + placeholders + ")";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int i = 1;
            for (String userId : userIds) {
                pstmt.setString(i++, userId);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    User user = mapResultSetToUser(rs);
                    usersMap.put(user.getUserId(), user);
                }
            }
        } catch (SQLException | IllegalArgumentException e) {
            System.err.println("ERROR_US_GET_USERS_MAP_BY_IDS: DB error retrieving users by IDs: " + e.getMessage());
        }
        return usersMap;
    }

    public boolean isUsernameTaken(String username) {
        if (username == null || username.trim().isEmpty()) return false;
        String sql = "SELECT 1 FROM Users WHERE username = ? LIMIT 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("ERROR_US_USERNAME_TAKEN: DB error checking username '" + username + "': " + e.getMessage());
        }
        // An toàn hơn khi trả về true nếu có lỗi DB, để tránh user đăng ký trùng khi DB lỗi
        // Tuy nhiên, điều này có thể gây phiền toái. Cân nhắc trả về false và log lỗi nặng.
        return true;
    }

    public boolean isEmailTaken(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        String sql = "SELECT 1 FROM Users WHERE email = ? LIMIT 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("ERROR_US_EMAIL_TAKEN: DB error checking email '" + email + "': " + e.getMessage());
        }
        return true; // Tương tự isUsernameTaken
    }

    public boolean applyPenaltyAndLockAccount(String userId, double fineAmountToAdd, int reputationPenalty) {
        Optional<User> userOpt = findUserById(userId);
        if (userOpt.isEmpty()) {
            System.err.println("ERROR_US_PENALTY: User " + userId + " not found to apply penalty.");
            return false;
        }
        User user = userOpt.get();
        user.setCurrentFineAmount(user.getCurrentFineAmount() + fineAmountToAdd);
        user.setReputationScore(Math.max(0, user.getReputationScore() - reputationPenalty));

        boolean shouldLock = user.getReputationScore() < 50;

        if (shouldLock && !user.isAccountLocked()) {
            user.setAccountLocked(true);
            System.out.println("INFO_US_PENALTY: User " + userId + " account locked.");
            // Nên gọi NotificationService ở đây
        }

        if (updateUser(user)) { // updateUser đã bao gồm setUpdatedAt
            System.out.println("DEBUG_US_PENALTY: Penalty applied for user " + userId);
            return true;
        } else {
            System.err.println("ERROR_US_PENALTY: Failed to update user " + userId + " after applying penalty.");
            return false;
        }
    }

    public boolean unlockUserAccount(String userId, boolean fineClearedManually) {
        Optional<User> userOpt = findUserById(userId);
        if (userOpt.isEmpty()) {
            System.err.println("ERROR_US_UNLOCK: User " + userId + " not found to unlock.");
            return false;
        }
        User user = userOpt.get();

        if (!user.isAccountLocked()) {
            System.out.println("INFO_US_UNLOCK: User " + userId + " is not locked.");
            return true;
        }

        if (fineClearedManually) { // Admin xác nhận đã xử lý các vấn đề
            user.setAccountLocked(false);
            user.setCurrentFineAmount(0.0); // Reset tiền phạt
            if (user.getReputationScore() < 50) { // Phục hồi điểm tối thiểu
                user.setReputationScore(50);
            }

            if (updateUser(user)) {
                System.out.println("DEBUG_US_UNLOCK: User " + userId + " account unlocked.");
                // Nên gọi NotificationService ở đây
                return true;
            } else {
                System.err.println("ERROR_US_UNLOCK: Failed to update user " + userId + " after unlocking.");
                return false;
            }
        } else {
            System.out.println("INFO_US_UNLOCK: Conditions not met to unlock user " + userId + " (e.g., fine not cleared by admin).");
            return false;
        }
    }
}
package com.lms.quanlythuvien.services.system;

import com.lms.quanlythuvien.models.system.Notification;
import com.lms.quanlythuvien.models.system.Notification.NotificationType; // Đảm bảo import đúng
import com.lms.quanlythuvien.utils.database.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class NotificationService {
    private static NotificationService instance;
    private static final DateTimeFormatter DB_DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private NotificationService() {}

    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    private Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        return new Notification(
                rs.getString("id"),
                rs.getString("userId"),
                rs.getString("message"),
                NotificationType.valueOf(rs.getString("type").toUpperCase()), // Giả sử type trong DB lưu là tên Enum
                rs.getInt("isRead") == 1,
                LocalDateTime.parse(rs.getString("createdAt"), DB_DATETIME_FORMATTER),
                rs.getString("relatedItemId"),
                rs.getString("actionLink")
        );
    }

    public Optional<Notification> createNotification(String userId, String message, NotificationType type, String relatedItemId, String actionLink) {
        String sql = "INSERT INTO Notifications (id, userId, message, type, isRead, createdAt, relatedItemId, actionLink) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        // Tạo đối tượng Notification mới, ID và createdAt sẽ được tự động gán trong constructor của Notification
        Notification notification = new Notification(userId, message, type, relatedItemId, actionLink);

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, notification.getId());
            pstmt.setString(2, notification.getUserId());
            pstmt.setString(3, notification.getMessage());
            pstmt.setString(4, notification.getType().name()); // Lưu tên của Enum constant
            pstmt.setInt(5, notification.isRead() ? 1 : 0);
            pstmt.setString(6, notification.getCreatedAt().format(DB_DATETIME_FORMATTER));
            pstmt.setString(7, notification.getRelatedItemId());
            pstmt.setString(8, notification.getActionLink());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_NOTIF_SVC_CREATE: Notification created. ID: " + notification.getId() + ", UserID: " + userId + ", Type: " + type);
                return Optional.of(notification);
            }
        } catch (SQLException e) {
            System.err.println("ERROR_NOTIF_SVC_CREATE: Could not create notification for UserID: " + userId + ", Type: " + type + ". Error: " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Notification> getNotificationsForUser(String userId, boolean unreadOnly) {
        List<Notification> notifications = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM Notifications WHERE userId = ?");
        if (unreadOnly) {
            sqlBuilder.append(" AND isRead = 0");
        }
        sqlBuilder.append(" ORDER BY createdAt DESC");

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapResultSetToNotification(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_NOTIF_SVC_GET_USER: Could not get notifications for user " + userId + ": " + e.getMessage());
        }
        return notifications;
    }

    public int getUnreadNotificationCount(String userId) {
        String sql = "SELECT COUNT(*) AS count FROM Notifications WHERE userId = ? AND isRead = 0";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_NOTIF_SVC_COUNT: Could not get unread count for user " + userId + ": " + e.getMessage());
        }
        return 0;
    }

    public boolean markAsRead(String notificationId) {
        String sql = "UPDATE Notifications SET isRead = 1 WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, notificationId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_NOTIF_SVC_MARK_READ: Notification " + notificationId + " marked as read.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("ERROR_NOTIF_SVC_MARK_READ: Could not mark notification " + notificationId + " as read: " + e.getMessage());
        }
        return false;
    }

    public boolean markAllAsRead(String userId) {
        String sql = "UPDATE Notifications SET isRead = 1 WHERE userId = ? AND isRead = 0";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_NOTIF_SVC_MARK_ALL_READ: Marked " + affectedRows + " notifications as read for user " + userId);
                return true;
            }
            // System.out.println("INFO_NOTIF_SVC_MARK_ALL_READ: No unread notifications to mark for user " + userId); // Bỏ log này nếu không có gì để update cũng là false
            return false; // Trả về false nếu không có dòng nào được cập nhật
        } catch (SQLException e) {
            System.err.println("ERROR_NOTIF_SVC_MARK_ALL_READ: Could not mark all as read for user " + userId + ": " + e.getMessage());
        }
        return false;
    }

    public boolean deleteNotification(String notificationId) {
        String sql = "DELETE FROM Notifications WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, notificationId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("ERROR_NOTIF_SVC_DELETE: Could not delete notification " + notificationId + ": " + e.getMessage());
        }
        return false;
    }

    public boolean deleteReadNotifications(String userId) {
        String sql = "DELETE FROM Notifications WHERE userId = ? AND isRead = 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("ERROR_NOTIF_SVC_DELETE_READ: Could not delete read notifications for user " + userId + ": " + e.getMessage());
        }
        return false;
    }


    public List<Notification> getNotificationsForAdmin(boolean unreadOnly) {
        System.out.println("DEBUG_NOTIF_SVC_GET_ADMIN: Fetching admin notifications (unreadOnly=" + unreadOnly + ")");
        List<Notification> adminNotifications = new ArrayList<>();

        // *** QUAN TRỌNG: Rà soát và cập nhật danh sách NotificationType này cho đúng với enum Notification.NotificationType của bạn ***
        List<NotificationType> adminNotificationTypes = Arrays.asList(
                NotificationType.NEW_LOAN_REQUEST,        // User yêu cầu mượn sách
                NotificationType.NEW_USER_QUESTION,       // User hỏi đáp
                NotificationType.USER_LOAN_OVERDUE_ADMIN, // Sách của User bị quá hạn
                NotificationType.NEW_DONATION_REQUEST,    // User quyên góp sách
                NotificationType.BOOK_STOCK_LOW_ADMIN,    // Ví dụ: Sách sắp hết trong kho (cần định nghĩa type này)
                NotificationType.SYSTEM_ALERT,            // Ví dụ: Cảnh báo hệ thống chung (cần định nghĩa type này)
                NotificationType.WARNING,                   // Có thể dùng cho các cảnh báo chung khác
                NotificationType.ERROR                      // Có thể dùng cho các lỗi chung khác
                // Bỏ: NotificationType.LOAN_OVERDUE (Vì đã có USER_LOAN_OVERDUE_ADMIN cụ thể hơn,
                // hoặc nếu LOAN_OVERDUE là một type riêng với ý nghĩa khác thì giữ lại và định nghĩa trong Enum)
        );

        if (adminNotificationTypes.isEmpty()) {
            System.out.println("DEBUG_NOTIF_SVC_GET_ADMIN: No specific admin notification types defined to query.");
            return adminNotifications;
        }

        String typePlaceholders = adminNotificationTypes.stream()
                .map(type -> "?")
                .collect(Collectors.joining(", "));

        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM Notifications WHERE type IN (");
        sqlBuilder.append(typePlaceholders);
        sqlBuilder.append(")");
        // Hoặc: Lấy các thông báo có userId là NULL hoặc một giá trị đặc biệt cho admin
        // StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM Notifications WHERE userId IS NULL OR userId = 'ADMIN_SYSTEM'");


        if (unreadOnly) {
            sqlBuilder.append(" AND isRead = 0");
        }
        sqlBuilder.append(" ORDER BY createdAt DESC");

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {

            int paramIndex = 1;
            for (NotificationType type : adminNotificationTypes) {
                pstmt.setString(paramIndex++, type.name()); // Lưu trữ và truy vấn bằng tên của Enum Constant
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    adminNotifications.add(mapResultSetToNotification(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_NOTIF_SVC_GET_ADMIN: DB error fetching admin notifications: " + e.getMessage());
        }

        System.out.println("DEBUG_NOTIF_SVC_GET_ADMIN: Returning " + adminNotifications.size() + " admin notifications.");
        return adminNotifications;
    }

    public boolean markAllAdminNotificationsAsRead() {
        System.out.println("DEBUG_NOTIF_SVC_MARK_ALL_ADMIN_READ: Marking all admin notifications as read.");
        int affectedRows = 0;

        // *** QUAN TRỌNG: Rà soát và cập nhật danh sách NotificationType này cho đúng với enum Notification.NotificationType của bạn ***
        List<NotificationType> adminNotificationTypes = Arrays.asList(
                NotificationType.NEW_LOAN_REQUEST,
                NotificationType.NEW_USER_QUESTION,
                NotificationType.USER_LOAN_OVERDUE_ADMIN,
                NotificationType.NEW_DONATION_REQUEST,
                NotificationType.BOOK_STOCK_LOW_ADMIN,
                NotificationType.SYSTEM_ALERT,
                NotificationType.WARNING,
                NotificationType.ERROR
        );

        if (adminNotificationTypes.isEmpty()) {
            System.out.println("DEBUG_NOTIF_SVC_MARK_ALL_ADMIN_READ: No specific admin notification types defined to update.");
            return false;
        }

        String typePlaceholders = adminNotificationTypes.stream()
                .map(type -> "?")
                .collect(Collectors.joining(", "));
        // Cập nhật tất cả các thông báo (chưa đọc) thuộc các loại dành cho admin
        String sql = "UPDATE Notifications SET isRead = 1 WHERE isRead = 0 AND type IN (" + typePlaceholders + ")";
        // Hoặc nếu admin notifications có userId đặc biệt:
        // String sql = "UPDATE Notifications SET isRead = 1 WHERE isRead = 0 AND (userId IS NULL OR userId = 'ADMIN_SYSTEM')";


        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int paramIndex = 1;
            for (NotificationType type : adminNotificationTypes) {
                pstmt.setString(paramIndex++, type.name());
            }
            affectedRows = pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("ERROR_NOTIF_SVC_MARK_ALL_ADMIN_READ: DB error: " + e.getMessage());
            return false;
        }
        System.out.println("DEBUG_NOTIF_SVC_MARK_ALL_ADMIN_READ: Affected rows: " + affectedRows);
        return affectedRows > 0;
    }
}
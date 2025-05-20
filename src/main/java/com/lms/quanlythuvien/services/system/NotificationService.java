package com.lms.quanlythuvien.services.system;

import com.lms.quanlythuvien.models.system.Notification;
import com.lms.quanlythuvien.models.system.Notification.NotificationType;
import com.lms.quanlythuvien.utils.database.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays; // Thêm import này nếu dùng List.of hoặc Arrays.asList
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors; // Thêm import này nếu dùng stream cho IN clause

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
                rs.getString("userId"), // Có thể null nếu là thông báo hệ thống cho admin
                rs.getString("message"),
                NotificationType.valueOf(rs.getString("type").toUpperCase()),
                rs.getInt("isRead") == 1,
                LocalDateTime.parse(rs.getString("createdAt"), DB_DATETIME_FORMATTER),
                rs.getString("relatedItemId"),
                rs.getString("actionLink")
        );
    }

    public Optional<Notification> createNotification(String userId, String message, NotificationType type, String relatedItemId, String actionLink) {
        String sql = "INSERT INTO Notifications (id, userId, message, type, isRead, createdAt, relatedItemId, actionLink) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Notification notification = new Notification(userId, message, type, relatedItemId, actionLink);

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, notification.getId());
            pstmt.setString(2, notification.getUserId()); // userId có thể null nếu thông báo không cho user cụ thể
            pstmt.setString(3, notification.getMessage());
            pstmt.setString(4, notification.getType().name());
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
            System.out.println("INFO_NOTIF_SVC_MARK_ALL_READ: No unread notifications to mark for user " + userId);
            return false;
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
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_NOTIF_SVC_DELETE: Notification " + notificationId + " deleted.");
                return true;
            }
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
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_NOTIF_SVC_DELETE_READ: Deleted " + affectedRows + " read notifications for user " + userId);
                return true;
            }
            System.out.println("INFO_NOTIF_SVC_DELETE_READ: No read notifications to delete for user " + userId);
            return false;
        } catch (SQLException e) {
            System.err.println("ERROR_NOTIF_SVC_DELETE_READ: Could not delete read notifications for user " + userId + ": " + e.getMessage());
        }
        return false;
    }

    // --- CÁC PHƯƠNG THỨC MỚI CHO ADMIN NOTIFICATIONS (STUBS) ---

    /**
     * Lấy danh sách các thông báo dành cho quản trị viên (chung cho tất cả admin).
     * @param unreadOnly true nếu chỉ lấy các thông báo chưa đọc.
     * @return Danh sách các thông báo.
     */
    public List<Notification> getNotificationsForAdmin(boolean unreadOnly) {
        System.out.println("DEBUG_NOTIF_SVC_GET_ADMIN: Fetching admin notifications (unreadOnly=" + unreadOnly + ") - STUB METHOD - Needs Real Implementation.");
        List<Notification> adminNotifications = new ArrayList<>();

        // TODO: Triển khai logic thực sự để lấy thông báo cho admin.
        // Các loại thông báo admin có thể quan tâm: LOAN_REQUEST, USER_QUESTION, LOAN_OVERDUE (system-wide), etc.
        // Một cách là query các NotificationType cụ thể.
        // Hoặc nếu Notification có trường `userId` là NULL hoặc một giá trị đặc biệt ("ADMIN_ALERTS") cho thông báo hệ thống.

        // Ví dụ query các loại cụ thể:
        List<NotificationType> adminNotificationTypes = Arrays.asList(
                // Thêm các NotificationType mà Admin cần theo dõi từ yêu cầu của cậu
                // Ví dụ (cần định nghĩa các Enum này trong NotificationType nếu chưa có):
                // NotificationType.NEW_LOAN_REQUEST,       // USER yêu cầu mượn sách
                // NotificationType.NEW_USER_QUESTION,      // USER hỏi đáp
                // NotificationType.LOAN_DUE_REMINDER_ADMIN, // USER nào sách đến hạn phải trả
                // NotificationType.FINE_CALCULATED,          // Thông báo khi người này quá hạn phải trả nhiêu phí
                // NotificationType.LOAN_CONFIRMED,           // Thông báo khi đã xác nhận yêu cầu mượn sách
                // NotificationType.BOOK_DELIVERED,           // Đã đưa sách thành công
                NotificationType.LOAN_OVERDUE,             // Thông báo sách quá hạn chung
                NotificationType.WARNING,                  // Cảnh báo hệ thống chung
                NotificationType.ERROR                     // Lỗi hệ thống chung
        );

        if (adminNotificationTypes.isEmpty()) {
            return adminNotifications; // Không có type nào để query
        }

        // Xây dựng phần (type = ? OR type = ? OR ...) hoặc (type IN (?,?,...))
        String typePlaceholders = adminNotificationTypes.stream()
                .map(type -> "?")
                .collect(Collectors.joining(", "));

        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM Notifications WHERE type IN (");
        sqlBuilder.append(typePlaceholders);
        sqlBuilder.append(")");

        if (unreadOnly) {
            sqlBuilder.append(" AND isRead = 0");
        }
        sqlBuilder.append(" ORDER BY createdAt DESC");

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {

            int paramIndex = 1;
            for (NotificationType type : adminNotificationTypes) {
                pstmt.setString(paramIndex++, type.name());
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    adminNotifications.add(mapResultSetToNotification(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_NOTIF_SVC_GET_ADMIN: DB error fetching admin notifications: " + e.getMessage());
        }

        System.out.println("DEBUG_NOTIF_SVC_GET_ADMIN: Returning " + adminNotifications.size() + " admin notifications from stub.");
        return adminNotifications;
    }

    /**
     * Đánh dấu tất cả các thông báo (phù hợp với admin) là đã đọc.
     * @return true nếu có ít nhất một thông báo được cập nhật, false nếu không hoặc có lỗi.
     */
    public boolean markAllAdminNotificationsAsRead() {
        System.out.println("DEBUG_NOTIF_SVC_MARK_ALL_ADMIN_READ: Marking all admin notifications as read - STUB METHOD - Needs Real Implementation.");
        int affectedRows = 0;

        // TODO: Triển khai logic thực sự.
        // Cập nhật tất cả các thông báo có type phù hợp với admin và isRead = 0.
        List<NotificationType> adminNotificationTypes = Arrays.asList(
                // Sao chép danh sách các type từ getNotificationsForAdmin
                NotificationType.LOAN_OVERDUE,
                NotificationType.WARNING,
                NotificationType.ERROR
                // ... thêm các type khác mà admin quản lý
        );

        if (adminNotificationTypes.isEmpty()) {
            return false;
        }

        String typePlaceholders = adminNotificationTypes.stream()
                .map(type -> "?")
                .collect(Collectors.joining(", "));
        String sql = "UPDATE Notifications SET isRead = 1 WHERE isRead = 0 AND type IN (" + typePlaceholders + ")";

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
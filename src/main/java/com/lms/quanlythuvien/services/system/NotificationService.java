package com.lms.quanlythuvien.services.system; // Hoặc services.notification

import com.lms.quanlythuvien.models.system.Notification;
import com.lms.quanlythuvien.models.system.Notification.NotificationType;
import com.lms.quanlythuvien.utils.database.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NotificationService {
    private static NotificationService instance;

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
                NotificationType.valueOf(rs.getString("type").toUpperCase()),
                rs.getInt("isRead") == 1,
                LocalDateTime.parse(rs.getString("createdAt"), DateTimeFormatter.ISO_LOCAL_DATE_TIME), // Giả sử lưu dạng ISO
                rs.getString("relatedItemId"),
                rs.getString("actionLink")
        );
    }

    public Optional<Notification> createNotification(String userId, String message, NotificationType type, String relatedItemId, String actionLink) {
        String sql = "INSERT INTO Notifications (id, userId, message, type, isRead, createdAt, relatedItemId, actionLink) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Notification notification = new Notification(userId, message, type, relatedItemId, actionLink); // Tạo object để lấy ID và createdAt

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, notification.getId());
            pstmt.setString(2, notification.getUserId());
            pstmt.setString(3, notification.getMessage());
            pstmt.setString(4, notification.getType().name());
            pstmt.setInt(5, notification.isRead() ? 1 : 0);
            pstmt.setString(6, notification.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)); // Lưu chuẩn ISO
            pstmt.setString(7, notification.getRelatedItemId());
            pstmt.setString(8, notification.getActionLink());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                return Optional.of(notification);
            }
        } catch (SQLException e) {
            System.err.println("ERROR_NOTIF_SVC_CREATE: Could not create notification: " + e.getMessage());
            e.printStackTrace();
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
            e.printStackTrace();
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
            return pstmt.executeUpdate() > 0;
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
            return pstmt.executeUpdate() > 0; // Trả về true nếu có bất kỳ row nào được cập nhật
        } catch (SQLException e) {
            System.err.println("ERROR_NOTIF_SVC_MARK_ALL_READ: Could not mark all as read for user " + userId + ": " + e.getMessage());
        }
        return false;
    }

    // (Tùy chọn) Xóa thông báo (ví dụ: xóa thông báo đã đọc quá cũ)
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
            // executeUpdate() trả về số dòng bị ảnh hưởng.
            // Nếu > 0 nghĩa là có thông báo đã được xóa.
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("ERROR_NOTIF_SVC_DELETE_READ: Could not delete read notifications for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}
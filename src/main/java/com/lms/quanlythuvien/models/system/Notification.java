package com.lms.quanlythuvien.models.system; // Hoặc một package .notification

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Notification {

    public enum NotificationType {
        INFO,           // Thông tin chung
        SUCCESS,        // Thành công (ví dụ: yêu cầu được duyệt)
        WARNING,        // Cảnh báo (ví dụ: sách sắp hết hạn)
        ERROR,          // Lỗi (ví dụ: yêu cầu bị từ chối)
        LOAN_APPROVED,
        LOAN_REJECTED,
        LOAN_DUE_SOON,
        LOAN_OVERDUE,
        NEW_BOOK_MATCHING_PREFERENCE // Ví dụ cho tương lai
    }

    private String id;          // UUID cho mỗi thông báo
    private String userId;        // ID của người dùng nhận thông báo
    private String message;       // Nội dung thông báo
    private NotificationType type;
    private boolean isRead;
    private LocalDateTime createdAt;
    private String relatedItemId; // ID của đối tượng liên quan (ví dụ: requestId, borrowingRecordId, bookIsbn13)
    private String actionLink;    // (Tùy chọn) Một "link" hoặc action key để điều hướng trong app khi click

    // Constructor khi tạo mới
    public Notification(String userId, String message, NotificationType type, String relatedItemId, String actionLink) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.message = message;
        this.type = type;
        this.isRead = false; // Mặc định là chưa đọc
        this.createdAt = LocalDateTime.now();
        this.relatedItemId = relatedItemId;
        this.actionLink = actionLink;
    }

    // Constructor đầy đủ (khi load từ DB)
    public Notification(String id, String userId, String message, NotificationType type, boolean isRead, LocalDateTime createdAt, String relatedItemId, String actionLink) {
        this.id = id;
        this.userId = userId;
        this.message = message;
        this.type = type;
        this.isRead = isRead;
        this.createdAt = createdAt;
        this.relatedItemId = relatedItemId;
        this.actionLink = actionLink;
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getMessage() { return message; }
    public NotificationType getType() { return type; }
    public boolean isRead() { return isRead; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getRelatedItemId() { return relatedItemId; }
    public String getActionLink() { return actionLink; }

    // Setters
    public void setRead(boolean read) { isRead = read; }
    // Các trường khác thường không thay đổi sau khi tạo

    public String getFormattedCreatedAt() {
        if (createdAt == null) return "";
        // Ví dụ: "10:30 19/05/2025" hoặc "Cách đây 5 phút" (logic phức tạp hơn)
        return createdAt.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
    }
}
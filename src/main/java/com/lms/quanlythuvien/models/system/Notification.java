package com.lms.quanlythuvien.models.system;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Notification {

    public enum NotificationType {
        INFO,                           // Thông tin chung
        SUCCESS,                        // Thành công (ví dụ: yêu cầu được duyệt USER)
        WARNING,                        // Cảnh báo (ví dụ: sách USER sắp hết hạn)
        ERROR,                          // Lỗi (ví dụ: yêu cầu USER bị từ chối)

        // Các loại thông báo cho User (ví dụ)
        LOAN_APPROVED_USER,             // Yêu cầu mượn của USER được duyệt
        LOAN_REJECTED_USER,             // Yêu cầu mượn của USER bị từ chối
        LOAN_DUE_SOON_USER,             // Sách của USER sắp đến hạn trả
        USER_YoutubeED,         // Câu hỏi của USER đã được trả lời
        DONATION_PROCESSED_USER,        // Yêu cầu quyên góp của USER đã được xử lý (duyệt/từ chối)

        // Các loại thông báo cho Admin (đây là các giá trị cậu cần thêm)
        NEW_LOAN_REQUEST,               // <<< THÊM VÀO: Có yêu cầu mượn sách mới từ User
        NEW_USER_QUESTION,              // <<< THÊM VÀO: Có câu hỏi mới từ User
        USER_LOAN_OVERDUE_ADMIN,        // <<< THÊM VÀO: Thông báo cho Admin về sách quá hạn của User cụ thể
        USER_LOAN_RETURNED_ADMIN,       // <<< THÊM VÀO: Thông báo cho Admin khi User trả sách (nếu cần)
        NEW_DONATION_REQUEST_ADMIN,     // <<< THÊM VÀO: Có yêu cầu quyên góp mới
        // Các loại cho Admin khác nếu cần
        LOAN_DUE_SOON_ADMIN,            // Thông báo cho Admin về các sách sắp đến hạn (tổng hợp)
        LOAN_OVERDUE,                   // Giữ lại nếu đây là loại chung cho admin về các sách đã quá hạn
        LOAN_APPROVED,                  // Giữ lại nếu đây là loại chung (xác nhận admin đã duyệt)

        SYSTEM_ALERT,                   // Cảnh báo hệ thống chung cho Admin
        NEW_BOOK_MATCHING_PREFERENCE    // Ví dụ cho tương lai (cho User)
    }

    private String id;
    private String userId; // Có thể là null nếu là thông báo hệ thống cho admin
    private String message;
    private NotificationType type;
    private boolean isRead;
    private LocalDateTime createdAt;
    private String relatedItemId;
    private String actionLink;

    // Constructor khi tạo mới (userId có thể null cho thông báo hệ thống/admin)
    public Notification(String userId, String message, NotificationType type, String relatedItemId, String actionLink) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId; // userId của người nhận, hoặc null/special ID cho admin
        this.message = message;
        this.type = type;
        this.isRead = false;
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

    // Getters and Setters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getMessage() { return message; }
    public NotificationType getType() { return type; }
    public boolean isRead() { return isRead; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getRelatedItemId() { return relatedItemId; }
    public String getActionLink() { return actionLink; }
    public void setRead(boolean read) { isRead = read; }
    // Các trường khác thường không thay đổi sau khi tạo

    public String getFormattedCreatedAt() {
        if (createdAt == null) return "";
        return createdAt.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
    }
}
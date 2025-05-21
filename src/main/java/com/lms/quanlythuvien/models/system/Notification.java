package com.lms.quanlythuvien.models.system;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Notification {

    public enum NotificationType {
        // User-facing types
        INFO("Thông tin chung"),
        SUCCESS("Thành công"),
        WARNING("Cảnh báo"),
        ERROR("Lỗi"),
        LOAN_APPROVED_USER("Yêu cầu mượn được duyệt"),
        LOAN_REJECTED_USER("Yêu cầu mượn bị từ chối"),
        LOAN_DUE_SOON_USER("Sách sắp đến hạn trả"),
        USER_YoutubeED("Câu hỏi đã được trả lời"), // <<< SỬA TYPO TỪ USER_YoutubeED
        DONATION_PROCESSED_USER("Quyên góp đã được xử lý"),
        ACCOUNT_LOCKED_USER("Tài khoản bị khóa"), // Ví dụ thêm
        FINE_ISSUED_USER("Bạn có khoản phạt mới"),   // Ví dụ thêm

        // Admin-facing types
        NEW_LOAN_REQUEST("Yêu cầu mượn mới"),
        NEW_USER_QUESTION("Câu hỏi mới từ người dùng"),
        USER_LOAN_OVERDUE_ADMIN("Sách của người dùng quá hạn"), // Thông báo cho Admin
        NEW_DONATION_REQUEST("Yêu cầu quyên góp mới"), // <<< Giữ lại cái này, có displayName
        // USER_LOAN_RETURNED_ADMIN("User đã trả sách"), // Bỏ nếu không cần thiết, hoặc thêm displayName
        // NEW_DONATION_REQUEST_ADMIN, // <<< BỎ cái này đi để tránh trùng lặp nếu NEW_DONATION_REQUEST đã đủ ý nghĩa

        LOAN_DUE_SOON_ADMIN("Sách sắp hết hạn (Admin)"), // Ví dụ
        // LOAN_OVERDUE, // Nếu dùng chung, nên có displayName rõ ràng "Sách quá hạn (Chung)"
        // LOAN_APPROVED, // Nếu dùng chung

        SYSTEM_ALERT("Cảnh báo hệ thống (Admin)"),
        BOOK_STOCK_LOW_ADMIN("Sách sắp hết trong kho"); // Ví dụ cho Admin

        private final String displayName;

        NotificationType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return this.displayName; // Giúp hiển thị tên thân thiện khi gọi toString()
        }
    }

    private String id;
    private String userId; // ID của người nhận (có thể là user ID, hoặc "ADMIN" nếu cho admin)
    private String message;
    private NotificationType type;
    private boolean isRead;
    private LocalDateTime createdAt;
    private String relatedItemId; // ID của đối tượng liên quan (ví dụ: bookId, requestId)
    private String actionLink;    // Một chuỗi định danh hành động (ví dụ: "VIEW_BOOK_DETAIL")

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    // Constructor khi tạo mới
    public Notification(String userId, String message, NotificationType type, String relatedItemId, String actionLink) {
        this.id = "NOTIF-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase(); // Tạo ID có tiền tố
        this.userId = userId;
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
    // Các trường id, userId, message, type, createdAt, relatedItemId, actionLink thường không thay đổi sau khi tạo.
    // Nếu cần thay đổi, bạn có thể thêm setters cho chúng.

    public String getFormattedCreatedAt() {
        if (createdAt == null) return "N/A";
        return createdAt.format(DATETIME_FORMATTER);
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", type=" + (type != null ? type.getDisplayName() : "null") +
                ", message='" + message + '\'' +
                ", isRead=" + isRead +
                '}';
    }
}
package com.lms.quanlythuvien.models.transaction; // Đảm bảo package này đúng với vị trí file của cậu

import java.time.LocalDate;
import java.util.UUID;

public class DonationRequest {

    // --- SỬA ĐỔI ENUM DONATIONSTATUS Ở ĐÂY ---
    public enum DonationStatus {
        PENDING_APPROVAL("Đang chờ duyệt"),
        APPROVED_PENDING_RECEIPT("Đã duyệt (Chờ nhận sách)"),
        COMPLETED("Đã hoàn tất (Thư viện đã nhận)"),
        REJECTED("Bị từ chối"),
        CANCELED_BY_USER("Người dùng hủy");
        // Bạn có thể thêm các trạng thái khác nếu cần

        private final String displayName;

        DonationStatus(String displayName) {
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
    // --- KẾT THÚC SỬA ĐỔI ENUM ---

    private String requestId;
    private String userId;
    private String bookName;
    private String authorName;
    private String category;
    private String language;
    private String reasonForContribution;
    private LocalDate requestDate;
    private DonationStatus status; // Sử dụng Enum DonationStatus đã cập nhật
    private String adminNotes;
    private LocalDate resolvedDate; // Ngày admin xử lý yêu cầu
    private LocalDate actualReceiptDate; // Ngày thư viện thực sự nhận sách

    // Constructor khi người dùng tạo yêu cầu mới
    public DonationRequest(String userId, String bookName, String authorName, String category, String language, String reason) {
        this.requestId = "DON_REQ_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(); // Ví dụ tạo ID dễ nhìn hơn
        this.userId = userId;
        this.bookName = bookName;
        this.authorName = authorName;
        this.category = category;
        this.language = language;
        this.reasonForContribution = reason;
        this.requestDate = LocalDate.now();
        this.status = DonationStatus.PENDING_APPROVAL; // Trạng thái mặc định khi mới tạo
    }

    // Constructor đầy đủ (khi load từ DB)
    public DonationRequest(String requestId, String userId, String bookName, String authorName,
                           String category, String language, String reasonForContribution,
                           LocalDate requestDate, DonationStatus status, String adminNotes,
                           LocalDate resolvedDate, LocalDate actualReceiptDate) {
        this.requestId = requestId;
        this.userId = userId;
        this.bookName = bookName;
        this.authorName = authorName;
        this.category = category;
        this.language = language;
        this.reasonForContribution = reasonForContribution;
        this.requestDate = requestDate;
        this.status = status;
        this.adminNotes = adminNotes;
        this.resolvedDate = resolvedDate;
        this.actualReceiptDate = actualReceiptDate;
    }

    // Getters
    public String getRequestId() { return requestId; }
    public String getUserId() { return userId; }
    public String getBookName() { return bookName; }
    public String getAuthorName() { return authorName; }
    public String getCategory() { return category; }
    public String getLanguage() { return language; }
    public String getReasonForContribution() { return reasonForContribution; }
    public LocalDate getRequestDate() { return requestDate; }
    public DonationStatus getStatus() { return status; }
    public String getAdminNotes() { return adminNotes; }
    public LocalDate getResolvedDate() { return resolvedDate; }
    public LocalDate getActualReceiptDate() { return actualReceiptDate; }

    // Setters
    // requestId, userId, bookName, authorName, category, language, reasonForContribution, requestDate
    // thường không thay đổi sau khi tạo yêu cầu ban đầu.
    // Các trường có thể thay đổi là status, adminNotes, resolvedDate, actualReceiptDate.
    public void setStatus(DonationStatus status) { this.status = status; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
    public void setResolvedDate(LocalDate resolvedDate) { this.resolvedDate = resolvedDate; }
    public void setActualReceiptDate(LocalDate actualReceiptDate) { this.actualReceiptDate = actualReceiptDate; }

    @Override
    public String toString() {
        return "DonationRequest{" +
                "requestId='" + requestId + '\'' +
                ", bookName='" + bookName + '\'' +
                ", userId='" + userId + '\'' +
                ", status=" + (status != null ? status.getDisplayName() : "null") + // Sử dụng getDisplayName
                '}';
    }
}
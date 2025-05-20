package com.lms.quanlythuvien.models.transaction; // Hoặc package phù hợp của bạn

import java.time.LocalDate;
import java.util.UUID;

public class BorrowingRequest {

    // --- SỬA ĐỔI ENUM REQUESTSTATUS Ở ĐÂY ---
    public enum RequestStatus {
        PENDING("Đang chờ duyệt"),
        APPROVED("Đã duyệt (Chờ lấy)"), // Sửa displayName cho rõ ràng hơn
        REJECTED("Bị từ chối"),
        CANCELED_BY_USER("Người dùng hủy"),
        COMPLETED("Hoàn thành (Đã mượn)"), // Khi sách đã được thực sự cho mượn sau khi duyệt
        EXPIRED("Hết hạn (Không lấy sách)"); // Nếu user không đến lấy sách sau khi được duyệt

        private final String displayName;

        RequestStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return this.displayName; // Có thể override toString để trả về displayName
        }
    }
    // --- KẾT THÚC SỬA ĐỔI ENUM ---

    private String requestId;
    private String userId;
    private String bookIsbn13; // Sử dụng ISBN-13 để tham chiếu sách
    private LocalDate requestDate;
    private RequestStatus status; // Sử dụng Enum RequestStatus đã cập nhật
    private String adminNotes;
    private LocalDate resolvedDate; // Ngày yêu cầu được xử lý (duyệt/từ chối/hủy)
    private LocalDate pickupDueDate;  // Hạn chót để người dùng đến lấy sách sau khi được duyệt

    // Constructor khi user tạo yêu cầu mới
    public BorrowingRequest(String userId, String bookIsbn13) {
        this.requestId = "REQ-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(); // Ví dụ tạo ID
        this.userId = userId;
        this.bookIsbn13 = bookIsbn13;
        this.requestDate = LocalDate.now();
        this.status = RequestStatus.PENDING; // Trạng thái mặc định khi mới tạo
    }

    // Constructor đầy đủ (khi load từ DB hoặc các trường hợp khác)
    public BorrowingRequest(String requestId, String userId, String bookIsbn13, LocalDate requestDate,
                            RequestStatus status, String adminNotes, LocalDate resolvedDate, LocalDate pickupDueDate) {
        this.requestId = requestId;
        this.userId = userId;
        this.bookIsbn13 = bookIsbn13;
        this.requestDate = requestDate;
        this.status = status;
        this.adminNotes = adminNotes;
        this.resolvedDate = resolvedDate;
        this.pickupDueDate = pickupDueDate;
    }

    // Getters
    public String getRequestId() { return requestId; }
    public String getUserId() { return userId; }
    public String getBookIsbn13() { return bookIsbn13; }
    public LocalDate getRequestDate() { return requestDate; }
    public RequestStatus getStatus() { return status; }
    public String getAdminNotes() { return adminNotes; }
    public LocalDate getResolvedDate() { return resolvedDate; }
    public LocalDate getPickupDueDate() { return pickupDueDate; }

    // Setters
    // requestId, userId, bookIsbn13, requestDate thường không thay đổi sau khi tạo
    // trừ khi có logic đặc biệt (ví dụ: gán requestId từ DB sau khi insert)
    public void setRequestId(String requestId) { this.requestId = requestId; }
    // public void setUserId(String userId) { this.userId = userId; }
    // public void setBookIsbn13(String bookIsbn13) { this.bookIsbn13 = bookIsbn13; }
    // public void setRequestDate(LocalDate requestDate) { this.requestDate = requestDate; }

    public void setStatus(RequestStatus status) { this.status = status; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
    public void setResolvedDate(LocalDate resolvedDate) { this.resolvedDate = resolvedDate; }
    public void setPickupDueDate(LocalDate pickupDueDate) { this.pickupDueDate = pickupDueDate; }


    @Override
    public String toString() {
        return "BorrowingRequest{" +
                "requestId='" + requestId + '\'' +
                ", userId='" + userId + '\'' +
                ", bookIsbn13='" + bookIsbn13 + '\'' +
                ", status=" + (status != null ? status.getDisplayName() : "null") + // Sử dụng displayName trong toString
                '}';
    }
}
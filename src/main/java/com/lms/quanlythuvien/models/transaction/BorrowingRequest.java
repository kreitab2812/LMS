package com.lms.quanlythuvien.models.transaction;

import java.time.LocalDate;
import java.util.UUID;

public class BorrowingRequest {

    public enum RequestStatus { PENDING, APPROVED, REJECTED, CANCELED_BY_USER, COMPLETED, EXPIRED }

    private String requestId;
    private String userId;
    private String bookIsbn13; // <<< Sử dụng ISBN-13 để tham chiếu sách
    private LocalDate requestDate;
    private RequestStatus status;
    private String adminNotes;
    private LocalDate resolvedDate; // Ngày yêu cầu được xử lý (duyệt/từ chối/hủy)
    private LocalDate pickupDueDate;  // Hạn chót để người dùng đến lấy sách sau khi được duyệt

    // Constructor khi user tạo yêu cầu mới
    public BorrowingRequest(String userId, String bookIsbn13) {
        this.requestId = UUID.randomUUID().toString();
        this.userId = userId;
        this.bookIsbn13 = bookIsbn13;
        this.requestDate = LocalDate.now();
        this.status = RequestStatus.PENDING;
    }

    // Constructor đầy đủ (khi load từ DB)
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
    public void setStatus(RequestStatus status) { this.status = status; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
    public void setResolvedDate(LocalDate resolvedDate) { this.resolvedDate = resolvedDate; }
    public void setPickupDueDate(LocalDate pickupDueDate) { this.pickupDueDate = pickupDueDate; }
    // requestId, userId, bookIsbn13, requestDate thường không thay đổi sau khi tạo

    @Override
    public String toString() {
        return "BorrowingRequest{" + "requestId='" + requestId + '\'' + ", userId='" + userId + '\'' +
                ", bookIsbn13='" + bookIsbn13 + '\'' + ", status=" + status + '}';
    }
}
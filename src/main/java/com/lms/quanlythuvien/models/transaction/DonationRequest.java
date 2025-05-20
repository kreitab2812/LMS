package com.lms.quanlythuvien.models.transaction; // Đảm bảo package này đúng với vị trí file của cậu

import java.time.LocalDate;
import java.util.UUID;

public class DonationRequest {

    public enum DonationStatus {
        PENDING_APPROVAL,
        APPROVED_PENDING_RECEIPT,
        COMPLETED,
        REJECTED,
        CANCELED_BY_USER
    }

    private String requestId;
    private String userId;
    private String bookName;
    private String authorName;
    private String category;
    private String language;
    private String reasonForContribution;
    private LocalDate requestDate;
    private DonationStatus status;
    private String adminNotes;
    private LocalDate resolvedDate;
    private LocalDate actualReceiptDate;

    // Constructor khi người dùng tạo yêu cầu mới
    public DonationRequest(String userId, String bookName, String authorName, String category, String language, String reason) {
        this.requestId = UUID.randomUUID().toString();
        this.userId = userId;
        this.bookName = bookName;
        this.authorName = authorName;
        this.category = category;
        this.language = language;
        this.reasonForContribution = reason;
        this.requestDate = LocalDate.now();
        this.status = DonationStatus.PENDING_APPROVAL;
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
    public void setStatus(DonationStatus status) { this.status = status; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
    public void setResolvedDate(LocalDate resolvedDate) { this.resolvedDate = resolvedDate; }
    public void setActualReceiptDate(LocalDate actualReceiptDate) { this.actualReceiptDate = actualReceiptDate; }

    @Override
    public String toString() {
        return "'" + bookName + "' bởi " + authorName + " (Ngày: " + requestDate + ", TT: " + status + ")";
    }
}
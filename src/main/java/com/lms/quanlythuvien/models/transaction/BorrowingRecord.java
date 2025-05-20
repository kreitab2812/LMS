package com.lms.quanlythuvien.models.transaction;

import java.time.LocalDate;

public class BorrowingRecord {
    private int recordId;          // ID tự tăng từ DB
    private int bookInternalId;    // Khóa ngoại đến Books.internalId
    private String userId;         // Khóa ngoại đến Users.id (YYYYMM-NNNN)
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;    // Nullable
    private LoanStatus status;

    // Constructor khi tạo mới (chưa có recordId)
    public BorrowingRecord(int bookInternalId, String userId, LocalDate borrowDate, LocalDate dueDate) {
        this.bookInternalId = bookInternalId;
        this.userId = userId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = null;
        this.status = LoanStatus.ACTIVE;
    }

    // Constructor đầy đủ (khi tải từ DB)
    public BorrowingRecord(int recordId, int bookInternalId, String userId,
                           LocalDate borrowDate, LocalDate dueDate,
                           LocalDate returnDate, LoanStatus status) {
        this.recordId = recordId;
        this.bookInternalId = bookInternalId;
        this.userId = userId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status;
    }

    // Getters
    public int getRecordId() { return recordId; }
    public int getBookInternalId() { return bookInternalId; }
    public String getUserId() { return userId; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public LoanStatus getStatus() { return status; }

    // Setters
    public void setRecordId(int recordId) { this.recordId = recordId; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public void setStatus(LoanStatus status) { this.status = status; }

    public boolean isOverdue(LocalDate currentDate) {
        return this.returnDate == null &&
                (this.status == LoanStatus.ACTIVE || this.status == LoanStatus.OVERDUE) &&
                currentDate.isAfter(this.dueDate);
    }

    @Override
    public String toString() {
        return "BorrowingRecord{" +
                "recordId=" + recordId +
                ", bookInternalId=" + bookInternalId +
                ", userId='" + userId + '\'' +
                ", borrowDate=" + borrowDate +
                ", dueDate=" + dueDate +
                ", returnDate=" + returnDate +
                ", status=" + status +
                '}';
    }
}
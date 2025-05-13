package com.lms.quanlythuvien.models; // Hoặc package phù hợp của cậu

import java.time.LocalDate;
import java.util.UUID;

public class BorrowingRecord {
    private String recordId;
    private String bookId;
    private String userId;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate; // Nullable
    private LoanStatus status;

    // Constructor khi tạo một lượt mượn mới
    public BorrowingRecord(String bookId, String userId, LocalDate borrowDate, LocalDate dueDate) {
        this.recordId = UUID.randomUUID().toString();
        this.bookId = bookId;
        this.userId = userId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = null; // Ban đầu chưa trả
        this.status = LoanStatus.ACTIVE; // Ban đầu là đang mượn
        // Có thể thêm logic kiểm tra nếu borrowDate > dueDate thì ném lỗi
    }

    // Constructor đầy đủ (ví dụ khi tải từ cơ sở dữ liệu)
    public BorrowingRecord(String recordId, String bookId, String userId,
                           LocalDate borrowDate, LocalDate dueDate,
                           LocalDate returnDate, LoanStatus status) {
        this.recordId = recordId;
        this.bookId = bookId;
        this.userId = userId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status;
    }

    // Getters
    public String getRecordId() { return recordId; }
    public String getBookId() { return bookId; }
    public String getUserId() { return userId; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public LoanStatus getStatus() { return status; }

    // Setters (chủ yếu cho returnDate và status khi có thay đổi)
    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    // (Tùy chọn) Các setter khác nếu cần, nhưng cẩn thận với việc thay đổi các thông tin cốt lõi sau khi tạo.
    // Ví dụ: bookId, userId, borrowDate, dueDate thường không nên thay đổi sau khi bản ghi đã được tạo.

    /**
     * Kiểm tra xem lượt mượn này có bị quá hạn hay không, dựa trên ngày hiện tại.
     * Chỉ có ý nghĩa nếu sách chưa được trả.
     * @param currentDate Ngày hiện tại để so sánh.
     * @return true nếu quá hạn và chưa trả, false nếu không.
     */
    public boolean isOverdue(LocalDate currentDate) {
        return this.returnDate == null && currentDate.isAfter(this.dueDate);
    }

    @Override
    public String toString() {
        return "BorrowingRecord{" +
                "recordId='" + recordId + '\'' +
                ", bookId='" + bookId + '\'' +
                ", userId='" + userId + '\'' +
                ", borrowDate=" + borrowDate +
                ", dueDate=" + dueDate +
                ", returnDate=" + returnDate +
                ", status=" + status +
                '}';
    }
}
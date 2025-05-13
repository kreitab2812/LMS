package com.lms.quanlythuvien.services; // Hoặc package phù hợp của cậu

import com.lms.quanlythuvien.models.BorrowingRecord;
import com.lms.quanlythuvien.models.LoanStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BorrowingRecordService {

    private final List<BorrowingRecord> loanRecords;

    public BorrowingRecordService() {
        this.loanRecords = new ArrayList<>();
        // Khởi tạo một vài bản ghi mượn mẫu nếu cần để test
        // initializeSampleLoanRecords();
    }

    // (Tùy chọn) Phương thức để thêm các bản ghi mượn mẫu
    private void initializeSampleLoanRecords() {
        // Cần bookId và userId hợp lệ từ các service khác hoặc tạo mẫu
        // Ví dụ:
        // BorrowingRecord record1 = new BorrowingRecord("bookId1", "userId1", LocalDate.now().minusDays(10), LocalDate.now().minusDays(3)); // Quá hạn
        // BorrowingRecord record2 = new BorrowingRecord("bookId2", "userId1", LocalDate.now().minusDays(5), LocalDate.now().plusDays(2));  // Đang mượn, chưa hạn
        // loanRecords.add(record1);
        // loanRecords.add(record2);
        // updateAllOverdueStatuses(); // Cập nhật trạng thái quá hạn
    }

    /**
     * Tạo một bản ghi mượn sách mới.
     * @param bookId ID của sách.
     * @param userId ID của người mượn.
     * @param borrowDate Ngày mượn.
     * @param dueDate Ngày hẹn trả.
     * @return Đối tượng BorrowingRecord vừa được tạo.
     */
    public BorrowingRecord createLoan(String bookId, String userId, LocalDate borrowDate, LocalDate dueDate) {
        if (bookId == null || userId == null || borrowDate == null || dueDate == null) {
            // Trong ứng dụng thực tế, nên ném một IllegalArgumentException
            System.err.println("Invalid parameters for creating a loan.");
            return null;
        }
        if (borrowDate.isAfter(dueDate)) {
            System.err.println("Borrow date cannot be after due date.");
            return null;
        }

        BorrowingRecord newLoan = new BorrowingRecord(bookId, userId, borrowDate, dueDate);
        this.loanRecords.add(newLoan);
        System.out.println("New loan created: " + newLoan.getRecordId() + " for book " + bookId);
        return newLoan;
    }

    /**
     * Ghi nhận việc trả sách cho một lượt mượn.
     * @param loanRecordId ID của bản ghi mượn.
     * @param actualReturnDate Ngày thực tế trả sách.
     * @return true nếu cập nhật thành công, false nếu không tìm thấy bản ghi hoặc sách đã được trả.
     */
    public boolean recordBookReturn(String loanRecordId, LocalDate actualReturnDate) {
        if (loanRecordId == null || actualReturnDate == null) {
            System.err.println("Loan Record ID or return date cannot be null.");
            return false;
        }
        Optional<BorrowingRecord> loanOpt = findLoanById(loanRecordId);
        if (loanOpt.isPresent()) {
            BorrowingRecord loan = loanOpt.get();
            if (loan.getStatus() == LoanStatus.RETURNED) {
                System.err.println("Book for loan " + loanRecordId + " has already been returned.");
                return false;
            }
            loan.setReturnDate(actualReturnDate);
            loan.setStatus(LoanStatus.RETURNED);
            System.out.println("Loan " + loanRecordId + " marked as RETURNED on " + actualReturnDate);
            return true;
        }
        System.err.println("Loan " + loanRecordId + " not found for returning.");
        return false;
    }

    /**
     * Tìm một bản ghi mượn theo ID.
     * @param loanRecordId ID của bản ghi mượn.
     * @return Optional chứa BorrowingRecord nếu tìm thấy.
     */
    public Optional<BorrowingRecord> findLoanById(String loanRecordId) {
        if (loanRecordId == null) return Optional.empty();
        return this.loanRecords.stream()
                .filter(record -> loanRecordId.equals(record.getRecordId()))
                .findFirst();
    }

    /**
     * Lấy tất cả các lượt mượn đang hoạt động (chưa trả) của một người dùng.
     * Bao gồm cả ACTIVE và OVERDUE.
     * @param userId ID của người dùng.
     * @return Danh sách các BorrowingRecord.
     */
    public List<BorrowingRecord> getActiveLoansByUserId(String userId) {
        if (userId == null) return new ArrayList<>();
        // Cập nhật trạng thái quá hạn trước khi trả về có thể là một ý hay
        updateAllOverdueStatuses(LocalDate.now());
        return this.loanRecords.stream()
                .filter(record -> userId.equals(record.getUserId()) &&
                        (record.getStatus() == LoanStatus.ACTIVE || record.getStatus() == LoanStatus.OVERDUE))
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả các lượt mượn của một cuốn sách cụ thể mà chưa được trả.
     * @param bookId ID của cuốn sách.
     * @return Danh sách các BorrowingRecord.
     */
    public List<BorrowingRecord> getActiveLoansByBookId(String bookId) {
        if (bookId == null) return new ArrayList<>();
        updateAllOverdueStatuses(LocalDate.now());
        return this.loanRecords.stream()
                .filter(record -> bookId.equals(record.getBookId()) &&
                        (record.getStatus() == LoanStatus.ACTIVE || record.getStatus() == LoanStatus.OVERDUE))
                .collect(Collectors.toList());
    }


    /**
     * Cập nhật trạng thái của các lượt mượn ACTIVE thành OVERDUE nếu cần.
     * @param currentDate Ngày hiện tại để so sánh.
     */
    public void updateAllOverdueStatuses(LocalDate currentDate) {
        this.loanRecords.stream()
                .filter(record -> record.getStatus() == LoanStatus.ACTIVE && record.isOverdue(currentDate))
                .forEach(record -> {
                    record.setStatus(LoanStatus.OVERDUE);
                    System.out.println("Loan " + record.getRecordId() + " status updated to OVERDUE.");
                });
    }

    /**
     * Lấy danh sách các lượt mượn đã quá hạn (và chưa trả).
     * Đồng thời cập nhật trạng thái của chúng nếu chưa phải là OVERDUE.
     * @param currentDate Ngày hiện tại để kiểm tra.
     * @return Danh sách các BorrowingRecord quá hạn.
     */
    public List<BorrowingRecord> getOverdueLoans(LocalDate currentDate) {
        updateAllOverdueStatuses(currentDate); // Đảm bảo trạng thái được cập nhật
        return this.loanRecords.stream()
                .filter(record -> record.getStatus() == LoanStatus.OVERDUE)
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả các bản ghi mượn trong hệ thống.
     * @return Danh sách tất cả BorrowingRecord.
     */
    public List<BorrowingRecord> getAllLoans() {
        // Cân nhắc việc cập nhật trạng thái quá hạn ở đây nếu cần dữ liệu mới nhất
        // updateAllOverdueStatuses(LocalDate.now());
        return new ArrayList<>(this.loanRecords); // Trả về bản sao
    }
}
package com.lms.quanlythuvien.services; // Hoặc package phù hợp của cậu

import com.lms.quanlythuvien.models.BorrowingRecord;
import com.lms.quanlythuvien.models.LoanStatus;
import com.lms.quanlythuvien.models.Book; // Cần cho initializeSampleData
import com.lms.quanlythuvien.models.User; // Cần cho initializeSampleData


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BorrowingRecordService {

    private static BorrowingRecordService instance; // <<<--- BIẾN STATIC CHO SINGLETON
    private final List<BorrowingRecord> loanRecords;

    // <<<--- CONSTRUCTOR LÀ PRIVATE CHO SINGLETON
    private BorrowingRecordService() {
        this.loanRecords = new ArrayList<>();
        // Không gọi initializeSampleData() ở đây ngay để tránh vấn đề thứ tự khởi tạo Singleton
        System.out.println("DEBUG_BRS_SINGLETON: BorrowingRecordService Singleton instance created. Initial loan records count: " + this.loanRecords.size());
    }

    // <<<--- PHƯƠNG THỨC STATIC ĐỂ LẤY INSTANCE SINGLETON
    public static synchronized BorrowingRecordService getInstance() {
        if (instance == null) {
            instance = new BorrowingRecordService();
        }
        return instance;
    }

    // Phương thức này nên được gọi một cách rõ ràng sau khi các service khác đã sẵn sàng
    public void initializeSampleData() {
        // Chỉ khởi tạo nếu danh sách rỗng, để tránh tạo lại dữ liệu mẫu mỗi lần gọi
        if (!this.loanRecords.isEmpty()) {
            System.out.println("DEBUG_BRS_INIT_SAMPLES: Sample loan records already exist or list is not empty. Skipping re-initialization.");
            // Cập nhật trạng thái quá hạn cho dữ liệu hiện có nếu cần
            updateAllOverdueStatuses(LocalDate.now());
            return;
        }
        System.out.println("DEBUG_BRS_INIT_SAMPLES: Initializing sample loan records...");

        BookManagementService bms = BookManagementService.getInstance(); // Lấy instance singleton
        UserService us = UserService.getInstance();                     // Lấy instance singleton

        List<Book> sampleBooks = bms.getAllBooksInLibrary();
        // Lấy user thường để tạo bản ghi mượn
        List<User> sampleUsers = us.getAllUsers().stream()
                .filter(u -> u.getRole() == User.Role.USER)
                .collect(Collectors.toList());

        if (!sampleBooks.isEmpty() && !sampleUsers.isEmpty()) {
            User testUser1 = sampleUsers.get(0); // Lấy user thường đầu tiên

            // Lượt mượn 1: Đang hoạt động, chưa quá hạn
            // Cố gắng tìm sách cụ thể bằng ID hoặc ISBN từ danh sách mẫu của BookManagementService
            Book bookToLoan1 = bms.findBookByIdInLibrary("978-0132350884").orElse(null);
            if (bookToLoan1 != null && bookToLoan1.getAvailableQuantity() > 0) {
                if (bms.handleBookBorrowed(bookToLoan1.getId())) { // Giảm availableQuantity
                    BorrowingRecord record1 = createLoan(bookToLoan1.getId(), testUser1.getUserId(),
                            LocalDate.now().minusDays(2), // Mượn 2 ngày trước
                            LocalDate.now().plusDays(5));  // Hẹn trả 5 ngày nữa
                    if (record1 != null) {
                        us.recordNewLoanForUser(testUser1.getUserId(), record1.getRecordId());
                        System.out.println("DEBUG_BRS_INIT_SAMPLES: Created sample loan 1: " + record1.getRecordId());
                    } else {
                        bms.handleBookReturned(bookToLoan1.getId()); // Hoàn tác nếu tạo loan lỗi
                    }
                } else {
                    System.out.println("DEBUG_BRS_INIT_SAMPLES: Could not borrow book " + bookToLoan1.getId() + " for sample loan 1 (already unavailable or error).");
                }
            } else {
                System.out.println("DEBUG_BRS_INIT_SAMPLES: Book for sample loan 1 (978-0132350884) not found or not available.");
            }

            // Lượt mượn 2: Đã quá hạn
            Book bookToLoan2 = bms.findBookByIdInLibrary("978-0321765723").orElse(null);
            if (bookToLoan2 != null && bookToLoan2.getAvailableQuantity() > 0) {
                if (bms.handleBookBorrowed(bookToLoan2.getId())) { // Giảm availableQuantity
                    BorrowingRecord record2 = createLoan(bookToLoan2.getId(), testUser1.getUserId(),
                            LocalDate.now().minusDays(10), // Mượn 10 ngày trước
                            LocalDate.now().minusDays(3)); // Đã quá hạn 3 ngày
                    if (record2 != null) {
                        us.recordNewLoanForUser(testUser1.getUserId(), record2.getRecordId());
                        System.out.println("DEBUG_BRS_INIT_SAMPLES: Created sample loan 2 (overdue): " + record2.getRecordId());
                    } else {
                        bms.handleBookReturned(bookToLoan2.getId());
                    }
                } else {
                    System.out.println("DEBUG_BRS_INIT_SAMPLES: Could not borrow book " + bookToLoan2.getId() + " for sample loan 2 (already unavailable or error).");
                }
            } else {
                System.out.println("DEBUG_BRS_INIT_SAMPLES: Book for sample loan 2 (978-0321765723) not found or not available.");
            }
        } else {
            System.out.println("DEBUG_BRS_INIT_SAMPLES: Not enough sample books or users to create sample loans.");
        }
        updateAllOverdueStatuses(LocalDate.now()); // Cập nhật trạng thái quá hạn
        System.out.println("DEBUG_BRS_INIT_SAMPLES: Sample loan data initialization finished. Total loans: " + this.loanRecords.size());
    }

    public BorrowingRecord createLoan(String bookId, String userId, LocalDate borrowDate, LocalDate dueDate) {
        if (bookId == null || userId == null || borrowDate == null || dueDate == null) {
            System.err.println("ERROR_BRS_CREATE_LOAN: Invalid parameters for creating a loan.");
            return null;
        }
        if (borrowDate.isAfter(dueDate)) {
            System.err.println("ERROR_BRS_CREATE_LOAN: Borrow date cannot be after due date.");
            return null;
        }
        // Kiểm tra xem có bản ghi mượn ACTIVE hoặc OVERDUE nào cho cùng sách và user không (tránh mượn trùng)
        boolean alreadyBorrowed = this.loanRecords.stream()
                .anyMatch(r -> r.getBookId().equals(bookId) &&
                        r.getUserId().equals(userId) &&
                        (r.getStatus() == LoanStatus.ACTIVE || r.getStatus() == LoanStatus.OVERDUE));
        if (alreadyBorrowed) {
            System.err.println("ERROR_BRS_CREATE_LOAN: User " + userId + " is already actively borrowing book " + bookId);
            return null; // Hoặc ném lỗi/trả về thông báo phù hợp
        }

        BorrowingRecord newLoan = new BorrowingRecord(bookId, userId, borrowDate, dueDate);
        this.loanRecords.add(newLoan);
        System.out.println("DEBUG_BRS_CREATE_LOAN: New loan created: " + newLoan.getRecordId() + " for book " + bookId + ". Total loans: " + loanRecords.size());
        return newLoan;
    }

    public boolean recordBookReturn(String loanRecordId, LocalDate actualReturnDate) {
        if (loanRecordId == null || actualReturnDate == null) {
            System.err.println("ERROR_BRS_RETURN: Loan Record ID or return date cannot be null.");
            return false;
        }
        Optional<BorrowingRecord> loanOpt = findLoanById(loanRecordId);
        if (loanOpt.isPresent()) {
            BorrowingRecord loan = loanOpt.get();
            if (loan.getStatus() == LoanStatus.RETURNED) {
                System.err.println("WARN_BRS_RETURN: Book for loan " + loanRecordId + " has already been returned.");
                return false;
            }
            loan.setReturnDate(actualReturnDate);
            loan.setStatus(LoanStatus.RETURNED); // Đặt trạng thái là RETURNED
            // Không cần kiểm tra isOverdue ở đây nữa vì sách đã trả, trạng thái cuối cùng là RETURNED
            System.out.println("DEBUG_BRS_RETURN: Loan " + loanRecordId + " marked as RETURNED on " + actualReturnDate);
            return true;
        }
        System.err.println("ERROR_BRS_RETURN: Loan " + loanRecordId + " not found for returning.");
        return false;
    }

    public Optional<BorrowingRecord> findLoanById(String loanRecordId) {
        if (loanRecordId == null) return Optional.empty();
        return this.loanRecords.stream()
                .filter(record -> loanRecordId.equals(record.getRecordId()))
                .findFirst();
    }

    public List<BorrowingRecord> getActiveLoansByUserId(String userId) {
        if (userId == null) return new ArrayList<>();
        updateAllOverdueStatuses(LocalDate.now()); // Cập nhật trạng thái trước khi lấy
        return this.loanRecords.stream()
                .filter(record -> userId.equals(record.getUserId()) &&
                        (record.getStatus() == LoanStatus.ACTIVE || record.getStatus() == LoanStatus.OVERDUE))
                .collect(Collectors.toList());
    }

    public List<BorrowingRecord> getActiveLoansByBookId(String bookId) {
        if (bookId == null) return new ArrayList<>();
        updateAllOverdueStatuses(LocalDate.now());
        return this.loanRecords.stream()
                .filter(record -> bookId.equals(record.getBookId()) &&
                        (record.getStatus() == LoanStatus.ACTIVE || record.getStatus() == LoanStatus.OVERDUE))
                .collect(Collectors.toList());
    }

    public void updateAllOverdueStatuses(LocalDate currentDate) {
        System.out.println("DEBUG_BRS_OVERDUE_UPDATE: Checking and updating overdue statuses for date: " + currentDate);
        final int[] updatedCount = {0}; // Biến để đếm số lượng cập nhật (phải là final array hoặc AtomicInteger trong lambda)
        this.loanRecords.stream()
                .filter(record -> record.getStatus() == LoanStatus.ACTIVE && record.isOverdue(currentDate))
                .forEach(record -> {
                    record.setStatus(LoanStatus.OVERDUE);
                    updatedCount[0]++;
                    System.out.println("DEBUG_BRS_OVERDUE_UPDATE: Loan " + record.getRecordId() + " status changed to OVERDUE.");
                });
        if (updatedCount[0] > 0) {
            System.out.println("DEBUG_BRS_OVERDUE_UPDATE: Total " + updatedCount[0] + " loans updated to OVERDUE.");
        }
    }

    public List<BorrowingRecord> getOverdueLoans(LocalDate currentDate) {
        updateAllOverdueStatuses(currentDate);
        return this.loanRecords.stream()
                .filter(record -> record.getStatus() == LoanStatus.OVERDUE)
                .collect(Collectors.toList());
    }

    public List<BorrowingRecord> getAllLoans() {
        // updateAllOverdueStatuses(LocalDate.now()); // Cân nhắc có nên gọi ở đây không, có thể làm chậm nếu list lớn
        return new ArrayList<>(this.loanRecords);
    }
}
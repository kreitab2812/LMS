package com.lms.quanlythuvien.services.transaction;

import com.lms.quanlythuvien.models.transaction.BorrowingRecord;
import com.lms.quanlythuvien.models.transaction.LoanStatus;
import com.lms.quanlythuvien.services.library.BookManagementService; // Đảm bảo import đúng
import com.lms.quanlythuvien.utils.database.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BorrowingRecordService {

    private static BorrowingRecordService instance;
    private final BookManagementService bookManagementService; // Sử dụng final
    private static final DateTimeFormatter DB_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private BorrowingRecordService() {
        this.bookManagementService = BookManagementService.getInstance();
        System.out.println("DEBUG_BRS_SINGLETON: BorrowingRecordService Singleton instance created.");
    }

    public static synchronized BorrowingRecordService getInstance() {
        if (instance == null) {
            instance = new BorrowingRecordService();
        }
        return instance;
    }

    private BorrowingRecord mapResultSetToBorrowingRecord(ResultSet rs) throws SQLException {
        String returnDateStr = rs.getString("returnDate");
        return new BorrowingRecord(
                rs.getInt("id"),
                rs.getInt("bookInternalId"),
                rs.getString("userId"),
                LocalDate.parse(rs.getString("borrowDate"), DB_DATE_FORMATTER),
                LocalDate.parse(rs.getString("dueDate"), DB_DATE_FORMATTER),
                returnDateStr != null ? LocalDate.parse(returnDateStr, DB_DATE_FORMATTER) : null,
                LoanStatus.valueOf(rs.getString("status").toUpperCase())
        );
    }

    public Optional<BorrowingRecord> createLoan(String bookIsbn13, String userId, LocalDate borrowDate, LocalDate dueDate) {
        if (bookIsbn13 == null || userId == null || borrowDate == null || dueDate == null) {
            System.err.println("ERROR_BRS_CREATE_LOAN: Invalid parameters for creating a loan.");
            return Optional.empty();
        }
        if (borrowDate.isAfter(dueDate)) {
            System.err.println("ERROR_BRS_CREATE_LOAN: Borrow date cannot be after due date.");
            return Optional.empty();
        }

        Optional<Integer> bookInternalIdOpt = bookManagementService.findInternalIdByIsbn13(bookIsbn13);
        if (bookInternalIdOpt.isEmpty()) {
            System.err.println("ERROR_BRS_CREATE_LOAN: Book with ISBN-13 " + bookIsbn13 + " not found.");
            return Optional.empty();
        }
        int bookInternalId = bookInternalIdOpt.get();

        String checkExistingSql = "SELECT COUNT(*) AS count FROM BorrowingRecords " +
                "WHERE bookInternalId = ? AND userId = ? AND status IN (?, ?)";
        String insertSql = "INSERT INTO BorrowingRecords (bookInternalId, userId, borrowDate, dueDate, status) VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;

        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false); // BẮT ĐẦU TRANSACTION

            // 1. Kiểm tra người dùng có đang mượn sách này mà chưa trả không
            try (PreparedStatement checkPstmt = conn.prepareStatement(checkExistingSql)) {
                checkPstmt.setInt(1, bookInternalId);
                checkPstmt.setString(2, userId);
                checkPstmt.setString(3, LoanStatus.ACTIVE.name());
                checkPstmt.setString(4, LoanStatus.OVERDUE.name());
                try (ResultSet rs = checkPstmt.executeQuery()) {
                    if (rs.next() && rs.getInt("count") > 0) {
                        System.err.println("ERROR_BRS_CREATE_LOAN: User " + userId + " is already actively borrowing book with internalId " + bookInternalId);
                        conn.rollback();
                        return Optional.empty();
                    }
                }
            }

            // 2. Giảm số lượng sách có sẵn (TRUYỀN CONNECTION)
            if (!bookManagementService.handleBookBorrowedByInternalId(conn, bookInternalId)) {
                // BookManagementService.handleBookBorrowedByInternalId(conn,...) sẽ trả về false nếu logic thất bại
                // (ví dụ: sách hết, không tìm thấy). SQLException sẽ được ném nếu có lỗi DB nghiêm trọng.
                System.err.println("ERROR_BRS_CREATE_LOAN: Failed to update book available quantity for internalId " + bookInternalId + " (handled by BookManagementService).");
                conn.rollback();
                return Optional.empty();
            }

            // 3. Tạo bản ghi mượn sách mới
            BorrowingRecord newLoan = new BorrowingRecord(bookInternalId, userId, borrowDate, dueDate); // Status mặc định là ACTIVE
            try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertPstmt.setInt(1, newLoan.getBookInternalId());
                insertPstmt.setString(2, newLoan.getUserId());
                insertPstmt.setString(3, newLoan.getBorrowDate().format(DB_DATE_FORMATTER));
                insertPstmt.setString(4, newLoan.getDueDate().format(DB_DATE_FORMATTER));
                insertPstmt.setString(5, newLoan.getStatus().name());

                int affectedRows = insertPstmt.executeUpdate();
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = insertPstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            newLoan.setRecordId(generatedKeys.getInt(1));
                            conn.commit(); // COMMIT TRANSACTION NẾU TẤT CẢ THÀNH CÔNG
                            System.out.println("DEBUG_BRS_CREATE_LOAN: New loan created: ID " + newLoan.getRecordId() + " for book internalId " + bookInternalId);
                            return Optional.of(newLoan);
                        }
                    }
                }
                // Nếu không lấy được ID hoặc không có dòng nào bị ảnh hưởng
                conn.rollback();
                System.err.println("ERROR_BRS_CREATE_LOAN: Creating BorrowingRecord failed, no ID obtained or no rows affected.");
                return Optional.empty();
            }

        } catch (SQLException e) { // Bắt SQLException từ bất kỳ thao tác DB nào trong khối try
            System.err.println("ERROR_BRS_CREATE_LOAN: SQLException occurred during createLoan transaction for bookId " + bookInternalId + ": " + e.getMessage());
            if (conn != null) {
                try {
                    System.err.println("Attempting to rollback transaction due to SQLException...");
                    conn.rollback();
                } catch (SQLException exRollback) {
                    System.err.println("ERROR_BRS_CREATE_LOAN_ROLLBACK_EX: Failed to rollback transaction: " + exRollback.getMessage());
                }
            }
            return Optional.empty();
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Khôi phục trạng thái autoCommit
                    conn.close();
                } catch (SQLException exClose) {
                    System.err.println("ERROR_BRS_CREATE_LOAN_CLOSE_EX: Failed to close connection: " + exClose.getMessage());
                }
            }
        }
    }

    public boolean recordBookReturn(int loanRecordId, LocalDate actualReturnDate) {
        if (actualReturnDate == null) {
            System.err.println("ERROR_BRS_RETURN: Actual return date cannot be null.");
            return false;
        }

        Optional<BorrowingRecord> loanOpt = findLoanById(loanRecordId); // findLoanById tự quản lý connection
        if (loanOpt.isEmpty()) {
            System.err.println("ERROR_BRS_RETURN: Loan " + loanRecordId + " not found.");
            return false;
        }
        BorrowingRecord loan = loanOpt.get();
        if (loan.getStatus() == LoanStatus.RETURNED) {
            System.out.println("WARN_BRS_RETURN: Book for loan " + loanRecordId + " has already been returned on " + loan.getReturnDate());
            return true;
        }

        String updateSql = "UPDATE BorrowingRecords SET returnDate = ?, status = ? WHERE id = ?";
        Connection conn = null;
        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false); // BẮT ĐẦU TRANSACTION

            // 1. Cập nhật bản ghi mượn
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setString(1, actualReturnDate.format(DB_DATE_FORMATTER));
                pstmt.setString(2, LoanStatus.RETURNED.name());
                pstmt.setInt(3, loanRecordId);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) {
                    conn.rollback();
                    System.err.println("ERROR_BRS_RETURN: Failed to update loan record " + loanRecordId + " in DB. Loan might not exist.");
                    return false;
                }
            }

            // 2. Tăng số lượng sách có sẵn (TRUYỀN CONNECTION)
            if (!bookManagementService.handleBookReturnedByInternalId(conn, loan.getBookInternalId())) {
                System.err.println("ERROR_BRS_RETURN: Failed to update book available quantity for internalId " + loan.getBookInternalId() + ". Rolling back loan status update.");
                conn.rollback();
                return false;
            }

            conn.commit(); // COMMIT TRANSACTION
            System.out.println("DEBUG_BRS_RETURN: Loan " + loanRecordId + " marked as RETURNED on " + actualReturnDate);
            return true;

        } catch (SQLException e) { // Bắt SQLException từ bất kỳ thao tác DB nào
            System.err.println("ERROR_BRS_RETURN: SQLException occurred during recordBookReturn transaction for loan " + loanRecordId + ": " + e.getMessage());
            if (conn != null) {
                try {
                    System.err.println("Attempting to rollback transaction due to SQLException...");
                    conn.rollback();
                } catch (SQLException exRollback) {
                    System.err.println("ERROR_BRS_RETURN_ROLLBACK_EX: Failed to rollback transaction: " + exRollback.getMessage());
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException exClose) {
                    System.err.println("ERROR_BRS_RETURN_CLOSE_EX: Failed to close connection: " + exClose.getMessage());
                }
            }
        }
    }

    public Optional<BorrowingRecord> findLoanById(int loanRecordId) {
        String sql = "SELECT * FROM BorrowingRecords WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection(); // Connection cho thao tác đọc này
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, loanRecordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToBorrowingRecord(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRS_FIND_ID: DB error finding loan by ID " + loanRecordId + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<BorrowingRecord> getLoansByUserId(String userId, boolean activeOnly) {
        if (userId == null || userId.trim().isEmpty()) {
            System.err.println("WARN_BRS_GET_USER_LOANS: User ID is null or empty.");
            return new ArrayList<>();
        }
        updateAllOverdueStatuses(LocalDate.now()); // Cập nhật trước khi lấy

        List<BorrowingRecord> records = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM BorrowingRecords WHERE userId = ?");
        if (activeOnly) {
            sqlBuilder.append(" AND status IN (?, ?)"); // ACTIVE or OVERDUE
        }
        sqlBuilder.append(" ORDER BY borrowDate DESC, dueDate DESC");

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            pstmt.setString(1, userId);
            if (activeOnly) {
                pstmt.setString(2, LoanStatus.ACTIVE.name());
                pstmt.setString(3, LoanStatus.OVERDUE.name());
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapResultSetToBorrowingRecord(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRS_GET_USER_LOANS: DB error for user " + userId + ": " + e.getMessage());
        }
        return records;
    }

    public List<BorrowingRecord> getLoansByBookInternalId(int bookInternalId, boolean activeOnly) {
        if (bookInternalId <= 0) {
            System.err.println("WARN_BRS_GET_BOOK_LOANS: Invalid bookInternalId: " + bookInternalId);
            return new ArrayList<>();
        }
        // Cân nhắc có nên gọi updateAllOverdueStatuses ở đây không,
        // nếu hàm này được gọi thường xuyên và updateAllOverdueStatuses tốn kém.
        // Tuy nhiên, để đảm bảo dữ liệu trạng thái chính xác, có thể giữ lại.
        if (activeOnly) { // Chỉ cập nhật nếu cần xem trạng thái active/overdue
            updateAllOverdueStatuses(LocalDate.now());
        }


        List<BorrowingRecord> records = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM BorrowingRecords WHERE bookInternalId = ?");
        if (activeOnly) {
            sqlBuilder.append(" AND status IN (?, ?)"); // ACTIVE or OVERDUE
        }
        sqlBuilder.append(" ORDER BY borrowDate DESC, dueDate DESC");

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            pstmt.setInt(1, bookInternalId);
            if (activeOnly) {
                pstmt.setString(2, LoanStatus.ACTIVE.name());
                pstmt.setString(3, LoanStatus.OVERDUE.name());
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapResultSetToBorrowingRecord(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRS_GET_BOOK_LOANS: DB error for book internalId " + bookInternalId + ": " + e.getMessage());
        }
        return records;
    }

    public void updateAllOverdueStatuses(LocalDate currentDate) {
        System.out.println("DEBUG_BRS_OVERDUE_UPDATE: Checking and updating overdue statuses for date: " + currentDate);
        String updateSql = "UPDATE BorrowingRecords SET status = ? WHERE status = ? AND dueDate < ?";
        int updatedCount = 0;

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {

            updatePstmt.setString(1, LoanStatus.OVERDUE.name());
            updatePstmt.setString(2, LoanStatus.ACTIVE.name());
            updatePstmt.setString(3, currentDate.format(DB_DATE_FORMATTER));

            updatedCount = updatePstmt.executeUpdate();

            if (updatedCount > 0) {
                System.out.println("DEBUG_BRS_OVERDUE_UPDATE: Total " + updatedCount + " loans updated to OVERDUE in DB.");
            } else {
                // System.out.println("DEBUG_BRS_OVERDUE_UPDATE: No active loans found to update to OVERDUE status for date " + currentDate);
            }

        } catch (SQLException e) {
            System.err.println("ERROR_BRS_OVERDUE_UPDATE: DB error updating overdue statuses: " + e.getMessage());
        }
    }

    public List<BorrowingRecord> getOverdueLoans(LocalDate currentDate) {
        updateAllOverdueStatuses(currentDate); // Đảm bảo trạng thái được cập nhật
        List<BorrowingRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM BorrowingRecords WHERE status = ? ORDER BY dueDate ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, LoanStatus.OVERDUE.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapResultSetToBorrowingRecord(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRS_GET_OVERDUE: DB error fetching overdue loans: " + e.getMessage());
        }
        return records;
    }

    public List<BorrowingRecord> getAllLoans() {
        List<BorrowingRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM BorrowingRecords ORDER BY borrowDate DESC, id DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                records.add(mapResultSetToBorrowingRecord(rs));
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRS_GET_ALL: DB error retrieving all loans: " + e.getMessage());
        }
        return records;
    }

    public List<BorrowingRecord> getAllActiveLoans() {
        updateAllOverdueStatuses(LocalDate.now());
        List<BorrowingRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM BorrowingRecords WHERE status = ? OR status = ? ORDER BY dueDate ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, LoanStatus.ACTIVE.name());
            pstmt.setString(2, LoanStatus.OVERDUE.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapResultSetToBorrowingRecord(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRS_GET_ALL_ACTIVE: DB error retrieving all active loans: " + e.getMessage());
        }
        return records;
    }

    public int countActiveLoans() {
        updateAllOverdueStatuses(LocalDate.now());
        String sql = "SELECT COUNT(*) AS count FROM BorrowingRecords WHERE status = ? OR status = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, LoanStatus.ACTIVE.name());
            pstmt.setString(2, LoanStatus.OVERDUE.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRS_COUNT_ACTIVE_OR_OVERDUE: DB error counting active/overdue loans: " + e.getMessage());
        }
        return 0;
    }

    public int countOverdueLoans() {
        updateAllOverdueStatuses(LocalDate.now());
        String sql = "SELECT COUNT(*) AS count FROM BorrowingRecords WHERE status = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, LoanStatus.OVERDUE.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRS_COUNT_OVERDUE: DB error counting overdue loans: " + e.getMessage());
        }
        return 0;
    }
}
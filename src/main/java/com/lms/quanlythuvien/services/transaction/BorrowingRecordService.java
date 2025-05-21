package com.lms.quanlythuvien.services.transaction;

import com.lms.quanlythuvien.models.transaction.BorrowingRecord;
import com.lms.quanlythuvien.models.transaction.LoanStatus;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.utils.database.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BorrowingRecordService {

    private static BorrowingRecordService instance;
    private final BookManagementService bookManagementService;
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

    // PHIÊN BẢN CŨ: Tự quản lý transaction (dùng cho mượn thủ công chẳng hạn)
    public Optional<BorrowingRecord> createLoan(String bookIsbn13, String userId, LocalDate borrowDate, LocalDate dueDate) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false); // Bắt đầu transaction ở đây

            Optional<BorrowingRecord> recordOpt = createLoanLogic(conn, bookIsbn13, userId, borrowDate, dueDate);

            if (recordOpt.isPresent()) {
                conn.commit(); // Commit nếu logic bên trong thành công
                return recordOpt;
            } else {
                conn.rollback(); // Rollback nếu logic bên trong thất bại
                return Optional.empty();
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRS_CREATE_LOAN (standalone): SQLException occurred: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException exRollback) {
                    System.err.println("ERROR_BRS_CREATE_LOAN_ROLLBACK_EX (standalone): " + exRollback.getMessage());
                }
            }
            return Optional.empty();
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException exClose) {
                    System.err.println("ERROR_BRS_CREATE_LOAN_CLOSE_EX (standalone): " + exClose.getMessage());
                }
            }
        }
    }

    /**
     * Logic cốt lõi để tạo một lượt mượn, sử dụng một Connection đã có.
     * Phương thức này KHÔNG quản lý commit/rollback/close connection.
     * Được gọi bởi createLoan() (phiên bản tự quản lý transaction) hoặc từ service khác (như BorrowingRequestService).
     */
    public Optional<BorrowingRecord> createLoanLogic(Connection conn, String bookIsbn13, String userId, LocalDate borrowDate, LocalDate dueDate) throws SQLException {
        if (bookIsbn13 == null || userId == null || borrowDate == null || dueDate == null) {
            System.err.println("ERROR_BRS_CREATE_LOAN_LOGIC: Invalid parameters.");
            return Optional.empty(); // Hoặc ném IllegalArgumentException
        }
        if (borrowDate.isAfter(dueDate)) {
            System.err.println("ERROR_BRS_CREATE_LOAN_LOGIC: Borrow date after due date.");
            return Optional.empty();
        }

        Optional<Integer> bookInternalIdOpt = bookManagementService.findInternalIdByIsbn13(bookIsbn13);
        if (bookInternalIdOpt.isEmpty()) {
            System.err.println("ERROR_BRS_CREATE_LOAN_LOGIC: Book with ISBN-13 " + bookIsbn13 + " not found.");
            return Optional.empty();
        }
        int bookInternalId = bookInternalIdOpt.get();

        String checkExistingSql = "SELECT COUNT(*) AS count FROM BorrowingRecords " +
                "WHERE bookInternalId = ? AND userId = ? AND status IN (?, ?)";
        try (PreparedStatement checkPstmt = conn.prepareStatement(checkExistingSql)) {
            checkPstmt.setInt(1, bookInternalId);
            checkPstmt.setString(2, userId);
            checkPstmt.setString(3, LoanStatus.ACTIVE.name());
            checkPstmt.setString(4, LoanStatus.OVERDUE.name());
            try (ResultSet rs = checkPstmt.executeQuery()) {
                if (rs.next() && rs.getInt("count") > 0) {
                    System.err.println("ERROR_BRS_CREATE_LOAN_LOGIC: User " + userId + " is already actively borrowing book internalId " + bookInternalId);
                    // Không rollback ở đây, để service gọi quyết định
                    return Optional.empty();
                }
            }
        }

        // Giảm số lượng sách (sử dụng Connection được truyền vào)
        if (!bookManagementService.handleBookBorrowedByInternalId(conn, bookInternalId)) {
            System.err.println("ERROR_BRS_CREATE_LOAN_LOGIC: Failed to update book available quantity for internalId " + bookInternalId);
            // Không rollback ở đây
            return Optional.empty();
        }

        String insertSql = "INSERT INTO BorrowingRecords (bookInternalId, userId, borrowDate, dueDate, status) VALUES (?, ?, ?, ?, ?)";
        BorrowingRecord newLoan = new BorrowingRecord(bookInternalId, userId, borrowDate, dueDate);
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
                        System.out.println("DEBUG_BRS_CREATE_LOAN_LOGIC: BorrowingRecord prepared with ID: " + newLoan.getRecordId());
                        return Optional.of(newLoan);
                    }
                }
            }
            System.err.println("ERROR_BRS_CREATE_LOAN_LOGIC: Creating BorrowingRecord failed in DB.");
            return Optional.empty();
        }
    }


    // Tương tự, tách logic cho recordBookReturn
    public boolean recordBookReturn(int loanRecordId, LocalDate actualReturnDate) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);

            boolean success = recordBookReturnLogic(conn, loanRecordId, actualReturnDate);

            if (success) {
                conn.commit();
                return true;
            } else {
                conn.rollback();
                return false;
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRS_RETURN (standalone): SQLException: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { System.err.println("ERROR_BRS_RETURN_ROLLBACK_EX (standalone): " + ex.getMessage());}
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { System.err.println("ERROR_BRS_RETURN_CLOSE_EX (standalone): " + ex.getMessage());}
            }
        }
    }

    public boolean recordBookReturnLogic(Connection conn, int loanRecordId, LocalDate actualReturnDate) throws SQLException {
        if (actualReturnDate == null) {
            System.err.println("ERROR_BRS_RETURN_LOGIC: Actual return date cannot be null.");
            return false; // Hoặc ném IllegalArgumentException
        }

        // Lấy thông tin lượt mượn. findLoanById này cần được xem xét: nó tự mở connection.
        // Để tối ưu, findLoanById cũng nên nhận connection nếu được gọi trong transaction.
        // Tạm thời chấp nhận nó mở connection riêng cho việc đọc này.
        Optional<BorrowingRecord> loanOpt = findLoanById(loanRecordId);
        if (loanOpt.isEmpty()) {
            System.err.println("ERROR_BRS_RETURN_LOGIC: Loan " + loanRecordId + " not found.");
            return false;
        }
        BorrowingRecord loan = loanOpt.get();
        if (loan.getStatus() == LoanStatus.RETURNED) {
            System.out.println("WARN_BRS_RETURN_LOGIC: Book for loan " + loanRecordId + " already returned.");
            return true; // Coi như thành công
        }

        String updateSql = "UPDATE BorrowingRecords SET returnDate = ?, status = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setString(1, actualReturnDate.format(DB_DATE_FORMATTER));
            pstmt.setString(2, LoanStatus.RETURNED.name());
            pstmt.setInt(3, loanRecordId);
            if (pstmt.executeUpdate() == 0) {
                System.err.println("ERROR_BRS_RETURN_LOGIC: Failed to update BorrowingRecord " + loanRecordId + " in DB.");
                return false;
            }
        }

        if (!bookManagementService.handleBookReturnedByInternalId(conn, loan.getBookInternalId())) {
            System.err.println("ERROR_BRS_RETURN_LOGIC: Failed to update book quantity for internalId " + loan.getBookInternalId());
            return false;
        }
        System.out.println("DEBUG_BRS_RETURN_LOGIC: Loan " + loanRecordId + " processed for return.");
        return true;
    }

    // Các phương thức get... không thay đổi, chúng tự quản lý connection cho việc đọc
    public Optional<BorrowingRecord> findLoanById(int loanRecordId) {
        // ... (Giữ nguyên code của bạn) ...
        String sql = "SELECT * FROM BorrowingRecords WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
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
        // ... (Giữ nguyên code của bạn) ...
        if (userId == null || userId.trim().isEmpty()) {
            System.err.println("WARN_BRS_GET_USER_LOANS: User ID is null or empty.");
            return new ArrayList<>();
        }
        updateAllOverdueStatuses(LocalDate.now());

        List<BorrowingRecord> records = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM BorrowingRecords WHERE userId = ?");
        if (activeOnly) {
            sqlBuilder.append(" AND status IN (?, ?)");
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
        // ... (Giữ nguyên code của bạn) ...
        if (bookInternalId <= 0) {
            System.err.println("WARN_BRS_GET_BOOK_LOANS: Invalid bookInternalId: " + bookInternalId);
            return new ArrayList<>();
        }
        if (activeOnly) {
            updateAllOverdueStatuses(LocalDate.now());
        }

        List<BorrowingRecord> records = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM BorrowingRecords WHERE bookInternalId = ?");
        if (activeOnly) {
            sqlBuilder.append(" AND status IN (?, ?)");
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
        // ... (Giữ nguyên code của bạn) ...
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
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRS_OVERDUE_UPDATE: DB error updating overdue statuses: " + e.getMessage());
        }
    }

    public List<BorrowingRecord> getOverdueLoans(LocalDate currentDate) {
        // ... (Giữ nguyên code của bạn) ...
        updateAllOverdueStatuses(currentDate);
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
        // ... (Giữ nguyên code của bạn) ...
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
        // ... (Giữ nguyên code của bạn) ...
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
        // ... (Giữ nguyên code của bạn) ...
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
        // ... (Giữ nguyên code của bạn) ...
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
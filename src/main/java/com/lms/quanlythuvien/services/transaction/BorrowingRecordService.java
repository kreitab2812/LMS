package com.lms.quanlythuvien.services.transaction;

import com.lms.quanlythuvien.models.transaction.BorrowingRecord;
import com.lms.quanlythuvien.models.transaction.LoanStatus;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.utils.database.DatabaseManager;
// Bỏ import User, Book không cần thiết cho service này nữa (trừ khi có logic nghiệp vụ đặc biệt)

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BorrowingRecordService {

    private static BorrowingRecordService instance;

    private BorrowingRecordService() {
        System.out.println("DEBUG_BRS_SINGLETON: BorrowingRecordService Singleton instance created. Data managed by database.");
    }

    public static synchronized BorrowingRecordService getInstance() {
        if (instance == null) {
            instance = new BorrowingRecordService();
        }
        return instance;
    }

    // Helper để map ResultSet to BorrowingRecord
    private BorrowingRecord mapResultSetToBorrowingRecord(ResultSet rs) throws SQLException {
        return new BorrowingRecord(
                rs.getInt("id"), // Tên cột "id" trong bảng BorrowingRecords
                rs.getInt("bookInternalId"),
                rs.getString("userId"),
                LocalDate.parse(rs.getString("borrowDate")),
                LocalDate.parse(rs.getString("dueDate")),
                rs.getString("returnDate") != null ? LocalDate.parse(rs.getString("returnDate")) : null,
                LoanStatus.valueOf(rs.getString("status").toUpperCase())
        );
    }

    /**
     * Tạo một lượt mượn mới.
     * @param bookIsbn13 ISBN-13 của sách được mượn (dùng để lấy internalId và cập nhật số lượng)
     * @param userId ID của người mượn
     * @param borrowDate Ngày mượn
     * @param dueDate Ngày hẹn trả
     * @return Optional chứa BorrowingRecord nếu thành công, rỗng nếu thất bại.
     */
    public Optional<BorrowingRecord> createLoan(String bookIsbn13, String userId, LocalDate borrowDate, LocalDate dueDate) {
        if (bookIsbn13 == null || userId == null || borrowDate == null || dueDate == null) {
            System.err.println("ERROR_BRS_CREATE_LOAN: Invalid parameters for creating a loan.");
            return Optional.empty();
        }
        if (borrowDate.isAfter(dueDate)) {
            System.err.println("ERROR_BRS_CREATE_LOAN: Borrow date cannot be after due date.");
            return Optional.empty();
        }

        BookManagementService bms = BookManagementService.getInstance();
        Optional<Integer> bookInternalIdOpt = bms.findInternalIdByIsbn13(bookIsbn13); // Cần hàm này trong BMS

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
            conn.setAutoCommit(false);

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

            // Gọi BMS để giảm số lượng sách có sẵn (dùng ISBN-13 như BMS hiện tại)
            if (!bms.handleBookBorrowed(bookIsbn13)) {
                System.err.println("ERROR_BRS_CREATE_LOAN: Failed to update book available quantity for ISBN " + bookIsbn13);
                conn.rollback();
                return Optional.empty();
            }

            BorrowingRecord newLoan = new BorrowingRecord(bookInternalId, userId, borrowDate, dueDate);
            try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertPstmt.setInt(1, newLoan.getBookInternalId());
                insertPstmt.setString(2, newLoan.getUserId());
                insertPstmt.setString(3, newLoan.getBorrowDate().toString());
                insertPstmt.setString(4, newLoan.getDueDate().toString());
                insertPstmt.setString(5, newLoan.getStatus().name());

                int affectedRows = insertPstmt.executeUpdate();
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = insertPstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            newLoan.setRecordId(generatedKeys.getInt(1));
                            conn.commit();
                            System.out.println("DEBUG_BRS_CREATE_LOAN: New loan created in DB: ID " + newLoan.getRecordId() + " for book internalId " + bookInternalId);
                            return Optional.of(newLoan);
                        }
                    }
                }
                conn.rollback();
                System.err.println("ERROR_BRS_CREATE_LOAN: Creating loan failed, no ID obtained or no rows affected.");
                return Optional.empty();
            }

        } catch (SQLException e) {
            System.err.println("ERROR_BRS_CREATE_LOAN: DB error: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { e.addSuppressed(ex); }
            return Optional.empty();
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public boolean recordBookReturn(int loanRecordId, LocalDate actualReturnDate) {
        if (actualReturnDate == null) {
            System.err.println("ERROR_BRS_RETURN: Actual return date cannot be null.");
            return false;
        }

        Optional<BorrowingRecord> loanOpt = findLoanById(loanRecordId);
        if (loanOpt.isEmpty()) {
            System.err.println("ERROR_BRS_RETURN: Loan " + loanRecordId + " not found.");
            return false;
        }
        BorrowingRecord loan = loanOpt.get();
        if (loan.getStatus() == LoanStatus.RETURNED) {
            System.err.println("WARN_BRS_RETURN: Book for loan " + loanRecordId + " has already been returned.");
            return false;
        }

        String updateSql = "UPDATE BorrowingRecords SET returnDate = ?, status = ? WHERE id = ?";
        Connection conn = null;
        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setString(1, actualReturnDate.toString());
                pstmt.setString(2, LoanStatus.RETURNED.name());
                pstmt.setInt(3, loanRecordId);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) {
                    conn.rollback();
                    System.err.println("ERROR_BRS_RETURN: Failed to update loan record " + loanRecordId + " in DB.");
                    return false;
                }
            }

            // Lấy ISBN-13 của sách để gọi BookManagementService
            BookManagementService bms = BookManagementService.getInstance();
            Optional<com.lms.quanlythuvien.models.item.Book> bookOpt = bms.findBookByInternalId(loan.getBookInternalId());
            if (bookOpt.isEmpty() || bookOpt.get().getIsbn13() == null) {
                System.err.println("ERROR_BRS_RETURN: Could not find book or its ISBN-13 to update quantity for internalId " + loan.getBookInternalId());
                conn.rollback(); // Quan trọng: rollback nếu không thể cập nhật số lượng sách
                return false;
            }

            if (!bms.handleBookReturned(bookOpt.get().getIsbn13())) {
                System.err.println("ERROR_BRS_RETURN: Failed to update book available quantity for ISBN " + bookOpt.get().getIsbn13());
                conn.rollback();
                return false;
            }

            conn.commit();
            System.out.println("DEBUG_BRS_RETURN: Loan " + loanRecordId + " marked as RETURNED on " + actualReturnDate);
            return true;

        } catch (SQLException e) {
            System.err.println("ERROR_BRS_RETURN: DB error for loan " + loanRecordId + ": " + e.getMessage());
            e.printStackTrace();
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { e.addSuppressed(ex); }
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public Optional<BorrowingRecord> findLoanById(int loanRecordId) {
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
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Lấy danh sách các lượt mượn của một người dùng.
     * @param userId ID của người dùng
     * @param activeOnly true nếu chỉ muốn lấy các lượt mượn đang ACTIVE hoặc OVERDUE
     * @return Danh sách BorrowingRecord
     */
    public List<BorrowingRecord> getLoansByUserId(String userId, boolean activeOnly) {
        if (userId == null) return new ArrayList<>();
        updateAllOverdueStatuses(LocalDate.now());

        List<BorrowingRecord> records = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM BorrowingRecords WHERE userId = ?");
        if (activeOnly) {
            sqlBuilder.append(" AND status IN (?, ?)");
        }
        sqlBuilder.append(" ORDER BY borrowDate DESC");

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
            e.printStackTrace();
        }
        return records;
    }

    /**
     * Lấy danh sách các lượt mượn của một sách (dựa trên internalId).
     * @param bookInternalId Internal ID của sách
     * @param activeOnly true nếu chỉ muốn lấy các lượt mượn đang ACTIVE hoặc OVERDUE
     * @return Danh sách BorrowingRecord
     */
    public List<BorrowingRecord> getLoansByBookInternalId(int bookInternalId, boolean activeOnly) {
        updateAllOverdueStatuses(LocalDate.now());
        List<BorrowingRecord> records = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM BorrowingRecords WHERE bookInternalId = ?");
        if (activeOnly) {
            sqlBuilder.append(" AND status IN (?, ?)");
        }
        sqlBuilder.append(" ORDER BY borrowDate DESC");

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
            e.printStackTrace();
        }
        return records;
    }


    public void updateAllOverdueStatuses(LocalDate currentDate) {
        System.out.println("DEBUG_BRS_OVERDUE_UPDATE: Checking and updating overdue statuses for date: " + currentDate);
        String selectActiveOverdueSql = "SELECT id FROM BorrowingRecords WHERE status = ? AND dueDate < ?";
        String updateStatusSql = "UPDATE BorrowingRecords SET status = ? WHERE id = ?";
        int updatedCount = 0;

        List<Integer> idsToUpdate = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement selectPstmt = conn.prepareStatement(selectActiveOverdueSql)) {

            selectPstmt.setString(1, LoanStatus.ACTIVE.name());
            selectPstmt.setString(2, currentDate.toString());
            try (ResultSet rs = selectPstmt.executeQuery()) {
                while (rs.next()) {
                    idsToUpdate.add(rs.getInt("id"));
                }
            }

            if (!idsToUpdate.isEmpty()) {
                try (PreparedStatement updatePstmt = conn.prepareStatement(updateStatusSql)) {
                    for (int recordId : idsToUpdate) {
                        updatePstmt.setString(1, LoanStatus.OVERDUE.name());
                        updatePstmt.setInt(2, recordId);
                        updatePstmt.addBatch();
                        updatedCount++;
                        System.out.println("DEBUG_BRS_OVERDUE_UPDATE: Loan " + recordId + " marked for OVERDUE status update.");
                    }
                    updatePstmt.executeBatch();
                    System.out.println("DEBUG_BRS_OVERDUE_UPDATE: Total " + updatedCount + " loans updated to OVERDUE in DB.");
                }
            } else {
                System.out.println("DEBUG_BRS_OVERDUE_UPDATE: No loans found to update to OVERDUE status.");
            }

        } catch (SQLException e) {
            System.err.println("ERROR_BRS_OVERDUE_UPDATE: DB error updating overdue statuses: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<BorrowingRecord> getOverdueLoans(LocalDate currentDate) {
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
            System.err.println("ERROR_BRS_GET_OVERDUE: DB error: " + e.getMessage());
            e.printStackTrace();
        }
        return records;
    }

    public List<BorrowingRecord> getAllLoans() {
        // updateAllOverdueStatuses(LocalDate.now()); // Cân nhắc gọi ở đây, có thể làm chậm
        List<BorrowingRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM BorrowingRecords ORDER BY borrowDate DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement(); // Dùng Statement vì không có tham số
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                records.add(mapResultSetToBorrowingRecord(rs));
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRS_GET_ALL: DB error: " + e.getMessage());
            e.printStackTrace();
        }
        return records;
    }
}
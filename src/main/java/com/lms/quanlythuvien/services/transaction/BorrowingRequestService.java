package com.lms.quanlythuvien.services.transaction;

import com.lms.quanlythuvien.models.transaction.BorrowingRecord;
import com.lms.quanlythuvien.models.transaction.BorrowingRequest;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.utils.database.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BorrowingRequestService {

    private static BorrowingRequestService instance;
    private final BookManagementService bookManagementService;
    private final BorrowingRecordService borrowingRecordService;
    private static final DateTimeFormatter DB_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;


    private BorrowingRequestService() {
        this.bookManagementService = BookManagementService.getInstance();
        this.borrowingRecordService = BorrowingRecordService.getInstance();
        System.out.println("DEBUG_BRQS_SINGLETON: BorrowingRequestService Singleton instance created.");
    }

    public static synchronized BorrowingRequestService getInstance() {
        if (instance == null) {
            instance = new BorrowingRequestService();
        }
        return instance;
    }

    private BorrowingRequest mapResultSetToRequest(ResultSet rs) throws SQLException {
        String resolvedDateStr = rs.getString("resolvedDate");
        String pickupDueDateStr = rs.getString("pickupDueDate");
        return new BorrowingRequest(
                rs.getString("requestId"),
                rs.getString("userId"),
                rs.getString("bookIsbn13"),
                LocalDate.parse(rs.getString("requestDate"), DB_DATE_FORMATTER),
                BorrowingRequest.RequestStatus.valueOf(rs.getString("status").toUpperCase()),
                rs.getString("adminNotes"),
                resolvedDateStr != null ? LocalDate.parse(resolvedDateStr, DB_DATE_FORMATTER) : null,
                pickupDueDateStr != null ? LocalDate.parse(pickupDueDateStr, DB_DATE_FORMATTER) : null
        );
    }

    public Optional<BorrowingRequest> addRequest(String userId, String bookIsbn13) {
        if (userId == null || userId.trim().isEmpty() || bookIsbn13 == null || bookIsbn13.trim().isEmpty()) {
            System.err.println("ERROR_BRQS_ADD: UserID or BookISBN13 cannot be null or empty.");
            return Optional.empty();
        }

        if (bookManagementService.findBookByIsbn13InLibrary(bookIsbn13).isEmpty()) {
            System.err.println("ERROR_BRQS_ADD: Book with ISBN " + bookIsbn13 + " not found.");
            return Optional.empty();
        }

        String checkExistingSql = "SELECT COUNT(*) AS count FROM BorrowingRequests " +
                "WHERE userId = ? AND bookIsbn13 = ? AND status IN (?, ?)"; // PENDING, APPROVED
        String insertSql = "INSERT INTO BorrowingRequests (requestId, userId, bookIsbn13, requestDate, status) VALUES (?, ?, ?, ?, ?)";
        BorrowingRequest newRequest = new BorrowingRequest(userId, bookIsbn13);

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            try (PreparedStatement checkPstmt = conn.prepareStatement(checkExistingSql)) {
                checkPstmt.setString(1, userId);
                checkPstmt.setString(2, bookIsbn13);
                checkPstmt.setString(3, BorrowingRequest.RequestStatus.PENDING.name());
                checkPstmt.setString(4, BorrowingRequest.RequestStatus.APPROVED.name());
                try (ResultSet rs = checkPstmt.executeQuery()) {
                    if (rs.next() && rs.getInt("count") > 0) {
                        System.out.println("INFO_BRQS_ADD: User " + userId + " already has an active/pending request for book " + bookIsbn13);
                        return Optional.empty();
                    }
                }
            }

            try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql)) {
                insertPstmt.setString(1, newRequest.getRequestId());
                insertPstmt.setString(2, newRequest.getUserId());
                insertPstmt.setString(3, newRequest.getBookIsbn13());
                insertPstmt.setString(4, newRequest.getRequestDate().format(DB_DATE_FORMATTER));
                insertPstmt.setString(5, newRequest.getStatus().name());

                int affectedRows = insertPstmt.executeUpdate();
                if (affectedRows > 0) {
                    System.out.println("DEBUG_BRQS_ADD: New borrowing request created: " + newRequest.getRequestId());
                    return Optional.of(newRequest);
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRQS_ADD: DB error creating request for user " + userId + ", book " + bookIsbn13 + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean approveRequestAndCreateLoan(String requestId, String adminNotes, LocalDate dueDateForLoan) {
        Optional<BorrowingRequest> requestOpt = getRequestById(requestId);
        if (requestOpt.isEmpty()) {
            System.err.println("ERROR_BRQS_APPROVE: Request " + requestId + " not found.");
            return false;
        }
        BorrowingRequest request = requestOpt.get();
        if (request.getStatus() != BorrowingRequest.RequestStatus.PENDING) {
            System.err.println("ERROR_BRQS_APPROVE: Request " + requestId + " is not in PENDING state, current state: " + request.getStatus());
            return false;
        }

        com.lms.quanlythuvien.models.item.Book bookToBorrow = bookManagementService.findBookByIsbn13InLibrary(request.getBookIsbn13())
                .orElse(null);

        if (bookToBorrow == null || bookToBorrow.getAvailableQuantity() <= 0) {
            System.err.println("ERROR_BRQS_APPROVE: Book " + request.getBookIsbn13() + " is no longer available or out of stock. Auto-rejecting request " + requestId);
            return rejectRequest(requestId, "Sách đã hết hoặc không còn sẵn có khi duyệt yêu cầu.");
        }

        LocalDate resolvedDate = LocalDate.now();
        LocalDate pickupDueDate = resolvedDate.plusDays(3);

        String updateRequestSql = "UPDATE BorrowingRequests SET status = ?, adminNotes = ?, resolvedDate = ?, pickupDueDate = ? WHERE requestId = ?";
        Connection conn = null;

        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmtUpdateReq = conn.prepareStatement(updateRequestSql)) {
                pstmtUpdateReq.setString(1, BorrowingRequest.RequestStatus.APPROVED.name());
                pstmtUpdateReq.setString(2, adminNotes != null ? adminNotes.trim() : "Đã duyệt, chờ nhận sách.");
                pstmtUpdateReq.setString(3, resolvedDate.format(DB_DATE_FORMATTER));
                pstmtUpdateReq.setString(4, pickupDueDate.format(DB_DATE_FORMATTER));
                pstmtUpdateReq.setString(5, requestId);
                int affected = pstmtUpdateReq.executeUpdate();
                if (affected == 0) {
                    throw new SQLException("Approving request failed, no rows updated for BorrowingRequest. Request ID: " + requestId);
                }
            }

            Optional<BorrowingRecord> loanOpt = borrowingRecordService.createLoan(
                    request.getBookIsbn13(),
                    request.getUserId(),
                    resolvedDate,
                    dueDateForLoan
            );

            if (loanOpt.isPresent()) {
                String completeRequestSql = "UPDATE BorrowingRequests SET status = ? WHERE requestId = ?";
                try (PreparedStatement pstmtCompleteReq = conn.prepareStatement(completeRequestSql)) {
                    pstmtCompleteReq.setString(1, BorrowingRequest.RequestStatus.COMPLETED.name());
                    pstmtCompleteReq.setString(2, requestId);
                    if (pstmtCompleteReq.executeUpdate() == 0) {
                        System.err.println("WARN_BRQS_APPROVE: Failed to update request " + requestId + " to COMPLETED after loan creation. Loan " + loanOpt.get().getRecordId() + " was created.");
                    }
                }
                conn.commit();
                System.out.println("DEBUG_BRQS_APPROVE: Request " + requestId + " APPROVED & COMPLETED. Loan " + loanOpt.get().getRecordId() + " created.");
                return true;
            } else {
                conn.rollback();
                System.err.println("ERROR_BRQS_APPROVE: Failed to create BorrowingRecord for approved request " + requestId + ". Request status remains PENDING or needs manual review.");
                return false;
            }

        } catch (SQLException e) {
            System.err.println("ERROR_BRQS_APPROVE: DB error approving request " + requestId + ": " + e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { System.err.println("ERROR_BRQS_APPROVE_ROLLBACK_EX: " + ex.getMessage()); }
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { System.err.println("ERROR_BRQS_APPROVE_CLOSE_EX: " + e.getMessage()); }
        }
    }

    public boolean rejectRequest(String requestId, String adminNotes) {
        Optional<BorrowingRequest> requestOpt = getRequestById(requestId);
        if (requestOpt.isEmpty()){
            System.err.println("ERROR_BRQS_REJECT: Request " + requestId + " not found.");
            return false;
        }
        if (requestOpt.get().getStatus() != BorrowingRequest.RequestStatus.PENDING) {
            System.err.println("WARN_BRQS_REJECT: Request " + requestId + " is not in PENDING state, current state: " + requestOpt.get().getStatus());
            return false;
        }

        String sql = "UPDATE BorrowingRequests SET status = ?, adminNotes = ?, resolvedDate = ? WHERE requestId = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, BorrowingRequest.RequestStatus.REJECTED.name());
            pstmt.setString(2, adminNotes != null ? adminNotes.trim() : "Yêu cầu bị từ chối.");
            pstmt.setString(3, LocalDate.now().format(DB_DATE_FORMATTER));
            pstmt.setString(4, requestId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_BRQS_REJECT: Request " + requestId + " REJECTED.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRQS_REJECT: DB error rejecting request " + requestId + ": " + e.getMessage());
        }
        return false;
    }

    public boolean cancelRequestByUser(String requestId, String userId) {
        Optional<BorrowingRequest> requestOpt = getRequestById(requestId);
        if (requestOpt.isEmpty() || !requestOpt.get().getUserId().equals(userId)) {
            System.err.println("WARN_BRQS_CANCEL: Request " + requestId + " not found or user " + userId + " not authorized.");
            return false;
        }
        BorrowingRequest request = requestOpt.get();
        if (request.getStatus() != BorrowingRequest.RequestStatus.PENDING) {
            System.err.println("WARN_BRQS_CANCEL: Request " + requestId + " is not PENDING (current: " + request.getStatus() + "). Cannot be canceled by user.");
            return false;
        }

        String sql = "UPDATE BorrowingRequests SET status = ?, resolvedDate = ?, adminNotes = ? WHERE requestId = ? AND userId = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, BorrowingRequest.RequestStatus.CANCELED_BY_USER.name());
            pstmt.setString(2, LocalDate.now().format(DB_DATE_FORMATTER));
            pstmt.setString(3, "User hủy yêu cầu.");
            pstmt.setString(4, requestId);
            pstmt.setString(5, userId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_BRQS_CANCEL: Request " + requestId + " CANCELED BY USER.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRQS_CANCEL: DB error canceling request " + requestId + ": " + e.getMessage());
        }
        return false;
    }

    public List<BorrowingRequest> getRequestsByStatus(BorrowingRequest.RequestStatus status) {
        List<BorrowingRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM BorrowingRequests WHERE status = ? ORDER BY requestDate DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapResultSetToRequest(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRQS_GET_STATUS: DB error getting requests by status " + status + ": " + e.getMessage());
        }
        return requests;
    }

    public List<BorrowingRequest> getRequestsByUserId(String userId) {
        List<BorrowingRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM BorrowingRequests WHERE userId = ? ORDER BY requestDate DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapResultSetToRequest(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRQS_GET_BY_USER: DB error for user " + userId + ": " + e.getMessage());
        }
        return requests;
    }

    public Optional<BorrowingRequest> getRequestById(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) return Optional.empty();
        String sql = "SELECT * FROM BorrowingRequests WHERE requestId = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, requestId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRequest(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRQS_GET_ID: DB error finding request by ID " + requestId + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<BorrowingRequest> getAllRequests() {
        List<BorrowingRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM BorrowingRequests ORDER BY requestDate DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                requests.add(mapResultSetToRequest(rs));
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRQS_GET_ALL: DB error retrieving all requests: " + e.getMessage());
        }
        return requests;
    }

    // PHƯƠNG THỨC MỚI ĐƯỢC THÊM VÀO
    public int countPendingRequests() {
        String sql = "SELECT COUNT(*) AS count FROM BorrowingRequests WHERE status = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Giả sử trạng thái chờ duyệt của bạn được lưu là "PENDING" trong Enum RequestStatus
            pstmt.setString(1, BorrowingRequest.RequestStatus.PENDING.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRQS_COUNT_PENDING: DB error counting PENDING borrowing requests: " + e.getMessage());
        }
        return 0; // Trả về 0 nếu có lỗi hoặc không có yêu cầu nào
    }
}
package com.lms.quanlythuvien.services.transaction;

import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.transaction.BorrowingRecord; // Cần cho approveRequest logic
import com.lms.quanlythuvien.models.transaction.BorrowingRequest;
import com.lms.quanlythuvien.models.transaction.LoanStatus; // Cần cho approveRequest logic
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.utils.database.DatabaseManager;
// Bỏ UserService nếu không trực tiếp dùng ở đây nữa

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BorrowingRequestService {

    private static BorrowingRequestService instance;
    private BookManagementService bookManagementService; // Cần để kiểm tra sách
    private BorrowingRecordService borrowingRecordService; // Cần để tạo BorrowingRecord khi duyệt

    private BorrowingRequestService() {
        this.bookManagementService = BookManagementService.getInstance();
        this.borrowingRecordService = BorrowingRecordService.getInstance(); // Khởi tạo
        System.out.println("DEBUG_BRQS_SINGLETON: BorrowingRequestService Singleton instance created (DB version).");
    }

    public static synchronized BorrowingRequestService getInstance() {
        if (instance == null) {
            instance = new BorrowingRequestService();
        }
        return instance;
    }

    private BorrowingRequest mapResultSetToRequest(ResultSet rs) throws SQLException {
        return new BorrowingRequest(
                rs.getString("requestId"),
                rs.getString("userId"),
                rs.getString("bookIsbn13"),
                LocalDate.parse(rs.getString("requestDate")),
                BorrowingRequest.RequestStatus.valueOf(rs.getString("status").toUpperCase()),
                rs.getString("adminNotes"),
                rs.getString("resolvedDate") != null ? LocalDate.parse(rs.getString("resolvedDate")) : null,
                rs.getString("pickupDueDate") != null ? LocalDate.parse(rs.getString("pickupDueDate")) : null
        );
    }

    public Optional<BorrowingRequest> addRequest(String userId, String bookIsbn13) {
        if (userId == null || bookIsbn13 == null) {
            System.err.println("ERROR_BRQS_ADD: UserID or BookISBN13 cannot be null.");
            return Optional.empty();
        }

        // Kiểm tra sách có tồn tại và còn hàng không (quan trọng)
        Optional<Book> bookOpt = bookManagementService.findBookByIsbn13InLibrary(bookIsbn13);
        if (bookOpt.isEmpty()) {
            System.err.println("ERROR_BRQS_ADD: Book with ISBN " + bookIsbn13 + " not found.");
            return Optional.empty();
        }
        // (Tùy chọn) Có thể không cho yêu cầu nếu sách đã hết ngay từ đầu
        // if (bookOpt.get().getAvailableQuantity() <= 0) {
        //     System.out.println("WARN_BRQS_ADD: Book " + bookIsbn13 + " is out of stock, request might be rejected.");
        // }

        // Kiểm tra user có yêu cầu PENDING/APPROVED chưa hoàn thành cho sách này không
        String checkExistingSql = "SELECT COUNT(*) AS count FROM BorrowingRequests " +
                "WHERE userId = ? AND bookIsbn13 = ? AND status IN (?, ?)";
        String insertSql = "INSERT INTO BorrowingRequests (requestId, userId, bookIsbn13, requestDate, status) VALUES (?, ?, ?, ?, ?)";

        BorrowingRequest newRequest = new BorrowingRequest(userId, bookIsbn13); // requestId được tạo bằng UUID

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            try (PreparedStatement checkPstmt = conn.prepareStatement(checkExistingSql)) {
                checkPstmt.setString(1, userId);
                checkPstmt.setString(2, bookIsbn13);
                checkPstmt.setString(3, BorrowingRequest.RequestStatus.PENDING.name());
                checkPstmt.setString(4, BorrowingRequest.RequestStatus.APPROVED.name());
                try (ResultSet rs = checkPstmt.executeQuery()) {
                    if (rs.next() && rs.getInt("count") > 0) {
                        System.out.println("WARN_BRQS_ADD: User " + userId + " already has an active/pending request for book " + bookIsbn13);
                        return Optional.empty();
                    }
                }
            }

            try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql)) {
                insertPstmt.setString(1, newRequest.getRequestId());
                insertPstmt.setString(2, newRequest.getUserId());
                insertPstmt.setString(3, newRequest.getBookIsbn13());
                insertPstmt.setString(4, newRequest.getRequestDate().toString());
                insertPstmt.setString(5, newRequest.getStatus().name());

                int affectedRows = insertPstmt.executeUpdate();
                if (affectedRows > 0) {
                    System.out.println("DEBUG_BRQS_ADD: New borrowing request created in DB: " + newRequest.getRequestId());
                    return Optional.of(newRequest);
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRQS_ADD: DB error creating request: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
        return requests;
    }

    public Optional<BorrowingRequest> getRequestById(String requestId) {
        if (requestId == null) return Optional.empty();
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
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public boolean approveRequestAndCreateLoan(String requestId, String adminNotes, LocalDate dueDateForLoan) {
        Optional<BorrowingRequest> requestOpt = getRequestById(requestId);
        if (requestOpt.isEmpty() || requestOpt.get().getStatus() != BorrowingRequest.RequestStatus.PENDING) {
            System.err.println("ERROR_BRQS_APPROVE: Request " + requestId + " not found or not in PENDING state.");
            return false;
        }
        BorrowingRequest request = requestOpt.get();

        // Kiểm tra sách có còn không trước khi duyệt
        Optional<Book> bookOpt = bookManagementService.findBookByIsbn13InLibrary(request.getBookIsbn13());
        if (bookOpt.isEmpty() || bookOpt.get().getAvailableQuantity() <= 0) {
            System.err.println("ERROR_BRQS_APPROVE: Book " + request.getBookIsbn13() + " is no longer available. Rejecting request " + requestId);
            return rejectRequest(requestId, "Sách đã hết khi duyệt yêu cầu."); // Tự động từ chối
        }

        String updateRequestSql = "UPDATE BorrowingRequests SET status = ?, adminNotes = ?, resolvedDate = ?, pickupDueDate = ? WHERE requestId = ?";
        LocalDate resolvedDate = LocalDate.now();
        LocalDate pickupDueDate = resolvedDate.plusDays(3); // Ví dụ: cho 3 ngày để đến lấy sách

        Connection conn = null;
        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. Cập nhật BorrowingRequest
            try (PreparedStatement pstmtUpdateReq = conn.prepareStatement(updateRequestSql)) {
                pstmtUpdateReq.setString(1, BorrowingRequest.RequestStatus.APPROVED.name());
                pstmtUpdateReq.setString(2, adminNotes != null ? adminNotes : "Đã duyệt");
                pstmtUpdateReq.setString(3, resolvedDate.toString());
                pstmtUpdateReq.setString(4, pickupDueDate.toString());
                pstmtUpdateReq.setString(5, requestId);
                int affected = pstmtUpdateReq.executeUpdate();
                if (affected == 0) throw new SQLException("Approving request failed, no rows updated.");
            }

            // 2. Tạo BorrowingRecord (chuyển từ yêu cầu sang mượn thực sự)
            // Lưu ý: createLoan của BorrowingRecordService đã bao gồm việc gọi handleBookBorrowed
            Optional<BorrowingRecord> loanOpt = borrowingRecordService.createLoan(
                    request.getBookIsbn13(),
                    request.getUserId(),
                    resolvedDate, // Ngày mượn là ngày duyệt
                    dueDateForLoan  // Hạn trả do admin quyết định hoặc tính toán
            );

            if (loanOpt.isPresent()) {
                // Cập nhật trạng thái request thành COMPLETED vì đã tạo lượt mượn thành công
                String completeRequestSql = "UPDATE BorrowingRequests SET status = ? WHERE requestId = ?";
                try (PreparedStatement pstmtCompleteReq = conn.prepareStatement(completeRequestSql)) {
                    pstmtCompleteReq.setString(1, BorrowingRequest.RequestStatus.COMPLETED.name());
                    pstmtCompleteReq.setString(2, requestId);
                    pstmtCompleteReq.executeUpdate();
                }
                conn.commit();
                System.out.println("DEBUG_BRQS_APPROVE: Request " + requestId + " APPROVED and Loan " + loanOpt.get().getRecordId() + " created.");
                return true;
            } else {
                // Nếu không tạo được BorrowingRecord (ví dụ sách hết đột ngột, user mượn trùng - dù đã check lại)
                // Rollback và có thể set lại trạng thái request là PENDING hoặc REJECTED
                conn.rollback();
                System.err.println("ERROR_BRQS_APPROVE: Failed to create BorrowingRecord for approved request " + requestId + ". Request status might need manual review or set back to PENDING.");
                // Cân nhắc: set lại trạng thái request thành PENDING
                // updateRequestStatus(conn, requestId, BorrowingRequest.RequestStatus.PENDING, "Lỗi khi tạo lượt mượn, thử lại sau.");
                return false;
            }

        } catch (SQLException e) {
            System.err.println("ERROR_BRQS_APPROVE: DB error approving request " + requestId + ": " + e.getMessage());
            e.printStackTrace();
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { e.addSuppressed(ex); }
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    // Helper để cập nhật trạng thái (nếu cần dùng riêng)
    private boolean updateRequestStatus(Connection conn, String requestId, BorrowingRequest.RequestStatus newStatus, String adminNotes) throws SQLException {
        String sql = "UPDATE BorrowingRequests SET status = ?, adminNotes = ?, resolvedDate = ? WHERE requestId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus.name());
            pstmt.setString(2, adminNotes);
            pstmt.setString(3, LocalDate.now().toString());
            pstmt.setString(4, requestId);
            return pstmt.executeUpdate() > 0;
        }
    }


    public boolean rejectRequest(String requestId, String adminNotes) {
        Optional<BorrowingRequest> requestOpt = getRequestById(requestId);
        if (requestOpt.isEmpty() || requestOpt.get().getStatus() != BorrowingRequest.RequestStatus.PENDING) {
            System.err.println("WARN_BRQS_REJECT: Request " + requestId + " not found or not in PENDING state.");
            return false;
        }

        String sql = "UPDATE BorrowingRequests SET status = ?, adminNotes = ?, resolvedDate = ? WHERE requestId = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, BorrowingRequest.RequestStatus.REJECTED.name());
            pstmt.setString(2, adminNotes != null ? adminNotes : "Bị từ chối.");
            pstmt.setString(3, LocalDate.now().toString());
            pstmt.setString(4, requestId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_BRQS_REJECT: Request " + requestId + " REJECTED in DB.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRQS_REJECT: DB error rejecting request " + requestId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean cancelRequestByUser(String requestId, String userId) {
        Optional<BorrowingRequest> requestOpt = getRequestById(requestId);
        if (requestOpt.isEmpty() || !requestOpt.get().getUserId().equals(userId)) {
            System.err.println("WARN_BRQS_CANCEL: Request " + requestId + " not found or user " + userId + " not authorized to cancel.");
            return false;
        }
        if (requestOpt.get().getStatus() != BorrowingRequest.RequestStatus.PENDING && requestOpt.get().getStatus() != BorrowingRequest.RequestStatus.APPROVED) {
            System.err.println("WARN_BRQS_CANCEL: Request " + requestId + " cannot be canceled as it's not PENDING or APPROVED.");
            return false;
        }
        // Nếu request đã APPROVED, và sách đã được trừ đi (do createLoan trong approveRequest),
        // thì khi user hủy, cần xem xét việc cộng lại sách vào availableQuantity.
        // Tuy nhiên, logic approveRequestAndCreateLoan hiện tại sẽ tạo BorrowingRecord.
        // Nếu vậy, việc "hủy" một request đã APPROVED và đã tạo LOAN sẽ phức tạp hơn,
        // có thể không nên cho phép hoặc cần Admin can thiệp để "trả sách" cho lượt mượn đó.
        // Tạm thời, chỉ cho hủy PENDING.
        if (requestOpt.get().getStatus() != BorrowingRequest.RequestStatus.PENDING) {
            System.err.println("WARN_BRQS_CANCEL: Only PENDING requests can be canceled by user. Request " + requestId + " is " + requestOpt.get().getStatus());
            return false;
        }


        String sql = "UPDATE BorrowingRequests SET status = ?, resolvedDate = ? WHERE requestId = ? AND userId = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, BorrowingRequest.RequestStatus.CANCELED_BY_USER.name());
            pstmt.setString(2, LocalDate.now().toString());
            pstmt.setString(3, requestId);
            pstmt.setString(4, userId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_BRQS_CANCEL: Request " + requestId + " CANCELED BY USER in DB.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRQS_CANCEL: DB error canceling request " + requestId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return false;
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
            e.printStackTrace();
        }
        return requests;
    }
}
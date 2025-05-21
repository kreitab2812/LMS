package com.lms.quanlythuvien.services.transaction;

import com.lms.quanlythuvien.models.item.Book; // Import Book model
import com.lms.quanlythuvien.models.transaction.BorrowingRecord;
import com.lms.quanlythuvien.models.transaction.BorrowingRequest;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.utils.database.DatabaseManager;
// Thêm các import cần thiết khác nếu có, ví dụ NotificationService
import com.lms.quanlythuvien.services.system.NotificationService;
import com.lms.quanlythuvien.models.system.Notification;


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
    private final NotificationService notificationService; // Thêm NotificationService
    private static final DateTimeFormatter DB_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;


    private BorrowingRequestService() {
        this.bookManagementService = BookManagementService.getInstance();
        this.borrowingRecordService = BorrowingRecordService.getInstance();
        this.notificationService = NotificationService.getInstance(); // Khởi tạo
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
        // ... (Giữ nguyên logic của bạn) ...
        if (userId == null || userId.trim().isEmpty() || bookIsbn13 == null || bookIsbn13.trim().isEmpty()) {
            System.err.println("ERROR_BRQS_ADD: UserID or BookISBN13 cannot be null or empty.");
            return Optional.empty();
        }

        Optional<Book> bookOpt = bookManagementService.findBookByIsbn13InLibrary(bookIsbn13);
        if (bookOpt.isEmpty()) {
            System.err.println("ERROR_BRQS_ADD: Book with ISBN " + bookIsbn13 + " not found.");
            return Optional.empty();
        }
        Book book = bookOpt.get();
        if (book.getAvailableQuantity() <=0) {
            System.err.println("INFO_BRQS_ADD: Book " + bookIsbn13 + " is currently out of stock.");
            // Có thể cho phép yêu cầu sách hết hàng, hoặc không. Tùy logic của bạn.
            // return Optional.empty(); // Nếu không cho phép yêu cầu sách hết
        }


        String checkExistingSql = "SELECT COUNT(*) AS count FROM BorrowingRequests " +
                "WHERE userId = ? AND bookIsbn13 = ? AND status IN (?, ?)";
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
                    // Gửi thông báo cho Admin
                    notificationService.createNotification(
                            null, // Admin notification
                            "Yêu cầu mượn sách mới từ user ID: " + userId + " cho sách ISBN: " + bookIsbn13,
                            Notification.NotificationType.NEW_LOAN_REQUEST,
                            newRequest.getRequestId(), // relatedItemId là requestId
                            "VIEW_LOAN_REQUESTS_TAB" // ActionLink để admin điều hướng
                    );
                    return Optional.of(newRequest);
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRQS_ADD: DB error creating request for user " + userId + ", book " + bookIsbn13 + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean approveRequestAndCreateLoan(String requestId, String adminNotes, LocalDate dueDateForLoan) {
        Optional<BorrowingRequest> requestOpt = getRequestById(requestId); // Tự quản lý connection
        if (requestOpt.isEmpty()) {
            System.err.println("ERROR_BRQS_APPROVE: Request " + requestId + " not found.");
            return false;
        }
        BorrowingRequest request = requestOpt.get();
        if (request.getStatus() != BorrowingRequest.RequestStatus.PENDING) {
            System.err.println("ERROR_BRQS_APPROVE: Request " + requestId + " is not PENDING, current: " + request.getStatus());
            return false;
        }

        Optional<Book> bookToBorrowOpt = bookManagementService.findBookByIsbn13InLibrary(request.getBookIsbn13()); // Tự quản lý connection
        if (bookToBorrowOpt.isEmpty() || bookToBorrowOpt.get().getAvailableQuantity() <= 0) {
            System.err.println("ERROR_BRQS_APPROVE: Book " + request.getBookIsbn13() + " unavailable. Auto-rejecting request " + requestId);
            // Tự động từ chối nếu sách không còn
            return rejectRequest(requestId, "Sách không còn sẵn có hoặc đã hết khi duyệt yêu cầu.");
        }
        Book bookToBorrow = bookToBorrowOpt.get();

        LocalDate resolvedDate = LocalDate.now();
        LocalDate pickupDueDate = resolvedDate.plusDays(3); // Ví dụ: 3 ngày để lấy sách

        String updateRequestInitialSql = "UPDATE BorrowingRequests SET status = ?, adminNotes = ?, resolvedDate = ?, pickupDueDate = ? WHERE requestId = ? AND status = ?";
        String updateRequestFinalSql = "UPDATE BorrowingRequests SET status = ? WHERE requestId = ?";
        Connection conn = null;

        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false); // BẮT ĐẦU TRANSACTION CHÍNH

            // 1. Cập nhật trạng thái yêu cầu sang APPROVED (và các thông tin khác)
            try (PreparedStatement pstmtUpdateReq = conn.prepareStatement(updateRequestInitialSql)) {
                pstmtUpdateReq.setString(1, BorrowingRequest.RequestStatus.APPROVED.name());
                pstmtUpdateReq.setString(2, adminNotes != null ? adminNotes.trim() : "Đã duyệt, mời đến nhận sách.");
                pstmtUpdateReq.setString(3, resolvedDate.format(DB_DATE_FORMATTER));
                pstmtUpdateReq.setString(4, pickupDueDate.format(DB_DATE_FORMATTER));
                pstmtUpdateReq.setString(5, requestId);
                pstmtUpdateReq.setString(6, BorrowingRequest.RequestStatus.PENDING.name()); // Đảm bảo chỉ update PENDING

                int affected = pstmtUpdateReq.executeUpdate();
                if (affected == 0) {
                    // Có thể request đã được xử lý bởi một admin khác, hoặc trạng thái không còn là PENDING
                    conn.rollback();
                    System.err.println("ERROR_BRQS_APPROVE: Request " + requestId + " no longer PENDING or not found during status update to APPROVED.");
                    return false;
                }
            }

            // 2. Gọi BorrowingRecordService.createLoanLogic VỚI CONNECTION HIỆN TẠI
            Optional<BorrowingRecord> loanOpt = borrowingRecordService.createLoanLogic(
                    conn, // <<< TRUYỀN CONNECTION
                    request.getBookIsbn13(),
                    request.getUserId(),
                    resolvedDate, // Ngày mượn là ngày duyệt
                    dueDateForLoan
            );

            if (loanOpt.isPresent()) {
                // 3. Nếu tạo BorrowingRecord thành công, cập nhật trạng thái request thành COMPLETED
                try (PreparedStatement pstmtCompleteReq = conn.prepareStatement(updateRequestFinalSql)) {
                    pstmtCompleteReq.setString(1, BorrowingRequest.RequestStatus.COMPLETED.name());
                    pstmtCompleteReq.setString(2, requestId);
                    if (pstmtCompleteReq.executeUpdate() == 0) {
                        // Lỗi hiếm gặp, nhưng vẫn nên log
                        System.err.println("WARN_BRQS_APPROVE: Failed to update request " + requestId + " to COMPLETED after loan creation. Loan " + loanOpt.get().getRecordId() + " was created. Transaction will still commit.");
                    }
                }
                conn.commit(); // COMMIT TRANSACTION CHÍNH
                System.out.println("DEBUG_BRQS_APPROVE: Request " + requestId + " APPROVED & COMPLETED. Loan " + loanOpt.get().getRecordId() + " created.");

                // Gửi thông báo cho User
                notificationService.createNotification(
                        request.getUserId(),
                        "Yêu cầu mượn sách '" + bookToBorrow.getTitleOrDefault("Không rõ") + "' của bạn đã được duyệt. Hạn cuối lấy sách: " + pickupDueDate.format(DB_DATE_FORMATTER),
                        Notification.NotificationType.LOAN_APPROVED_USER,
                        String.valueOf(bookToBorrow.getInternalId()), // Hoặc ISBN, hoặc requestId
                        "VIEW_MY_REQUESTS" // Hoặc VIEW_MY_LOANS
                );
                return true;
            } else {
                // Nếu createLoanLogic thất bại (ví dụ: sách hết đột ngột do logic trong đó, hoặc lỗi khác)
                conn.rollback(); // ROLLBACK TRANSACTION CHÍNH
                System.err.println("ERROR_BRQS_APPROVE: Failed to create BorrowingRecord (via createLoanLogic) for request " + requestId + ". Transaction rolled back.");
                // Cân nhắc: Có thể cập nhật lại trạng thái request về PENDING hoặc một trạng thái lỗi khác ở đây nếu cần.
                // Hiện tại, nếu createLoanLogic trả về empty, request vẫn ở trạng thái APPROVED (từ bước 1) mà không có loan được tạo.
                // Điều này không lý tưởng. Tốt hơn là rollback cả việc update sang APPROVED.
                // => Logic hiện tại đã rollback ở trên là đúng.
                return false;
            }

        } catch (SQLException e) {
            System.err.println("ERROR_BRQS_APPROVE: SQLException occurred during approveRequestAndCreateLoan for request " + requestId + ": " + e.getMessage());
            if (conn != null) {
                try {
                    System.err.println("Attempting to rollback transaction due to SQLException...");
                    conn.rollback();
                } catch (SQLException exRollback) {
                    System.err.println("ERROR_BRQS_APPROVE_ROLLBACK_EX: Failed to rollback transaction: " + exRollback.getMessage());
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException exClose) {
                    System.err.println("ERROR_BRQS_APPROVE_CLOSE_EX: Failed to close connection: " + exClose.getMessage());
                }
            }
        }
    }

    public boolean rejectRequest(String requestId, String adminNotes) {
        // ... (Giữ nguyên logic của bạn, đảm bảo nó chỉ thực hiện 1 update và không gọi lồng service khác gây lock) ...
        Optional<BorrowingRequest> requestOpt = getRequestById(requestId);
        if (requestOpt.isEmpty()){
            System.err.println("ERROR_BRQS_REJECT: Request " + requestId + " not found.");
            return false;
        }
        if (requestOpt.get().getStatus() != BorrowingRequest.RequestStatus.PENDING) {
            System.err.println("WARN_BRQS_REJECT: Request " + requestId + " is not in PENDING state, current state: " + requestOpt.get().getStatus());
            return false; // Chỉ từ chối yêu cầu đang PENDING
        }
        Book bookInfoForNotification = bookManagementService.findBookByIsbn13InLibrary(requestOpt.get().getBookIsbn13()).orElse(null);


        String sql = "UPDATE BorrowingRequests SET status = ?, adminNotes = ?, resolvedDate = ? WHERE requestId = ? AND status = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection(); // Connection riêng cho thao tác này
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, BorrowingRequest.RequestStatus.REJECTED.name());
            pstmt.setString(2, adminNotes != null ? adminNotes.trim() : "Yêu cầu bị từ chối.");
            pstmt.setString(3, LocalDate.now().format(DB_DATE_FORMATTER));
            pstmt.setString(4, requestId);
            pstmt.setString(5, BorrowingRequest.RequestStatus.PENDING.name()); // Đảm bảo an toàn

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_BRQS_REJECT: Request " + requestId + " REJECTED.");
                // Gửi thông báo cho User
                notificationService.createNotification(
                        requestOpt.get().getUserId(),
                        "Yêu cầu mượn sách '" + (bookInfoForNotification != null ? bookInfoForNotification.getTitleOrDefault(requestOpt.get().getBookIsbn13()) : requestOpt.get().getBookIsbn13()) + "' của bạn đã bị từ chối. Lý do: " + adminNotes,
                        Notification.NotificationType.LOAN_REJECTED_USER,
                        String.valueOf(bookInfoForNotification != null ? bookInfoForNotification.getInternalId() : requestOpt.get().getBookIsbn13()),
                        "VIEW_MY_REQUESTS"
                );
                return true;
            } else {
                System.out.println("WARN_BRQS_REJECT: Request " + requestId + " not rejected. May have already been processed or not found in PENDING state.");
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRQS_REJECT: DB error rejecting request " + requestId + ": " + e.getMessage());
        }
        return false;
    }

    public boolean cancelRequestByUser(String requestId, String userId) {
        // ... (Giữ nguyên logic của bạn) ...
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

        String sql = "UPDATE BorrowingRequests SET status = ?, resolvedDate = ?, adminNotes = ? WHERE requestId = ? AND userId = ? AND status = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, BorrowingRequest.RequestStatus.CANCELED_BY_USER.name());
            pstmt.setString(2, LocalDate.now().format(DB_DATE_FORMATTER));
            pstmt.setString(3, "Người dùng tự hủy yêu cầu.");
            pstmt.setString(4, requestId);
            pstmt.setString(5, userId);
            pstmt.setString(6, BorrowingRequest.RequestStatus.PENDING.name());


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

    // Các phương thức get... không thay đổi
    public List<BorrowingRequest> getRequestsByStatus(BorrowingRequest.RequestStatus status) {
        // ... (Giữ nguyên code của bạn) ...
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
        // ... (Giữ nguyên code của bạn) ...
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
        // ... (Giữ nguyên code của bạn) ...
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
        // ... (Giữ nguyên code của bạn) ...
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

    public int countPendingRequests() {
        // ... (Giữ nguyên code của bạn) ...
        String sql = "SELECT COUNT(*) AS count FROM BorrowingRequests WHERE status = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, BorrowingRequest.RequestStatus.PENDING.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRQS_COUNT_PENDING: DB error counting PENDING borrowing requests: " + e.getMessage());
        }
        return 0;
    }
}
package com.lms.quanlythuvien.services.transaction;

import com.lms.quanlythuvien.models.transaction.DonationRequest;
import com.lms.quanlythuvien.utils.database.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DonationRequestService {
    private static DonationRequestService instance;
    private static final DateTimeFormatter DB_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private DonationRequestService() {}

    public static synchronized DonationRequestService getInstance() {
        if (instance == null) {
            instance = new DonationRequestService();
        }
        return instance;
    }

    private DonationRequest mapResultSetToDonationRequest(ResultSet rs) throws SQLException {
        String resolvedDateStr = rs.getString("resolvedDate");
        String actualReceiptDateStr = rs.getString("actualReceiptDate");
        return new DonationRequest(
                rs.getString("requestId"),
                rs.getString("userId"),
                rs.getString("bookName"),
                rs.getString("authorName"),
                rs.getString("category"),
                rs.getString("language"),
                rs.getString("reasonForContribution"),
                LocalDate.parse(rs.getString("requestDate"), DB_DATE_FORMATTER),
                DonationRequest.DonationStatus.valueOf(rs.getString("status").toUpperCase()),
                rs.getString("adminNotes"),
                resolvedDateStr != null ? LocalDate.parse(resolvedDateStr, DB_DATE_FORMATTER) : null,
                actualReceiptDateStr != null ? LocalDate.parse(actualReceiptDateStr, DB_DATE_FORMATTER) : null
        );
    }

    public Optional<DonationRequest> createRequest(DonationRequest request) {
        if (request == null || request.getRequestId() == null || request.getUserId() == null || request.getBookName() == null) {
            System.err.println("ERROR_DRS_CREATE: Invalid donation request data (null fields).");
            return Optional.empty();
        }
        String sql = "INSERT INTO DonationRequests (requestId, userId, bookName, authorName, category, language, reasonForContribution, requestDate, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, request.getRequestId());
            pstmt.setString(2, request.getUserId());
            pstmt.setString(3, request.getBookName());
            pstmt.setString(4, request.getAuthorName());
            pstmt.setString(5, request.getCategory());
            pstmt.setString(6, request.getLanguage());
            pstmt.setString(7, request.getReasonForContribution());
            pstmt.setString(8, request.getRequestDate().format(DB_DATE_FORMATTER));
            pstmt.setString(9, request.getStatus().name());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_DRS_CREATE: Donation request created. ID: " + request.getRequestId());
                return Optional.of(request);
            }
        } catch (SQLException e) {
            System.err.println("ERROR_DRS_CREATE: Could not create donation request for user " + request.getUserId() + ", book " + request.getBookName() + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<DonationRequest> getRequestsByUserId(String userId) {
        List<DonationRequest> requests = new ArrayList<>();
        if (userId == null || userId.trim().isEmpty()) return requests;
        String sql = "SELECT * FROM DonationRequests WHERE userId = ? ORDER BY requestDate DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapResultSetToDonationRequest(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_DRS_GET_BY_USER: Could not get donation requests for user " + userId + ": " + e.getMessage());
        }
        return requests;
    }

    /**
     * Lấy danh sách các yêu cầu quyên góp dựa trên trạng thái.
     * @param status Trạng thái cần lọc (ví dụ: PENDING_APPROVAL).
     * @return Danh sách các DonationRequest.
     */
    public List<DonationRequest> getRequestsByStatus(DonationRequest.DonationStatus status) {
        List<DonationRequest> requests = new ArrayList<>();
        if (status == null) {
            System.err.println("WARN_DRS_GET_BY_STATUS: Status parameter is null. Returning empty list.");
            return requests;
        }
        String sql = "SELECT * FROM DonationRequests WHERE status = ? ORDER BY requestDate DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapResultSetToDonationRequest(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_DRS_GET_BY_STATUS: Could not get donation requests for status " + status + ": " + e.getMessage());
        }
        return requests;
    }

    public List<DonationRequest> getAllRequests() {
        List<DonationRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM DonationRequests ORDER BY requestDate DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                requests.add(mapResultSetToDonationRequest(rs));
            }
        } catch (SQLException e) {
            System.err.println("ERROR_DRS_GET_ALL: Could not get all donation requests: " + e.getMessage());
        }
        return requests;
    }

    public Optional<DonationRequest> getRequestById(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) return Optional.empty();
        String sql = "SELECT * FROM DonationRequests WHERE requestId = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, requestId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToDonationRequest(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_DRS_GET_BY_ID: Could not get donation request " + requestId + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean updateRequestStatus(String requestId, DonationRequest.DonationStatus newStatus, String adminNotes, LocalDate actualReceiptDate) {
        if (requestId == null || newStatus == null) {
            System.err.println("ERROR_DRS_UPDATE_STATUS: Request ID or new status cannot be null.");
            return false;
        }
        if (newStatus != DonationRequest.DonationStatus.COMPLETED && newStatus != DonationRequest.DonationStatus.APPROVED_PENDING_RECEIPT && actualReceiptDate != null) {
            System.err.println("WARN_DRS_UPDATE_STATUS: actualReceiptDate can only be set if status is COMPLETED or APPROVED_PENDING_RECEIPT. It will be ignored for status: " + newStatus);
            actualReceiptDate = null; // Hoặc bỏ qua việc set nếu không phù hợp
        }

        String sql = "UPDATE DonationRequests SET status = ?, adminNotes = ?, resolvedDate = ?, actualReceiptDate = ? WHERE requestId = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus.name());
            pstmt.setString(2, adminNotes);
            pstmt.setString(3, LocalDate.now().format(DB_DATE_FORMATTER)); // resolvedDate
            if (actualReceiptDate != null && (newStatus == DonationRequest.DonationStatus.COMPLETED || newStatus == DonationRequest.DonationStatus.APPROVED_PENDING_RECEIPT)) {
                pstmt.setString(4, actualReceiptDate.format(DB_DATE_FORMATTER));
            } else {
                pstmt.setNull(4, Types.VARCHAR); // Hoặc Types.DATE tùy schema
            }
            pstmt.setString(5, requestId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_DRS_UPDATE_STATUS: Donation request " + requestId + " status updated to " + newStatus);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("ERROR_DRS_UPDATE_STATUS: Could not update status for donation request " + requestId + ": " + e.getMessage());
        }
        return false;
    }
}
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

    private DonationRequestService() {}

    public static synchronized DonationRequestService getInstance() {
        if (instance == null) {
            instance = new DonationRequestService();
        }
        return instance;
    }

    private DonationRequest mapResultSetToDonationRequest(ResultSet rs) throws SQLException {
        return new DonationRequest(
                rs.getString("requestId"),
                rs.getString("userId"),
                rs.getString("bookName"),
                rs.getString("authorName"),
                rs.getString("category"),
                rs.getString("language"),
                rs.getString("reasonForContribution"),
                LocalDate.parse(rs.getString("requestDate")),
                DonationRequest.DonationStatus.valueOf(rs.getString("status")),
                rs.getString("adminNotes"),
                rs.getString("resolvedDate") != null ? LocalDate.parse(rs.getString("resolvedDate")) : null,
                rs.getString("actualReceiptDate") != null ? LocalDate.parse(rs.getString("actualReceiptDate")) : null
        );
    }

    public Optional<DonationRequest> createRequest(DonationRequest request) {
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
            pstmt.setString(8, request.getRequestDate().toString());
            pstmt.setString(9, request.getStatus().name());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                return Optional.of(request);
            }
        } catch (SQLException e) {
            System.err.println("ERROR_DRS_CREATE: Could not create donation request: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<DonationRequest> getRequestsByUserId(String userId) {
        List<DonationRequest> requests = new ArrayList<>();
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
            e.printStackTrace();
        }
        return requests;
    }

    // Các hàm khác cho Admin (ví dụ: getAllPendingRequests, updateRequestStatus) sẽ làm sau
}
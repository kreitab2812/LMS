package com.lms.quanlythuvien.services.library; // Hoặc package phù hợp

import com.lms.quanlythuvien.models.item.BookReview;
import com.lms.quanlythuvien.models.user.User; // Cần để lấy username
import com.lms.quanlythuvien.services.user.UserService;
import com.lms.quanlythuvien.utils.database.DatabaseManager;


import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookReviewService {
    private static BookReviewService instance;
    private UserService userService; // Để lấy username của người review

    private BookReviewService() {
        userService = UserService.getInstance();
    }

    public static synchronized BookReviewService getInstance() {
        if (instance == null) {
            instance = new BookReviewService();
        }
        return instance;
    }

    private BookReview mapResultSetToBookReview(ResultSet rs) throws SQLException {
        BookReview review = new BookReview(
                rs.getString("reviewId"),
                rs.getInt("bookInternalId"),
                rs.getString("userId"),
                null, // userUsername sẽ được lấy riêng
                rs.getInt("rating"),
                rs.getString("commentText"),
                LocalDateTime.parse(rs.getString("reviewDate"), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
        // Lấy username
        Optional<User> userOpt = userService.findUserById(review.getUserId());
        userOpt.ifPresent(user -> review.setUserUsername(user.getUsername()));
        return review;
    }

    public Optional<BookReview> addReview(BookReview review) {
        // Kiểm tra xem user đã review sách này chưa (có thể cho phép sửa review thay vì thêm mới)
        // String checkSql = "SELECT COUNT(*) FROM BookReviews WHERE bookInternalId = ? AND userId = ?"; ...

        String sql = "INSERT INTO BookReviews (reviewId, bookInternalId, userId, rating, commentText, reviewDate) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, review.getReviewId());
            pstmt.setInt(2, review.getBookInternalId());
            pstmt.setString(3, review.getUserId());
            pstmt.setInt(4, review.getRating());
            pstmt.setString(5, review.getCommentText());
            pstmt.setString(6, review.getReviewDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                // Lấy lại username cho đối tượng review vừa thêm
                Optional<User> userOpt = userService.findUserById(review.getUserId());
                userOpt.ifPresent(user -> review.setUserUsername(user.getUsername()));
                return Optional.of(review);
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRS_ADD_REVIEW: Could not add review: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<BookReview> getReviewsByBookInternalId(int bookInternalId) {
        List<BookReview> reviews = new ArrayList<>();
        String sql = "SELECT * FROM BookReviews WHERE bookInternalId = ? ORDER BY reviewDate DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookInternalId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapResultSetToBookReview(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRS_GET_REVIEWS: Could not get reviews for book " + bookInternalId + ": " + e.getMessage());
        }
        return reviews;
    }

    public Optional<Double> getAverageRatingForBook(int bookInternalId) {
        String sql = "SELECT AVG(rating) AS avgRating FROM BookReviews WHERE bookInternalId = ? AND rating > 0"; // Chỉ tính rating > 0
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookInternalId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    double avg = rs.getDouble("avgRating");
                    if (rs.wasNull()) { // Không có rating nào (hoặc tất cả là 0)
                        return Optional.empty();
                    }
                    return Optional.of(avg);
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BRS_AVG_RATING: Error getting average rating for book " + bookInternalId + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    // (Tùy chọn) Các hàm updateReview, deleteReview
}
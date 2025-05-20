package com.lms.quanlythuvien.services.user;

import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.utils.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
// Bỏ Collectors nếu không dùng ở đây

public class FavoriteBookService {
    private static FavoriteBookService instance;
    private final BookManagementService bookManagementService;
    private static final DateTimeFormatter DB_DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private FavoriteBookService() {
        this.bookManagementService = BookManagementService.getInstance();
    }

    public static synchronized FavoriteBookService getInstance() {
        if (instance == null) {
            instance = new FavoriteBookService();
        }
        return instance;
    }

    public boolean addFavorite(String userId, int bookInternalId) {
        if (userId == null || userId.trim().isEmpty() || bookInternalId <= 0) {
            System.err.println("ERROR_FAV_SVC_ADD: Invalid userId or bookInternalId.");
            return false;
        }
        if (isFavorite(userId, bookInternalId)) {
            System.out.println("INFO_FAV_SVC: Book " + bookInternalId + " is already a favorite for user " + userId);
            return true;
        }
        String sql = "INSERT INTO UserFavoriteBooks (userId, bookInternalId, favoritedAt) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setInt(2, bookInternalId);
            pstmt.setString(3, LocalDateTime.now().format(DB_DATETIME_FORMATTER));
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_FAV_SVC_ADD: Book " + bookInternalId + " added to favorites for user " + userId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("ERROR_FAV_SVC_ADD: Could not add favorite for user " + userId + ", book " + bookInternalId + ": " + e.getMessage());
        }
        return false;
    }

    public boolean removeFavorite(String userId, int bookInternalId) {
        if (userId == null || userId.trim().isEmpty() || bookInternalId <= 0) {
            System.err.println("ERROR_FAV_SVC_REMOVE: Invalid userId or bookInternalId.");
            return false;
        }
        String sql = "DELETE FROM UserFavoriteBooks WHERE userId = ? AND bookInternalId = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setInt(2, bookInternalId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_FAV_SVC_REMOVE: Book " + bookInternalId + " removed from favorites for user " + userId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("ERROR_FAV_SVC_REMOVE: Could not remove favorite for user " + userId + ", book " + bookInternalId + ": " + e.getMessage());
        }
        return false;
    }

    public boolean isFavorite(String userId, int bookInternalId) {
        if (userId == null || userId.trim().isEmpty() || bookInternalId <= 0) return false;
        String sql = "SELECT 1 FROM UserFavoriteBooks WHERE userId = ? AND bookInternalId = ? LIMIT 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setInt(2, bookInternalId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("ERROR_FAV_SVC_IS_FAV: Error checking favorite status for user " + userId + ", book " + bookInternalId + ": " + e.getMessage());
        }
        return false;
    }

    /**
     * Lấy danh sách các đối tượng Book là sách yêu thích của người dùng.
     * Đã được tối ưu để giảm N+1 query.
     * @param userId ID của người dùng.
     * @return List các Book object.
     */
    public List<Book> getFavoriteBooksByUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return new ArrayList<>();
        }

        Set<Integer> favoriteBookInternalIds = new HashSet<>();
        String sqlGetIds = "SELECT bookInternalId FROM UserFavoriteBooks WHERE userId = ? ORDER BY favoritedAt DESC";

        // Bước 1: Lấy tất cả internalId của sách yêu thích
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmtGetIds = conn.prepareStatement(sqlGetIds)) {
            pstmtGetIds.setString(1, userId);
            try (ResultSet rsIds = pstmtGetIds.executeQuery()) {
                while (rsIds.next()) {
                    favoriteBookInternalIds.add(rsIds.getInt("bookInternalId"));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_FAV_SVC_GET_FAV_IDS: Could not get favorite book IDs for user " + userId + ": " + e.getMessage());
            return new ArrayList<>(); // Trả về danh sách rỗng nếu có lỗi
        }

        if (favoriteBookInternalIds.isEmpty()) {
            return new ArrayList<>(); // Không có sách yêu thích nào
        }

        // Bước 2: Gọi BookManagementService để lấy chi tiết các sách này một lần
        // Phương thức getBooksByInternalIds trong BookManagementService đã được tối ưu
        return bookManagementService.getBooksByInternalIds(favoriteBookInternalIds);
    }

    public List<UserFavoriteBook> getRawFavoriteEntriesByUserId(String userId) {
        List<UserFavoriteBook> entries = new ArrayList<>();
        if (userId == null || userId.trim().isEmpty()) return entries;

        String sql = "SELECT userId, bookInternalId, favoritedAt FROM UserFavoriteBooks WHERE userId = ? ORDER BY favoritedAt DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(new UserFavoriteBook(
                            rs.getString("userId"),
                            rs.getInt("bookInternalId"),
                            LocalDateTime.parse(rs.getString("favoritedAt"), DB_DATETIME_FORMATTER)
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_FAV_SVC_GET_RAW: Could not get raw favorite entries for user " + userId + ": " + e.getMessage());
        }
        return entries;
    }
}
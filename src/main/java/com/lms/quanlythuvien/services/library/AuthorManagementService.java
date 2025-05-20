package com.lms.quanlythuvien.services.library; // Hoặc package service phù hợp của cậu

import com.lms.quanlythuvien.models.item.Author; // Model Author đã được mở rộng
import com.lms.quanlythuvien.utils.database.DatabaseManager;
import com.lms.quanlythuvien.exceptions.DeletionRestrictedException; // Custom exception

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class AuthorManagementService {

    private static AuthorManagementService instance;

    private AuthorManagementService() {
        // Constructor private cho Singleton
    }

    public static synchronized AuthorManagementService getInstance() {
        if (instance == null) {
            instance = new AuthorManagementService();
        }
        return instance;
    }

    // Helper để map ResultSet sang đối tượng Author đầy đủ
    private Author mapResultSetToAuthor(ResultSet rs) throws SQLException {
        return new Author(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("biography"),
                rs.getObject("yearOfBirth") != null ? rs.getInt("yearOfBirth") : null,
                rs.getObject("yearOfDeath") != null ? rs.getInt("yearOfDeath") : null,
                rs.getString("gender"),
                rs.getString("nationality"),
                rs.getString("placeOfBirth"),
                rs.getString("avatarUrl"),
                rs.getString("createdAt"),
                rs.getString("updatedAt")
        );
    }

    /**
     * Thêm một tác giả mới vào database.
     * Tên tác giả là bắt buộc và phải là duy nhất.
     * @param authorDetails Đối tượng Author chứa thông tin tác giả mới (ID và timestamps sẽ được bỏ qua).
     * @return Optional chứa đối tượng Author đã được lưu (có ID và timestamps từ DB) nếu thành công, rỗng nếu thất bại.
     */
    public Optional<Author> addAuthor(Author authorDetails) {
        if (authorDetails == null || authorDetails.getName() == null || authorDetails.getName().trim().isEmpty()) {
            System.err.println("ERROR_AMS_ADD: Author name cannot be null or empty.");
            return Optional.empty();
        }

        // Kiểm tra tên tác giả đã tồn tại chưa
        if (findAuthorByName(authorDetails.getName().trim()).isPresent()) {
            System.err.println("ERROR_AMS_ADD: Author with name '" + authorDetails.getName().trim() + "' already exists.");
            return Optional.empty();
        }

        String sql = "INSERT INTO Authors (name, biography, yearOfBirth, yearOfDeath, gender, nationality, placeOfBirth, avatarUrl, createdAt, updatedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, authorDetails.getName().trim());
            pstmt.setString(2, authorDetails.getBiography());
            // Sử dụng setObject để xử lý Integer có thể null
            if (authorDetails.getYearOfBirth() != null) pstmt.setInt(3, authorDetails.getYearOfBirth()); else pstmt.setNull(3, Types.INTEGER);
            if (authorDetails.getYearOfDeath() != null) pstmt.setInt(4, authorDetails.getYearOfDeath()); else pstmt.setNull(4, Types.INTEGER);
            pstmt.setString(5, authorDetails.getGender());
            pstmt.setString(6, authorDetails.getNationality());
            pstmt.setString(7, authorDetails.getPlaceOfBirth());
            pstmt.setString(8, authorDetails.getAvatarUrl());
            pstmt.setString(9, currentTime);  // createdAt
            pstmt.setString(10, currentTime); // updatedAt

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        // Tạo một đối tượng Author mới với ID và timestamps từ DB để trả về
                        Author createdAuthor = new Author(
                                generatedKeys.getInt(1), // id
                                authorDetails.getName().trim(),
                                authorDetails.getBiography(),
                                authorDetails.getYearOfBirth(),
                                authorDetails.getYearOfDeath(),
                                authorDetails.getGender(),
                                authorDetails.getNationality(),
                                authorDetails.getPlaceOfBirth(),
                                authorDetails.getAvatarUrl(),
                                currentTime, // createdAt
                                currentTime  // updatedAt
                        );
                        System.out.println("DEBUG_AMS_ADD: Author added successfully: " + createdAuthor.getName() + " (ID: " + createdAuthor.getId() + ")");
                        return Optional.of(createdAuthor);
                    } else {
                        throw new SQLException("Creating author failed, no ID obtained.");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_AMS_ADD: DB error adding author '" + authorDetails.getName() + "': " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<Author> findAuthorById(int authorId) {
        String sql = "SELECT * FROM Authors WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, authorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAuthor(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_AMS_FIND_ID: DB error finding author by ID " + authorId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<Author> findAuthorByName(String name) {
        if (name == null || name.trim().isEmpty()) return Optional.empty();
        String sql = "SELECT * FROM Authors WHERE name = ?"; // Tìm kiếm chính xác theo tên (có thể đổi thành LIKE nếu muốn)
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAuthor(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_AMS_FIND_NAME: DB error finding author by name '" + name + "': " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<Author> getAllAuthors() {
        List<Author> authors = new ArrayList<>();
        String sql = "SELECT * FROM Authors ORDER BY name COLLATE NOCASE ASC"; // Sắp xếp A-Z không phân biệt hoa thường
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                authors.add(mapResultSetToAuthor(rs));
            }
        } catch (SQLException e) {
            System.err.println("ERROR_AMS_GET_ALL: DB error retrieving all authors: " + e.getMessage());
            e.printStackTrace();
        }
        return authors;
    }

    public boolean updateAuthor(Author authorToUpdate) {
        if (authorToUpdate == null || authorToUpdate.getId() == 0) {
            System.err.println("ERROR_AMS_UPDATE: Author to update or its ID is invalid.");
            return false;
        }
        if (authorToUpdate.getName() == null || authorToUpdate.getName().trim().isEmpty()){
            System.err.println("ERROR_AMS_UPDATE: Author name cannot be empty for update.");
            return false;
        }

        // Kiểm tra nếu tên mới (nếu thay đổi) có bị trùng với tác giả khác không
        Optional<Author> authorByNameOpt = findAuthorByName(authorToUpdate.getName().trim());
        if (authorByNameOpt.isPresent() && authorByNameOpt.get().getId() != authorToUpdate.getId()) {
            System.err.println("ERROR_AMS_UPDATE: New name '" + authorToUpdate.getName().trim() + "' is already taken by another author (ID: " + authorByNameOpt.get().getId() + ").");
            return false;
        }

        String sql = "UPDATE Authors SET name = ?, biography = ?, yearOfBirth = ?, yearOfDeath = ?, " +
                "gender = ?, nationality = ?, placeOfBirth = ?, avatarUrl = ?, updatedAt = ? " +
                "WHERE id = ?";
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, authorToUpdate.getName().trim());
            pstmt.setString(2, authorToUpdate.getBiography());
            if (authorToUpdate.getYearOfBirth() != null) pstmt.setInt(3, authorToUpdate.getYearOfBirth()); else pstmt.setNull(3, Types.INTEGER);
            if (authorToUpdate.getYearOfDeath() != null) pstmt.setInt(4, authorToUpdate.getYearOfDeath()); else pstmt.setNull(4, Types.INTEGER);
            pstmt.setString(5, authorToUpdate.getGender());
            pstmt.setString(6, authorToUpdate.getNationality());
            pstmt.setString(7, authorToUpdate.getPlaceOfBirth());
            pstmt.setString(8, authorToUpdate.getAvatarUrl());
            pstmt.setString(9, currentTime); // updatedAt
            pstmt.setInt(10, authorToUpdate.getId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_AMS_UPDATE: Author updated successfully: " + authorToUpdate.getName());
                return true;
            } else {
                System.err.println("ERROR_AMS_UPDATE: Author with ID " + authorToUpdate.getId() + " not found or no data changed.");
            }
        } catch (SQLException e) {
            System.err.println("ERROR_AMS_UPDATE: DB error updating author '" + authorToUpdate.getName() + "': " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Xóa một tác giả. Sẽ ném DeletionRestrictedException nếu tác giả còn sách.
     * @param authorId ID của tác giả cần xóa.
     * @return true nếu xóa thành công.
     * @throws DeletionRestrictedException nếu tác giả còn sách liên kết.
     */
    public boolean deleteAuthor(int authorId) throws DeletionRestrictedException {
        // Kiểm tra xem tác giả có sách nào không (nhờ ON DELETE RESTRICT trên BookAuthors)
        // Tuy nhiên, để có thông báo thân thiện hơn, chúng ta nên kiểm tra trước
        String checkBooksSql = "SELECT COUNT(*) AS bookCount FROM BookAuthors WHERE authorId = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection()) { // Mở connection một lần
            try (PreparedStatement checkStmt = conn.prepareStatement(checkBooksSql)) {
                checkStmt.setInt(1, authorId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt("bookCount") > 0) {
                        String message = "Không thể xóa tác giả này (ID: " + authorId + ") vì họ đang có " + rs.getInt("bookCount") + " đầu sách trong thư viện.";
                        System.err.println("ERROR_AMS_DELETE: " + message);
                        throw new DeletionRestrictedException(message);
                    }
                }
            }

            // Nếu không có sách, tiến hành xóa tác giả
            String deleteAuthorSql = "DELETE FROM Authors WHERE id = ?";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteAuthorSql)) {
                deleteStmt.setInt(1, authorId);
                int affectedRows = deleteStmt.executeUpdate();
                if (affectedRows > 0) {
                    System.out.println("DEBUG_AMS_DELETE: Author deleted from DB with ID: " + authorId);
                    return true;
                } else {
                    System.err.println("ERROR_AMS_DELETE: Author with ID " + authorId + " not found for deletion (after check).");
                    return false;
                }
            }
        } catch (SQLException e) {
            // Nếu lỗi là do vi phạm ràng buộc (dù đã kiểm tra, trường hợp hiếm)
            if (e.getMessage().toLowerCase().contains("constraint") || e.getMessage().toLowerCase().contains("foreign key")) {
                System.err.println("ERROR_AMS_DELETE: DB constraint error for author ID " + authorId + ": " + e.getMessage());
                throw new DeletionRestrictedException("Không thể xóa tác giả do ràng buộc dữ liệu. Vui lòng kiểm tra lại các sách liên quan.");
            }
            System.err.println("ERROR_AMS_DELETE: DB error during deletion process for author ID " + authorId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Tìm kiếm tác giả theo tên hoặc một phần tiểu sử.
     * @param keyword Từ khóa tìm kiếm.
     * @return Danh sách các Author khớp.
     */
    public List<Author> searchAuthors(String keyword) {
        List<Author> authors = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllAuthors(); // Trả về tất cả nếu keyword rỗng
        }
        String sql = "SELECT * FROM Authors WHERE LOWER(name) LIKE ? OR LOWER(biography) LIKE ? ORDER BY name COLLATE NOCASE ASC";
        String searchTerm = "%" + keyword.toLowerCase().trim() + "%";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, searchTerm); // Tìm trong tên
            pstmt.setString(2, searchTerm); // Tìm trong tiểu sử
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    authors.add(mapResultSetToAuthor(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_AMS_SEARCH: DB error searching authors with keyword '" + keyword + "': " + e.getMessage());
            e.printStackTrace();
        }
        return authors;
    }
}
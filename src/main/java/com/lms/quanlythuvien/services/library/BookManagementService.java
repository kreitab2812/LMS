package com.lms.quanlythuvien.services.library;

import com.lms.quanlythuvien.models.item.Author;
import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.utils.database.DatabaseManager;
import com.lms.quanlythuvien.exceptions.DeletionRestrictedException;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BookManagementService {

    private static BookManagementService instance;

    private BookManagementService() {
        System.out.println("DEBUG_BMS_SINGLETON: BookManagementService Singleton instance created. Data managed by database.");
        // Việc tạo sách mẫu (initializeSampleBooks) nên được xử lý bằng một cơ chế
        // database seeding riêng, chạy một lần sau khi DatabaseManager tạo bảng, nếu cần.
    }

    public static synchronized BookManagementService getInstance() {
        if (instance == null) {
            instance = new BookManagementService();
        }
        return instance;
    }

    // --- HELPER METHODS FOR AUTHORS (trong cùng transaction với sách) ---
    private Optional<Author> findAuthorByName(Connection conn, String name) throws SQLException {
        // Lấy tất cả các trường của Author từ DB
        String sql = "SELECT id, name, biography, yearOfBirth, yearOfDeath, gender, nationality, placeOfBirth, avatarUrl, createdAt, updatedAt " +
                "FROM Authors WHERE name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Sử dụng constructor đầy đủ của Author
                    Author author = new Author(
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
                    return Optional.of(author);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Lấy tác giả theo tên, nếu chưa có thì tạo mới trong DB.
     * Trả về đối tượng Author đầy đủ thông tin (bao gồm cả ID và timestamps).
     */
    private Author getOrCreateAuthor(Connection conn, String authorName) throws SQLException {
        Optional<Author> existingAuthorOpt = findAuthorByName(conn, authorName); // findAuthorByName giờ trả về Author đầy đủ
        if (existingAuthorOpt.isPresent()) {
            return existingAuthorOpt.get();
        } else {
            // Tạo đối tượng Author mới với các thông tin cơ bản, các trường khác có thể null
            // Sử dụng constructor: Author(name, biography, yearOfBirth, yearOfDeath, gender, nationality, placeOfBirth, avatarUrl)
            Author newAuthor = new Author(
                    authorName.trim(), // name
                    null,             // biography (để null hoặc chuỗi rỗng nếu không có thông tin ban đầu)
                    null,             // yearOfBirth
                    null,             // yearOfDeath
                    null,             // gender
                    null,             // nationality
                    null,             // placeOfBirth
                    null              // avatarUrl
            );

            String sqlInsertAuthor = "INSERT INTO Authors (name, biography, yearOfBirth, yearOfDeath, gender, nationality, placeOfBirth, avatarUrl, createdAt, updatedAt) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            try (PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsertAuthor, Statement.RETURN_GENERATED_KEYS)) {
                pstmtInsert.setString(1, newAuthor.getName());
                pstmtInsert.setString(2, newAuthor.getBiography()); // Sẽ là null nếu không cung cấp
                // Dùng setObject cho các Integer có thể null
                if (newAuthor.getYearOfBirth() != null) pstmtInsert.setInt(3, newAuthor.getYearOfBirth()); else pstmtInsert.setNull(3, Types.INTEGER);
                if (newAuthor.getYearOfDeath() != null) pstmtInsert.setInt(4, newAuthor.getYearOfDeath()); else pstmtInsert.setNull(4, Types.INTEGER);
                pstmtInsert.setString(5, newAuthor.getGender());
                pstmtInsert.setString(6, newAuthor.getNationality());
                pstmtInsert.setString(7, newAuthor.getPlaceOfBirth());
                pstmtInsert.setString(8, newAuthor.getAvatarUrl());
                pstmtInsert.setString(9, currentTime);  // createdAt
                pstmtInsert.setString(10, currentTime); // updatedAt

                pstmtInsert.executeUpdate();
                try (ResultSet generatedKeys = pstmtInsert.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        newAuthor.setId(generatedKeys.getInt(1));
                        newAuthor.setCreatedAt(currentTime); // Cập nhật lại cho đối tượng Java
                        newAuthor.setUpdatedAt(currentTime); // Cập nhật lại cho đối tượng Java
                        return newAuthor; // Trả về đối tượng Author đã có ID và timestamps
                    } else {
                        throw new SQLException("Creating author failed, no ID obtained for: " + authorName);
                    }
                }
            }
        }
    }
    private void linkBookToAuthors(Connection conn, int bookInternalId, List<String> authorNames) throws SQLException {
        if (authorNames == null || authorNames.isEmpty()) {
            return;
        }
        String sqlLink = "INSERT INTO BookAuthors (bookInternalId, authorId) VALUES (?, ?)";
        for (String authorName : authorNames) {
            if (authorName == null || authorName.trim().isEmpty()) continue;
            Author author = getOrCreateAuthor(conn, authorName.trim()); // Đảm bảo tác giả tồn tại hoặc được tạo
            try (PreparedStatement pstmtLink = conn.prepareStatement(sqlLink)) {
                pstmtLink.setInt(1, bookInternalId);
                pstmtLink.setInt(2, author.getId());
                pstmtLink.executeUpdate();
            }
        }
    }

    private void unlinkAllAuthorsFromBook(Connection conn, int bookInternalId) throws SQLException {
        String sqlDeleteLinks = "DELETE FROM BookAuthors WHERE bookInternalId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sqlDeleteLinks)) {
            pstmt.setInt(1, bookInternalId);
            pstmt.executeUpdate();
        }
    }

    private List<String> getAuthorNamesForBook(Connection conn, int bookInternalId) throws SQLException {
        List<String> authorNames = new ArrayList<>();
        String sql = "SELECT a.name FROM Authors a JOIN BookAuthors ba ON a.id = ba.authorId WHERE ba.bookInternalId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookInternalId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    authorNames.add(rs.getString("name"));
                }
            }
        }
        return authorNames;
    }


    // --- BOOK MANAGEMENT METHODS ---

    public boolean addBookToLibrary(Book newBook) {
        if (newBook == null || newBook.getIsbn13() == null || newBook.getIsbn13().trim().isEmpty()) {
            System.err.println("ERROR_BMS_ADD: Book or its ISBN-13 cannot be null or empty.");
            return false;
        }
        if (findBookByIsbn13InLibrary(newBook.getIsbn13()).isPresent()) {
            System.err.println("ERROR_BMS_ADD: Book with ISBN-13 " + newBook.getIsbn13() + " already exists.");
            return false;
        }

        String insertBookSQL = "INSERT INTO Books (isbn13, customDisplayId, title, publisher, publishedDate, description, pageCount, categories, averageRating, ratingsCount, thumbnailUrl, infoLink, isbn10, totalQuantity, availableQuantity, shelfLocation, addedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        Connection conn = null;

        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false); // Start transaction

            int bookInternalId;
            try (PreparedStatement pstmtBook = conn.prepareStatement(insertBookSQL, Statement.RETURN_GENERATED_KEYS)) {
                pstmtBook.setString(1, newBook.getIsbn13());
                // customDisplayId cần được tạo hoặc lấy từ newBook.getId() nếu nó mang ý nghĩa đó
                // Hiện tại, Book.getId() có thể là Google ID. Cần logic rõ ràng cho customDisplayId
                pstmtBook.setString(2, newBook.getId()); // Tạm dùng newBook.getId() cho customDisplayId
                pstmtBook.setString(3, newBook.getTitle());
                pstmtBook.setString(4, newBook.getPublisher());
                pstmtBook.setString(5, newBook.getPublishedDate());
                pstmtBook.setString(6, newBook.getDescription());
                pstmtBook.setObject(7, newBook.getPageCount());
                pstmtBook.setString(8, newBook.getCategories() != null ? String.join(";", newBook.getCategories()) : null); // Dùng dấu ; để phân cách
                pstmtBook.setObject(9, newBook.getAverageRating());
                pstmtBook.setObject(10, newBook.getRatingsCount());
                pstmtBook.setString(11, newBook.getThumbnailUrl());
                pstmtBook.setString(12, newBook.getInfoLink());
                pstmtBook.setString(13, newBook.getIsbn10());
                pstmtBook.setInt(14, newBook.getTotalQuantity());
                pstmtBook.setInt(15, newBook.getAvailableQuantity());
                pstmtBook.setString(16, newBook.getShelfLocation());
                pstmtBook.setString(17, currentTime);

                pstmtBook.executeUpdate();
                try (ResultSet generatedKeys = pstmtBook.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        bookInternalId = generatedKeys.getInt(1); // Lấy internalId tự tăng
                    } else {
                        throw new SQLException("Creating book failed, no internal ID obtained for ISBN: " + newBook.getIsbn13());
                    }
                }
            }

            linkBookToAuthors(conn, bookInternalId, newBook.getAuthors());

            conn.commit();
            System.out.println("DEBUG_BMS_ADD: Book added to DB: '" + newBook.getTitle() + "' (ISBN-13: " + newBook.getIsbn13() + ", InternalID: " + bookInternalId + ")");
            return true;

        } catch (SQLException e) {
            System.err.println("ERROR_BMS_ADD: DB error adding book with ISBN-13 " + newBook.getIsbn13() + ": " + e.getMessage());
            e.printStackTrace();
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { e.addSuppressed(ex); }
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public boolean updateBookInLibrary(Book updatedBook) {
        if (updatedBook == null || updatedBook.getIsbn13() == null || updatedBook.getIsbn13().trim().isEmpty()) {
            System.err.println("ERROR_BMS_UPDATE: Updated book or its ISBN-13 cannot be null or empty.");
            return false;
        }

        // Tìm internalId của sách dựa trên ISBN-13
        Optional<Integer> internalIdOpt = findInternalIdByIsbn13(updatedBook.getIsbn13());
        if (internalIdOpt.isEmpty()) {
            System.err.println("ERROR_BMS_UPDATE: Book with ISBN-13 " + updatedBook.getIsbn13() + " not found for update.");
            return false;
        }
        int bookInternalId = internalIdOpt.get();

        String updateBookSQL = "UPDATE Books SET title=?, publisher=?, publishedDate=?, description=?, pageCount=?, categories=?, averageRating=?, ratingsCount=?, thumbnailUrl=?, infoLink=?, isbn10=?, totalQuantity=?, availableQuantity=?, shelfLocation=?, customDisplayId=? WHERE internalId = ?";
        Connection conn = null;

        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmtBook = conn.prepareStatement(updateBookSQL)) {
                pstmtBook.setString(1, updatedBook.getTitle());
                pstmtBook.setString(2, updatedBook.getPublisher());
                pstmtBook.setString(3, updatedBook.getPublishedDate());
                pstmtBook.setString(4, updatedBook.getDescription());
                pstmtBook.setObject(5, updatedBook.getPageCount());
                pstmtBook.setString(6, updatedBook.getCategories() != null ? String.join(";", updatedBook.getCategories()) : null);
                pstmtBook.setObject(7, updatedBook.getAverageRating());
                pstmtBook.setObject(8, updatedBook.getRatingsCount());
                pstmtBook.setString(9, updatedBook.getThumbnailUrl());
                pstmtBook.setString(10, updatedBook.getInfoLink());
                pstmtBook.setString(11, updatedBook.getIsbn10());
                pstmtBook.setInt(12, updatedBook.getTotalQuantity());
                pstmtBook.setInt(13, updatedBook.getAvailableQuantity()); // Cần đảm bảo logic tính availableQuantity đúng trước khi gọi update
                pstmtBook.setString(14, updatedBook.getShelfLocation());
                pstmtBook.setString(15, updatedBook.getId()); // customDisplayId (tạm dùng getId())
                pstmtBook.setInt(16, bookInternalId);
                pstmtBook.executeUpdate();
            }

            unlinkAllAuthorsFromBook(conn, bookInternalId); // Xóa liên kết tác giả cũ
            linkBookToAuthors(conn, bookInternalId, updatedBook.getAuthors()); // Tạo liên kết tác giả mới

            conn.commit();
            System.out.println("DEBUG_BMS_UPDATE: Book updated in DB: " + updatedBook.getTitle() + " (ISBN-13: " + updatedBook.getIsbn13() + ")");
            return true;

        } catch (SQLException e) {
            System.err.println("ERROR_BMS_UPDATE: DB error updating book with ISBN-13 " + updatedBook.getIsbn13() + ": " + e.getMessage());
            e.printStackTrace();
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { e.addSuppressed(ex); }
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public boolean deleteBookFromLibrary(String isbn13) throws DeletionRestrictedException { // << THÊM "throws DeletionRestrictedException"
        if (isbn13 == null || isbn13.trim().isEmpty()) {
            System.err.println("ERROR_BMS_DELETE: ISBN-13 cannot be null or empty.");
            // throw new IllegalArgumentException("ISBN-13 cannot be null or empty.");
            return false;
        }

        // Lấy internalId của sách từ isbn13 để kiểm tra và xóa
        Optional<Integer> internalIdOpt = findInternalIdByIsbn13(isbn13); // Hàm này cậu đã có
        if (internalIdOpt.isEmpty()) {
            System.err.println("ERROR_BMS_DELETE: Book with ISBN-13 '" + isbn13 + "' not found.");
            return false; // Sách không tồn tại thì không thể xóa
        }
        int bookInternalId = internalIdOpt.get();

        // Bước 1: Kiểm tra xem sách có đang được mượn không
        String checkLoansSql = "SELECT COUNT(*) AS loanCount FROM BorrowingRecords WHERE bookInternalId = ? AND status IN ('BORROWED', 'ACTIVE', 'OVERDUE')";
        try (Connection conn = DatabaseManager.getInstance().getConnection()) { // Mở connection một lần
            try (PreparedStatement checkStmt = conn.prepareStatement(checkLoansSql)) {
                checkStmt.setInt(1, bookInternalId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt("loanCount") > 0) {
                        String message = "Không thể xóa sách (ISBN: " + isbn13 + ", InternalID: " + bookInternalId + ") vì đang có " + rs.getInt("loanCount") + " lượt mượn chưa trả.";
                        System.err.println("ERROR_BMS_DELETE: " + message);
                        throw new DeletionRestrictedException(message);
                    }
                }
            }

            // Bước 2: Nếu không có lượt mượn, tiến hành xóa sách bằng internalId (an toàn hơn)
            // Liên kết trong BookAuthors sẽ tự xóa do ON DELETE CASCADE đối với bookInternalId
            String deleteBookSql = "DELETE FROM Books WHERE internalId = ?";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteBookSql)) {
                deleteStmt.setInt(1, bookInternalId);
                int affectedRows = deleteStmt.executeUpdate();
                if (affectedRows > 0) {
                    System.out.println("DEBUG_BMS_DELETE: Book (InternalID: " + bookInternalId + ", ISBN: " + isbn13 + ") deleted from DB.");
                    return true;
                } else {
                    // Trường hợp này ít khi xảy ra nếu findInternalIdByIsbn13 đã tìm thấy sách
                    System.err.println("ERROR_BMS_DELETE: Book (InternalID: " + bookInternalId + ") not found for deletion (after check).");
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BMS_DELETE: DB error during deletion process for book (ISBN: " + isbn13 + "): " + e.getMessage());
            e.printStackTrace();
            if (e.getMessage().toLowerCase().contains("constraint") || e.getMessage().toLowerCase().contains("foreign key")) {
                throw new DeletionRestrictedException("Lỗi ràng buộc dữ liệu khi xóa sách. Sách có thể vẫn còn lượt mượn.");
            }
            // throw new RuntimeException("Database error during book deletion.", e);
            return false;
        }
    }

    public Optional<Integer> findInternalIdByIsbn13(String isbn13) {
        String sql = "SELECT internalId FROM Books WHERE isbn13 = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, isbn13);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getInt("internalId"));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BMS_FIND_INTERNAL_ID: DB error: " + e.getMessage());
        }
        return Optional.empty();
    }


    private Book mapResultSetToBook(Connection connForAuthors, ResultSet rsBook) throws SQLException {
        int internalId = rsBook.getInt("internalId");
        List<String> authorNames = getAuthorNamesForBook(connForAuthors, internalId);
        List<String> categories = rsBook.getString("categories") != null ?
                List.of(rsBook.getString("categories").split(";")) : new ArrayList<>();

        // Book.getId() (từ Document) sẽ là isbn13 nếu đó là tham chiếu chính.
        // Hoặc có thể là customDisplayId nếu muốn.
        // Schema DB của Books có isbn13 và customDisplayId.
        // Constructor Book của cậu nhận (id, title, authors, ...). id này sẽ là gì?
        // Tạm thời, id sẽ là isbn13.
        Book book = new Book(
                rsBook.getString("isbn13"), // <<<--- ID chính là ISBN13
                rsBook.getString("title"),
                authorNames,
                rsBook.getString("publisher"),
                rsBook.getString("publishedDate"),
                rsBook.getString("description"),
                categories,
                rsBook.getString("thumbnailUrl"),
                rsBook.getString("infoLink"),
                rsBook.getString("isbn10"),
                rsBook.getString("isbn13"),
                rsBook.getObject("pageCount") != null ? rsBook.getInt("pageCount") : null,
                rsBook.getObject("averageRating") != null ? rsBook.getDouble("averageRating") : null,
                rsBook.getObject("ratingsCount") != null ? rsBook.getInt("ratingsCount") : null,
                rsBook.getInt("totalQuantity")
        );
        book.setAvailableQuantity(rsBook.getInt("availableQuantity"));
        book.setShelfLocation(rsBook.getString("shelfLocation"));
        // Cậu có thể muốn set thêm customDisplayId vào một trường riêng của Book nếu cần
        // book.setCustomDisplayId(rsBook.getString("customDisplayId"));
        return book;
    }

    public Optional<Book> findBookByIsbn13InLibrary(String isbn13) {
        if (isbn13 == null || isbn13.trim().isEmpty()) return Optional.empty();
        String sqlBook = "SELECT * FROM Books WHERE isbn13 = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmtBook = conn.prepareStatement(sqlBook)) {
            pstmtBook.setString(1, isbn13);
            try (ResultSet rsBook = pstmtBook.executeQuery()) {
                if (rsBook.next()) {
                    return Optional.of(mapResultSetToBook(conn, rsBook));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BMS_FIND_ISBN13: DB error finding book by ISBN-13 " + isbn13 + ": " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    // Phương thức này có thể dùng để tìm sách bằng internalId nếu cần
    public Optional<Book> findBookByInternalId(int internalId) {
        String sqlBook = "SELECT * FROM Books WHERE internalId = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmtBook = conn.prepareStatement(sqlBook)) {
            pstmtBook.setInt(1, internalId);
            try (ResultSet rsBook = pstmtBook.executeQuery()) {
                if (rsBook.next()) {
                    return Optional.of(mapResultSetToBook(conn, rsBook));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BMS_FIND_INTERNAL_ID: DB error: " + e.getMessage());
        }
        return Optional.empty();
    }


    public List<Book> getAllBooksInLibrary() {
        List<Book> books = new ArrayList<>();
        // Tải tất cả internalId trước, sau đó load chi tiết từng sách (bao gồm tác giả)
        // Điều này vẫn là N+1 query cho tác giả, nhưng đơn giản hơn việc viết 1 câu JOIN phức tạp
        // để trả về nhiều dòng cho cùng 1 sách nếu có nhiều tác giả.
        // Một cách tối ưu hơn nữa là fetch tất cả sách, rồi fetch tất cả liên kết BookAuthors,
        // rồi fetch tất cả Authors cần thiết, sau đó ráp lại trong Java.
        String sqlAllBookIds = "SELECT internalId FROM Books ORDER BY title";
        List<Integer> bookInternalIds = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rsIds = stmt.executeQuery(sqlAllBookIds)) {
            while (rsIds.next()) {
                bookInternalIds.add(rsIds.getInt("internalId"));
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BMS_GET_ALL_IDS: DB error getting all book IDs: " + e.getMessage());
            e.printStackTrace();
            return books; // Trả về list rỗng nếu có lỗi
        }

        // Lấy chi tiết cho từng sách (bao gồm tác giả)
        for (int internalId : bookInternalIds) {
            findBookByInternalId(internalId).ifPresent(books::add);
        }
        return books;
    }

    public List<Book> searchBooksInLibrary(String keyword, String searchType) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllBooksInLibrary();
        }
        List<Book> books = new ArrayList<>();
        String lowerCaseKeyword = "%" + keyword.toLowerCase() + "%";

        // Sử dụng một CTE (Common Table Expression) để lấy internalId của các sách khớp điều kiện
        // sau đó join để lấy thông tin đầy đủ và tác giả.
        // Điều này giúp tránh N+1 query khi lấy thông tin tác giả.
        String sqlSearch = "WITH MatchedBookIds AS (" +
                "  SELECT DISTINCT b.internalId " +
                "  FROM Books b " +
                "  LEFT JOIN BookAuthors ba ON b.internalId = ba.bookInternalId " +
                "  LEFT JOIN Authors a ON ba.authorId = a.id " +
                "  WHERE ((? = 'ALL' OR ? = 'TITLE') AND LOWER(b.title) LIKE ?) " +
                "     OR ((? = 'ALL' OR ? = 'AUTHOR') AND LOWER(a.name) LIKE ?) " +
                "     OR ((? = 'ALL' OR ? = 'ISBN') AND (b.isbn10 LIKE ? OR b.isbn13 LIKE ?)) " +
                "     OR ((? = 'ALL' OR ? = 'PUBLISHER') AND LOWER(b.publisher) LIKE ?) " +
                ") " +
                "SELECT b.* FROM Books b JOIN MatchedBookIds mbi ON b.internalId = mbi.internalId " +
                "ORDER BY b.title";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlSearch)) {

            String type = searchType != null ? searchType.toUpperCase() : "ALL";

            pstmt.setString(1, type); pstmt.setString(2, type); pstmt.setString(3, lowerCaseKeyword); // Title
            pstmt.setString(4, type); pstmt.setString(5, type); pstmt.setString(6, lowerCaseKeyword); // Author
            pstmt.setString(7, type); pstmt.setString(8, type); pstmt.setString(9, lowerCaseKeyword.replace("%","")); pstmt.setString(10, lowerCaseKeyword.replace("%","")); // ISBN (exact match without % for isbn)
            pstmt.setString(11, type); pstmt.setString(12, type); pstmt.setString(13, lowerCaseKeyword); // Publisher

            try (ResultSet rsBook = pstmt.executeQuery()) {
                while (rsBook.next()) {
                    books.add(mapResultSetToBook(conn, rsBook)); // mapResultSetToBook sẽ lấy tác giả
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BMS_SEARCH: DB error searching books: " + e.getMessage());
            e.printStackTrace();
        }
        return books;
    }


    public boolean handleBookBorrowed(String isbn13) {
        if (isbn13 == null || isbn13.trim().isEmpty()) {
            System.err.println("ERROR_BMS_BORROW: ISBN-13 cannot be null.");
            return false;
        }
        String sql = "UPDATE Books SET availableQuantity = availableQuantity - 1 WHERE isbn13 = ? AND availableQuantity > 0";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, isbn13);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_BMS_BORROW: Book (ISBN-13: " + isbn13 + ") processed for borrowing in DB.");
                return true;
            } else {
                System.err.println("ERROR_BMS_BORROW: Book (ISBN-13: " + isbn13 + ") not available or not found for borrowing in DB.");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BMS_BORROW: DB error for book (ISBN-13: " + isbn13 + "): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean handleBookReturned(String isbn13) {
        if (isbn13 == null || isbn13.trim().isEmpty()) {
            System.err.println("ERROR_BMS_RETURN: ISBN-13 cannot be null.");
            return false;
        }
        // Đảm bảo availableQuantity không vượt quá totalQuantity
        String sql = "UPDATE Books SET availableQuantity = CASE " +
                "WHEN availableQuantity + 1 > totalQuantity THEN totalQuantity " +
                "ELSE availableQuantity + 1 END " +
                "WHERE isbn13 = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, isbn13);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_BMS_RETURN: Book (ISBN-13: " + isbn13 + ") processed for return in DB.");
                return true;
            } else {
                System.err.println("ERROR_BMS_RETURN: Book (ISBN-13: " + isbn13 + ") not found for returning in DB.");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BMS_RETURN: DB error for book (ISBN-13: " + isbn13 + "): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
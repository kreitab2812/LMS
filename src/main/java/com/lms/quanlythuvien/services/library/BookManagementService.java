package com.lms.quanlythuvien.services.library;

import com.lms.quanlythuvien.models.item.Author;
import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.utils.database.DatabaseManager;
import com.lms.quanlythuvien.exceptions.DeletionRestrictedException;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

public class BookManagementService {

    private static BookManagementService instance;

    private BookManagementService() {
        System.out.println("DEBUG_BMS_SINGLETON: BookManagementService Singleton instance created.");
    }

    public static synchronized BookManagementService getInstance() {
        if (instance == null) {
            instance = new BookManagementService();
        }
        return instance;
    }

    // =====================================================================================
    // SECTION: HELPER METHODS FOR AUTHORS (within book transactions)
    // =====================================================================================

    private Optional<Author> findAuthorByName(Connection conn, String name) throws SQLException {
        String sql = "SELECT id, name, biography, yearOfBirth, yearOfDeath, gender, nationality, placeOfBirth, avatarUrl, createdAt, updatedAt " +
                "FROM Authors WHERE LOWER(name) = LOWER(?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Author(
                            rs.getInt("id"), rs.getString("name"), rs.getString("biography"),
                            rs.getObject("yearOfBirth", Integer.class),
                            rs.getObject("yearOfDeath", Integer.class),
                            rs.getString("gender"), rs.getString("nationality"),
                            rs.getString("placeOfBirth"), rs.getString("avatarUrl"),
                            rs.getString("createdAt"), rs.getString("updatedAt")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    private Author getOrCreateAuthor(Connection conn, String authorNameInput) throws SQLException {
        String trimmedAuthorName = authorNameInput.trim();
        if (trimmedAuthorName.isEmpty()) {
            throw new SQLException("Author name cannot be empty when getting or creating.");
        }

        Optional<Author> existingAuthorOpt = findAuthorByName(conn, trimmedAuthorName);
        if (existingAuthorOpt.isPresent()) {
            return existingAuthorOpt.get();
        } else {
            Author newAuthor = new Author(trimmedAuthorName, null, null, null, null, null, null, null);
            String sqlInsertAuthor = "INSERT INTO Authors (name, biography, yearOfBirth, yearOfDeath, gender, nationality, placeOfBirth, avatarUrl, createdAt, updatedAt) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            try (PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsertAuthor, Statement.RETURN_GENERATED_KEYS)) {
                pstmtInsert.setString(1, newAuthor.getName());
                pstmtInsert.setString(2, newAuthor.getBiography());
                pstmtInsert.setObject(3, newAuthor.getYearOfBirth());
                pstmtInsert.setObject(4, newAuthor.getYearOfDeath());
                pstmtInsert.setString(5, newAuthor.getGender());
                pstmtInsert.setString(6, newAuthor.getNationality());
                pstmtInsert.setString(7, newAuthor.getPlaceOfBirth());
                pstmtInsert.setString(8, newAuthor.getAvatarUrl());
                pstmtInsert.setString(9, currentTime);
                pstmtInsert.setString(10, currentTime);

                pstmtInsert.executeUpdate();
                try (ResultSet generatedKeys = pstmtInsert.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        newAuthor.setId(generatedKeys.getInt(1));
                        newAuthor.setCreatedAt(currentTime);
                        newAuthor.setUpdatedAt(currentTime);
                        System.out.println("DEBUG_BMS_CREATE_AUTHOR: Created new author '" + newAuthor.getName() + "' with ID " + newAuthor.getId());
                        return newAuthor;
                    } else {
                        throw new SQLException("Creating author failed, no ID obtained for: " + trimmedAuthorName);
                    }
                }
            }
        }
    }

    private void linkBookToAuthors(Connection conn, int bookInternalId, List<String> authorNames) throws SQLException {
        if (authorNames == null || authorNames.isEmpty() || bookInternalId <= 0) {
            return;
        }
        String sqlLink = "INSERT INTO BookAuthors (bookInternalId, authorId) VALUES (?, ?)";
        Set<String> uniqueAuthorNames = new HashSet<>(authorNames);

        for (String authorName : uniqueAuthorNames) {
            if (authorName == null || authorName.trim().isEmpty()) continue;
            try {
                Author author = getOrCreateAuthor(conn, authorName.trim());
                try (PreparedStatement pstmtLink = conn.prepareStatement(sqlLink)) {
                    pstmtLink.setInt(1, bookInternalId);
                    pstmtLink.setInt(2, author.getId());
                    pstmtLink.executeUpdate();
                }
            } catch (SQLException e) {
                if (e.getMessage().toLowerCase().contains("unique constraint") || e.getMessage().toLowerCase().contains("duplicate key")) {
                    System.err.println("WARN_BMS_LINK_AUTHOR: Attempted to link already linked author '" + authorName + "' to book " + bookInternalId + ". Skipping.");
                } else {
                    System.err.println("ERROR_BMS_LINK_AUTHOR: Failed to link author '" + authorName + "' to book " + bookInternalId + ": " + e.getMessage());
                    throw e; // Ném lại lỗi để transaction cha có thể rollback nếu cần
                }
            }
        }
    }

    private void unlinkAllAuthorsFromBook(Connection conn, int bookInternalId) throws SQLException {
        String sqlDeleteLinks = "DELETE FROM BookAuthors WHERE bookInternalId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sqlDeleteLinks)) {
            pstmt.setInt(1, bookInternalId);
            int deletedCount = pstmt.executeUpdate();
            System.out.println("DEBUG_BMS_UNLINK_AUTHORS: Unlinked " + deletedCount + " authors from bookInternalId: " + bookInternalId);
        }
    }

    // =====================================================================================
    // SECTION: BOOK MAPPING
    // =====================================================================================

    private Book mapResultSetToBook(ResultSet rsBook, Map<Integer, List<String>> bookAuthorsMap) throws SQLException {
        int internalId = rsBook.getInt("internalId");
        List<String> authorNames = bookAuthorsMap.getOrDefault(internalId, new ArrayList<>());

        String categoriesString = rsBook.getString("categories");
        List<String> categoriesList = new ArrayList<>();
        if (categoriesString != null && !categoriesString.isEmpty()) {
            for (String cat : categoriesString.split("\\s*;\\s*")) { // Tách bằng dấu ; và khoảng trắng xung quanh
                if (!cat.trim().isEmpty()) {
                    categoriesList.add(cat.trim());
                }
            }
        }

        Book book = new Book(
                rsBook.getString("isbn13"), // ID chính từ Document
                rsBook.getString("title"),
                authorNames,
                rsBook.getString("publisher"),
                rsBook.getString("publishedDate"),
                rsBook.getString("description"),
                categoriesList,
                rsBook.getString("thumbnailUrl"),
                rsBook.getString("infoLink"),
                rsBook.getString("isbn10"),
                rsBook.getObject("pageCount", Integer.class), // An toàn hơn với getObject
                rsBook.getObject("averageRating", Double.class),
                rsBook.getObject("ratingsCount", Integer.class),
                rsBook.getInt("totalQuantity")
        );
        book.setInternalId(internalId); // ID tự tăng của DB
        book.setAvailableQuantity(rsBook.getInt("availableQuantity"));
        book.setShelfLocation(rsBook.getString("shelfLocation"));
        book.setQrCodeData(rsBook.getString("qrCodeData"));
        book.setCustomDisplayId(rsBook.getString("customDisplayId"));
        return book;
    }

    // Lớp BookProvisional và phương thức toBook vẫn giữ nguyên như bạn đã cung cấp
    private static class BookProvisional {
        int internalId; String isbn13; String title; String publisher; String publishedDate;
        String description; String categoriesString; String thumbnailUrl; String infoLink; String isbn10;
        Integer pageCount; Double averageRating; Integer ratingsCount; int totalQuantity;
        int availableQuantity; String shelfLocation; String qrCodeData; String customDisplayId;

        BookProvisional(ResultSet rs) throws SQLException {
            this.internalId = rs.getInt("internalId");
            this.isbn13 = rs.getString("isbn13");
            this.title = rs.getString("title");
            this.publisher = rs.getString("publisher");
            this.publishedDate = rs.getString("publishedDate");
            this.description = rs.getString("description");
            this.categoriesString = rs.getString("categories");
            this.thumbnailUrl = rs.getString("thumbnailUrl");
            this.infoLink = rs.getString("infoLink");
            this.isbn10 = rs.getString("isbn10");
            this.pageCount = rs.getObject("pageCount", Integer.class);
            this.averageRating = rs.getObject("averageRating", Double.class);
            this.ratingsCount = rs.getObject("ratingsCount", Integer.class);
            this.totalQuantity = rs.getInt("totalQuantity");
            this.availableQuantity = rs.getInt("availableQuantity");
            this.shelfLocation = rs.getString("shelfLocation");
            this.qrCodeData = rs.getString("qrCodeData");
            this.customDisplayId = rs.getString("customDisplayId");
        }

        Book toBook(Map<Integer, List<String>> bookAuthorsMap) {
            List<String> authorNames = bookAuthorsMap.getOrDefault(this.internalId, new ArrayList<>());
            List<String> categoriesList = new ArrayList<>();
            if (this.categoriesString != null && !this.categoriesString.isEmpty()) {
                for (String cat : this.categoriesString.split("\\s*;\\s*")) {
                    if (!cat.trim().isEmpty()) categoriesList.add(cat.trim());
                }
            }
            Book book = new Book(this.isbn13, this.title, authorNames, this.publisher, this.publishedDate,
                    this.description, categoriesList, this.thumbnailUrl, this.infoLink, this.isbn10,
                    this.pageCount, this.averageRating, this.ratingsCount, this.totalQuantity);
            book.setInternalId(this.internalId);
            book.setAvailableQuantity(this.availableQuantity);
            book.setShelfLocation(this.shelfLocation);
            book.setQrCodeData(this.qrCodeData);
            book.setCustomDisplayId(this.customDisplayId);
            return book;
        }
    }


    // =====================================================================================
    // SECTION: CRUD OPERATIONS FOR BOOKS
    // =====================================================================================

    public Optional<Book> addBookToLibrary(Book newBook) {
        // ... (Giữ nguyên logic của bạn, đảm bảo nó xử lý transaction đúng đắn)
        // Mã QR nên được tạo/cập nhật một cách nhất quán ở đây.
        if (newBook == null || newBook.getTitle() == null || newBook.getTitle().trim().isEmpty()) {
            System.err.println("ERROR_BMS_ADD: Book title cannot be null or empty.");
            return Optional.empty();
        }
        // Kiểm tra ISBN13 (ID chính) nếu có
        if (newBook.getId() != null && !newBook.getId().trim().isEmpty()) {
            if (findBookByIsbn13InLibrary(newBook.getId().trim()).isPresent()) {
                System.err.println("ERROR_BMS_ADD: Book with ISBN-13 (ID) '" + newBook.getId().trim() + "' already exists.");
                return Optional.empty();
            }
        }
        // (Có thể thêm kiểm tra customDisplayId nếu nó cũng phải là unique và không null)

        // Xử lý qrCodeData (đảm bảo không null/empty trước khi insert nếu cột DB yêu cầu)
        String qrData = newBook.getQrCodeData();
        if (qrData == null || qrData.trim().isEmpty()) {
            qrData = newBook.getId(); // Ưu tiên ID chính (ISBN13)
            if (qrData == null || qrData.trim().isEmpty()) {
                // Nếu ID chính cũng rỗng, sẽ gán bằng internalId sau khi insert
            }
        }
        newBook.setQrCodeData(qrData); // Gán lại cho đối tượng book


        String insertBookSQL = "INSERT INTO Books (isbn13, customDisplayId, title, publisher, publishedDate, description, pageCount, categories, averageRating, ratingsCount, thumbnailUrl, infoLink, isbn10, qrCodeData, totalQuantity, availableQuantity, shelfLocation, addedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        Connection conn = null;
        int generatedInternalId = -1;

        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmtBook = conn.prepareStatement(insertBookSQL, Statement.RETURN_GENERATED_KEYS)) {
                pstmtBook.setString(1, newBook.getId()); // ID chính (ISBN13 từ Document)
                pstmtBook.setString(2, newBook.getCustomDisplayId()); // customDisplayId từ Document (đã xử lý null nếu cần ở controller)
                pstmtBook.setString(3, newBook.getTitle().trim());
                pstmtBook.setString(4, newBook.getPublisher());
                pstmtBook.setString(5, newBook.getPublishedDate());
                pstmtBook.setString(6, newBook.getDescription());
                pstmtBook.setObject(7, newBook.getPageCount(), Types.INTEGER);
                pstmtBook.setString(8, newBook.getCategories() != null && !newBook.getCategories().isEmpty() ? String.join(";", newBook.getCategories()) : null);
                pstmtBook.setObject(9, newBook.getAverageRating() != null ? newBook.getAverageRating() : 0.0, Types.DOUBLE);
                pstmtBook.setObject(10, newBook.getRatingsCount() != null ? newBook.getRatingsCount() : 0, Types.INTEGER);
                pstmtBook.setString(11, newBook.getThumbnailUrl());
                pstmtBook.setString(12, newBook.getInfoLink());
                pstmtBook.setString(13, newBook.getIsbn10());
                pstmtBook.setString(14, newBook.getQrCodeData()); // qrCodeData đã được chuẩn bị
                pstmtBook.setInt(15, newBook.getTotalQuantity());
                pstmtBook.setInt(16, newBook.getTotalQuantity()); // Ban đầu available = total
                pstmtBook.setString(17, newBook.getShelfLocation());
                pstmtBook.setString(18, currentTime); // addedAt

                int affectedRows = pstmtBook.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating book failed, no rows affected for: " + newBook.getTitle());
                }

                try (ResultSet generatedKeys = pstmtBook.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        generatedInternalId = generatedKeys.getInt(1);
                        newBook.setInternalId(generatedInternalId);
                        newBook.setAvailableQuantity(newBook.getTotalQuantity()); // Đảm bảo available = total

                        // Nếu qrCodeData ban đầu rỗng (hoặc là ID chính mà ID chính cũng rỗng),
                        // hoặc nếu muốn QR code luôn dựa trên internalId, thì cập nhật lại.
                        if ((newBook.getQrCodeData() == null || newBook.getQrCodeData().trim().isEmpty() || newBook.getQrCodeData().equals(newBook.getId()))
                                && generatedInternalId > 0) {
                            String finalQrData = "BOOK_" + generatedInternalId; // Tạo QR data dựa trên internalId cho duy nhất
                            newBook.setQrCodeData(finalQrData); // Cập nhật đối tượng Book
                            // Cập nhật lại qrCodeData trong DB
                            String updateQrSql = "UPDATE Books SET qrCodeData = ? WHERE internalId = ?";
                            try (PreparedStatement pstmtUpdateQr = conn.prepareStatement(updateQrSql)) {
                                pstmtUpdateQr.setString(1, finalQrData);
                                pstmtUpdateQr.setInt(2, generatedInternalId);
                                pstmtUpdateQr.executeUpdate();
                                System.out.println("DEBUG_BMS_ADD: Updated QR Code for new book " + generatedInternalId + " to: " + finalQrData);
                            }
                        }
                    } else {
                        throw new SQLException("Creating book failed, no internal ID obtained for: " + newBook.getTitle());
                    }
                }
            }

            // Link sách với tác giả
            if (generatedInternalId > 0 && newBook.getAuthors() != null && !newBook.getAuthors().isEmpty()) {
                linkBookToAuthors(conn, generatedInternalId, newBook.getAuthors());
            }

            conn.commit();
            System.out.println("DEBUG_BMS_ADD: Book added: '" + newBook.getTitle() + "' (InternalID: " + generatedInternalId + ")");
            return Optional.of(newBook);

        } catch (SQLException e) {
            System.err.println("ERROR_BMS_ADD: DB error adding book '" + (newBook != null ? newBook.getTitle() : "null book object") + "': " + e.getMessage());
            // e.printStackTrace(); // Bật để debug nếu cần
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException exRollback) {
                    System.err.println("ERROR_BMS_ADD_ROLLBACK: Failed to rollback transaction: " + exRollback.getMessage());
                }
            }
            return Optional.empty();
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException exClose) {
                    System.err.println("ERROR_BMS_ADD_CLOSE: Failed to close connection: " + exClose.getMessage());
                }
            }
        }
    }

    public boolean updateBookInLibrary(Book updatedBookFromUI) {
        // ... (Giữ nguyên logic updateBookInLibrary của bạn, đảm bảo xử lý transaction)
        if (updatedBookFromUI == null || updatedBookFromUI.getInternalId() <= 0) {
            System.err.println("ERROR_BMS_UPDATE: Book to update or its Internal ID is invalid.");
            return false;
        }
        if (updatedBookFromUI.getTitle() == null || updatedBookFromUI.getTitle().trim().isEmpty()){
            System.err.println("ERROR_BMS_UPDATE: Book title cannot be empty for update.");
            return false;
        }

        Optional<Book> existingBookOpt = findBookByInternalId(updatedBookFromUI.getInternalId());
        if(existingBookOpt.isEmpty()){
            System.err.println("ERROR_BMS_UPDATE: Book with InternalID " + updatedBookFromUI.getInternalId() + " not found in DB.");
            return false;
        }
        Book existingBookInDB = existingBookOpt.get();

        // Kiểm tra nếu ISBN-13 (ID chính) thay đổi và có bị trùng không
        String newPrimaryId = updatedBookFromUI.getId();
        if (newPrimaryId != null && !newPrimaryId.trim().isEmpty() && !newPrimaryId.equals(existingBookInDB.getId())) {
            Optional<Book> bookByNewIsbn = findBookByIsbn13InLibrary(newPrimaryId.trim());
            if (bookByNewIsbn.isPresent() && bookByNewIsbn.get().getInternalId() != updatedBookFromUI.getInternalId()) {
                System.err.println("ERROR_BMS_UPDATE: New ISBN-13/ID '" + newPrimaryId.trim() + "' is already taken by another book.");
                return false;
            }
        }
        // (Tương tự, có thể thêm kiểm tra unique cho customDisplayId nếu nó thay đổi)

        // Tính toán lại availableQuantity dựa trên thay đổi totalQuantity
        int newTotalQuantity = updatedBookFromUI.getTotalQuantity();
        int oldTotalQuantity = existingBookInDB.getTotalQuantity();
        int oldAvailableQuantity = existingBookInDB.getAvailableQuantity();
        int quantityDifference = newTotalQuantity - oldTotalQuantity;
        int newAvailableQuantity = oldAvailableQuantity + quantityDifference;
        newAvailableQuantity = Math.max(0, newAvailableQuantity); // Không được âm
        newAvailableQuantity = Math.min(newAvailableQuantity, newTotalQuantity); // Không vượt quá tổng mới
        updatedBookFromUI.setAvailableQuantity(newAvailableQuantity);

        // Xử lý qrCodeData (đảm bảo không null/empty trước khi update nếu cột DB yêu cầu, hoặc tạo mới nếu cần)
        String qrData = updatedBookFromUI.getQrCodeData();
        if (qrData == null || qrData.trim().isEmpty()){
            qrData = updatedBookFromUI.getId(); // ISBN13
            if (qrData == null || qrData.trim().isEmpty()) {
                qrData = updatedBookFromUI.getCustomDisplayId();
                if(qrData == null || qrData.trim().isEmpty()){
                    qrData = "BOOK_" + updatedBookFromUI.getInternalId(); // Fallback nhất quán
                }
            }
        }
        updatedBookFromUI.setQrCodeData(qrData);


        String updateBookSQL = "UPDATE Books SET isbn13=?, customDisplayId=?, title=?, publisher=?, publishedDate=?, description=?, pageCount=?, categories=?, averageRating=?, ratingsCount=?, thumbnailUrl=?, infoLink=?, isbn10=?, qrCodeData=?, totalQuantity=?, availableQuantity=?, shelfLocation=? WHERE internalId = ?";
        Connection conn = null;
        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmtBook = conn.prepareStatement(updateBookSQL)) {
                pstmtBook.setString(1, updatedBookFromUI.getId());
                pstmtBook.setString(2, updatedBookFromUI.getCustomDisplayId()); // Đã xử lý null ở controller
                pstmtBook.setString(3, updatedBookFromUI.getTitle().trim());
                pstmtBook.setString(4, updatedBookFromUI.getPublisher());
                pstmtBook.setString(5, updatedBookFromUI.getPublishedDate());
                pstmtBook.setString(6, updatedBookFromUI.getDescription());
                pstmtBook.setObject(7, updatedBookFromUI.getPageCount(), Types.INTEGER);
                pstmtBook.setString(8, updatedBookFromUI.getCategories() != null && !updatedBookFromUI.getCategories().isEmpty() ? String.join(";", updatedBookFromUI.getCategories()) : null);
                pstmtBook.setObject(9, updatedBookFromUI.getAverageRating() !=null ? updatedBookFromUI.getAverageRating() : existingBookInDB.getAverageRating(), Types.DOUBLE); // Giữ giá trị cũ nếu không có
                pstmtBook.setObject(10, updatedBookFromUI.getRatingsCount() !=null ? updatedBookFromUI.getRatingsCount() : existingBookInDB.getRatingsCount(), Types.INTEGER); // Giữ giá trị cũ
                pstmtBook.setString(11, updatedBookFromUI.getThumbnailUrl());
                pstmtBook.setString(12, updatedBookFromUI.getInfoLink());
                pstmtBook.setString(13, updatedBookFromUI.getIsbn10());
                pstmtBook.setString(14, updatedBookFromUI.getQrCodeData());
                pstmtBook.setInt(15, newTotalQuantity);
                pstmtBook.setInt(16, newAvailableQuantity);
                pstmtBook.setString(17, updatedBookFromUI.getShelfLocation());
                pstmtBook.setInt(18, updatedBookFromUI.getInternalId());

                int affectedRows = pstmtBook.executeUpdate();
                if (affectedRows == 0) {
                    // Không có dòng nào được cập nhật, có thể sách đã bị xóa hoặc ID sai
                    System.err.println("WARN_BMS_UPDATE: No book record updated for internalId: " + updatedBookFromUI.getInternalId());
                    conn.rollback();
                    return false;
                }
            }

            // Cập nhật liên kết tác giả: xóa cũ, thêm mới
            unlinkAllAuthorsFromBook(conn, updatedBookFromUI.getInternalId());
            if (updatedBookFromUI.getAuthors() != null && !updatedBookFromUI.getAuthors().isEmpty()){
                linkBookToAuthors(conn, updatedBookFromUI.getInternalId(), updatedBookFromUI.getAuthors());
            }

            conn.commit();
            System.out.println("DEBUG_BMS_UPDATE: Book updated successfully: " + updatedBookFromUI.getTitle());
            return true;
        } catch (SQLException e) {
            System.err.println("ERROR_BMS_UPDATE: DB error updating book '" + updatedBookFromUI.getTitle() + "': " + e.getMessage());
            // e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException exRollback) {
                    System.err.println("ERROR_BMS_UPDATE_ROLLBACK: Failed to rollback transaction: " + exRollback.getMessage());
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException exClose) {
                    System.err.println("ERROR_BMS_UPDATE_CLOSE: Failed to close connection: " + exClose.getMessage());
                }
            }
        }
    }

    public boolean deleteBookFromLibrary(String identifier) throws DeletionRestrictedException {
        // ... (Giữ nguyên code của bạn)
        if (identifier == null || identifier.trim().isEmpty()) {
            System.err.println("ERROR_BMS_DELETE: Identifier cannot be null or empty.");
            return false;
        }
        Optional<Book> bookOpt;
        try {
            int internalId = Integer.parseInt(identifier.trim());
            bookOpt = findBookByInternalId(internalId);
        } catch (NumberFormatException e) {
            bookOpt = findBookByIsbn13InLibrary(identifier.trim());
        }
        if (bookOpt.isEmpty()) {
            System.err.println("ERROR_BMS_DELETE: Book with identifier '" + identifier.trim() + "' not found.");
            return false;
        }
        Book bookToDelete = bookOpt.get();
        int bookInternalId = bookToDelete.getInternalId();
        String displayIdentifier = bookToDelete.getId() != null ? bookToDelete.getId() : String.valueOf(bookInternalId);

        Connection conn = null;
        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);
            String checkLoansSql = "SELECT COUNT(*) AS loanCount FROM BorrowingRecords WHERE bookInternalId = ? AND status IN ('ACTIVE', 'OVERDUE')";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkLoansSql)) {
                checkStmt.setInt(1, bookInternalId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt("loanCount") > 0) {
                        throw new DeletionRestrictedException("Không thể xóa sách (ID: " + displayIdentifier + ") vì đang có " + rs.getInt("loanCount") + " lượt mượn chưa trả.");
                    }
                }
            }
            unlinkAllAuthorsFromBook(conn, bookInternalId); // Xóa liên kết BookAuthors trước
            // (Thêm xóa các dữ liệu liên quan khác nếu có, ví dụ: BookReviews)
            String deleteReviewsSql = "DELETE FROM BookReviews WHERE bookInternalId = ?"; // Giả sử bạn có bảng này
            try (PreparedStatement ps = conn.prepareStatement(deleteReviewsSql)) { ps.setInt(1, bookInternalId); ps.executeUpdate(); }

            String deleteBookSql = "DELETE FROM Books WHERE internalId = ?";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteBookSql)) {
                deleteStmt.setInt(1, bookInternalId);
                int affectedRows = deleteStmt.executeUpdate();
                if (affectedRows > 0) {
                    conn.commit();
                    System.out.println("DEBUG_BMS_DELETE: Book deleted successfully: " + displayIdentifier);
                    return true;
                } else {
                    conn.rollback();
                    System.err.println("WARN_BMS_DELETE: Book " + displayIdentifier + " not found during deletion or no rows affected.");
                    return false;
                }
            }
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { System.err.println("ERROR_BMS_DELETE_ROLLBACK_EX_SQLEX: " + ex.getMessage()); }
            String sqlState = e.getSQLState();
            // Các mã lỗi SQLite cho foreign key constraint thường là '23000' hoặc chứa 'FOREIGN KEY constraint failed'
            if (sqlState != null && sqlState.startsWith("23") || (e.getMessage() != null && e.getMessage().toLowerCase().contains("foreign key constraint failed"))) {
                throw new DeletionRestrictedException("Không thể xóa sách (ID: " + displayIdentifier + ") do còn dữ liệu liên quan trong hệ thống (ví dụ: lịch sử mượn, yêu cầu quyên góp). Lỗi DB: " + e.getMessage());
            } else if (e.getMessage() != null && e.getMessage().toLowerCase().contains("constraint")) {
                throw new DeletionRestrictedException("Không thể xóa sách (ID: " + displayIdentifier + ") do ràng buộc dữ liệu. Chi tiết: " + e.getMessage());
            }
            System.err.println("ERROR_BMS_DELETE_SQLEX: SQL error deleting book " + displayIdentifier + ": " + e.getMessage());
            return false;
        } catch (DeletionRestrictedException dre) { // Bắt lại DeletionRestrictedException đã ném
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { System.err.println("ERROR_BMS_DELETE_ROLLBACK_EX_DRE: " + ex.getMessage()); }
            throw dre; // Ném lại để controller có thể hiển thị thông báo
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { System.err.println("ERROR_BMS_DELETE_CLOSE: " + e.getMessage()); }
        }
    }


    // =====================================================================================
    // SECTION: BOOK RETRIEVAL METHODS (FINDERS)
    // =====================================================================================
    // Giữ nguyên các phương thức find, get, search của bạn. Đảm bảo chúng hoạt động hiệu quả.
    // Ví dụ: findInternalIdByIsbn13, getAuthorsForBooks, findBookByInternalId, ... getAllBooksInLibrary, ...
    // Chúng thường tự quản lý connection cho các thao tác đọc.

    public Optional<Integer> findInternalIdByIsbn13(String isbn13) {
        if (isbn13 == null || isbn13.trim().isEmpty()) return Optional.empty();
        String sql = "SELECT internalId FROM Books WHERE isbn13 = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, isbn13.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Optional.of(rs.getInt("internalId"));
            }
        } catch (SQLException e) { System.err.println("ERROR_BMS_FIND_INTERNAL_ID_BY_ISBN: " + e.getMessage()); }
        return Optional.empty();
    }

    private Map<Integer, List<String>> getAuthorsForBooks(Connection conn, Set<Integer> bookInternalIds) throws SQLException {
        Map<Integer, List<String>> bookAuthorsMap = new HashMap<>();
        if (bookInternalIds == null || bookInternalIds.isEmpty()) return bookAuthorsMap;
        String placeholders = bookInternalIds.stream().map(id -> "?").collect(Collectors.joining(","));
        // Sắp xếp tên tác giả để đảm bảo thứ tự nhất quán nếu cần
        String sql = "SELECT ba.bookInternalId, a.name FROM BookAuthors ba JOIN Authors a ON ba.authorId = a.id WHERE ba.bookInternalId IN (" + placeholders + ") ORDER BY ba.bookInternalId, a.name";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int i = 1;
            for (Integer bookId : bookInternalIds) pstmt.setInt(i++, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bookAuthorsMap.computeIfAbsent(rs.getInt("bookInternalId"), k -> new ArrayList<>()).add(rs.getString("name"));
                }
            }
        }
        return bookAuthorsMap;
    }

    public Optional<Book> findBookByInternalId(int internalId) {
        if (internalId <= 0) return Optional.empty();
        String sqlBook = "SELECT * FROM Books WHERE internalId = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection(); // Connection riêng cho việc đọc này
             PreparedStatement pstmtBook = conn.prepareStatement(sqlBook)) {
            pstmtBook.setInt(1, internalId);
            try (ResultSet rsBook = pstmtBook.executeQuery()) {
                if (rsBook.next()) {
                    // Lấy tác giả trong cùng connection để hiệu quả hơn
                    Map<Integer, List<String>> authorsMap = getAuthorsForBooks(conn, Set.of(internalId));
                    return Optional.of(mapResultSetToBook(rsBook, authorsMap));
                }
            }
        } catch (SQLException e) { System.err.println("ERROR_BMS_FIND_INTERNAL_ID: " + e.getMessage()); }
        return Optional.empty();
    }

    public Optional<Book> findBookByIsbn13InLibrary(String isbn13) {
        if (isbn13 == null || isbn13.trim().isEmpty()) return Optional.empty();
        String sqlBook = "SELECT * FROM Books WHERE isbn13 = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmtBook = conn.prepareStatement(sqlBook)) {
            pstmtBook.setString(1, isbn13.trim());
            try (ResultSet rsBook = pstmtBook.executeQuery()) {
                if (rsBook.next()) {
                    int internalId = rsBook.getInt("internalId");
                    Map<Integer, List<String>> authorsMap = getAuthorsForBooks(conn, Set.of(internalId));
                    return Optional.of(mapResultSetToBook(rsBook, authorsMap));
                }
            }
        } catch (SQLException e) { System.err.println("ERROR_BMS_FIND_ISBN13: " + e.getMessage()); }
        return Optional.empty();
    }

    public List<Book> getAllBooksInLibrary() {
        List<Book> books = new ArrayList<>();
        String sqlAllBooks = "SELECT * FROM Books ORDER BY title COLLATE NOCASE ASC";
        List<BookProvisional> provisionalBooks = new ArrayList<>();
        Set<Integer> bookInternalIds = new HashSet<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection(); // Mở connection một lần
             Statement stmt = conn.createStatement();
             ResultSet rsBooks = stmt.executeQuery(sqlAllBooks)) {
            while (rsBooks.next()) {
                int internalId = rsBooks.getInt("internalId");
                bookInternalIds.add(internalId);
                provisionalBooks.add(new BookProvisional(rsBooks));
            }
            if (provisionalBooks.isEmpty()) return books;

            Map<Integer, List<String>> bookAuthorsMap = getAuthorsForBooks(conn, bookInternalIds); // Dùng lại connection
            for (BookProvisional pb : provisionalBooks) {
                try {
                    books.add(pb.toBook(bookAuthorsMap));
                } catch (Exception e) {
                    System.err.println("ERROR_BMS_GET_ALL_MAP: Error mapping provisional book: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BMS_GET_ALL: " + e.getMessage());
        }
        return books;
    }

    public List<Book> searchBooksInLibrary(String keyword, String searchType) {
        // ... (Logic search của bạn, đảm bảo tối ưu N+1 nếu có thể)
        List<Book> books = new ArrayList<>();
        String queryKeyword = (keyword == null || keyword.trim().isEmpty()) ? "" : keyword.toLowerCase().trim();
        String isbnKeyword = (keyword == null || keyword.trim().isEmpty()) ? "" : keyword.trim(); // Giữ nguyên case cho ISBN
        String effectiveSearchType = (searchType == null || searchType.trim().isEmpty()) ? "ALL" : searchType.toUpperCase();

        StringBuilder sqlBuilder = new StringBuilder("SELECT DISTINCT b.* FROM Books b ");
        boolean requiresAuthorJoin = "AUTHOR".equals(effectiveSearchType) || ("ALL".equals(effectiveSearchType) && !queryKeyword.isEmpty());
        if (requiresAuthorJoin) {
            sqlBuilder.append("LEFT JOIN BookAuthors ba ON b.internalId = ba.bookInternalId ");
            sqlBuilder.append("LEFT JOIN Authors a ON ba.authorId = a.id ");
        }
        sqlBuilder.append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (!queryKeyword.isEmpty() || "ISBN".equals(effectiveSearchType) && !isbnKeyword.isEmpty()) {
            String likeKeyword = "%" + queryKeyword + "%";
            if ("ALL".equals(effectiveSearchType) && !queryKeyword.isEmpty()) {
                sqlBuilder.append("AND (LOWER(b.title) LIKE ? OR LOWER(a.name) LIKE ? OR LOWER(b.categories) LIKE ? OR LOWER(b.publisher) LIKE ? OR b.isbn13 = ? OR b.isbn10 = ?) ");
                params.addAll(List.of(likeKeyword, likeKeyword, likeKeyword, likeKeyword, isbnKeyword, isbnKeyword));
            } else if ("TITLE".equals(effectiveSearchType) && !queryKeyword.isEmpty()) {
                sqlBuilder.append("AND LOWER(b.title) LIKE ? "); params.add(likeKeyword);
            } else if ("AUTHOR".equals(effectiveSearchType) && !queryKeyword.isEmpty()) {
                sqlBuilder.append("AND LOWER(a.name) LIKE ? "); params.add(likeKeyword);
            } else if ("ISBN".equals(effectiveSearchType) && !isbnKeyword.isEmpty()) {
                sqlBuilder.append("AND (b.isbn13 = ? OR b.isbn10 = ?) "); params.add(isbnKeyword); params.add(isbnKeyword);
            } else if ("CATEGORY".equalsIgnoreCase(effectiveSearchType) && !queryKeyword.isEmpty()) {
                sqlBuilder.append("AND LOWER(b.categories) LIKE ? "); params.add(likeKeyword);
            } else if ("PUBLISHER".equals(effectiveSearchType) && !queryKeyword.isEmpty()) {
                sqlBuilder.append("AND LOWER(b.publisher) LIKE ? "); params.add(likeKeyword);
            }
        }
        sqlBuilder.append("ORDER BY b.title COLLATE NOCASE ASC");

        List<BookProvisional> provisionalBooks = new ArrayList<>();
        Set<Integer> bookInternalIds = new HashSet<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rsBooks = pstmt.executeQuery()) {
                while (rsBooks.next()) {
                    bookInternalIds.add(rsBooks.getInt("internalId"));
                    provisionalBooks.add(new BookProvisional(rsBooks));
                }
            }
            if (provisionalBooks.isEmpty()) return books;

            Map<Integer, List<String>> bookAuthorsMap = getAuthorsForBooks(conn, bookInternalIds);
            for (BookProvisional pb : provisionalBooks) {
                try {
                    books.add(pb.toBook(bookAuthorsMap));
                } catch (Exception e) {
                    System.err.println("ERROR_BMS_SEARCH_MAP: Error mapping provisional book during search: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BMS_SEARCH: " + e.getMessage());
        }
        return books;
    }

    public List<Book> getBooksByInternalIds(Set<Integer> internalIds) {
        // ... (Giữ nguyên code của bạn, đảm bảo N+1 được tối ưu)
        List<Book> books = new ArrayList<>();
        if (internalIds == null || internalIds.isEmpty()) return books;
        String placeholders = internalIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sqlBooks = "SELECT * FROM Books WHERE internalId IN (" + placeholders + ") ORDER BY title COLLATE NOCASE ASC";
        List<BookProvisional> provisionalBooks = new ArrayList<>();
        Set<Integer> foundBookInternalIds = new HashSet<>(); // Để chỉ lấy tác giả cho các sách tìm thấy

        try (Connection conn = DatabaseManager.getInstance().getConnection()) { // Mở connection một lần
            try (PreparedStatement pstmtBooks = conn.prepareStatement(sqlBooks)) {
                int i = 1;
                for (Integer bookId : internalIds) {
                    pstmtBooks.setInt(i++, bookId);
                }
                try (ResultSet rsBooks = pstmtBooks.executeQuery()) {
                    while (rsBooks.next()) {
                        foundBookInternalIds.add(rsBooks.getInt("internalId"));
                        provisionalBooks.add(new BookProvisional(rsBooks));
                    }
                }
            }

            if (provisionalBooks.isEmpty()) return books;

            Map<Integer, List<String>> bookAuthorsMap = getAuthorsForBooks(conn, foundBookInternalIds); // Dùng lại connection
            for (BookProvisional pb : provisionalBooks) {
                books.add(pb.toBook(bookAuthorsMap));
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BMS_GET_BY_INTERNAL_IDS: " + e.getMessage());
        }
        return books;
    }

    public List<Book> getBooksByAuthorId(int authorId) {
        List<Book> books = new ArrayList<>();
        String sqlBookInternalIds = "SELECT bookInternalId FROM BookAuthors WHERE authorId = ?";
        Set<Integer> bookIds = new HashSet<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection()) { // Mở connection một lần
            try (PreparedStatement pstmtIds = conn.prepareStatement(sqlBookInternalIds)) {
                pstmtIds.setInt(1, authorId);
                try (ResultSet rsIds = pstmtIds.executeQuery()) {
                    while (rsIds.next()) {
                        bookIds.add(rsIds.getInt("bookInternalId"));
                    }
                }
            }
            if (!bookIds.isEmpty()) {
                // Gọi getBooksByInternalIds, nhưng tốt hơn là truyền connection vào nếu có thể
                // Hiện tại getBooksByInternalIds sẽ mở connection mới.
                // Để tối ưu hoàn toàn, getBooksByInternalIds cũng nên nhận Connection
                return getBooksByInternalIds(bookIds);
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BMS_GET_BY_AUTHOR: " + e.getMessage());
        }
        return books;
    }

    public Map<String, Book> getBooksByIsbns(Set<String> isbns) {
        Map<String, Book> booksMap = new HashMap<>();
        if (isbns == null || isbns.isEmpty()) return booksMap;

        String placeholders = isbns.stream().map(isbn -> "?").collect(Collectors.joining(","));
        String sqlBooks = "SELECT * FROM Books WHERE isbn13 IN (" + placeholders + ")"; // Giả sử isbn13 là ID chính

        List<BookProvisional> provisionalBooks = new ArrayList<>();
        Set<Integer> foundBookInternalIds = new HashSet<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection()) { // Mở connection một lần
            try (PreparedStatement pstmtBooks = conn.prepareStatement(sqlBooks)) {
                int i = 1;
                for (String isbn : isbns) {
                    pstmtBooks.setString(i++, isbn);
                }
                try (ResultSet rsBooks = pstmtBooks.executeQuery()) {
                    while (rsBooks.next()) {
                        foundBookInternalIds.add(rsBooks.getInt("internalId"));
                        provisionalBooks.add(new BookProvisional(rsBooks));
                    }
                }
            }

            if (provisionalBooks.isEmpty()) return booksMap;

            Map<Integer, List<String>> bookAuthorsMap = getAuthorsForBooks(conn, foundBookInternalIds); // Dùng lại connection
            for (BookProvisional pb : provisionalBooks) {
                Book book = pb.toBook(bookAuthorsMap);
                if (book.getId() != null && !book.getId().isEmpty()) { // Dùng ID chính để làm key
                    booksMap.put(book.getId(), book);
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_BMS_GET_BY_ISBNS_DB: " + e.getMessage());
        }
        return booksMap;
    }


    // =====================================================================================
    // SECTION: UTILITY AND OTHER METHODS - ĐÂY LÀ PHẦN CHỈNH SỬA QUAN TRỌNG
    // =====================================================================================

    /**
     * Giảm số lượng sách có sẵn. Được gọi trong một transaction do service khác quản lý.
     * @param conn Connection đã được quản lý transaction từ bên ngoài.
     * @param bookInternalId ID nội bộ của sách.
     * @return true nếu thành công, false nếu thất bại (ví dụ: sách hết).
     * @throws SQLException nếu có lỗi DB nghiêm trọng.
     */
    public boolean handleBookBorrowedByInternalId(Connection conn, int bookInternalId) throws SQLException {
        String sql = "UPDATE Books SET availableQuantity = availableQuantity - 1 WHERE internalId = ? AND availableQuantity > 0";
        // Không dùng try-with-resources cho 'conn' ở đây vì nó được quản lý từ bên ngoài
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookInternalId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_BMS_BORROW_ID (conn_passed): Decreased available quantity for book internalId " + bookInternalId);
                return true;
            } else {
                // Lỗi logic: sách không có sẵn hoặc không tìm thấy, không phải lỗi SQL nghiêm trọng để ném Exception
                System.err.println("ERROR_BMS_BORROW_ID (conn_passed): Failed to decrease quantity or book not available/found for internalId " + bookInternalId);
                return false;
            }
        }
        // SQLException từ conn.prepareStatement(sql) hoặc pstmt.executeUpdate() sẽ tự động được ném ra
    }

    /**
     * Tăng số lượng sách có sẵn. Được gọi trong một transaction do service khác quản lý.
     * @param conn Connection đã được quản lý transaction từ bên ngoài.
     * @param bookInternalId ID nội bộ của sách.
     * @return true nếu thành công, false nếu thất bại.
     * @throws SQLException nếu có lỗi DB nghiêm trọng.
     */
    public boolean handleBookReturnedByInternalId(Connection conn, int bookInternalId) throws SQLException {
        String sql = "UPDATE Books SET availableQuantity = CASE " +
                "WHEN availableQuantity + 1 > totalQuantity THEN totalQuantity " +
                "ELSE availableQuantity + 1 END " +
                "WHERE internalId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookInternalId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_BMS_RETURN_ID (conn_passed): Increased/adjusted available quantity for book internalId " + bookInternalId);
                return true;
            } else {
                System.err.println("ERROR_BMS_RETURN_ID (conn_passed): Failed to increase quantity for internalId " + bookInternalId + ". Book might not exist.");
                return false; // Không có dòng nào được cập nhật
            }
        }
    }

    // Các phương thức sau (handleBookBorrowed và handleBookReturned theo ISBN)
    // sẽ tự quản lý connection riêng nếu được gọi độc lập.
    // Nếu bạn chỉ gọi chúng từ BorrowingRecordService (đã có internalId),
    // thì các phương thức này có thể không cần thiết hoặc cần được xem xét lại.
    public boolean handleBookBorrowed(String isbn13) {
        Optional<Integer> internalIdOpt = findInternalIdByIsbn13(isbn13);
        if (internalIdOpt.isEmpty()) return false;

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Phương thức này tự quản lý transaction của riêng nó nếu được gọi độc lập
            // conn.setAutoCommit(false); // Bỏ nếu chỉ là một lệnh UPDATE đơn giản
            boolean success = handleBookBorrowedByInternalId(conn, internalIdOpt.get());
            // if (success) conn.commit(); else conn.rollback(); // Bỏ nếu không dùng setAutoCommit(false)
            return success;
        } catch (SQLException e) {
            System.err.println("ERROR_BMS_BORROW_ISBN (standalone): " + e.getMessage());
            return false;
        }
    }

    public boolean handleBookReturned(String isbn13) {
        Optional<Integer> internalIdOpt = findInternalIdByIsbn13(isbn13);
        if (internalIdOpt.isEmpty()) return false;

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Tương tự như trên, quản lý transaction riêng
            boolean success = handleBookReturnedByInternalId(conn, internalIdOpt.get());
            return success;
        } catch (SQLException e) {
            System.err.println("ERROR_BMS_RETURN_ISBN (standalone): " + e.getMessage());
            return false;
        }
    }

    public List<String> getAllDistinctCategories() {
        // ... (Giữ nguyên code của bạn)
        Set<String> categoriesSet = new HashSet<>();
        String sql = "SELECT categories FROM Books WHERE categories IS NOT NULL AND categories != ''";
        try (Connection conn = DatabaseManager.getInstance().getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) { String categoryString = rs.getString("categories"); if (categoryString != null && !categoryString.isEmpty()) {
                for (String cat : categoryString.split("\\s*;\\s*")) if (!cat.trim().isEmpty()) categoriesSet.add(cat.trim());}}
        } catch (SQLException e) { System.err.println("ERROR_BMS_GET_CATEGORIES: " + e.getMessage()); }
        List<String> sortedCategories = new ArrayList<>(categoriesSet);
        sortedCategories.sort(String.CASE_INSENSITIVE_ORDER);
        return sortedCategories;
    }

    public int getBorrowedCountForBook(int bookInternalId) {
        // ... (Giữ nguyên code của bạn)
        String sql = "SELECT COUNT(*) FROM BorrowingRecords WHERE bookInternalId = ? AND status IN ('ACTIVE', 'OVERDUE')";
        try (Connection conn = DatabaseManager.getInstance().getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookInternalId);
            try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { System.err.println("ERROR_BMS_GET_BORROWED_COUNT: " + e.getMessage()); }
        return 0;
    }

    public Map<Integer, Integer> getBookCountsForMultipleAuthors(Set<Integer> authorIds) {
        // ... (Giữ nguyên code của bạn)
        Map<Integer, Integer> authorBookCounts = new HashMap<>();
        if (authorIds == null || authorIds.isEmpty()) return authorBookCounts;
        authorIds.forEach(id -> authorBookCounts.put(id, 0)); // Khởi tạo
        String placeholders = authorIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT authorId, COUNT(bookInternalId) as bookCount FROM BookAuthors WHERE authorId IN (" + placeholders + ") GROUP BY authorId";
        try (Connection conn = DatabaseManager.getInstance().getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int i = 1; for (Integer authorId : authorIds) pstmt.setInt(i++, authorId);
            try (ResultSet rs = pstmt.executeQuery()) { while (rs.next()) authorBookCounts.put(rs.getInt("authorId"), rs.getInt("bookCount"));}
        } catch (SQLException e) { System.err.println("ERROR_BMS_GET_BOOK_COUNTS_AUTHORS: " + e.getMessage());}
        return authorBookCounts;
    }
}
package com.lms.quanlythuvien.utils.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DATABASE_URL = "jdbc:sqlite:library.db"; // Tên file DB, sẽ nằm cùng thư mục chạy ứng dụng
    private static DatabaseManager instance;

    private DatabaseManager() {
        try {
            // Đăng ký driver SQLite, cần có thư viện sqlite-jdbc trong dependencies
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("CRITICAL_DB_LOAD_DRIVER: Failed to load SQLite JDBC driver. Ensure sqlite-jdbc.jar is in classpath/dependencies: " + e.getMessage());
            // Ném RuntimeException ở đây sẽ làm ứng dụng dừng lại nếu không load được driver, điều này hợp lý.
            throw new RuntimeException("Failed to load SQLite JDBC driver. Application cannot continue.", e);
        }
        initializeDatabase(); // Khởi tạo schema DB khi đối tượng được tạo
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        // Mỗi lần gọi sẽ tạo một kết nối mới. Điều này ổn với SQLite vì nó là file-based
        // và không có connection pool phức tạp như các DB server khác.
        return DriverManager.getConnection(DATABASE_URL);
    }

    private void initializeDatabase() {
        // Sử dụng try-with-resources cho Connection và Statement
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Bật hỗ trợ khóa ngoại cho SQLite (quan trọng!)
            stmt.execute("PRAGMA foreign_keys = ON;");

            // --- 1. Tạo bảng Users ---
            // Thêm các cột mới đã được định nghĩa trong User model (vd: fullName, reputationScore, ...)
            String createUserTableSQL = "CREATE TABLE IF NOT EXISTS Users (" +
                    "id TEXT PRIMARY KEY, " +                     // YYYYMM-NNNN
                    "username TEXT UNIQUE NOT NULL, " +
                    "passwordHash TEXT NOT NULL, " +
                    "email TEXT UNIQUE NOT NULL, " +
                    "role TEXT NOT NULL CHECK(role IN ('ADMIN', 'USER')), " + // Ràng buộc giá trị cho role
                    "fullName TEXT, " +
                    "dateOfBirth TEXT, " +                      // Lưu dạng ISO YYYY-MM-DD
                    "address TEXT, " +
                    "phoneNumber TEXT, " +
                    "avatarUrl TEXT, " +
                    "introduction TEXT, " +
                    "isAccountLocked INTEGER NOT NULL DEFAULT 0, " + // 0 for false, 1 for true
                    "currentFineAmount REAL NOT NULL DEFAULT 0.0, " +
                    "reputationScore INTEGER NOT NULL DEFAULT 80, " +
                    "createdAt TEXT NOT NULL, " +               // ISO DateTime
                    "updatedAt TEXT NOT NULL" +                // ISO DateTime
                    ");";
            stmt.execute(createUserTableSQL);
            System.out.println("INFO_DB_INIT: Users table (extended schema) created or already exists.");

            // --- 2. Tạo bảng Authors ---
            String createAuthorsTableSQL = "CREATE TABLE IF NOT EXISTS Authors (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL UNIQUE, " +
                    "biography TEXT, " +
                    "yearOfBirth INTEGER, " +
                    "yearOfDeath INTEGER, " +
                    "gender TEXT, " +
                    "nationality TEXT, " +
                    "placeOfBirth TEXT, " +
                    "avatarUrl TEXT, " +
                    "createdAt TEXT, " +
                    "updatedAt TEXT" +
                    ");";
            stmt.execute(createAuthorsTableSQL);
            System.out.println("INFO_DB_INIT: Authors table created or already exists.");

            // --- 3. Tạo bảng Books ---
            String createBooksTableSQL = "CREATE TABLE IF NOT EXISTS Books (" +
                    "internalId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "isbn13 TEXT UNIQUE, " +        // Có thể NULL nếu sách không có ISBN13 (sách cũ, tự nhập)
                    "customDisplayId TEXT UNIQUE, " +// ID tùy chỉnh để hiển thị (nếu có)
                    "title TEXT NOT NULL, " +
                    "publisher TEXT, " +
                    "publishedDate TEXT, " +
                    "description TEXT, " +
                    "pageCount INTEGER, " +
                    "categories TEXT, " +           // Lưu dạng "Thể loại 1;Thể loại 2"
                    "averageRating REAL DEFAULT 0.0, " +
                    "ratingsCount INTEGER DEFAULT 0, " +
                    "thumbnailUrl TEXT, " +
                    "infoLink TEXT, " +
                    "isbn10 TEXT, " +
                    "qrCodeData TEXT UNIQUE, " +    // Dữ liệu cho mã QR (có thể là ISBN13 hoặc internalId)
                    "totalQuantity INTEGER NOT NULL DEFAULT 0, " +
                    "availableQuantity INTEGER NOT NULL DEFAULT 0, " +
                    "shelfLocation TEXT, " +
                    "addedAt TEXT NOT NULL" +       // Thời điểm sách được thêm vào thư viện
                    ");";
            stmt.execute(createBooksTableSQL);
            System.out.println("INFO_DB_INIT: Books table created or already exists.");

            // --- 4. Tạo bảng BookAuthors (bảng nối sách và tác giả) ---
            String createBookAuthorsTableSQL = "CREATE TABLE IF NOT EXISTS BookAuthors (" +
                    "bookInternalId INTEGER NOT NULL, " +
                    "authorId INTEGER NOT NULL, " +
                    "PRIMARY KEY (bookInternalId, authorId), " +
                    "FOREIGN KEY (bookInternalId) REFERENCES Books(internalId) ON DELETE CASCADE, " + // Nếu xóa sách, xóa cả liên kết này
                    "FOREIGN KEY (authorId) REFERENCES Authors(id) ON DELETE RESTRICT" + // Không cho xóa tác giả nếu còn sách
                    ");";
            stmt.execute(createBookAuthorsTableSQL);
            System.out.println("INFO_DB_INIT: BookAuthors table created or already exists.");

            // --- 5. Tạo bảng BorrowingRecords (lịch sử mượn trả) ---
            String createBorrowingRecordsTableSQL = "CREATE TABLE IF NOT EXISTS BorrowingRecords (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " + // ID của bản ghi mượn
                    "bookInternalId INTEGER NOT NULL, " +
                    "userId TEXT NOT NULL, " +
                    "borrowDate TEXT NOT NULL, " +          // ISO Date
                    "dueDate TEXT NOT NULL, " +             // ISO Date
                    "returnDate TEXT, " +                   // ISO Date, nullable
                    "status TEXT NOT NULL CHECK(status IN ('ACTIVE', 'RETURNED', 'OVERDUE')), " + // Trạng thái mượn
                    "FOREIGN KEY (bookInternalId) REFERENCES Books(internalId) ON DELETE RESTRICT, " + // Không cho xóa sách nếu đang có trong lịch sử mượn (cân nhắc)
                    "FOREIGN KEY (userId) REFERENCES Users(id) ON DELETE RESTRICT" + // Không cho xóa user nếu đang có lịch sử mượn
                    ");";
            stmt.execute(createBorrowingRecordsTableSQL);
            System.out.println("INFO_DB_INIT: BorrowingRecords table created or already exists.");

            // --- 6. Tạo bảng BorrowingRequests (yêu cầu mượn sách) ---
            String createBorrowingRequestsTableSQL = "CREATE TABLE IF NOT EXISTS BorrowingRequests (" +
                    "requestId TEXT PRIMARY KEY, " +      // UUID
                    "userId TEXT NOT NULL, " +
                    "bookIsbn13 TEXT NOT NULL, " +        // Tham chiếu đến ISBN13 của sách (nếu sách có)
                    "requestDate TEXT NOT NULL, " +       // ISO Date
                    "status TEXT NOT NULL CHECK(status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELED_BY_USER', 'COMPLETED', 'EXPIRED')), " +
                    "adminNotes TEXT, " +
                    "resolvedDate TEXT, " +               // ISO Date
                    "pickupDueDate TEXT, " +              // ISO Date
                    "FOREIGN KEY (userId) REFERENCES Users(id) ON DELETE CASCADE, " + // Nếu xóa user, xóa các request của họ
                    "FOREIGN KEY (bookIsbn13) REFERENCES Books(isbn13) ON DELETE SET NULL" + // Nếu sách bị xóa (ISBN13 đổi/null), request vẫn còn nhưng bookIsbn13 có thể là null
                    // Hoặc ON DELETE RESTRICT nếu không muốn xóa sách khi có request
                    ");";
            stmt.execute(createBorrowingRequestsTableSQL);
            System.out.println("INFO_DB_INIT: BorrowingRequests table created or already exists.");

            // --- 7. Tạo bảng Notifications ---
            String createNotificationsTableSQL = "CREATE TABLE IF NOT EXISTS Notifications (" +
                    "id TEXT PRIMARY KEY, " +             // UUID
                    "userId TEXT NOT NULL, " +
                    "message TEXT NOT NULL, " +
                    "type TEXT NOT NULL, " +              // Tên của Enum NotificationType
                    "isRead INTEGER NOT NULL DEFAULT 0, " + // 0 for false, 1 for true
                    "createdAt TEXT NOT NULL, " +         // ISO DateTime
                    "relatedItemId TEXT, " +              // ID của đối tượng liên quan (sách, request,...)
                    "actionLink TEXT, " +                 // Key để điều hướng trong app
                    "FOREIGN KEY (userId) REFERENCES Users(id) ON DELETE CASCADE" + // Nếu xóa user, xóa thông báo của họ
                    ");";
            stmt.execute(createNotificationsTableSQL);
            System.out.println("INFO_DB_INIT: Notifications table created or already exists.");

            // --- 8. Tạo bảng DonationRequests ---
            String createDonationRequestsTableSQL = "CREATE TABLE IF NOT EXISTS DonationRequests (" +
                    "requestId TEXT PRIMARY KEY, " +      // UUID
                    "userId TEXT NOT NULL, " +
                    "bookName TEXT NOT NULL, " +
                    "authorName TEXT, " +                // Tên tác giả (user tự nhập)
                    "category TEXT, " +
                    "language TEXT, " +
                    "reasonForContribution TEXT, " +
                    "requestDate TEXT NOT NULL, " +       // ISO Date
                    "status TEXT NOT NULL, " +            // Tên của Enum DonationStatus
                    "adminNotes TEXT, " +
                    "resolvedDate TEXT, " +               // ISO Date
                    "actualReceiptDate TEXT, " +          // ISO Date
                    "FOREIGN KEY (userId) REFERENCES Users(id) ON DELETE CASCADE" +
                    ");";
            stmt.execute(createDonationRequestsTableSQL);
            System.out.println("INFO_DB_INIT: DonationRequests table created or already exists.");

            // --- 9. Tạo bảng UserQuestions ---
            String createUserQuestionsTableSQL = "CREATE TABLE IF NOT EXISTS UserQuestions (" +
                    "id TEXT PRIMARY KEY, " +
                    "userId TEXT NOT NULL, " +
                    "questionText TEXT NOT NULL, " +
                    "questionDate TEXT NOT NULL, " +        // ISO DateTime
                    "answerText TEXT, " +
                    "answeredByAdminId TEXT, " +          // ID của Admin đã trả lời
                    "answerDate TEXT, " +                 // ISO DateTime
                    "status TEXT NOT NULL, " +            // Tên của Enum QuestionStatus
                    "isPublic INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY (userId) REFERENCES Users(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (answeredByAdminId) REFERENCES Users(id) ON DELETE SET NULL" + // Nếu admin bị xóa, giữ lại câu trả lời nhưng không rõ ai trả lời
                    ");";
            stmt.execute(createUserQuestionsTableSQL);
            System.out.println("INFO_DB_INIT: UserQuestions table created or already exists.");

            // --- 10. Tạo bảng UserFavoriteBooks ---
            String createUserFavoriteBooksTableSQL = "CREATE TABLE IF NOT EXISTS UserFavoriteBooks (" +
                    "userId TEXT NOT NULL, " +
                    "bookInternalId INTEGER NOT NULL, " +
                    "favoritedAt TEXT NOT NULL, " +       // ISO DateTime
                    "PRIMARY KEY (userId, bookInternalId), " +
                    "FOREIGN KEY (userId) REFERENCES Users(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (bookInternalId) REFERENCES Books(internalId) ON DELETE CASCADE" +
                    ");";
            stmt.execute(createUserFavoriteBooksTableSQL);
            System.out.println("INFO_DB_INIT: UserFavoriteBooks table created or already exists.");

            // --- 11. Tạo bảng BookReviews ---
            String createBookReviewsTableSQL = "CREATE TABLE IF NOT EXISTS BookReviews (" +
                    "reviewId TEXT PRIMARY KEY, " +       // UUID
                    "bookInternalId INTEGER NOT NULL, " +
                    "userId TEXT NOT NULL, " +
                    "rating INTEGER CHECK(rating >= 0 AND rating <= 5), " + // 0 nếu chỉ comment, 1-5 nếu có rating
                    "commentText TEXT NOT NULL, " +
                    "reviewDate TEXT NOT NULL, " +        // ISO DateTime
                    "FOREIGN KEY (bookInternalId) REFERENCES Books(internalId) ON DELETE CASCADE, " +
                    "FOREIGN KEY (userId) REFERENCES Users(id) ON DELETE CASCADE" +
                    ");";
            stmt.execute(createBookReviewsTableSQL);
            System.out.println("INFO_DB_INIT: BookReviews table created or already exists.");

            // (Thêm các chỉ mục (INDEX) nếu cần để tăng tốc độ truy vấn cho các cột thường xuyên được tìm kiếm)
            // Ví dụ:
            // stmt.execute("CREATE INDEX IF NOT EXISTS idx_books_title ON Books(LOWER(title));");
            // stmt.execute("CREATE INDEX IF NOT EXISTS idx_authors_name ON Authors(LOWER(name));");
            // stmt.execute("CREATE INDEX IF NOT EXISTS idx_borrowingrecords_userid ON BorrowingRecords(userId);");
            // stmt.execute("CREATE INDEX IF NOT EXISTS idx_borrowingrecords_bookid ON BorrowingRecords(bookInternalId);");
            // stmt.execute("CREATE INDEX IF NOT EXISTS idx_notifications_userid_isread ON Notifications(userId, isRead);");

            System.out.println("INFO_DB_INIT: Database schema initialization complete.");

        } catch (SQLException e) {
            System.err.println("CRITICAL_DB_INIT_SCHEMA: Error initializing database schema: " + e.getMessage());
            // e.printStackTrace(); // Bật nếu cần debug sâu
            // Ném RuntimeException để ứng dụng dừng lại nếu không khởi tạo được DB schema
            throw new RuntimeException("Failed to initialize database schema. Application cannot continue.", e);
        }
    }
}
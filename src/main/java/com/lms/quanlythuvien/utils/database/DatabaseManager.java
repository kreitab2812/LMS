package com.lms.quanlythuvien.utils.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DATABASE_URL = "jdbc:sqlite:library.db";
    private static DatabaseManager instance;

    private DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("CRITICAL_DB_LOAD_DRIVER: Failed to load SQLite JDBC driver: " + e.getMessage());
            throw new RuntimeException("Failed to load SQLite JDBC driver. Application cannot continue.", e);
        }
        initializeDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL);
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");

            // --- 1. Tạo bảng Users ---
            String createUserTableSQL = "CREATE TABLE IF NOT EXISTS Users (" +
                    "id TEXT PRIMARY KEY, " +
                    "username TEXT UNIQUE NOT NULL, " +
                    "passwordHash TEXT NOT NULL, " +
                    "email TEXT UNIQUE NOT NULL, " +
                    "role TEXT NOT NULL, " +
                    "createdAt TEXT, " +
                    "updatedAt TEXT" +
                    ");";
            stmt.execute(createUserTableSQL);
            System.out.println("INFO_DB_INIT: Users table created or already exists.");

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
            System.out.println("INFO_DB_INIT: Authors table (extended schema) created or already exists.");

            // --- 3. Tạo bảng Books ---
            String createBooksTableSQL = "CREATE TABLE IF NOT EXISTS Books (" +
                    "internalId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "isbn13 TEXT UNIQUE, " +
                    "customDisplayId TEXT UNIQUE, " +
                    "title TEXT NOT NULL, " +
                    "publisher TEXT, " +
                    "publishedDate TEXT, " +
                    "description TEXT, " +
                    "pageCount INTEGER, " +
                    "categories TEXT, " +
                    "averageRating REAL, " +
                    "ratingsCount INTEGER, " +
                    "thumbnailUrl TEXT, " +
                    "infoLink TEXT, " +
                    "isbn10 TEXT, " +
                    "totalQuantity INTEGER NOT NULL DEFAULT 0, " +
                    "availableQuantity INTEGER NOT NULL DEFAULT 0, " +
                    "shelfLocation TEXT, " +
                    "addedAt TEXT" +
                    ");";
            stmt.execute(createBooksTableSQL);
            System.out.println("INFO_DB_INIT: Books table created or already exists.");

            // --- 4. Tạo bảng BookAuthors ---
            String createBookAuthorsTableSQL = "CREATE TABLE IF NOT EXISTS BookAuthors (" +
                    "bookInternalId INTEGER NOT NULL, " +
                    "authorId INTEGER NOT NULL, " +
                    "PRIMARY KEY (bookInternalId, authorId), " +
                    "FOREIGN KEY (bookInternalId) REFERENCES Books(internalId) ON DELETE CASCADE, " +
                    "FOREIGN KEY (authorId) REFERENCES Authors(id) ON DELETE RESTRICT" +
                    ");";
            stmt.execute(createBookAuthorsTableSQL);
            System.out.println("INFO_DB_INIT: BookAuthors table (authorId ON DELETE RESTRICT) created or already exists.");

            // --- 5. Tạo bảng BorrowingRecords ---
            String createBorrowingRecordsTableSQL = "CREATE TABLE IF NOT EXISTS BorrowingRecords (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "bookInternalId INTEGER NOT NULL, " +
                    "userId TEXT NOT NULL, " +
                    "borrowDate TEXT NOT NULL, " +
                    "dueDate TEXT NOT NULL, " +
                    "returnDate TEXT, " +
                    "status TEXT NOT NULL, " +
                    "FOREIGN KEY (bookInternalId) REFERENCES Books(internalId) ON DELETE RESTRICT, " +
                    "FOREIGN KEY (userId) REFERENCES Users(id) ON DELETE RESTRICT" +
                    ");";
            stmt.execute(createBorrowingRecordsTableSQL);
            System.out.println("INFO_DB_INIT: BorrowingRecords table (book/user ON DELETE RESTRICT) created or already exists.");

            // --- 6. Tạo bảng BorrowingRequests ---
            String createBorrowingRequestsTableSQL = "CREATE TABLE IF NOT EXISTS BorrowingRequests (" +
                    "requestId TEXT PRIMARY KEY, " +
                    "userId TEXT NOT NULL, " +
                    "bookIsbn13 TEXT NOT NULL, " +
                    "requestDate TEXT NOT NULL, " +
                    "status TEXT NOT NULL, " +
                    "adminNotes TEXT, " +
                    "resolvedDate TEXT, " +
                    "pickupDueDate TEXT, " +
                    "FOREIGN KEY (userId) REFERENCES Users(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (bookIsbn13) REFERENCES Books(isbn13) ON DELETE RESTRICT" +
                    ");";
            stmt.execute(createBorrowingRequestsTableSQL);
            System.out.println("INFO_DB_INIT: BorrowingRequests table created or already exists.");

            // --- 7. Tạo bảng Notifications ---
            String createNotificationsTableSQL = "CREATE TABLE IF NOT EXISTS Notifications (" +
                    "id TEXT PRIMARY KEY, " +
                    "userId TEXT NOT NULL, " +
                    "message TEXT NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "isRead INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt TEXT NOT NULL, " +
                    "relatedItemId TEXT, " +
                    "actionLink TEXT, " +
                    "FOREIGN KEY (userId) REFERENCES Users(id) ON DELETE CASCADE" +
                    ");";
            stmt.execute(createNotificationsTableSQL);
            System.out.println("INFO_DB_INIT: Notifications table created or already exists.");

            // --- 8. Tạo bảng DonationRequests ---
            String createDonationRequestsTableSQL = "CREATE TABLE IF NOT EXISTS DonationRequests (" + // Sửa tên biến cho rõ ràng (dù không bắt buộc)
                    "requestId TEXT PRIMARY KEY, " +
                    "userId TEXT NOT NULL, " +
                    "bookName TEXT NOT NULL, " +
                    "authorName TEXT NOT NULL, " +
                    "category TEXT, " +
                    "language TEXT, " +
                    "reasonForContribution TEXT, " +
                    "requestDate TEXT NOT NULL, " +
                    "status TEXT NOT NULL, " +
                    "adminNotes TEXT, " +
                    "resolvedDate TEXT, " +
                    "actualReceiptDate TEXT, " +
                    "FOREIGN KEY (userId) REFERENCES Users(id) ON DELETE CASCADE" +
                    ");";
            stmt.execute(createDonationRequestsTableSQL);
            System.out.println("INFO_DB_INIT: DonationRequests table created or already exists.");

            // --- 9. Tạo bảng UserQuestions (ĐÃ DI CHUYỂN VÀO TRONG TRY-CATCH) ---
            String createUserQuestionsTableSQL = "CREATE TABLE IF NOT EXISTS UserQuestions (" +
                    "id TEXT PRIMARY KEY, " +                   // UUID
                    "userId TEXT NOT NULL, " +                // FK Users.id
                    "questionText TEXT NOT NULL, " +
                    "questionDate TEXT NOT NULL, " +            // ISO8601 DateTime
                    "answerText TEXT, " +
                    "answeredByAdminId TEXT, " +              // FK Users.id (của Admin)
                    "answerDate TEXT, " +                     // ISO8601 DateTime
                    "status TEXT NOT NULL, " +                // PENDING_REVIEW, ANSWERED, PUBLISHED_AS_FAQ, REJECTED
                    "isPublic INTEGER NOT NULL DEFAULT 0, " + // 0 for false, 1 for true
                    "FOREIGN KEY (userId) REFERENCES Users(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (answeredByAdminId) REFERENCES Users(id) ON DELETE SET NULL" +
                    ");";
            stmt.execute(createUserQuestionsTableSQL); // <<--- DÒNG NÀY GIỜ NẰM TRONG TRY-CATCH
            System.out.println("INFO_DB_INIT: UserQuestions table created or already exists.");


            System.out.println("INFO_DB_INIT: Database schema initialization complete.");

        } catch (SQLException e) {
            System.err.println("CRITICAL_DB_INIT_SCHEMA: Error initializing database schema: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize database schema. Application cannot continue.", e);
        }
    }
}
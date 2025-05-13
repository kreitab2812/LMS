package com.lms.quanlythuvien.utils; // Hoặc package phù hợp của cậu

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    // Tên file database, sẽ được tạo trong thư mục gốc của project nếu chưa có
    private static final String DATABASE_URL = "jdbc:sqlite:library_management.db";

    // Phương thức để lấy kết nối đến database
    public static Connection getConnection() throws SQLException {
        try {
            // Đảm bảo driver đã được đăng ký (thường không cần với JDBC 4.0 trở lên nhưng để cho chắc)
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found.");
            e.printStackTrace();
            throw new SQLException("SQLite JDBC driver not found.", e);
        }
        return DriverManager.getConnection(DATABASE_URL);
    }

    // Phương thức khởi tạo tất cả các bảng nếu chúng chưa tồn tại
    public static void initializeDatabase() {
        System.out.println("DEBUG_DB: Initializing database...");
        createUsersTable();
        createBooksTable();
        createBorrowingRecordsTable();
        System.out.println("DEBUG_DB: Database initialization complete.");
    }

    private static void createUsersTable() {
        String sql = "CREATE TABLE IF NOT EXISTS Users (\n"
                + "    userId TEXT PRIMARY KEY NOT NULL,\n"
                + "    username TEXT NOT NULL UNIQUE,\n"
                + "    email TEXT NOT NULL UNIQUE,\n"
                + "    passwordHash TEXT NOT NULL,\n"
                + "    role TEXT NOT NULL CHECK(role IN ('ADMIN', 'USER'))\n"
                + ");";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("DEBUG_DB: Users table created or already exists.");
        } catch (SQLException e) {
            System.err.println("ERROR_DB: Error creating Users table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createBooksTable() {
        String sql = "CREATE TABLE IF NOT EXISTS Books (\n"
                + "    bookId TEXT PRIMARY KEY NOT NULL,\n"
                + "    title TEXT NOT NULL,\n"
                + "    authors TEXT,\n"
                + "    publisher TEXT,\n"
                + "    publishedDate TEXT,\n"
                + "    description TEXT,\n"
                + "    categories TEXT,\n"
                + "    thumbnailUrl TEXT,\n"
                + "    infoLink TEXT,\n"
                + "    isbn10 TEXT UNIQUE,\n"
                + "    isbn13 TEXT UNIQUE,\n"
                + "    pageCount INTEGER,\n"
                + "    totalQuantity INTEGER NOT NULL DEFAULT 0,\n"
                + "    availableQuantity INTEGER NOT NULL DEFAULT 0,\n"
                + "    shelfLocation TEXT,\n"
                + "    averageRating REAL DEFAULT 0.0,\n"
                + "    ratingsCount INTEGER DEFAULT 0\n"
                + ");";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("DEBUG_DB: Books table created or already exists.");
        } catch (SQLException e) {
            System.err.println("ERROR_DB: Error creating Books table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createBorrowingRecordsTable() {
        String sql = "CREATE TABLE IF NOT EXISTS BorrowingRecords (\n"
                + "    recordId TEXT PRIMARY KEY NOT NULL,\n"
                + "    bookId TEXT NOT NULL REFERENCES Books(bookId) ON DELETE CASCADE ON UPDATE CASCADE,\n" // Thêm ON DELETE/UPDATE CASCADE
                + "    userId TEXT NOT NULL REFERENCES Users(userId) ON DELETE CASCADE ON UPDATE CASCADE,\n" // Thêm ON DELETE/UPDATE CASCADE
                + "    borrowDate TEXT NOT NULL,\n"
                + "    dueDate TEXT NOT NULL,\n"
                + "    returnDate TEXT,\n"
                + "    status TEXT NOT NULL CHECK(status IN ('ACTIVE', 'RETURNED', 'OVERDUE'))\n"
                + ");";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("DEBUG_DB: BorrowingRecords table created or already exists.");
        } catch (SQLException e) {
            System.err.println("ERROR_DB: Error creating BorrowingRecords table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Cậu có thể gọi phương thức này một lần duy nhất khi ứng dụng khởi động,
    // ví dụ trong phương thức start() của lớp MainApp.
    public static void main(String[] args) {
        // Chỉ để test nhanh việc tạo database và bảng
        initializeDatabase();
    }
}
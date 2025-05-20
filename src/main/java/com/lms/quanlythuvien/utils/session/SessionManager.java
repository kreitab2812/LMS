// Trong file SessionManager.java
package com.lms.quanlythuvien.utils.session;

import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.item.Author; // <<<--- THÊM IMPORT CHO AUTHOR

public class SessionManager {

    private static SessionManager instance;
    private User currentUser;
    private String globalSearchQuery;
    private String globalSearchType;
    private Book selectedBook;
    private Author selectedAuthor; // <<<--- THÊM BIẾN NÀY ĐỂ LƯU TÁC GIẢ ĐƯỢC CHỌN

    private SessionManager() {
        System.out.println("DEBUG_SESSION: SessionManager Singleton instance created.");
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User userToSet) {
        this.currentUser = userToSet;
        if (userToSet != null) {
            System.out.println("DEBUG_SESSION: Current user set to: " + userToSet.getUsername() + " (Role: " + userToSet.getRole() + ")");
        } else {
            System.out.println("DEBUG_SESSION: Current user set to null (session cleared or no user).");
        }
    }

    public void clearSession() {
        this.currentUser = null;
        this.globalSearchQuery = null;
        this.globalSearchType = null;
        this.selectedBook = null; // Xóa cả selectedBook khi clear session
        this.selectedAuthor = null; // <<<--- XÓA CẢ selectedAuthor KHI CLEAR SESSION
        System.out.println("DEBUG_SESSION: Session cleared. All session data is now null.");
    }

    public boolean isLoggedIn() {
        return this.currentUser != null;
    }

    public String getGlobalSearchQuery() {
        return globalSearchQuery;
    }

    public void setGlobalSearchQuery(String globalSearchQuery) {
        this.globalSearchQuery = globalSearchQuery;
    }

    public String getGlobalSearchType() {
        return globalSearchType;
    }

    public void setGlobalSearchType(String globalSearchType) {
        this.globalSearchType = globalSearchType;
    }

    public Book getSelectedBook() {
        Book book = this.selectedBook;
        // Tùy chọn: Xóa selectedBook sau khi lấy để nó chỉ dùng một lần
        // this.selectedBook = null;
        return book;
    }

    public void setSelectedBook(Book selectedBook) {
        this.selectedBook = selectedBook;
    }

    // <<< --- CÁC PHƯƠNG THỨC MỚI CHO SELECTED AUTHOR --- >>>
    public Author getSelectedAuthor() {
        Author author = this.selectedAuthor;
        // Tùy chọn: Xóa selectedAuthor sau khi lấy để nó chỉ dùng một lần
        // this.selectedAuthor = null;
        return author;
    }

    public void setSelectedAuthor(Author selectedAuthor) {
        this.selectedAuthor = selectedAuthor;
        if (selectedAuthor != null) {
            System.out.println("DEBUG_SESSION: Selected author set to: " + selectedAuthor.getName());
        } else {
            System.out.println("DEBUG_SESSION: Selected author cleared.");
        }
    }
    // <<< ---------------------------------------------- >>>
}
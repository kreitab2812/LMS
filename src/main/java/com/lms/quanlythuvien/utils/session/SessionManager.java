package com.lms.quanlythuvien.utils.session;

import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.item.Author;

import java.util.HashMap;
import java.util.Map;

public class SessionManager {

    private static SessionManager instance;

    private User currentUser;
    private String globalSearchQuery;
    private String globalSearchType;
    private Book selectedBook;
    private Author selectedAuthor;

    // For navigating to a specific tab in MyBookshelf
    private String targetMyBookshelfTab;

    // Flags for Admin viewing details (if BookDetail/AuthorDetail controllers need different logic)
    private boolean isAdminViewingBookDetail = false;
    private boolean isAdminViewingAuthorDetail = false;

    // For passing a book to a history view (e.g., Admin viewing loan history of a specific book)
    private Book selectedBookForHistory = null;

    // For SuccessfulScreenController configuration
    private String successMessageLine1;
    private String successMessageLine2;
    private String successButtonText;
    private String successButtonNextScreen;

    // For LockedAccountController
    private User lockedUserAccountInfo;

    // (Optional) Controller registry
    private Map<String, Object> controllerRegistry = new HashMap<>();

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
            System.out.println("DEBUG_SESSION: Current user set to: " + userToSet.getUsername() +
                    " (Role: " + userToSet.getRole() + ")");
        } else {
            System.out.println("DEBUG_SESSION: Current user set to null (logged out or session cleared).");
        }
    }

    public boolean isLoggedIn() {
        return this.currentUser != null;
    }

    public void clearSession() {
        this.currentUser = null;
        this.globalSearchQuery = null;
        this.globalSearchType = null;
        this.selectedBook = null;
        this.selectedAuthor = null;
        this.targetMyBookshelfTab = null;
        this.isAdminViewingBookDetail = false;
        this.isAdminViewingAuthorDetail = false;
        this.selectedBookForHistory = null;
        this.controllerRegistry.clear();
        clearSuccessScreenData(); // Clear success screen data
        this.lockedUserAccountInfo = null; // Clear locked user info
        System.out.println("DEBUG_SESSION: Session cleared. All session data reset.");
    }

    // --- Global Search Data ---
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

    // --- Selected Item for Detail Views ---
    public Book getSelectedBook() {
        // Consider if this should be cleared after getting:
        // Book bookToReturn = this.selectedBook;
        // this.selectedBook = null; // Clears after one get
        // return bookToReturn;
        return selectedBook;
    }

    public void setSelectedBook(Book selectedBook) {
        this.selectedBook = selectedBook;
        if (selectedBook != null) {
            System.out.println("DEBUG_SESSION: Selected book set to: " + selectedBook.getTitleOrDefault("N/A"));
        } else {
            System.out.println("DEBUG_SESSION: Selected book cleared.");
        }
    }

    public Author getSelectedAuthor() {
        return selectedAuthor;
    }

    public void setSelectedAuthor(Author selectedAuthor) {
        this.selectedAuthor = selectedAuthor;
        if (selectedAuthor != null) {
            System.out.println("DEBUG_SESSION: Selected author set to: " + selectedAuthor.getName());
        } else {
            System.out.println("DEBUG_SESSION: Selected author cleared.");
        }
    }

    // --- MyBookshelf Tab Navigation ---
    public String getTargetMyBookshelfTab() {
        String tab = this.targetMyBookshelfTab;
        this.targetMyBookshelfTab = null; // Clear after use (one-time instruction)
        return tab;
    }

    public void setTargetMyBookshelfTab(String targetMyBookshelfTab) {
        this.targetMyBookshelfTab = targetMyBookshelfTab;
        System.out.println("DEBUG_SESSION: Target MyBookshelf tab set to: " + targetMyBookshelfTab);
    }

    // --- Admin Viewing Flags ---
    public boolean isAdminViewingBookDetail() {
        return isAdminViewingBookDetail;
    }

    public void setAdminViewingBookDetail(boolean isAdminView) {
        this.isAdminViewingBookDetail = isAdminView;
        System.out.println("DEBUG_SESSION: isAdminViewingBookDetail set to: " + isAdminView);
    }

    public boolean isAdminViewingAuthorDetail() {
        return isAdminViewingAuthorDetail;
    }

    public void setAdminViewingAuthorDetail(boolean isAdminView) {
        this.isAdminViewingAuthorDetail = isAdminView;
        System.out.println("DEBUG_SESSION: isAdminViewingAuthorDetail set to: " + isAdminView);
    }

    // --- Book for History View ---
    public Book getSelectedBookForHistory() {
        Book book = this.selectedBookForHistory;
        this.selectedBookForHistory = null; // Clear after use
        return book;
    }

    public void setSelectedBookForHistory(Book book) {
        this.selectedBookForHistory = book;
        System.out.println("DEBUG_SESSION: Selected book for history set: " +
                (book != null ? book.getTitleOrDefault("N/A") : "null"));
    }

    // --- SuccessfulScreen Configuration ---
    public void setSuccessScreenMessage(String line1, String line2) {
        this.successMessageLine1 = line1;
        this.successMessageLine2 = line2;
    }

    public void setSuccessScreenButton(String buttonText, String nextScreenFxmlPath) {
        this.successButtonText = buttonText;
        this.successButtonNextScreen = nextScreenFxmlPath;
    }

    public String getSuccessMessageLine1() { return successMessageLine1; }
    public String getSuccessMessageLine2() { return successMessageLine2; }
    public String getSuccessButtonText() { return successButtonText; }
    public String getSuccessButtonNextScreen() { return successButtonNextScreen; }

    public void clearSuccessScreenData() {
        this.successMessageLine1 = null;
        this.successMessageLine2 = null;
        this.successButtonText = null;
        this.successButtonNextScreen = null;
        System.out.println("DEBUG_SESSION: Success screen data cleared.");
    }

    // --- Locked Account Info ---
    public void setLockedUserAccountInfo(User user) {
        this.lockedUserAccountInfo = user;
        if (user != null) {
            System.out.println("DEBUG_SESSION: Locked user account info set for: " + user.getUsername());
        }
    }

    public User getLockedUserAccountInfo() {
        User tempUser = this.lockedUserAccountInfo;
        // Decide if you want to clear it after one get:
        // this.lockedUserAccountInfo = null;
        return tempUser;
    }

    // --- Controller Registry (Optional) ---
    public void registerController(String name, Object controller) {
        if (name != null && !name.isEmpty() && controller != null) {
            controllerRegistry.put(name.trim(), controller);
            System.out.println("DEBUG_SESSION: Controller registered: " + name);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getController(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        Object controller = controllerRegistry.get(name.trim());
        if (controller == null) {
            System.err.println("ERROR_SESSION: Controller '" + name + "' not found in registry.");
            return null;
        }
        try {
            return (T) controller;
        } catch (ClassCastException e) {
            System.err.println("ERROR_SESSION: Controller '" + name + "' found but is of unexpected type. Found: " +
                    controller.getClass().getName() + ", Error: " + e.getMessage());
            return null;
        }
    }
}
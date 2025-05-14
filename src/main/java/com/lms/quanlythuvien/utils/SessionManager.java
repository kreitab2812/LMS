package com.lms.quanlythuvien.utils; // Hoặc package phù hợp của cậu

import com.lms.quanlythuvien.models.User; // Đảm bảo import đúng lớp User model

public class SessionManager {

    private static SessionManager instance; // Biến static để giữ instance duy nhất
    private User currentUser; // User đang đăng nhập hiện tại

    // Constructor là private để không cho phép tạo đối tượng từ bên ngoài bằng từ khóa 'new'
    private SessionManager() {
        // Khởi tạo ban đầu (nếu cần)
        System.out.println("DEBUG_SESSION: SessionManager Singleton instance created.");
    }

    /**
     * Phương thức static để lấy instance duy nhất của SessionManager.
     * Đồng bộ hóa (synchronized) để đảm bảo an toàn trong môi trường đa luồng (mặc dù
     * với JavaFX client, điều này thường ít khi là vấn đề cấp bách cho SessionManager).
     *
     * @return instance duy nhất của SessionManager.
     */
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            System.out.println("DEBUG_SESSION: SessionManager getInstance() - Creating new instance.");
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Lấy thông tin người dùng đang đăng nhập hiện tại.
     *
     * @return Đối tượng User đang đăng nhập, hoặc null nếu chưa có ai đăng nhập.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Thiết lập người dùng đang đăng nhập hiện tại.
     * Thường được gọi sau khi đăng nhập thành công.
     *
     * @param userToSet Đối tượng User vừa đăng nhập thành công.
     */
    public void setCurrentUser(User userToSet) {
        this.currentUser = userToSet;
        if (userToSet != null) {
            System.out.println("DEBUG_SESSION: Current user set to: " + userToSet.getUsername() + " (Role: " + userToSet.getRole() + ")");
        } else {
            System.out.println("DEBUG_SESSION: Current user set to null (session cleared or no user).");
        }
    }

    /**
     * Xóa thông tin người dùng đang đăng nhập (khi logout).
     */
    public void clearSession() {
        this.currentUser = null;
        System.out.println("DEBUG_SESSION: Session cleared. Current user is now null.");
    }

    /**
     * Kiểm tra xem có người dùng nào đang đăng nhập không.
     * @return true nếu có người dùng đang đăng nhập, false nếu không.
     */
    public boolean isLoggedIn() {
        return this.currentUser != null;
    }
}
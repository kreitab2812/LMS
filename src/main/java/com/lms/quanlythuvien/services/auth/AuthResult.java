package com.lms.quanlythuvien.services.auth; // Đảm bảo đúng package

import com.lms.quanlythuvien.models.user.User;

public record AuthResult(boolean isSuccess, User user, String errorMessage) {
    // Các phương thức isSuccess(), user(), errorMessage() sẽ tự động được tạo
    // Nếu bạn gọi getUser() thì phải là user() với record

    public static AuthResult success(User user) {
        return new AuthResult(true, user, null);
    }

    public static AuthResult failure(String errorMessage) {
        return new AuthResult(false, null, errorMessage);
    }
}
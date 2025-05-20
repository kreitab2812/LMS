package com.lms.quanlythuvien.services.auth;

import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.user.UserService;
import com.lms.quanlythuvien.utils.security.PasswordUtils;

import java.util.Optional;

public class AuthService {

    private final UserService userService;

    public AuthService(UserService userService) {
        System.out.println("DEBUG_AS_CONSTRUCTOR: AuthService constructor started (with UserService).");
        this.userService = userService;
        System.out.println("DEBUG_AS_CONSTRUCTOR: AuthService constructor finished.");
    }

    public AuthResult login(String email, String password) {
        System.out.println("DEBUG_AS_LOGIN: AuthService.login() - Method Started. Email: [" + email + "]");

        Optional<User> userOptional = userService.findUserByEmail(email);

        if (userOptional.isEmpty()) {
            System.out.println("DEBUG_AS_LOGIN: User not found for email: [" + email + "]");
            return AuthResult.failure("Email hoặc mật khẩu không chính xác. Vui lòng thử lại.");
        }

        User user = userOptional.get();
        System.out.println("DEBUG_AS_LOGIN: User found: [" + user.getEmail() + "]. Hash: [" + user.getPasswordHash() + "]");

        if (password == null || password.isEmpty()) {
            System.out.println("DEBUG_AS_LOGIN: Input password is null or empty.");
            return AuthResult.failure("Mật khẩu không được để trống.");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
            System.err.println("CRITICAL_AS_LOGIN: Stored password hash for user [" + user.getEmail() + "] is null or empty!");
            return AuthResult.failure("Lỗi cấu hình tài khoản người dùng. Vui lòng liên hệ hỗ trợ.");
        }

        try {
            boolean passwordsMatch = PasswordUtils.verifyPassword(password, user.getPasswordHash());
            System.out.println("DEBUG_AS_LOGIN: Password verification result for [" + user.getEmail() + "]: " + passwordsMatch);

            if (passwordsMatch) {
                System.out.println("DEBUG_AS_LOGIN: Password verification successful for " + user.getEmail());
                return AuthResult.success(user);
            } else {
                System.out.println("DEBUG_AS_LOGIN: Password verification FAILED for " + user.getEmail());
                return AuthResult.failure("Email hoặc mật khẩu không chính xác. Vui lòng thử lại.");
            }
        } catch (Exception e) {
            System.err.println("CRITICAL_AS_LOGIN: Exception during password verification for " + user.getEmail());
            e.printStackTrace();
            return AuthResult.failure("Đã xảy ra lỗi nội bộ trong quá trình xác thực. Vui lòng liên hệ hỗ trợ.");
        }
    }

    public AuthResult register(String username, String email, String rawPassword, User.Role role) {
        System.out.println("DEBUG_AS_REGISTER: Attempting to register user. Email: [" + email + "], Username: [" + username + "]");

        if (userService.isUsernameTaken(username)) {
            System.out.println("DEBUG_AS_REGISTER: Username [" + username + "] already taken.");
            return AuthResult.failure("Tên đăng nhập đã tồn tại. Vui lòng chọn tên khác.");
        }
        if (userService.isEmailTaken(email)) {
            System.out.println("DEBUG_AS_REGISTER: Email [" + email + "] already taken.");
            return AuthResult.failure("Email đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập.");
        }
        if (rawPassword == null || rawPassword.isEmpty()) {
            System.out.println("DEBUG_AS_REGISTER: Registration failed - Raw password is empty.");
            return AuthResult.failure("Mật khẩu không được để trống khi đăng ký.");
        }
        if (rawPassword.length() < 7) { // Thêm kiểm tra độ dài mật khẩu
            System.out.println("DEBUG_AS_REGISTER: Registration failed - Password too short.");
            return AuthResult.failure("Mật khẩu phải có ít nhất 7 ký tự.");
        }


        String hashedPassword;
        try {
            hashedPassword = PasswordUtils.hashPassword(rawPassword);
            System.out.println("DEBUG_AS_REGISTER: Password hashed for new user [" + email + "].");
        } catch (Exception e) {
            System.err.println("CRITICAL_AS_REGISTER: Exception while hashing password for new user [" + email + "]");
            e.printStackTrace();
            return AuthResult.failure("Lỗi xử lý mật khẩu trong quá trình đăng ký.");
        }

        if (hashedPassword == null || hashedPassword.isEmpty()){
            System.err.println("CRITICAL_AS_REGISTER: Hashed password is null or empty for new user [" + email + "]");
            return AuthResult.failure("Không thể bảo mật mật khẩu trong quá trình đăng ký.");
        }

        // Tạo đối tượng User với thông tin cơ bản.
        // ID và thời gian tạo/cập nhật sẽ được UserService.addUser() xử lý và trả về trong đối tượng User mới.
        User basicUserDetails = new User(username, email, hashedPassword, role);

        // SỬA LỖI Ở ĐÂY:
        Optional<User> addedUserOptional = userService.addUser(basicUserDetails);

        if (addedUserOptional.isPresent()) {
            User successfullyAddedUser = addedUserOptional.get(); // Lấy User object đã được thêm (có ID đúng)
            System.out.println("DEBUG_AS_REGISTER: New user registered via UserService: " +
                    successfullyAddedUser.getEmail() + " with ID " + successfullyAddedUser.getUserId() +
                    " and role " + successfullyAddedUser.getRole());
            return AuthResult.success(successfullyAddedUser); // Trả về User object đã được Service xử lý
        } else {
            System.err.println("ERROR_AS_REGISTER: Failed to add user via UserService for email: " + email);
            // Thông báo lỗi này có thể được cải thiện nếu UserService.addUser trả về lý do cụ thể hơn khi thất bại
            return AuthResult.failure("Không thể đăng ký người dùng do lỗi nội bộ hoặc xung đột dữ liệu. Vui lòng kiểm tra lại thông tin hoặc thử lại sau.");
        }
    }
}
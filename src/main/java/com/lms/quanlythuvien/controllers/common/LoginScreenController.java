package com.lms.quanlythuvien.controllers.common; // Giữ nguyên package của cậu

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.auth.AuthService;
import com.lms.quanlythuvien.services.auth.AuthResult;
import com.lms.quanlythuvien.services.user.UserService;
import com.lms.quanlythuvien.utils.session.SessionManager; // <<--- THÊM IMPORT NÀY (nếu chưa có)

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginScreenController implements Initializable {

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField visiblePasswordField;
    @FXML
    private ImageView togglePasswordVisibility;
    @FXML
    private Label errorLabel;

    private UserService userService; // Đã được khởi tạo trong initialize
    private AuthService authService; // Đã được khởi tạo trong initialize

    private Image eyeIcon;
    private Image eyeSlashIcon;
    private boolean isPasswordVisible = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("DEBUG_LSC_INIT: LoginScreenController initialize() started.");

        // Khởi tạo service một lần duy nhất
        if (userService == null) { // Chỉ tạo nếu chưa có (cho trường hợp FXML được load lại)
            userService = UserService.getInstance();
            System.out.println("DEBUG_LSC_INIT: UserService instantiated.");
        }
        if (authService == null) { // Chỉ tạo nếu chưa có
            authService = new AuthService(userService); // AuthService cần UserService
            System.out.println("DEBUG_LSC_INIT: AuthService instantiated (with UserService).");
        }

        // Tải icons
        try {
            String eyeIconPath = "/com/lms/quanlythuvien/images/eye_icon.png";
            String eyeSlashIconPath = "/com/lms/quanlythuvien/images/eye_slash_icon.png";

            try (InputStream eyeStream = getClass().getResourceAsStream(eyeIconPath);
                 InputStream eyeSlashStream = getClass().getResourceAsStream(eyeSlashIconPath)) {

                if (eyeStream != null) {
                    eyeIcon = new Image(eyeStream);
                } else {
                    System.err.println("ERROR_LSC_INIT: Failed to load eye_icon.png. Path: " + eyeIconPath);
                }

                if (eyeSlashStream != null) {
                    eyeSlashIcon = new Image(eyeSlashStream);
                } else {
                    System.err.println("ERROR_LSC_INIT: Failed to load eye_slash_icon.png. Path: " + eyeSlashIconPath);
                }
            } // Streams sẽ tự đóng ở đây

            if (togglePasswordVisibility != null && eyeSlashIcon != null && !eyeSlashIcon.isError()) {
                togglePasswordVisibility.setImage(eyeSlashIcon);
            } else {
                System.err.println("ERROR_LSC_INIT: togglePasswordVisibility ImageView or eyeSlashIcon is null/error.");
            }

        } catch (Exception e) {
            System.err.println("CRITICAL_LSC_INIT: Exception during eye icon loading:");
            e.printStackTrace();
        }

        setupPasswordFieldVisibility(false); // Mặc định ẩn mật khẩu
        bindPasswordFields();
        clearError();
        System.out.println("DEBUG_LSC_INIT: LoginScreenController initialize() finished.");
    }

    private void bindPasswordFields() {
        // Đồng bộ nội dung giữa hai trường mật khẩu
        visiblePasswordField.textProperty().addListener((obs, oldText, newText) -> {
            if (isPasswordVisible) {
                // Cập nhật trường ẩn một cách lặng lẽ, không gây vòng lặp sự kiện
                if (!passwordField.getText().equals(newText)) {
                    passwordField.setText(newText);
                }
            }
        });
        passwordField.textProperty().addListener((obs, oldText, newText) -> {
            if (!isPasswordVisible) {
                // Cập nhật trường hiện một cách lặng lẽ
                if (!visiblePasswordField.getText().equals(newText)) {
                    visiblePasswordField.setText(newText);
                }
            }
        });
    }

    private void setupPasswordFieldVisibility(boolean showPassword) {
        isPasswordVisible = showPassword;
        if (showPassword) {
            passwordField.setManaged(false);
            passwordField.setVisible(false);
            visiblePasswordField.setText(passwordField.getText()); // Đồng bộ text khi chuyển
            visiblePasswordField.setManaged(true);
            visiblePasswordField.setVisible(true);
            if (togglePasswordVisibility != null && eyeIcon != null && !eyeIcon.isError()) {
                togglePasswordVisibility.setImage(eyeIcon);
            }
        } else {
            visiblePasswordField.setManaged(false);
            visiblePasswordField.setVisible(false);
            passwordField.setText(visiblePasswordField.getText()); // Đồng bộ text khi chuyển
            passwordField.setManaged(true);
            passwordField.setVisible(true);
            if (togglePasswordVisibility != null && eyeSlashIcon != null && !eyeSlashIcon.isError()) {
                togglePasswordVisibility.setImage(eyeSlashIcon);
            }
        }
    }

    @FXML
    private void togglePasswordVisibilityClicked(MouseEvent event) {
        setupPasswordFieldVisibility(!isPasswordVisible); // Đảo trạng thái và cập nhật UI
        // Di chuyển con trỏ về cuối nếu cần
        if(isPasswordVisible) {
            visiblePasswordField.requestFocus();
            if (visiblePasswordField.getText() != null) visiblePasswordField.end();
        } else {
            passwordField.requestFocus();
            if (passwordField.getText() != null) passwordField.end();
        }
    }

    @FXML
    private void handleLoginButtonAction() {
        System.out.println("DEBUG_LSC_LOGIN: handleLoginButtonAction - Method Started.");
        String email = emailField.getText().trim();
        String password = passwordField.getText(); // Luôn lấy từ passwordField vì nó đã được đồng bộ
        clearError();

        if (email.isEmpty() && password.isEmpty()) {
            showError("Vui lòng nhập email và mật khẩu.");
            return;
        }
        if (email.isEmpty()) {
            showError("Email không được để trống.");
            return;
        }
        if (password.isEmpty()) {
            showError("Mật khẩu không được để trống.");
            return;
        }
        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) { // Kiểm tra định dạng email đơn giản
            showError("Định dạng email không hợp lệ.");
            return;
        }

        if (authService == null) {
            System.err.println("CRITICAL_LSC_LOGIN: authService is NULL!");
            showError("Lỗi hệ thống: Dịch vụ xác thực không khả dụng.");
            return;
        }

        AuthResult authResult = authService.login(email, password);

        if (authResult == null) {
            System.err.println("CRITICAL_LSC_LOGIN: authResult from authService.login() is NULL!");
            showError("Lỗi hệ thống nghiêm trọng trong quá trình đăng nhập.");
            return;
        }

        if (authResult.isSuccess()) {
            User loggedInUser = authResult.user();
            if (loggedInUser == null) {
                System.err.println("CRITICAL_LSC_LOGIN: Login reported success but user object is NULL!");
                showError("Đăng nhập thành công nhưng thiếu dữ liệu người dùng.");
                return;
            }

            // <<<<< --- ĐÂY LÀ THAY ĐỔI QUAN TRỌNG NHẤT --- >>>>>
            SessionManager.getInstance().setCurrentUser(loggedInUser);
            // <<<<< ------------------------------------------ >>>>>
            System.out.println("DEBUG_LSC_LOGIN: SessionManager.setCurrentUser called with user: " + loggedInUser.getUsername());


            System.out.println("DEBUG_LSC_LOGIN: Login successful! User: " + loggedInUser.getUsername() + ", Role: " + loggedInUser.getRole());
            System.out.println("DEBUG_LSC_LOGIN: Attempting to load scene for role: " + loggedInUser.getRole());

            if (loggedInUser.getRole() == User.Role.ADMIN) {
                MainApp.loadScene("admin/AdminDashboard.fxml");
            } else { // User.Role.USER
                MainApp.loadScene("user/UserDashboard.fxml");
            }
            System.out.println("DEBUG_LSC_LOGIN: MainApp.loadScene method called.");
        } else {
            System.out.println("DEBUG_LSC_LOGIN: Login failed. Displaying error: " + authResult.errorMessage());
            showError(authResult.errorMessage() != null ? authResult.errorMessage() : "Email hoặc mật khẩu không chính xác.");
        }
        System.out.println("DEBUG_LSC_LOGIN: handleLoginButtonAction - Method Finished.");
    }

    @FXML
    private void handleForgotPasswordLinkClick(MouseEvent event) {
        System.out.println("DEBUG_LSC: ForgotPasswordLink clicked.");
        MainApp.loadScene("common/ForgotPasswordScreen.fxml");
    }

    @FXML
    private void handleRegisterHereLinkClick(MouseEvent event) {
        System.out.println("DEBUG_LSC: RegisterHereLink clicked.");
        MainApp.loadScene("common/RegistrationScreen.fxml");
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        } else {
            System.err.println("ERROR_LSC_SHOW_ERROR: errorLabel is null! Cannot display message: " + message);
        }
    }

    private void clearError() {
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }
}
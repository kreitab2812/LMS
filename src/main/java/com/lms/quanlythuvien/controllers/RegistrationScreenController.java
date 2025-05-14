package com.lms.quanlythuvien.controllers; // Package của Controller

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.models.User;
import com.lms.quanlythuvien.services.AuthService;
import com.lms.quanlythuvien.services.AuthResult;
import com.lms.quanlythuvien.services.UserService; // <<<--- THÊM IMPORT NÀY

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
// import javafx.scene.control.Button; // Bỏ comment nếu cần
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

import java.io.InputStream; // Thêm import này nếu chưa có
import java.net.URL;
import java.util.ResourceBundle;

public class RegistrationScreenController implements Initializable {

    @FXML
    private TextField usernameField;
    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField visiblePasswordField;
    @FXML
    private ImageView togglePasswordVisibility;

    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private TextField visibleConfirmPasswordField;
    @FXML
    private ImageView toggleConfirmPasswordVisibility;

    @FXML
    private Label errorLabel;
    // @FXML private Button registerButton; // Không cần nếu chỉ dùng onAction trong FXML
    @FXML
    private Label loginNowLink;

    // Khai báo các Service
    private UserService userService; // KHAI BÁO UserService
    private AuthService authService;

    private Image eyeIcon;
    private Image eyeSlashIcon;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("DEBUG_RSC_INIT: RegistrationScreenController initialize() started.");
        // KHỞI TẠO UserService TRƯỚC
        userService = UserService.getInstance();
        System.out.println("DEBUG_RSC_INIT: UserService instantiated.");

        // SAU ĐÓ KHỞI TẠO AuthService VÀ TRUYỀN userService VÀO
        authService = new AuthService(userService);
        System.out.println("DEBUG_RSC_INIT: AuthService instantiated (with UserService).");

        // Tải icons
        try {
            String eyeIconPath = "/com/lms/quanlythuvien/images/eye_icon.png";
            String eyeSlashIconPath = "/com/lms/quanlythuvien/images/eye_slash_icon.png";

            System.out.println("DEBUG_RSC_INIT: Attempting to load eye icons...");
            InputStream eyeStream = getClass().getResourceAsStream(eyeIconPath);
            InputStream eyeSlashStream = getClass().getResourceAsStream(eyeSlashIconPath);

            if (eyeStream != null) {
                eyeIcon = new Image(eyeStream);
                System.out.println("DEBUG_RSC_INIT: eye_icon.png loaded successfully.");
            } else {
                System.err.println("ERROR_RSC_INIT: Failed to load eye_icon.png. Path or file incorrect: " + eyeIconPath);
            }

            if (eyeSlashStream != null) {
                eyeSlashIcon = new Image(eyeSlashStream);
                System.out.println("DEBUG_RSC_INIT: eye_slash_icon.png loaded successfully.");
            } else {
                System.err.println("ERROR_RSC_INIT: Failed to load eye_slash_icon.png. Path or file incorrect: " + eyeSlashIconPath);
            }

            // Chỉ gán ảnh nếu nó đã được tải thành công và không lỗi
            if (eyeSlashIcon != null && !eyeSlashIcon.isError()) {
                togglePasswordVisibility.setImage(eyeSlashIcon);
                toggleConfirmPasswordVisibility.setImage(eyeSlashIcon);
            } else {
                System.err.println("ERROR_RSC_INIT: Default eyeSlashIcon could not be set for toggles due to loading error or null.");
            }

        } catch (Exception e) {
            System.err.println("CRITICAL_RSC_INIT: Exception during eye icon loading in RegistrationScreenController:");
            e.printStackTrace();
        }

        // Thiết lập trạng thái ban đầu cho các trường mật khẩu
        setupFieldVisibility(passwordField, visiblePasswordField, false);
        setupFieldVisibility(confirmPasswordField, visibleConfirmPasswordField, false);

        // Bind giá trị giữa PasswordField và TextField tương ứng
        bindPasswordFields(passwordField, visiblePasswordField, () -> isPasswordVisible);
        bindPasswordFields(confirmPasswordField, visibleConfirmPasswordField, () -> isConfirmPasswordVisible);

        clearError(); // Xóa lỗi ban đầu (nếu có)
        System.out.println("DEBUG_RSC_INIT: RegistrationScreenController initialize() finished.");
    }

    // Helper để setup ẩn/hiện ban đầu cho một cặp PasswordField và TextField
    private void setupFieldVisibility(PasswordField pf, TextField tf, boolean isInitiallyVisible) {
        if (isInitiallyVisible) {
            pf.setManaged(false); pf.setVisible(false);
            tf.setManaged(true); tf.setVisible(true);
            // Nếu muốn TextField hiện text của PasswordField ngay từ đầu (thường không cần)
            // tf.setText(pf.getText());
        } else {
            pf.setManaged(true); pf.setVisible(true);
            tf.setManaged(false); tf.setVisible(false);
            // Nếu muốn PasswordField hiện text của TextField ngay từ đầu (thường không cần)
            // pf.setText(tf.getText());
        }
    }

    // Functional interface để kiểm tra trạng thái visible của một trường mật khẩu cụ thể
    @FunctionalInterface
    interface VisibilityCheck {
        boolean get();
    }

    // Bind giá trị hai chiều giữa PasswordField và TextField
    private void bindPasswordFields(PasswordField pf, TextField tf, VisibilityCheck visibilityCheck) {
        // Khi TextField (hiển thị) thay đổi, cập nhật PasswordField (ẩn)
        tf.textProperty().addListener((obs, oldText, newText) -> {
            if (visibilityCheck.get()) { // Chỉ cập nhật nếu TextField đang thực sự active (đang hiển thị mật khẩu)
                pf.setText(newText);
            }
        });
        // Khi PasswordField (ẩn) thay đổi, cập nhật TextField (hiển thị)
        // Điều này quan trọng để giữ giá trị đồng bộ nếu có cách nào đó PasswordField thay đổi mà không qua TextField
        pf.textProperty().addListener((obs, oldText, newText) -> {
            if (!visibilityCheck.get()) { // Chỉ cập nhật nếu PasswordField đang thực sự active
                tf.setText(newText);
            }
        });
    }

    @FXML
    private void toggleMainPasswordVisibilityClicked(MouseEvent event) {
        isPasswordVisible = !isPasswordVisible; // Đảo trạng thái
        updatePasswordView(passwordField, visiblePasswordField, togglePasswordVisibility, isPasswordVisible);
    }

    @FXML
    private void toggleConfirmPasswordVisibilityClicked(MouseEvent event) {
        isConfirmPasswordVisible = !isConfirmPasswordVisible; // Đảo trạng thái
        updatePasswordView(confirmPasswordField, visibleConfirmPasswordField, toggleConfirmPasswordVisibility, isConfirmPasswordVisible);
    }

    // Phương thức chung để cập nhật giao diện khi ẩn/hiện mật khẩu
    private void updatePasswordView(PasswordField pf, TextField tf, ImageView toggleIcon, boolean showPassword) {
        setupFieldVisibility(pf, tf, showPassword); // Gọi lại helper để đổi trạng thái managed/visible
        if (showPassword) {
            tf.setText(pf.getText()); // Đồng bộ text từ PasswordField sang TextField
            tf.requestFocus();        // Focus vào TextField
            if (tf.getText() != null) tf.end(); // Di chuyển con trỏ về cuối
            if (eyeIcon != null && !eyeIcon.isError()) toggleIcon.setImage(eyeIcon);
        } else {
            pf.setText(tf.getText()); // Đồng bộ text từ TextField sang PasswordField
            pf.requestFocus();        // Focus vào PasswordField
            if (pf.getText() != null) pf.end(); // Di chuyển con trỏ về cuối
            if (eyeSlashIcon != null && !eyeSlashIcon.isError()) toggleIcon.setImage(eyeSlashIcon);
        }
    }

    @FXML
    private void handleRegisterButtonAction() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        // Lấy mật khẩu từ trường đang hiển thị
        String password = isPasswordVisible ? visiblePasswordField.getText() : passwordField.getText();
        String confirmPassword = isConfirmPasswordVisible ? visibleConfirmPasswordField.getText() : confirmPasswordField.getText();

        clearError(); // Xóa lỗi cũ trước khi kiểm tra

        // Các bước kiểm tra đầu vào
        if (username.isEmpty()) { showError("Username cannot be empty."); return; }
        if (email.isEmpty()) { showError("Email cannot be empty."); return; }
        if (password.isEmpty()) { showError("Password cannot be empty."); return; }
        if (confirmPassword.isEmpty()) { showError("Confirm Password cannot be empty."); return; }
        if (!email.contains("@") || !email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) { // Kiểm tra email kỹ hơn một chút
            showError("Invalid email format."); return;
        }
        if (password.length() < 7) { // Hoặc quy tắc mật khẩu của cậu
            showError("Password must be at least 7 characters long."); return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match."); return;
        }

        // Gọi AuthService để đăng ký (mặc định vai trò là USER)
        // authService đã được khởi tạo với userService rồi
        AuthResult registrationResult = authService.register(username, email, password, User.Role.USER);

        if (registrationResult.isSuccess()) {
            System.out.println("Registration successful for: " + username);
            // TODO: Chuyển đến màn hình thông báo đăng ký thành công hoặc màn hình đăng nhập
            MainApp.loadScene("RegistrationSuccessfulScreen.fxml"); // Ví dụ
        } else {
            // Hiển thị lỗi từ AuthService (ví dụ: username/email đã tồn tại)
            showError(registrationResult.errorMessage());
        }
    }

    @FXML
    private void handleLoginNowLinkClick(MouseEvent event) {
        MainApp.loadScene("LoginScreen.fxml");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
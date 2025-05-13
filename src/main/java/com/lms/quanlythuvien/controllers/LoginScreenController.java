package com.lms.quanlythuvien.controllers; // Package của Controller

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.models.User;
import com.lms.quanlythuvien.services.AuthService;
import com.lms.quanlythuvien.services.AuthResult; // IMPORT CHO AUTHRESULT ĐÚNG VỊ TRÍ MỚI

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

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
    // @FXML private Button loginButton; // Không cần nếu chỉ dùng onAction trong FXML
    @FXML
    private Label forgotPasswordLink;
    @FXML
    private Label registerHereLink;

    private AuthService authService;

    private Image eyeIcon;
    private Image eyeSlashIcon;
    private boolean isPasswordVisible = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        authService = new AuthService(); // Khởi tạo AuthService

        // Tải icons
        try {
            // Đảm bảo đường dẫn đúng đến thư mục images trong resources của bạn
            String eyeIconPath = "/com/lms/quanlythuvien/images/eye_icon.png";
            String eyeSlashIconPath = "/com/lms/quanlythuvien/images/eye_slash_icon.png";

            eyeIcon = new Image(getClass().getResourceAsStream(eyeIconPath));
            eyeSlashIcon = new Image(getClass().getResourceAsStream(eyeSlashIconPath));

            if (eyeIcon.isError() || eyeSlashIcon.isError()) {
                System.err.println("LoginScreen: Error loading eye icons. Check paths.");
            } else {
                togglePasswordVisibility.setImage(eyeSlashIcon); // Mặc định là ẩn
            }
        } catch (Exception e) {
            System.err.println("LoginScreen: Exception loading eye icons: " + e.getMessage());
        }

        // Setup ban đầu cho trường mật khẩu
        setupPasswordFieldVisibility(false);
        bindPasswordFields();

        // Ẩn errorLabel ban đầu
        clearError();
    }

    private void bindPasswordFields() {
        visiblePasswordField.textProperty().addListener((obs, oldText, newText) -> {
            if (isPasswordVisible) passwordField.setText(newText);
        });
        passwordField.textProperty().addListener((obs, oldText, newText) -> {
            if (!isPasswordVisible) visiblePasswordField.setText(newText);
        });
    }

    private void setupPasswordFieldVisibility(boolean showPassword) {
        isPasswordVisible = showPassword;
        if (showPassword) {
            passwordField.setManaged(false);
            passwordField.setVisible(false);
            visiblePasswordField.setManaged(true);
            visiblePasswordField.setVisible(true);
            if (eyeIcon != null && !eyeIcon.isError()) togglePasswordVisibility.setImage(eyeIcon);
        } else {
            visiblePasswordField.setManaged(false);
            visiblePasswordField.setVisible(false);
            passwordField.setManaged(true);
            passwordField.setVisible(true);
            if (eyeSlashIcon != null && !eyeSlashIcon.isError()) togglePasswordVisibility.setImage(eyeSlashIcon);
        }
    }

    @FXML
    private void togglePasswordVisibilityClicked(MouseEvent event) { // Đổi tên phương thức này cho rõ ràng
        setupPasswordFieldVisibility(!isPasswordVisible); // Đảo ngược trạng thái hiện tại
        if(isPasswordVisible) {
            visiblePasswordField.setText(passwordField.getText());
            visiblePasswordField.requestFocus();
            visiblePasswordField.end(); // Di chuyển con trỏ về cuối
        } else {
            passwordField.setText(visiblePasswordField.getText());
            passwordField.requestFocus();
            passwordField.end();
        }
    }

    @FXML
    private void handleLoginButtonAction() {
        String email = emailField.getText().trim();
        String password = isPasswordVisible ? visiblePasswordField.getText() : passwordField.getText();

        clearError();

        if (email.isEmpty() && password.isEmpty()) {
            showError("Email and Password are required.");
            return;
        }
        if (email.isEmpty()) {
            showError("Email cannot be empty.");
            return;
        }
        if (password.isEmpty()) {
            showError("Password cannot be empty.");
            return;
        }
        if (!email.contains("@")) { // Kiểm tra email đơn giản
            showError("Invalid email format. Please include '@'.");
            return;
        }

        AuthResult authResult = authService.login(email, password); // Sử dụng AuthResult ở đây

        if (authResult.isSuccess()) { // Sử dụng isSuccess()
            User loggedInUser = authResult.user(); // SỬA THÀNH .user()
            System.out.println("Login successful! User: " + loggedInUser.getUsername() + ", Role: " + loggedInUser.getRole());
            // TODO: Chuyển sang màn hình tương ứng với vai trò
            if (loggedInUser.getRole() == User.Role.ADMIN) {
                MainApp.loadScene("AdminDashboard.fxml"); // Tên FXML ví dụ
            } else {
                MainApp.loadScene("UserDashboard.fxml"); // Tên FXML ví dụ
            }
        } else {
            showError(authResult.errorMessage()); // SỬA THÀNH .errorMessage()
        }
    }

    @FXML
    private void handleForgotPasswordLinkClick(MouseEvent event) {
        MainApp.loadScene("ForgotPasswordScreen.fxml");
    }

    @FXML
    private void handleRegisterHereLinkClick(MouseEvent event) {
        MainApp.loadScene("RegistrationScreen.fxml");
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
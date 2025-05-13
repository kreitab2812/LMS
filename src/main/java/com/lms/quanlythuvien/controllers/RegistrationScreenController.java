package com.lms.quanlythuvien.controllers; // Package của Controller

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.models.User;
import com.lms.quanlythuvien.services.AuthService;
import com.lms.quanlythuvien.services.AuthResult; // IMPORT CHO AUTHRESULT

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button; // Mặc dù không có fx:id cho button nhưng có thể giữ lại
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

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

    private AuthService authService;

    private Image eyeIcon;
    private Image eyeSlashIcon;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        authService = new AuthService();

        // Tải icons
        try {
            String eyeIconPath = "/com/lms/quanlythuvien/images/eye_icon.png";
            String eyeSlashIconPath = "/com/lms/quanlythuvien/images/eye_slash_icon.png";
            eyeIcon = new Image(getClass().getResourceAsStream(eyeIconPath));
            eyeSlashIcon = new Image(getClass().getResourceAsStream(eyeSlashIconPath));

            if (eyeIcon.isError() || eyeSlashIcon.isError()) {
                System.err.println("RegistrationScreen: Error loading eye icons.");
            } else {
                togglePasswordVisibility.setImage(eyeSlashIcon);
                toggleConfirmPasswordVisibility.setImage(eyeSlashIcon);
            }
        } catch (Exception e) {
            System.err.println("RegistrationScreen: Exception loading eye icons: " + e.getMessage());
        }

        // Thiết lập trạng thái ban đầu
        setupFieldVisibility(passwordField, visiblePasswordField, false);
        setupFieldVisibility(confirmPasswordField, visibleConfirmPasswordField, false);

        bindPasswordFields(passwordField, visiblePasswordField, () -> isPasswordVisible);
        bindPasswordFields(confirmPasswordField, visibleConfirmPasswordField, () -> isConfirmPasswordVisible);

        clearError();
    }

    // Helper để setup ẩn/hiện ban đầu
    private void setupFieldVisibility(PasswordField pf, TextField tf, boolean isInitiallyVisible) {
        if (isInitiallyVisible) { // Nếu muốn hiện ban đầu (thường là không)
            pf.setManaged(false); pf.setVisible(false);
            tf.setManaged(true); tf.setVisible(true);
        } else { // Mặc định ẩn text field, hiện password field
            pf.setManaged(true); pf.setVisible(true);
            tf.setManaged(false); tf.setVisible(false);
        }
    }

    // Functional interface để kiểm tra trạng thái visible
    @FunctionalInterface
    interface VisibilityCheck {
        boolean get();
    }

    private void bindPasswordFields(PasswordField pf, TextField tf, VisibilityCheck visibilityCheck) {
        tf.textProperty().addListener((obs, oldText, newText) -> {
            if (visibilityCheck.get()) pf.setText(newText);
        });
        pf.textProperty().addListener((obs, oldText, newText) -> {
            if (!visibilityCheck.get()) tf.setText(newText);
        });
    }

    @FXML
    private void toggleMainPasswordVisibilityClicked(MouseEvent event) {
        isPasswordVisible = !isPasswordVisible;
        updatePasswordView(passwordField, visiblePasswordField, togglePasswordVisibility, isPasswordVisible);
    }

    @FXML
    private void toggleConfirmPasswordVisibilityClicked(MouseEvent event) {
        isConfirmPasswordVisible = !isConfirmPasswordVisible;
        updatePasswordView(confirmPasswordField, visibleConfirmPasswordField, toggleConfirmPasswordVisibility, isConfirmPasswordVisible);
    }

    private void updatePasswordView(PasswordField pf, TextField tf, ImageView toggleIcon, boolean showPassword) {
        setupFieldVisibility(pf, tf, showPassword); // Sử dụng helper
        if (showPassword) {
            tf.setText(pf.getText());
            tf.requestFocus();
            tf.end();
            if (eyeIcon != null && !eyeIcon.isError()) toggleIcon.setImage(eyeIcon);
        } else {
            pf.setText(tf.getText());
            pf.requestFocus();
            pf.end();
            if (eyeSlashIcon != null && !eyeSlashIcon.isError()) toggleIcon.setImage(eyeSlashIcon);
        }
    }

    @FXML
    private void handleRegisterButtonAction() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = isPasswordVisible ? visiblePasswordField.getText() : passwordField.getText();
        String confirmPassword = isConfirmPasswordVisible ? visibleConfirmPasswordField.getText() : confirmPasswordField.getText();

        clearError();

        if (username.isEmpty()) { showError("Username cannot be empty."); return; }
        if (email.isEmpty()) { showError("Email cannot be empty."); return; }
        if (password.isEmpty()) { showError("Password cannot be empty."); return; }
        if (confirmPassword.isEmpty()) { showError("Confirm Password cannot be empty."); return; }
        if (!email.contains("@")) { showError("Invalid email format. Please include '@'."); return; }
        if (password.length() < 7) { showError("Password must be at least 7 characters long."); return; }
        if (!password.equals(confirmPassword)) { showError("Passwords do not match."); return; }

        // Gọi AuthService để đăng ký (mặc định vai trò là USER)
        AuthResult registrationResult = authService.register(username, email, password, User.Role.USER);

        if (registrationResult.isSuccess()) {
            System.out.println("Registration successful for: " + username);
            MainApp.loadScene("RegistrationSuccessfulScreen.fxml");
        } else {
            // Hiển thị lỗi từ AuthService (ví dụ: username/email đã tồn tại)
            showError(registrationResult.errorMessage()); // SỬA THÀNH .errorMessage()
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
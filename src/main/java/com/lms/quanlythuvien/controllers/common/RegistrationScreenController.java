package com.lms.quanlythuvien.controllers.common;

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.auth.AuthService;
import com.lms.quanlythuvien.services.auth.AuthResult;
import com.lms.quanlythuvien.services.user.UserService;
import com.lms.quanlythuvien.utils.session.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent; // Thêm cho Enter
import javafx.scene.input.MouseEvent;

import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.BooleanSupplier; // Sửa lại FunctionalInterface

public class RegistrationScreenController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private ImageView togglePasswordVisibility;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField visibleConfirmPasswordField;
    @FXML private ImageView toggleConfirmPasswordVisibility;
    @FXML private Label errorLabel;
    // @FXML private Label loginNowLink; // fx:id này đã có trong FXML và có handler

    private UserService userService;
    private AuthService authService;
    private Image eyeIcon, eyeSlashIcon;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("DEBUG_RSC_INIT: RegistrationScreenController initialize() started.");
        if (userService == null) userService = UserService.getInstance();
        if (authService == null) authService = new AuthService(userService);

        loadToggleIcons();
        updatePasswordView(passwordField, visiblePasswordField, togglePasswordVisibility, isPasswordVisible);
        updatePasswordView(confirmPasswordField, visibleConfirmPasswordField, toggleConfirmPasswordVisibility, isConfirmPasswordVisible);
        bindPasswordFields();
        clearError();

        // Cho phép nhấn Enter để đăng ký từ trường confirm password
        if(confirmPasswordField != null) confirmPasswordField.setOnKeyPressed(this::handleEnterPressed);
        if(visibleConfirmPasswordField != null) visibleConfirmPasswordField.setOnKeyPressed(this::handleEnterPressed);

        System.out.println("DEBUG_RSC_INIT: RegistrationScreenController initialize() finished.");
    }

    private void handleEnterPressed(KeyEvent event) {
        if (event.getCode().toString().equals("ENTER")) {
            handleRegisterButtonAction();
        }
    }

    private void loadToggleIcons() {
        // Tương tự LoginScreenController
        try {
            String eyeIconPath = "/com/lms/quanlythuvien/images/eye_icon.png";
            String eyeSlashIconPath = "/com/lms/quanlythuvien/images/eye_slash_icon.png";
            try (InputStream eyeStream = getClass().getResourceAsStream(eyeIconPath);
                 InputStream eyeSlashStream = getClass().getResourceAsStream(eyeSlashIconPath)) {
                if (eyeStream != null) eyeIcon = new Image(eyeStream);
                else System.err.println("ERROR_RSC_ICON: Failed to load eye_icon.png from " + eyeIconPath);
                if (eyeSlashStream != null) eyeSlashIcon = new Image(eyeSlashStream);
                else System.err.println("ERROR_RSC_ICON: Failed to load eye_slash_icon.png from " + eyeSlashIconPath);
            }
            if (eyeSlashIcon != null && !eyeSlashIcon.isError()) {
                if (togglePasswordVisibility != null) togglePasswordVisibility.setImage(eyeSlashIcon);
                if (toggleConfirmPasswordVisibility != null) toggleConfirmPasswordVisibility.setImage(eyeSlashIcon);
            }
        } catch (Exception e) {
            System.err.println("CRITICAL_RSC_ICON: Exception loading eye icons: " + e.getMessage());
        }
    }

    // Thay vì VisibilityCheck, việc bind nên được thực hiện trong updatePasswordView
    private void bindPasswordFields() {
        visiblePasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isPasswordVisible && !passwordField.getText().equals(newVal)) passwordField.setText(newVal);
        });
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isPasswordVisible && !visiblePasswordField.getText().equals(newVal)) visiblePasswordField.setText(newVal);
        });
        visibleConfirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isConfirmPasswordVisible && !confirmPasswordField.getText().equals(newVal)) confirmPasswordField.setText(newVal);
        });
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isConfirmPasswordVisible && !visibleConfirmPasswordField.getText().equals(newVal)) visibleConfirmPasswordField.setText(newVal);
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
        String currentText = showPassword ? pf.getText() : tf.getText();
        if (showPassword) {
            tf.setText(currentText); // Đồng bộ trước khi thay đổi visibility
            pf.setManaged(false); pf.setVisible(false);
            tf.setManaged(true); tf.setVisible(true);
            if (toggleIcon != null && eyeIcon != null) toggleIcon.setImage(eyeIcon);
            tf.requestFocus();
            if (tf.getText() != null) tf.positionCaret(tf.getText().length());
        } else {
            pf.setText(currentText); // Đồng bộ trước khi thay đổi visibility
            tf.setManaged(false); tf.setVisible(false);
            pf.setManaged(true); pf.setVisible(true);
            if (toggleIcon != null && eyeSlashIcon != null) toggleIcon.setImage(eyeSlashIcon);
            pf.requestFocus();
            if (pf.getText() != null) pf.positionCaret(pf.getText().length());
        }
    }

    @FXML
    private void handleRegisterButtonAction() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        // Luôn lấy text từ trường đang được hiển thị (đã đồng bộ)
        String password = isPasswordVisible ? visiblePasswordField.getText() : passwordField.getText();
        String confirmPassword = isConfirmPasswordVisible ? visibleConfirmPasswordField.getText() : confirmPasswordField.getText();
        clearError();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Vui lòng điền đầy đủ tất cả các trường bắt buộc (*)."); return;
        }
        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showError("Định dạng email không hợp lệ."); return;
        }
        if (password.length() < 7) {
            showError("Mật khẩu phải có ít nhất 7 ký tự."); return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Mật khẩu và xác nhận mật khẩu không khớp."); return;
        }

        // Gọi service để đăng ký, với vai trò mặc định là USER
        AuthResult registrationResult = authService.register(username, email, password, User.Role.USER);

        if (registrationResult.isSuccess()) {
            System.out.println("DEBUG_RSC_REGISTER: Registration successful for: " + username);
            // Truyền thông điệp và nút nhấn cho màn hình thành công
            SessionManager.getInstance().setSuccessScreenMessage("Đăng ký tài khoản thành công!", "Bạn có thể đăng nhập ngay bây giờ.");
            SessionManager.getInstance().setSuccessScreenButton("Về trang Đăng nhập", "common/LoginScreen.fxml");
            MainApp.loadScene("common/SuccessfulScreen.fxml");
        } else {
            showError(registrationResult.errorMessage() != null ? registrationResult.errorMessage() : "Đăng ký không thành công. Vui lòng thử lại.");
        }
    }

    @FXML
    private void handleLoginNowLinkClick(MouseEvent event) { MainApp.loadScene("common/LoginScreen.fxml"); }
    private void showError(String message) { if (errorLabel != null) { errorLabel.setText(message); errorLabel.setVisible(true); errorLabel.setManaged(true); }}
    private void clearError() { if (errorLabel != null) { errorLabel.setText(""); errorLabel.setVisible(false); errorLabel.setManaged(false); }}
}
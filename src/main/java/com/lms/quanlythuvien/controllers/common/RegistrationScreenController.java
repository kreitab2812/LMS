package com.lms.quanlythuvien.controllers.common; // Package của Controller

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.auth.AuthService;
import com.lms.quanlythuvien.services.auth.AuthResult;
import com.lms.quanlythuvien.services.user.UserService;

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
    @FXML
    private Label loginNowLink; // Đã có fx:id trong FXML mới nhất

    private UserService userService;
    private AuthService authService;

    private Image eyeIcon;
    private Image eyeSlashIcon;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("DEBUG_RSC_INIT: RegistrationScreenController initialize() started.");
        userService = UserService.getInstance();
        System.out.println("DEBUG_RSC_INIT: UserService instantiated.");

        authService = new AuthService(userService);
        System.out.println("DEBUG_RSC_INIT: AuthService instantiated (with UserService).");

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

            if (eyeSlashIcon != null && !eyeSlashIcon.isError()) {
                if (togglePasswordVisibility != null) togglePasswordVisibility.setImage(eyeSlashIcon);
                if (toggleConfirmPasswordVisibility != null) toggleConfirmPasswordVisibility.setImage(eyeSlashIcon);
            } else {
                System.err.println("ERROR_RSC_INIT: Default eyeSlashIcon could not be set for toggles due to loading error or null.");
            }

        } catch (Exception e) {
            System.err.println("CRITICAL_RSC_INIT: Exception during eye icon loading in RegistrationScreenController:");
            e.printStackTrace();
        }

        setupFieldVisibility(passwordField, visiblePasswordField, false);
        setupFieldVisibility(confirmPasswordField, visibleConfirmPasswordField, false);

        bindPasswordFields(passwordField, visiblePasswordField, () -> isPasswordVisible);
        bindPasswordFields(confirmPasswordField, visibleConfirmPasswordField, () -> isConfirmPasswordVisible);

        clearError();
        System.out.println("DEBUG_RSC_INIT: RegistrationScreenController initialize() finished.");
    }

    private void setupFieldVisibility(PasswordField pf, TextField tf, boolean isInitiallyVisible) {
        if (isInitiallyVisible) {
            pf.setManaged(false); pf.setVisible(false);
            tf.setManaged(true); tf.setVisible(true);
        } else {
            pf.setManaged(true); pf.setVisible(true);
            tf.setManaged(false); tf.setVisible(false);
        }
    }

    @FunctionalInterface
    interface VisibilityCheck {
        boolean get();
    }

    private void bindPasswordFields(PasswordField pf, TextField tf, VisibilityCheck visibilityCheck) {
        tf.textProperty().addListener((obs, oldText, newText) -> {
            if (visibilityCheck.get()) {
                pf.setText(newText);
            }
        });
        pf.textProperty().addListener((obs, oldText, newText) -> {
            if (!visibilityCheck.get()) {
                tf.setText(newText);
            }
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
        setupFieldVisibility(pf, tf, showPassword);
        if (showPassword) {
            tf.setText(pf.getText());
            tf.requestFocus();
            if (tf.getText() != null) tf.end();
            if (toggleIcon != null && eyeIcon != null && !eyeIcon.isError()) toggleIcon.setImage(eyeIcon);
        } else {
            pf.setText(tf.getText());
            pf.requestFocus();
            if (pf.getText() != null) pf.end();
            if (toggleIcon != null && eyeSlashIcon != null && !eyeSlashIcon.isError()) toggleIcon.setImage(eyeSlashIcon);
        }
    }

    @FXML
    private void handleRegisterButtonAction() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = isPasswordVisible ? visiblePasswordField.getText() : passwordField.getText();
        String confirmPassword = isConfirmPasswordVisible ? visibleConfirmPasswordField.getText() : confirmPasswordField.getText();

        clearError();

        if (username.isEmpty()) { showError("Tên đăng nhập không được để trống."); return; }
        if (email.isEmpty()) { showError("Email không được để trống."); return; }
        if (password.isEmpty()) { showError("Mật khẩu không được để trống."); return; }
        if (confirmPassword.isEmpty()) { showError("Xác nhận mật khẩu không được để trống."); return; }
        if (!email.contains("@") || !email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showError("Định dạng email không hợp lệ."); return;
        }
        if (password.length() < 7) {
            showError("Mật khẩu phải có ít nhất 7 ký tự."); return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Mật khẩu không khớp."); return;
        }

        AuthResult registrationResult = authService.register(username, email, password, User.Role.USER);

        if (registrationResult.isSuccess()) {
            System.out.println("Registration successful for: " + username);
            // Chuyển đến màn hình thông báo đăng ký thành công
            MainApp.loadScene("common/SuccessfulScreen.fxml"); // <<<--- SỬA ĐƯỜNG DẪN
        } else {
            // Hiển thị lỗi từ AuthService
            // Lưu ý: registrationResult.errorMessage() cũng cần được dịch nếu nó trả về tiếng Anh
            // Ví dụ: "Tên đăng nhập đã tồn tại." hoặc "Email đã được đăng ký."
            showError(registrationResult.errorMessage());
        }
    }

    @FXML
    private void handleLoginNowLinkClick(MouseEvent event) {
        System.out.println("DEBUG_RSC: LoginNowLink clicked."); // Thêm log để dễ theo dõi
        MainApp.loadScene("common/LoginScreen.fxml"); // <<<--- SỬA ĐƯỜNG DẪN
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        } else {
            System.err.println("ERROR_RSC_SHOW_ERROR: errorLabel is null! Cannot display message: " + message);
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
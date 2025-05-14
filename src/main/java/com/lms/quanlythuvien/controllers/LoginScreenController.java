package com.lms.quanlythuvien.controllers; // Giữ nguyên package của cậu

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.models.User;
import com.lms.quanlythuvien.services.AuthService;
import com.lms.quanlythuvien.services.AuthResult;
import com.lms.quanlythuvien.services.UserService; // THÊM IMPORT NÀY

import javafx.event.ActionEvent; // Kiểm tra xem có dùng ActionEvent không cho handleLoginButtonAction
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
// import javafx.scene.control.Alert; // Bỏ comment nếu cậu có dùng Alert ở đâu đó trong các hàm private
import javafx.scene.control.Button; // Giữ lại nếu FXML có Button với fx:id, dù không dùng trong code này
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
    @FXML
    private Label forgotPasswordLink;
    @FXML
    private Label registerHereLink;

    // Khai báo các Service
    private UserService userService; // KHAI BÁO UserService
    private AuthService authService;

    private Image eyeIcon;
    private Image eyeSlashIcon;
    private boolean isPasswordVisible = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("DEBUG_LSC_INIT: LoginScreenController initialize() started.");

        // KHỞI TẠO UserService TRƯỚC
        userService = UserService.getInstance();
        System.out.println("DEBUG_LSC_INIT: UserService instantiated.");

        // SAU ĐÓ KHỞI TẠO AuthService VÀ TRUYỀN userService VÀO
        authService = new AuthService(userService);
        System.out.println("DEBUG_LSC_INIT: AuthService instantiated (with UserService).");

        // Tải icons
        try {
            String eyeIconPath = "/com/lms/quanlythuvien/images/eye_icon.png";
            String eyeSlashIconPath = "/com/lms/quanlythuvien/images/eye_slash_icon.png";

            System.out.println("DEBUG_LSC_INIT: Attempting to load eye icons...");
            InputStream eyeStream = getClass().getResourceAsStream(eyeIconPath);
            InputStream eyeSlashStream = getClass().getResourceAsStream(eyeSlashIconPath);

            if (eyeStream != null) {
                eyeIcon = new Image(eyeStream);
                System.out.println("DEBUG_LSC_INIT: eye_icon.png loaded successfully.");
            } else {
                System.err.println("ERROR_LSC_INIT: Failed to load eye_icon.png. Path or file incorrect: " + eyeIconPath);
            }

            if (eyeSlashStream != null) {
                eyeSlashIcon = new Image(eyeSlashStream);
                System.out.println("DEBUG_LSC_INIT: eye_slash_icon.png loaded successfully.");
            } else {
                System.err.println("ERROR_LSC_INIT: Failed to load eye_slash_icon.png. Path or file incorrect: " + eyeSlashIconPath);
            }

            if (togglePasswordVisibility != null && eyeSlashIcon != null && !eyeSlashIcon.isError()) {
                togglePasswordVisibility.setImage(eyeSlashIcon);
            } else {
                System.err.println("ERROR_LSC_INIT: togglePasswordVisibility ImageView is null or eyeSlashIcon is null/error. Cannot set default toggle image.");
            }

        } catch (Exception e) {
            System.err.println("CRITICAL_LSC_INIT: Exception during eye icon loading in LoginScreenController:");
            e.printStackTrace();
        }

        setupPasswordFieldVisibility(false);
        bindPasswordFields();
        clearError();
        System.out.println("DEBUG_LSC_INIT: LoginScreenController initialize() finished.");
    }

    private void bindPasswordFields() {
        // Đồng bộ giá trị giữa visiblePasswordField và passwordField
        visiblePasswordField.textProperty().addListener((obs, oldText, newText) -> {
            if (isPasswordVisible) { // Chỉ cập nhật passwordField nếu visiblePasswordField đang active
                passwordField.setText(newText);
            }
        });
        passwordField.textProperty().addListener((obs, oldText, newText) -> {
            if (!isPasswordVisible) { // Chỉ cập nhật visiblePasswordField nếu passwordField đang active
                visiblePasswordField.setText(newText);
            }
        });
    }

    private void setupPasswordFieldVisibility(boolean showPassword) {
        isPasswordVisible = showPassword;
        if (showPassword) {
            passwordField.setManaged(false);
            passwordField.setVisible(false);
            visiblePasswordField.setManaged(true);
            visiblePasswordField.setVisible(true);
            // visiblePasswordField.setText(passwordField.getText()); // Chuyển text khi đổi trạng thái
            if (togglePasswordVisibility != null && eyeIcon != null && !eyeIcon.isError()) {
                togglePasswordVisibility.setImage(eyeIcon);
            } else {
                System.err.println("DEBUG_LSC_VISIBILITY: eyeIcon is null or error, or toggleImageView is null. Cannot set visible toggle image.");
            }
        } else {
            visiblePasswordField.setManaged(false);
            visiblePasswordField.setVisible(false);
            passwordField.setManaged(true);
            passwordField.setVisible(true);
            // passwordField.setText(visiblePasswordField.getText()); // Chuyển text khi đổi trạng thái
            if (togglePasswordVisibility != null && eyeSlashIcon != null && !eyeSlashIcon.isError()) {
                togglePasswordVisibility.setImage(eyeSlashIcon);
            } else {
                System.err.println("DEBUG_LSC_VISIBILITY: eyeSlashIcon is null or error, or toggleImageView is null. Cannot set hidden toggle image.");
            }
        }
    }

    @FXML
    private void togglePasswordVisibilityClicked(MouseEvent event) {
        System.out.println("DEBUG_LSC: togglePasswordVisibilityClicked - Method Started.");
        isPasswordVisible = !isPasswordVisible; // Đảo trạng thái trước
        setupPasswordFieldVisibility(isPasswordVisible); // Cập nhật UI dựa trên trạng thái mới

        // Đồng bộ text và focus sau khi thay đổi visibility
        if(isPasswordVisible) {
            visiblePasswordField.setText(passwordField.getText()); // Đảm bảo text được chuyển
            visiblePasswordField.requestFocus();
            if (visiblePasswordField.getText() != null) visiblePasswordField.end();
        } else {
            passwordField.setText(visiblePasswordField.getText()); // Đảm bảo text được chuyển
            passwordField.requestFocus();
            if (passwordField.getText() != null) passwordField.end();
        }
        System.out.println("DEBUG_LSC: togglePasswordVisibilityClicked - Method Finished. isPasswordVisible: " + isPasswordVisible);
    }

    @FXML
    private void handleLoginButtonAction() {
        System.out.println("DEBUG_LSC_LOGIN: handleLoginButtonAction - Method Started.");

        String email = emailField.getText().trim();
        String password = isPasswordVisible ? visiblePasswordField.getText() : passwordField.getText();

        clearError();
        System.out.println("DEBUG_LSC_LOGIN: Error cleared. Email: [" + email + "], Password length: [" + password.length() + "]");

        if (email.isEmpty() && password.isEmpty()) {
            showError("Email and Password are required.");
            System.out.println("DEBUG_LSC_LOGIN: Validation failed - Email and Password empty.");
            return;
        }
        if (email.isEmpty()) {
            showError("Email cannot be empty.");
            System.out.println("DEBUG_LSC_LOGIN: Validation failed - Email empty.");
            return;
        }
        if (password.isEmpty()) {
            showError("Password cannot be empty.");
            System.out.println("DEBUG_LSC_LOGIN: Validation failed - Password empty.");
            return;
        }
        if (!email.contains("@") || !email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showError("Invalid email format.");
            System.out.println("DEBUG_LSC_LOGIN: Validation failed - Invalid email format.");
            return;
        }

        System.out.println("DEBUG_LSC_LOGIN: Basic validations passed.");

        if (authService == null) {
            System.err.println("CRITICAL_LSC_LOGIN: authService is NULL!");
            showError("System error: Authentication service not available. Please restart the application.");
            return;
        }
        System.out.println("DEBUG_LSC_LOGIN: authService is not null. Calling authService.login()...");

        AuthResult authResult = authService.login(email, password);

        if (authResult == null) {
            System.err.println("CRITICAL_LSC_LOGIN: authResult from authService.login() is NULL!");
            showError("Critical system error during login. AuthResult is null.");
            return;
        }

        System.out.println("DEBUG_LSC_LOGIN: Returned from authService.login(). AuthResult isSuccess: " + authResult.isSuccess() + ", User: [" + (authResult.user() != null ? authResult.user().getUsername() : "null") + "], Msg: [" + authResult.errorMessage() + "]");

        if (authResult.isSuccess()) {
            User loggedInUser = authResult.user();
            if (loggedInUser == null) {
                System.err.println("CRITICAL_LSC_LOGIN: Login reported success but user object is NULL!");
                showError("Login successful but user data is missing.");
                return;
            }
            System.out.println("DEBUG_LSC_LOGIN: Login successful! User: " + loggedInUser.getUsername() + ", Role: " + loggedInUser.getRole());
            System.out.println("DEBUG_LSC_LOGIN: Attempting to load scene for role: " + loggedInUser.getRole());
            if (loggedInUser.getRole() == User.Role.ADMIN) {
                MainApp.loadScene("AdminDashboard.fxml");
            } else {
                MainApp.loadScene("UserDashboard.fxml"); // Hoặc một FXML khác cho user thường
            }
            System.out.println("DEBUG_LSC_LOGIN: MainApp.loadScene method called.");
        } else {
            System.out.println("DEBUG_LSC_LOGIN: Login failed. Displaying error: " + authResult.errorMessage());
            showError(authResult.errorMessage());
        }
        System.out.println("DEBUG_LSC_LOGIN: handleLoginButtonAction - Method Finished.");
    }

    @FXML
    private void handleForgotPasswordLinkClick(MouseEvent event) {
        System.out.println("DEBUG_LSC: ForgotPasswordLink clicked.");
        MainApp.loadScene("ForgotPasswordScreen.fxml");
    }

    @FXML
    private void handleRegisterHereLinkClick(MouseEvent event) {
        System.out.println("DEBUG_LSC: RegisterHereLink clicked.");
        MainApp.loadScene("RegistrationScreen.fxml");
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
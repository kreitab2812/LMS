package com.lms.quanlythuvien.controllers.common;

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.models.user.User; // Thêm nếu dùng
import com.lms.quanlythuvien.services.auth.EmailService;
import com.lms.quanlythuvien.services.user.UserService;
import com.lms.quanlythuvien.utils.security.PasswordUtils;
import com.lms.quanlythuvien.utils.session.SessionManager;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane; // Thêm cho applyDialogStyles
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert; // Thêm cho showAlert
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.InputStream;
import java.net.URL;
import java.util.Optional; // Thêm import này
import java.util.ResourceBundle;
import java.util.Random;

public class ForgotPasswordScreenController implements Initializable {

    @FXML private Button backButton;
    @FXML private TextField emailField;
    @FXML private Button sendCodeButton;
    @FXML private Label messageLabel;
    @FXML private Label instructionLabel;

    @FXML private HBox resendBox;
    @FXML private Label countdownLabel;
    @FXML private Button resendCodeButton;

    @FXML private VBox emailEntrySection;
    @FXML private VBox resetProcessSection;
    @FXML private VBox codeEntrySubSection;
    @FXML private TextField codeField;
    @FXML private Button confirmCodeButton;
    @FXML private VBox newPasswordSubSection;

    @FXML private PasswordField newPasswordField;
    @FXML private TextField newPasswordFieldVisible; // Tên này được dùng trong code Java
    @FXML private ImageView toggleNewPasswordIcon;

    @FXML private PasswordField confirmNewPasswordField;
    @FXML private TextField confirmNewPasswordFieldVisible; // <<< Đảm bảo FXML có fx:id="confirmNewPasswordFieldVisible"
    @FXML private ImageView toggleConfirmPasswordIcon;

    @FXML private Button resetPasswordButton;
    @FXML private Label errorDetailLabel;

    private UserService userService;
    private EmailService emailService;
    // private VerificationCodeService codeService; // TODO: Nên có service riêng cho mã OTP

    private Image eyeIcon;
    private Image eyeSlashIcon;
    private boolean isNewPasswordTextVisible = false;
    private boolean isConfirmNewPasswordTextVisible = false;

    private Timeline countdownTimeline;
    private int countdownSeconds = 60;
    private String currentSentCode; // Chỉ cho demo, không an toàn
    private String emailForPasswordReset;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("ForgotPasswordScreenController initialized.");
        userService = UserService.getInstance();
        emailService = new EmailService(); // Giả sử EmailService có thể tạo mới hoặc có getInstance()

        loadToggleIcons();
        clearAllMessages();
        showInitialState();

        emailField.setOnKeyPressed(this::handleEmailFieldEnter);
        codeField.setOnKeyPressed(this::handleCodeFieldEnter);
        // Gán sự kiện Enter cho trường xác nhận mật khẩu cuối cùng
        if (confirmNewPasswordField != null) confirmNewPasswordField.setOnKeyPressed(this::handleNewPasswordFieldEnter);
        if (confirmNewPasswordFieldVisible != null) confirmNewPasswordFieldVisible.setOnKeyPressed(this::handleNewPasswordFieldEnter);
    }

    private void handleEmailFieldEnter(KeyEvent event) {
        if (event.getCode().toString().equals("ENTER") && sendCodeButton.isVisible() && !sendCodeButton.isDisable()) {
            handleSendCodeAction(null);
        }
    }
    private void handleCodeFieldEnter(KeyEvent event) {
        if (event.getCode().toString().equals("ENTER") && confirmCodeButton.isVisible() && !confirmCodeButton.isDisable()) {
            handleConfirmCodeAction(null);
        }
    }
    private void handleNewPasswordFieldEnter(KeyEvent event) {
        if (event.getCode().toString().equals("ENTER") && resetPasswordButton.isVisible() && !resetPasswordButton.isDisable()) {
            handleResetPasswordAction(null);
        }
    }

    private void loadToggleIcons() {
        try {
            String eyeIconPath = "/com/lms/quanlythuvien/images/eye_icon.png";
            String eyeSlashIconPath = "/com/lms/quanlythuvien/images/eye_slash_icon.png";
            try (InputStream eyeStream = getClass().getResourceAsStream(eyeIconPath);
                 InputStream eyeSlashStream = getClass().getResourceAsStream(eyeSlashIconPath)) {
                if (eyeStream != null) eyeIcon = new Image(eyeStream);
                else System.err.println("ERROR_FPSC_ICON: eye_icon.png not found from " + eyeIconPath);
                if (eyeSlashStream != null) eyeSlashIcon = new Image(eyeSlashStream);
                else System.err.println("ERROR_FPSC_ICON: eye_slash_icon.png not found from " + eyeSlashIconPath);
            }
            if (eyeSlashIcon != null && !eyeSlashIcon.isError()) { // Kiểm tra eyeSlashIcon trước khi dùng
                if (toggleNewPasswordIcon != null) toggleNewPasswordIcon.setImage(eyeSlashIcon);
                if (toggleConfirmPasswordIcon != null) toggleConfirmPasswordIcon.setImage(eyeSlashIcon);
            }
        } catch (Exception e) { System.err.println("CRITICAL_FPSC_ICON: " + e.getMessage()); }
    }

    private void showInitialState() {
        instructionLabel.setText("Nhập email đã đăng ký để nhận mã khôi phục.");
        emailEntrySection.setVisible(true); emailEntrySection.setManaged(true);
        sendCodeButton.setDisable(false);
        emailField.setDisable(false); emailField.clear();

        resetProcessSection.setVisible(false); resetProcessSection.setManaged(false);
        // Các sub-section bên trong resetProcessSection sẽ tự động ẩn/hiện theo nó

        resendBox.setVisible(false); resendBox.setManaged(false);
        if (countdownTimeline != null) countdownTimeline.stop();

        codeField.clear();
        newPasswordField.clear(); newPasswordFieldVisible.clear();
        confirmNewPasswordField.clear(); confirmNewPasswordFieldVisible.clear(); // Sử dụng đúng tên biến
        isNewPasswordTextVisible = false; isConfirmNewPasswordTextVisible = false;
        updatePasswordView(newPasswordField, newPasswordFieldVisible, toggleNewPasswordIcon, false);
        updatePasswordView(confirmNewPasswordField, confirmNewPasswordFieldVisible, toggleConfirmPasswordIcon, false); // Sử dụng đúng tên biến
        Platform.runLater(emailField::requestFocus);
    }

    private void showCodeEntryState() {
        instructionLabel.setText("Mã xác thực 6 chữ số đã được gửi đến:\n" + emailForPasswordReset);
        emailEntrySection.setVisible(false); emailEntrySection.setManaged(false);

        resetProcessSection.setVisible(true); resetProcessSection.setManaged(true);
        codeEntrySubSection.setVisible(true); codeEntrySubSection.setManaged(true);
        newPasswordSubSection.setVisible(false); newPasswordSubSection.setManaged(false);

        resendBox.setVisible(true); resendBox.setManaged(true);
        codeField.clear();
        Platform.runLater(codeField::requestFocus);
    }

    private void showNewPasswordState() {
        instructionLabel.setText("Mã xác thực chính xác. Đặt mật khẩu mới cho:\n" + emailForPasswordReset);
        emailEntrySection.setVisible(false); emailEntrySection.setManaged(false);
        resendBox.setVisible(false); resendBox.setManaged(false);

        resetProcessSection.setVisible(true); resetProcessSection.setManaged(true);
        codeEntrySubSection.setVisible(false); codeEntrySubSection.setManaged(false);
        newPasswordSubSection.setVisible(true); newPasswordSubSection.setManaged(true);

        newPasswordField.clear(); newPasswordFieldVisible.clear();
        confirmNewPasswordField.clear(); confirmNewPasswordFieldVisible.clear(); // Sử dụng đúng tên biến
        isNewPasswordTextVisible = false; isConfirmNewPasswordTextVisible = false;
        updatePasswordView(newPasswordField, newPasswordFieldVisible, toggleNewPasswordIcon, false);
        updatePasswordView(confirmNewPasswordField, confirmNewPasswordFieldVisible, toggleConfirmPasswordIcon, false); // Sử dụng đúng tên biến
        Platform.runLater(newPasswordField::requestFocus);
    }

    @FXML
    private void handleBackButtonAction(ActionEvent event) {
        if (countdownTimeline != null) countdownTimeline.stop();
        MainApp.loadScene("common/LoginScreen.fxml");
    }

    @FXML
    private void handleSendCodeAction(ActionEvent event) {
        emailForPasswordReset = emailField.getText().trim();
        clearAllMessages();
        if (emailForPasswordReset.isEmpty() || !emailForPasswordReset.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showErrorDetail("Định dạng email không hợp lệ."); return;
        }
        if (!userService.isEmailTaken(emailForPasswordReset)) {
            showErrorDetail("Email này chưa được đăng ký."); return;
        }

        currentSentCode = generateRandomCode(6);
        System.out.println("INFO_FPSC: Generated code " + currentSentCode + " for " + emailForPasswordReset);
        // TODO: Lưu code vào DB/Cache với thời gian hết hạn

        String subject = "Mã khôi phục mật khẩu - Thư viện UETLIB";
        String body = "Mã xác thực của bạn là: " + currentSentCode + "\nMã này có hiệu lực trong 5 phút.";

        // SỬA LỖI: Gọi đúng tên phương thức sendVerificationCode
        boolean emailSent = emailService.sendVerificationCode(emailForPasswordReset, subject, body);

        if (emailSent) {
            showMessage("Mã xác thực đã được gửi tới " + emailForPasswordReset + ".", false);
            showCodeEntryState();
            startCountdown();
        } else {
            showMessage("Lỗi gửi email. Vui lòng thử lại sau.", true);
        }
    }

    @FXML
    private void handleResendCodeAction(ActionEvent event) {
        clearAllMessages();
        if (emailForPasswordReset == null || emailForPasswordReset.isEmpty()) {
            showMessage("Lỗi: Không có email để gửi lại mã.", true); showInitialState(); return;
        }
        currentSentCode = generateRandomCode(6);
        System.out.println("INFO_FPSC: Resent code " + currentSentCode + " for " + emailForPasswordReset);
        // TODO: Cập nhật mã mới trong DB/Cache

        String subject = "Mã khôi phục mật khẩu mới - Thư viện UETLIB";
        String body = "Mã xác thực mới của bạn là: " + currentSentCode + "\nMã này có hiệu lực trong 5 phút.";

        // SỬA LỖI: Gọi đúng tên phương thức sendVerificationCode
        if (emailService.sendVerificationCode(emailForPasswordReset, subject, body)) {
            showMessage("Mã xác thực mới đã được gửi.", false);
            startCountdown();
        } else {
            showMessage("Lỗi gửi lại email. Vui lòng thử lại sau.", true);
        }
    }

    @FXML
    private void handleConfirmCodeAction(ActionEvent event) {
        String enteredCode = codeField.getText().trim();
        clearAllMessages();
        if (enteredCode.isEmpty() || !enteredCode.matches("\\d{6}")) {
            showErrorDetail("Mã xác thực phải là 6 chữ số."); return;
        }
        // TODO: Xác thực code từ service
        if (enteredCode.equals(currentSentCode)) { // Chỉ cho demo
            showMessage("Mã xác thực chính xác.", false);
            if (countdownTimeline != null) countdownTimeline.stop();
            showNewPasswordState();
        } else {
            showErrorDetail("Mã xác thực không đúng hoặc đã hết hạn.");
        }
    }

    @FXML
    private void handleResetPasswordAction(ActionEvent event) {
        String newPassword = isNewPasswordTextVisible ? newPasswordFieldVisible.getText() : newPasswordField.getText();
        String confirmNewPasswordVal = isConfirmNewPasswordTextVisible ? confirmNewPasswordFieldVisible.getText() : confirmNewPasswordField.getText(); // Đổi tên biến
        clearAllMessages();

        if (newPassword.isEmpty() || newPassword.length() < 7) {
            showErrorDetail("Mật khẩu mới phải có ít nhất 7 ký tự."); return;
        }
        if (!newPassword.equals(confirmNewPasswordVal)) {
            showErrorDetail("Mật khẩu mới và xác nhận không khớp."); return;
        }

        Optional<User> userOpt = userService.findUserByEmail(emailForPasswordReset);
        if (userOpt.isEmpty()) {
            showMessage("Lỗi: Không tìm thấy tài khoản với email " + emailForPasswordReset, true);
            showInitialState(); return;
        }
        User userToUpdate = userOpt.get();
        String newHashedPassword = PasswordUtils.hashPassword(newPassword);

        if (userService.changePassword(userToUpdate.getUserId(), newHashedPassword)) {
            System.out.println("INFO_FPSC: Password reset successfully for " + emailForPasswordReset);
            SessionManager.getInstance().setSuccessScreenMessage("Khôi phục mật khẩu thành công!", "Bạn có thể đăng nhập bằng mật khẩu mới.");
            SessionManager.getInstance().setSuccessScreenButton("Đăng nhập ngay", "common/LoginScreen.fxml");
            MainApp.loadScene("common/SuccessfulScreen.fxml");
        } else {
            showMessage("Lỗi: Không thể cập nhật mật khẩu. Vui lòng thử lại.", true);
        }
    }

    private void updatePasswordView(PasswordField pf, TextField tfVisible, ImageView toggleIcon, boolean showText) {
        String currentText = showText ? pf.getText() : tfVisible.getText();
        if (showText) {
            tfVisible.setText(currentText);
            tfVisible.setManaged(true); tfVisible.setVisible(true);
            pf.setManaged(false); pf.setVisible(false);
            if (toggleIcon != null && eyeIcon != null && !eyeIcon.isError()) toggleIcon.setImage(eyeIcon); // Kiểm tra eyeIcon không null
            Platform.runLater(() -> { tfVisible.requestFocus(); if (tfVisible.getText() != null) tfVisible.positionCaret(tfVisible.getText().length()); });
        } else {
            pf.setText(currentText);
            pf.setManaged(true); pf.setVisible(true);
            tfVisible.setManaged(false); tfVisible.setVisible(false);
            if (toggleIcon != null && eyeSlashIcon != null && !eyeSlashIcon.isError()) toggleIcon.setImage(eyeSlashIcon); // Kiểm tra eyeSlashIcon không null
            Platform.runLater(() -> { pf.requestFocus(); if (pf.getText() != null) pf.positionCaret(pf.getText().length()); });
        }
    }

    @FXML
    private void handleToggleNewPasswordIconClick(MouseEvent event) {
        isNewPasswordTextVisible = !isNewPasswordTextVisible;
        updatePasswordView(newPasswordField, newPasswordFieldVisible, toggleNewPasswordIcon, isNewPasswordTextVisible);
    }

    @FXML
    private void handleToggleConfirmPasswordIconClick(MouseEvent event) {
        isConfirmNewPasswordTextVisible = !isConfirmNewPasswordTextVisible;
        // SỬA LỖI: Dùng đúng tên biến cho TextField của confirm password
        updatePasswordView(confirmNewPasswordField, confirmNewPasswordFieldVisible, toggleConfirmPasswordIcon, isConfirmNewPasswordTextVisible);
    }

    private void startCountdown() {
        if (countdownTimeline != null) countdownTimeline.stop();
        countdownSeconds = 60;
        emailField.setDisable(true);
        sendCodeButton.setDisable(true);
        resendCodeButton.setDisable(true);
        updateCountdownLabel();
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            countdownSeconds--;
            updateCountdownLabel();
            if (countdownSeconds <= 0) {
                countdownTimeline.stop();
                resendCodeButton.setDisable(false);
                currentSentCode = null;
                showMessage("Mã đã hết hạn. Yêu cầu mã mới.", true);
            }
        }));
        countdownTimeline.setCycleCount(countdownSeconds + 1);
        countdownTimeline.play();
    }

    private void updateCountdownLabel() {
        Platform.runLater(() -> countdownLabel.setText(countdownSeconds > 0 ? "Gửi lại sau: " + countdownSeconds + "s" : "Có thể gửi lại mã."));
    }

    private void showMessage(String message, boolean isError) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.getStyleClass().removeAll("error-message", "success-message");
            messageLabel.getStyleClass().add(isError ? "error-message" : "success-message");
            messageLabel.setVisible(true); messageLabel.setManaged(true);
        }
    }
    private void showErrorDetail(String message) {
        if (errorDetailLabel != null) {
            errorDetailLabel.setText(message);
            errorDetailLabel.setVisible(true); errorDetailLabel.setManaged(true);
        } else { showMessage(message, true); }
    }
    private void clearAllMessages() {
        if (messageLabel != null) { messageLabel.setText(""); messageLabel.setVisible(false); messageLabel.setManaged(false); }
        if (errorDetailLabel != null) { errorDetailLabel.setText(""); errorDetailLabel.setVisible(false); errorDetailLabel.setManaged(false); }
    }

    private String generateRandomCode(int length) {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) code.append(random.nextInt(10));
        return code.toString();
    }
}
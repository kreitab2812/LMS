package com.lms.quanlythuvien.controllers.common;

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.services.auth.EmailService;
import com.lms.quanlythuvien.services.user.UserService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent; // Đã thêm
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.InputStream;
import java.net.URL;
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
    @FXML private TextField newPasswordFieldVisible;
    @FXML private ImageView toggleNewPasswordIcon; // Đổi từ ToggleButton sang ImageView

    @FXML private PasswordField confirmNewPasswordField;
    @FXML private TextField confirmNewPasswordFieldVisible;
    @FXML private ImageView toggleConfirmPasswordIcon; // Đổi từ ToggleButton sang ImageView

    @FXML private Button resetPasswordButton;

    private UserService userService;
    private EmailService emailService;
    // private VerificationCodeService codeService; // Service để quản lý mã OTP

    private Image eyeIcon;
    private Image eyeSlashIcon;
    private boolean isNewPasswordTextVisible = false;
    private boolean isConfirmNewPasswordTextVisible = false;

    private Timeline countdownTimeline;
    private int countdownSeconds = 60;
    private String currentSentCode;
    private String emailForPasswordReset;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("ForgotPasswordScreenController initialized.");

        userService = UserService.getInstance();
        emailService = new EmailService();

        // Tải icons
        try {
            String eyeIconPath = "/com/lms/quanlythuvien/images/eye_icon.png";
            String eyeSlashIconPath = "/com/lms/quanlythuvien/images/eye_slash_icon.png";

            InputStream eyeStream = getClass().getResourceAsStream(eyeIconPath);
            if (eyeStream != null) eyeIcon = new Image(eyeStream);
            else System.err.println("ERROR_FPSC_INIT: Cannot load eye_icon.png from " + eyeIconPath);

            InputStream eyeSlashStream = getClass().getResourceAsStream(eyeSlashIconPath);
            if (eyeSlashStream != null) eyeSlashIcon = new Image(eyeSlashStream);
            else System.err.println("ERROR_FPSC_INIT: Cannot load eye_slash_icon.png from " + eyeSlashIconPath);

            // Đặt icon mặc định là mắt đóng (eyeSlashIcon)
            if (eyeSlashIcon != null) {
                if (toggleNewPasswordIcon != null) toggleNewPasswordIcon.setImage(eyeSlashIcon);
                if (toggleConfirmPasswordIcon != null) toggleConfirmPasswordIcon.setImage(eyeSlashIcon);
            } else {
                System.err.println("ERROR_FPSC_INIT: eyeSlashIcon is null, cannot set default toggle images.");
            }
        } catch (Exception e) {
            System.err.println("CRITICAL_FPSC_INIT: Exception during eye icon loading: " + e.getMessage());
            e.printStackTrace();
        }

        clearMessage();
        showInitialState();
        // Không cần bindBidirectional nữa vì đã có phương thức updatePasswordView xử lý đồng bộ
    }

    private void showInitialState() {
        instructionLabel.setText("Nhập địa chỉ email đã đăng ký của bạn. Chúng tôi sẽ gửi một mã xác thực đến email đó.");
        emailEntrySection.setManaged(true); emailEntrySection.setVisible(true);
        sendCodeButton.setManaged(true); sendCodeButton.setVisible(true);
        sendCodeButton.setDisable(false);
        emailField.setDisable(false);
        emailField.clear();
        codeField.clear();
        newPasswordField.clear();
        newPasswordFieldVisible.clear();
        confirmNewPasswordField.clear();
        confirmNewPasswordFieldVisible.clear();

        isNewPasswordTextVisible = false; // Reset trạng thái
        isConfirmNewPasswordTextVisible = false;
        updatePasswordView(newPasswordField, newPasswordFieldVisible, toggleNewPasswordIcon, isNewPasswordTextVisible);
        updatePasswordView(confirmNewPasswordField, confirmNewPasswordFieldVisible, toggleConfirmPasswordIcon, isConfirmNewPasswordTextVisible);

        resendBox.setManaged(false); resendBox.setVisible(false);
        resetProcessSection.setManaged(false); resetProcessSection.setVisible(false);
        codeEntrySubSection.setManaged(true); codeEntrySubSection.setVisible(true);
        newPasswordSubSection.setManaged(false); newPasswordSubSection.setVisible(false);
    }

    private void showCodeEntryStateOnly() {
        instructionLabel.setText("Mã xác thực đã được gửi đến " + emailForPasswordReset + ". Vui lòng nhập mã vào ô bên dưới.");
        emailEntrySection.setManaged(false); emailEntrySection.setVisible(false);
        sendCodeButton.setManaged(false); sendCodeButton.setVisible(false);

        resendBox.setManaged(true); resendBox.setVisible(true);

        resetProcessSection.setManaged(true); resetProcessSection.setVisible(true);
        codeEntrySubSection.setManaged(true); codeEntrySubSection.setVisible(true);
        newPasswordSubSection.setManaged(false); newPasswordSubSection.setVisible(false);

        Platform.runLater(() -> codeField.requestFocus());
    }

    private void showNewPasswordEntryState() {
        instructionLabel.setText("Mã xác thực chính xác. Vui lòng đặt mật khẩu mới.");
        resendBox.setManaged(false); resendBox.setVisible(false);

        resetProcessSection.setManaged(true); resetProcessSection.setVisible(true);
        codeEntrySubSection.setManaged(false); codeEntrySubSection.setVisible(false);
        newPasswordSubSection.setManaged(true); newPasswordSubSection.setVisible(true);

        Platform.runLater(() -> newPasswordField.requestFocus());
    }

    @FXML
    private void handleBackButtonAction(ActionEvent event) {
        System.out.println("Back button on Forgot Password screen clicked.");
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        // showInitialState(); // Không cần thiết vì sẽ load scene mới
        MainApp.loadScene("common/LoginScreen.fxml");
    }

    @FXML
    private void handleSendCodeAction(ActionEvent event) {
        emailForPasswordReset = emailField.getText().trim();
        clearMessage();

        if (emailForPasswordReset.isEmpty()) {
            showMessage("Vui lòng nhập địa chỉ email.", true); return;
        }
        if (!emailForPasswordReset.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showMessage("Định dạng email không hợp lệ.", true); return;
        }

        if (!userService.isEmailTaken(emailForPasswordReset)) {
            showMessage("Địa chỉ email này chưa được đăng ký trong hệ thống.", true);
            return;
        }

        System.out.println("Attempting to send verification code to: " + emailForPasswordReset);
        currentSentCode = generateRandomCode(6);
        System.out.println("Generated code for " + emailForPasswordReset + ": " + currentSentCode);

        // TODO: Lưu currentSentCode và emailForPasswordReset vào DB/Cache với thời gian hết hạn
        // Ví dụ: codeService.storeVerificationCode(emailForPasswordReset, currentSentCode, 600);

        String subject = "Mã xác thực khôi phục mật khẩu - Thư viện UET-VNU";
        String body = "Chào bạn,\n\nMã xác thực của bạn để khôi phục mật khẩu là: " + currentSentCode +
                "\nMã này có hiệu lực trong 10 phút.\nNếu bạn không yêu cầu thao tác này, vui lòng bỏ qua email này.\n\nTrân trọng,\nThư viện UET-VNU";

        boolean emailSentSuccess = emailService.sendVerificationCode(emailForPasswordReset, subject, body);

        if (!emailSentSuccess) {
            showMessage("Lỗi: Không thể gửi mã xác thực. Vui lòng thử lại sau hoặc kiểm tra cấu hình email.", true);
            return;
        }

        showMessage("Mã xác thực đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư (bao gồm cả thư mục Spam/Junk).", false);
        startCountdown();
        showCodeEntryStateOnly();
    }

    @FXML
    private void handleResendCodeAction(ActionEvent event) {
        clearMessage();
        if (emailForPasswordReset == null || emailForPasswordReset.isEmpty()) {
            showMessage("Lỗi: Không tìm thấy địa chỉ email để gửi lại mã. Vui lòng thử lại từ đầu.", true);
            showInitialState();
            return;
        }
        System.out.println("Resend code requested for: " + emailForPasswordReset);

        currentSentCode = generateRandomCode(6);
        System.out.println("Generated new code for " + emailForPasswordReset + ": " + currentSentCode);

        // TODO: Lưu mã mới
        // codeService.storeVerificationCode(emailForPasswordReset, currentSentCode, 600);

        String subjectResend = "Mã xác thực mới - Thư viện UET-VNU";
        String bodyResend = "Mã xác thực mới của bạn là: " + currentSentCode + "\nMã này có hiệu lực trong 10 phút.";
        boolean emailResentSuccess = emailService.sendVerificationCode(emailForPasswordReset, subjectResend, bodyResend);

        if (!emailResentSuccess) {
            showMessage("Lỗi: Không thể gửi lại mã xác thực. Vui lòng thử lại sau.", true);
            return;
        }

        showMessage("Một mã xác thực mới đã được gửi đến email của bạn.", false);
        startCountdown();
    }

    @FXML
    private void handleConfirmCodeAction(ActionEvent event) {
        String code = codeField.getText().trim();
        clearMessage();

        if (code.isEmpty()) { showMessage("Vui lòng nhập mã xác thực.", true); return; }
        if (code.length() != 6 || !code.matches("\\d{6}")) {
            showMessage("Mã xác thực phải là 6 chữ số.", true); return;
        }

        // TODO: Xác thực mã code với mã đã lưu (dùng codeService)
        // boolean isCodeValid = codeService.verifyCode(emailForPasswordReset, code);
        // if (!isCodeValid) { ... }
        if (!code.equals(currentSentCode)) { // Chỉ cho mục đích demo
            showMessage("Mã xác thực không đúng hoặc đã hết hạn. Vui lòng thử lại.", true);
            codeField.requestFocus();
            return;
        }

        showMessage("Mã xác thực chính xác. Vui lòng đặt mật khẩu mới.", false);
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        showNewPasswordEntryState();
    }

    @FXML
    private void handleResetPasswordAction(ActionEvent event) {
        String newPassword = isNewPasswordTextVisible ? newPasswordFieldVisible.getText() : newPasswordField.getText();
        String confirmNewPasswordText = isConfirmNewPasswordTextVisible ? confirmNewPasswordFieldVisible.getText() : confirmNewPasswordField.getText();
        clearMessage();

        if (newPassword.isEmpty()) { showMessage("Vui lòng nhập mật khẩu mới.", true); return; }
        if (confirmNewPasswordText.isEmpty()) { showMessage("Vui lòng xác nhận mật khẩu mới.", true); return; }
        if (newPassword.length() < 7) { showMessage("Mật khẩu mới phải có ít nhất 7 ký tự.", true); return; }
        if (!newPassword.equals(confirmNewPasswordText)) { showMessage("Mật khẩu mới và xác nhận mật khẩu không khớp.", true); return; }

        // TODO: Cập nhật mật khẩu mới cho người dùng trong database (dùng UserService)
        // String hashedNewPassword = PasswordUtils.hashPassword(newPassword);
        // boolean passwordUpdated = userService.resetPasswordForEmail(emailForPasswordReset, hashedNewPassword);
        // if (!passwordUpdated) { ... }

        System.out.println("Password reset successfully for " + emailForPasswordReset);
        MainApp.loadScene("common/SuccessfulScreen.fxml");
    }

    private void updatePasswordView(PasswordField pf, TextField tfVisible, ImageView toggleIcon, boolean showText) {
        String currentText = showText ? pf.getText() : tfVisible.getText(); // Lấy text từ trường đang active
        if (showText) { // Hiển thị text, ẩn PasswordField
            tfVisible.setText(currentText);
            tfVisible.setManaged(true);
            tfVisible.setVisible(true);
            pf.setManaged(false);
            pf.setVisible(false);
            if (toggleIcon != null && eyeIcon != null) toggleIcon.setImage(eyeIcon);
            Platform.runLater(() -> {
                tfVisible.requestFocus();
                if (tfVisible.getText() != null) tfVisible.positionCaret(tfVisible.getText().length());
            });
        } else { // Ẩn text, hiển thị PasswordField
            pf.setText(currentText);
            pf.setManaged(true);
            pf.setVisible(true);
            tfVisible.setManaged(false);
            tfVisible.setVisible(false);
            if (toggleIcon != null && eyeSlashIcon != null) toggleIcon.setImage(eyeSlashIcon);
            Platform.runLater(() -> {
                pf.requestFocus();
                if (pf.getText() != null) pf.positionCaret(pf.getText().length());
            });
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
        updatePasswordView(confirmNewPasswordField, confirmNewPasswordFieldVisible, toggleConfirmPasswordIcon, isConfirmNewPasswordTextVisible);
    }

    private void startCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
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
            }
        }));
        countdownTimeline.setCycleCount(countdownSeconds);
        countdownTimeline.play();
    }

    private void updateCountdownLabel() {
        Platform.runLater(() -> {
            if (countdownSeconds > 0) {
                countdownLabel.setText("Gửi lại mã sau: " + countdownSeconds + "s");
            } else {
                countdownLabel.setText("Bạn có thể gửi lại mã.");
            }
        });
    }

    private void showMessage(String message, boolean isError) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.getStyleClass().removeAll("error-label", "success-message");
            if (isError) {
                messageLabel.getStyleClass().add("error-label");
            } else {
                messageLabel.getStyleClass().add("success-message");
            }
            messageLabel.setVisible(true);
            messageLabel.setManaged(true);
        }
    }

    private void clearMessage() {
        if (messageLabel != null) {
            messageLabel.setText("");
            messageLabel.setVisible(false);
            messageLabel.setManaged(false);
            messageLabel.getStyleClass().removeAll("error-label", "success-message");
        }
    }

    private String generateRandomCode(int length) {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}
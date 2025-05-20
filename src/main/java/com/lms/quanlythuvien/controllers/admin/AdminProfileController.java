package com.lms.quanlythuvien.controllers.admin;

import com.lms.quanlythuvien.MainApp; // Cho CSS
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.user.UserService;
import com.lms.quanlythuvien.utils.session.SessionManager;
import com.lms.quanlythuvien.utils.security.PasswordUtils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*; // Import Control chung
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
// import javafx.stage.FileChooser; // Cho việc upload ảnh (chưa triển khai)
// import java.io.File;

import java.io.InputStream;
import java.net.URL;
import java.util.Optional; // Thêm nếu dùng Optional
import java.util.ResourceBundle;

public class AdminProfileController implements Initializable {

    @FXML private ImageView adminAvatarImageView;
    @FXML private Button changeAdminAvatarButton; // Hiện tại là placeholder
    @FXML private Label adminUsernameLabel;
    @FXML private TextField adminEmailField;
    @FXML private TextField adminFullNameField; // Admin cũng có họ tên
    // Các trường khác như SĐT, Bio có thể thêm vào FXML và User model nếu cần
    @FXML private Button saveAdminProfileButton;
    @FXML private Label adminProfileInfoErrorLabel;

    @FXML private PasswordField adminOldPasswordField;
    @FXML private PasswordField adminNewPasswordField;
    @FXML private PasswordField adminConfirmNewPasswordField;
    @FXML private Button adminChangePasswordButton;
    @FXML private Label adminPasswordChangeErrorLabel;

    private User currentAdmin;
    private UserService userService;
    private Image defaultAdminAvatar; // Ảnh đại diện mặc định

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = UserService.getInstance();
        currentAdmin = SessionManager.getInstance().getCurrentUser();

        loadDefaultAdminAvatar(); // Tải ảnh mặc định trước

        if (currentAdmin != null && currentAdmin.getRole() == User.Role.ADMIN) {
            loadAdminProfileData();
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi Phân Quyền", "Không thể tải thông tin hồ sơ quản trị viên. Vui lòng đăng nhập lại với tài khoản Admin.");
            disableAllFields(true); // Vô hiệu hóa tất cả các trường
        }
        clearAllErrors(); // Ẩn các label lỗi ban đầu
    }

    private void loadDefaultAdminAvatar() {
        try (InputStream defaultStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_avatar.png")) {
            if (defaultStream != null) {
                defaultAdminAvatar = new Image(defaultStream);
            } else {
                System.err.println("ERROR_ADMIN_PROFILE: Default admin avatar image not found.");
            }
        } catch (Exception e) {
            System.err.println("ERROR_ADMIN_PROFILE: Exception loading default admin avatar: " + e.getMessage());
        }
    }

    private void disableAllFields(boolean disable) {
        if (adminEmailField != null) adminEmailField.setDisable(disable);
        if (adminFullNameField != null) adminFullNameField.setDisable(disable);
        if (saveAdminProfileButton != null) saveAdminProfileButton.setDisable(disable);
        if (adminOldPasswordField != null) adminOldPasswordField.setDisable(disable);
        if (adminNewPasswordField != null) adminNewPasswordField.setDisable(disable);
        if (adminConfirmNewPasswordField != null) adminConfirmNewPasswordField.setDisable(disable);
        if (adminChangePasswordButton != null) adminChangePasswordButton.setDisable(disable);
        if (changeAdminAvatarButton != null) changeAdminAvatarButton.setDisable(disable);
    }

    private void loadAdminProfileData() {
        if (currentAdmin == null) return;

        if (adminUsernameLabel != null) adminUsernameLabel.setText(currentAdmin.getUsername());
        if (adminEmailField != null) adminEmailField.setText(currentAdmin.getEmail());
        if (adminFullNameField != null) adminFullNameField.setText(currentAdmin.getFullName() != null ? currentAdmin.getFullName() : "");

        // Load avatar
        if (adminAvatarImageView != null) {
            String avatarUrl = currentAdmin.getAvatarUrl();
            Image imageToSet = defaultAdminAvatar; // Mặc định
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                try {
                    Image loadedImage = new Image(avatarUrl, true); // true cho background loading
                    if (!loadedImage.isError()) {
                        imageToSet = loadedImage;
                    } else {
                        System.err.println("WARN_ADMIN_PROFILE_AVATAR: Error loading admin avatar from URL: " + avatarUrl);
                    }
                } catch (Exception e) {
                    System.err.println("ERROR_ADMIN_PROFILE_AVATAR: Exception loading avatar from URL: " + avatarUrl + ". " + e.getMessage());
                }
            }
            adminAvatarImageView.setImage(imageToSet);
        }
    }

    @FXML
    private void handleChangeAdminAvatar(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Chức năng thay đổi ảnh đại diện sẽ được phát triển sau.");
    }

    @FXML
    private void handleSaveAdminProfile(ActionEvent event) {
        clearAllErrors();
        if (currentAdmin == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không có thông tin quản trị viên để cập nhật.");
            return;
        }

        String newEmail = adminEmailField.getText().trim();
        String newFullName = adminFullNameField.getText().trim();

        if (newEmail.isEmpty() || !newEmail.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            setProfileError("Định dạng email không hợp lệ.");
            return;
        }
        // Tên đầy đủ có thể không bắt buộc
        // if (newFullName.isEmpty()) {
        //     setProfileError("Họ tên không được để trống.");
        //     return;
        // }

        // Kiểm tra email mới có bị trùng với user khác không (trừ chính admin này)
        if (!newEmail.equalsIgnoreCase(currentAdmin.getEmail())) {
            Optional<User> userWithNewEmail = userService.findUserByEmail(newEmail);
            if (userWithNewEmail.isPresent() && !userWithNewEmail.get().getUserId().equals(currentAdmin.getUserId())) {
                setProfileError("Địa chỉ email này đã được sử dụng.");
                return;
            }
        }

        currentAdmin.setEmail(newEmail);
        currentAdmin.setFullName(newFullName);
        // Cập nhật các trường khác nếu có (phone, bio)
        // currentAdmin.setPhoneNumber(...)
        // currentAdmin.setIntroduction(...)

        if (userService.updateUser(currentAdmin)) { // updateUser sẽ tự cập nhật `updatedAt`
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Thông tin hồ sơ quản trị viên đã được cập nhật.");
            // Cập nhật lại thông tin trên Top Bar của AdminDashboard
            Object dashboardObj = SessionManager.getInstance().getController("AdminDashboardController");
            if (dashboardObj instanceof AdminDashboardController) {
                ((AdminDashboardController) dashboardObj).refreshTopBarAdminInfo();
            }
            // Cập nhật lại session user nếu cần
            SessionManager.getInstance().setCurrentUser(currentAdmin);
        } else {
            setProfileError("Không thể cập nhật thông tin hồ sơ. Vui lòng thử lại.");
            // Load lại dữ liệu gốc nếu cập nhật thất bại
            Optional<User> refreshedAdmin = userService.findUserById(currentAdmin.getUserId());
            if (refreshedAdmin.isPresent()) {
                this.currentAdmin = refreshedAdmin.get();
                SessionManager.getInstance().setCurrentUser(this.currentAdmin); // Cập nhật lại session
                loadAdminProfileData(); // Load lại UI
            }
        }
    }

    @FXML
    private void handleAdminChangePassword(ActionEvent event) {
        clearAllErrors();
        if (currentAdmin == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không có thông tin quản trị viên để đổi mật khẩu.");
            return;
        }

        String oldPass = adminOldPasswordField.getText();
        String newPass = adminNewPasswordField.getText();
        String confirmPass = adminConfirmNewPasswordField.getText();

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            setPasswordError("Vui lòng điền đầy đủ các trường mật khẩu."); return;
        }
        if (newPass.length() < 7) {
            setPasswordError("Mật khẩu mới phải có ít nhất 7 ký tự."); return;
        }
        if (!newPass.equals(confirmPass)) {
            setPasswordError("Mật khẩu mới và xác nhận không khớp."); return;
        }
        if (!PasswordUtils.verifyPassword(oldPass, currentAdmin.getPasswordHash())) {
            setPasswordError("Mật khẩu cũ không chính xác."); return;
        }

        // Sử dụng phương thức changePassword của UserService
        String newHashedPassword = PasswordUtils.hashPassword(newPass);
        if (userService.changePassword(currentAdmin.getUserId(), newHashedPassword)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Mật khẩu quản trị viên đã được thay đổi.");
            adminOldPasswordField.clear();
            adminNewPasswordField.clear();
            adminConfirmNewPasswordField.clear();
            // Cập nhật passwordHash trong đối tượng currentAdmin và session
            currentAdmin.setPasswordHash(newHashedPassword); // Cập nhật local object
            SessionManager.getInstance().setCurrentUser(currentAdmin); // Cập nhật session
        } else {
            setPasswordError("Không thể thay đổi mật khẩu. Đã có lỗi xảy ra.");
        }
    }

    private void clearAllErrors() {
        if (adminProfileInfoErrorLabel != null) {
            adminProfileInfoErrorLabel.setText("");
            adminProfileInfoErrorLabel.setVisible(false);
            adminProfileInfoErrorLabel.setManaged(false);
        }
        if (adminPasswordChangeErrorLabel != null) {
            adminPasswordChangeErrorLabel.setText("");
            adminPasswordChangeErrorLabel.setVisible(false);
            adminPasswordChangeErrorLabel.setManaged(false);
        }
    }

    private void setProfileError(String message) {
        if (adminProfileInfoErrorLabel != null) {
            adminProfileInfoErrorLabel.setText(message);
            adminProfileInfoErrorLabel.setVisible(true);
            adminProfileInfoErrorLabel.setManaged(true);
        }
    }

    private void setPasswordError(String message) {
        if (adminPasswordChangeErrorLabel != null) {
            adminPasswordChangeErrorLabel.setText(message);
            adminPasswordChangeErrorLabel.setVisible(true);
            adminPasswordChangeErrorLabel.setManaged(true);
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyDialogStyles(alert.getDialogPane());
        alert.showAndWait();
    }

    private void applyDialogStyles(DialogPane dialogPane) {
        try {
            URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
            if (cssUrl != null) {
                dialogPane.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) { System.err.println("WARN_DIALOG_CSS: Failed to load CSS for dialog: " + e.getMessage()); }
    }
    private AdminDashboardController dashboardController; // Biến này cần được khai báo

    // Thêm phương thức này nếu chưa có hoặc đảm bảo nó là public
    public void setDashboardController(AdminDashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }
}
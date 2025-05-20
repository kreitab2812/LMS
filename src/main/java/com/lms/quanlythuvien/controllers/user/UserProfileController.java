package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.user.UserService;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.utils.session.SessionManager;
import com.lms.quanlythuvien.utils.security.PasswordUtils;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
// import javafx.stage.FileChooser; // Bỏ comment nếu triển khai upload avatar
import javafx.util.StringConverter;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException; // <<< ĐÃ THÊM IMPORT
import java.util.ArrayList;
import java.util.Collections; // Cho Collections.sort
import java.util.HashSet;
import java.util.List;
import java.util.Optional; // <<< ĐÃ THÊM IMPORT
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class UserProfileController implements Initializable {

    @FXML private ImageView profileAvatarImageView;
    // @FXML private Button changeAvatarButton;
    @FXML private Label profileUsernameLabel;
    @FXML private TextField profileEmailField;
    @FXML private TextField profileFullNameField;
    @FXML private TextField profilePhoneNumberField;
    @FXML private DatePicker profileDobPicker;
    @FXML private TextArea profileBioTextArea;
    @FXML private Button saveProfileButton;
    @FXML private Label profileInfoErrorLabel;

    @FXML private PasswordField oldPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmNewPasswordField;
    @FXML private Button changePasswordButton;
    @FXML private Label passwordChangeErrorLabel;

    @FXML private ListView<String> favoriteGenresListView;
    @FXML private Button saveFavoriteGenresButton;
    @FXML private Label favoriteGenresErrorLabel;

    private User currentUser;
    private UserService userService;
    private BookManagementService bookManagementService;
    private ObservableList<String> allAvailableGenres;
    private Set<String> currentFavoriteGenres = new HashSet<>();

    private UserDashboardController dashboardController;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void setDashboardController(UserDashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = UserService.getInstance();
        bookManagementService = BookManagementService.getInstance();
        currentUser = SessionManager.getInstance().getCurrentUser();

        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi Phiên", "Không thể tải thông tin. Vui lòng đăng nhập lại.");
            disableFormFields(true);
            return;
        }
        configureDobPicker();
        loadUserProfile();
        loadAndDisplayFavoriteGenres();
        clearAllErrors();
    }

    private void disableFormFields(boolean disable) {
        if(saveProfileButton != null) saveProfileButton.setDisable(disable);
        if(changePasswordButton != null) changePasswordButton.setDisable(disable);
        if(saveFavoriteGenresButton != null) saveFavoriteGenresButton.setDisable(disable);
        if(profileEmailField != null) profileEmailField.setEditable(!disable);
        if(profileFullNameField != null) profileFullNameField.setEditable(!disable);
        if(profilePhoneNumberField != null) profilePhoneNumberField.setEditable(!disable);
        if(profileDobPicker != null) profileDobPicker.setDisable(disable);
        if(profileBioTextArea != null) profileBioTextArea.setEditable(!disable);
        if(oldPasswordField != null) oldPasswordField.setDisable(disable);
        if(newPasswordField != null) newPasswordField.setDisable(disable);
        if(confirmNewPasswordField != null) confirmNewPasswordField.setDisable(disable);
    }

    private void configureDobPicker() {
        if (profileDobPicker == null) return;
        profileDobPicker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return (date != null) ? dateFormatter.format(date) : "";
            }
            @Override
            public LocalDate fromString(String string) {
                try {
                    return (string != null && !string.isEmpty()) ? LocalDate.parse(string, dateFormatter) : null;
                } catch (DateTimeParseException e) { // <<< ĐÃ CÓ IMPORT
                    System.err.println("Error parsing date from string: " + string + " - " + e.getMessage()); // <<< SỬ DỤNG e.getMessage()
                    return null;
                }
            }
        });
        profileDobPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now()));
            }
        });
    }

    private void loadUserProfile() {
        if (currentUser == null) return;
        // Sử dụng các phương thức ...OrDefault đã thêm vào User.java
        profileUsernameLabel.setText(currentUser.getUsernameOrDefault("N/A"));
        profileEmailField.setText(currentUser.getEmailOrDefault(""));
        profileFullNameField.setText(currentUser.getFullNameOrDefault(""));
        profilePhoneNumberField.setText(currentUser.getPhoneNumberOrDefault(""));
        profileBioTextArea.setText(currentUser.getIntroductionOrDefault(""));
        profileDobPicker.setValue(currentUser.getDateOfBirth());
        loadUserAvatarImage();
    }

    private void loadUserAvatarImage() {
        // ... (Giữ nguyên logic loadUserAvatarImage đã sửa ở lượt #19)
        if (profileAvatarImageView == null) return;
        Image imageToSet = null;
        String avatarPath = (currentUser != null) ? currentUser.getAvatarUrl() : null;

        if (avatarPath != null && !avatarPath.isEmpty()) {
            try {
                File avatarFile = new File(avatarPath);
                if (avatarFile.exists() && avatarFile.isFile() && avatarFile.canRead()) {
                    imageToSet = new Image(avatarFile.toURI().toString(), true);
                } else {
                    imageToSet = new Image(avatarPath, true);
                }
                if (imageToSet.isError()) imageToSet = null;
            } catch (Exception e) {
                System.err.println("WARN_UPC_AVATAR: Error loading avatar from: " + avatarPath);
                imageToSet = null;
            }
        }
        if (imageToSet == null) {
            try (InputStream defaultStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_avatar.png")) {
                if (defaultStream != null) imageToSet = new Image(defaultStream);
                else System.err.println("ERROR_UPC_AVATAR: Default avatar resource not found.");
            } catch (Exception e) { System.err.println("ERROR_UPC_AVATAR: Failed to load default avatar: " + e.getMessage());}
        }
        profileAvatarImageView.setImage(imageToSet);
    }

    @FXML
    private void handleChangeAvatar(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Tính năng chưa hoàn thiện", "Chức năng thay đổi ảnh đại diện sẽ được phát triển sau.");
    }

    @FXML
    private void handleSaveProfileChanges(ActionEvent event) {
        clearAllErrors(); // Gọi đúng tên hàm
        if (currentUser == null) return;

        String email = profileEmailField.getText().trim();
        if (email.isEmpty() || !email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showProfileError("Email không hợp lệ."); return;
        }

        if (!email.equalsIgnoreCase(currentUser.getEmail())) {
            Optional<User> userWithNewEmail = userService.findUserByEmail(email);
            if (userWithNewEmail.isPresent() && !userWithNewEmail.get().getUserId().equals(currentUser.getUserId())) { // Sử dụng isPresent() và get()
                showProfileError("Địa chỉ email này đã được sử dụng."); return;
            }
        }

        currentUser.setEmail(email);
        currentUser.setFullName(profileFullNameField.getText().trim());
        currentUser.setPhoneNumber(profilePhoneNumberField.getText().trim());
        currentUser.setDateOfBirth(profileDobPicker.getValue());
        currentUser.setIntroduction(profileBioTextArea.getText().trim());

        if (userService.updateUser(currentUser)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật thông tin cá nhân.");
            if (dashboardController != null) {
                dashboardController.populateTopBarUserInfo(); // Hàm này trong UserDashboardController phải là public
            }
            SessionManager.getInstance().setCurrentUser(currentUser); // Cập nhật lại user trong session
        } else {
            showProfileError("Không thể cập nhật thông tin. Email có thể đã được người khác sử dụng.");
            Optional<User> refreshedUserOpt = userService.findUserById(currentUser.getUserId());
            if (refreshedUserOpt.isPresent()) { // Sử dụng isPresent()
                User refreshedUser = refreshedUserOpt.get(); // Sử dụng get()
                SessionManager.getInstance().setCurrentUser(refreshedUser);
                this.currentUser = refreshedUser;
                loadUserProfile();
            }
        }
    }

    @FXML
    private void handleChangePassword(ActionEvent event) {
        clearAllErrors(); // Gọi đúng tên hàm
        if (currentUser == null) return;

        String oldPass = oldPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmNewPasswordField.getText();

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showPasswordError("Vui lòng điền đầy đủ các trường mật khẩu."); return;
        }
        if (newPass.length() < 7) {
            showPasswordError("Mật khẩu mới phải có ít nhất 7 ký tự."); return;
        }
        if (!newPass.equals(confirmPass)) {
            showPasswordError("Mật khẩu mới và xác nhận không khớp."); return;
        }
        if (!PasswordUtils.verifyPassword(oldPass, currentUser.getPasswordHash())) {
            showPasswordError("Mật khẩu cũ không chính xác."); return;
        }

        String newHashedPassword = PasswordUtils.hashPassword(newPass);
        if (userService.changePassword(currentUser.getUserId(), newHashedPassword)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã thay đổi mật khẩu thành công.");
            oldPasswordField.clear(); newPasswordField.clear(); confirmNewPasswordField.clear();

            Optional<User> refreshedUserOpt = userService.findUserById(currentUser.getUserId());
            // Sửa lỗi: dùng ifPresent đúng cách
            refreshedUserOpt.ifPresent(refreshedUser -> { // refreshedUser là User, không phải Optional<User>
                SessionManager.getInstance().setCurrentUser(refreshedUser);
                this.currentUser = refreshedUser; // Cập nhật local currentUser
            });
        } else {
            showPasswordError("Không thể thay đổi mật khẩu. Vui lòng thử lại.");
        }
    }

    private void loadAndDisplayFavoriteGenres() {
        if (favoriteGenresListView == null || bookManagementService == null || currentUser == null) return;

        // TODO: UserService cần hàm getFavoriteGenres(String userId) trả về Set<String>
        // Tạm thời khởi tạo currentFavoriteGenres là rỗng hoặc load từ một nguồn tạm
        // this.currentFavoriteGenres = userService.getFavoriteGenres(currentUser.getUserId());
        this.currentFavoriteGenres = new HashSet<>(); // Ví dụ
        System.out.println("UserProfileController: Favorite genres (placeholder) loaded: " + currentFavoriteGenres.size());

        allAvailableGenres = FXCollections.observableArrayList(bookManagementService.getAllDistinctCategories());
        favoriteGenresListView.setItems(allAvailableGenres);

        favoriteGenresListView.setCellFactory(CheckBoxListCell.forListView(item -> {
            BooleanProperty observable = new SimpleBooleanProperty();
            observable.set(currentFavoriteGenres.contains(item));
            observable.addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) currentFavoriteGenres.add(item);
                else currentFavoriteGenres.remove(item);
            });
            return observable;
        }));
        favoriteGenresListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        if (allAvailableGenres.isEmpty()) {
            favoriteGenresListView.setPlaceholder(new Label("Chưa có thể loại nào trong thư viện."));
        }
    }

    @FXML
    private void handleSaveFavoriteGenres(ActionEvent event) {
        clearAllErrors(); // Gọi đúng tên hàm
        if (currentUser == null || favoriteGenresListView == null) return;

        System.out.println("Saving favorite genres for user " + currentUser.getUserId() + ": " + currentFavoriteGenres);

        // TODO: UserService cần hàm updateUserFavoriteGenres(String userId, Set<String> genres)
        // boolean success = userService.updateUserFavoriteGenres(currentUser.getUserId(), currentFavoriteGenres);
        // if (success) { showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã lưu sở thích thể loại."); }
        // else { showFavoriteGenresError("Không thể lưu sở thích thể loại."); }
        showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Chức năng lưu sở thích sẽ được hoàn thiện (lựa chọn đã được log).");
    }

    private void clearAllErrors() { // Đảm bảo phương thức này được định nghĩa đúng
        if(profileInfoErrorLabel!=null) { profileInfoErrorLabel.setText(""); profileInfoErrorLabel.setVisible(false); profileInfoErrorLabel.setManaged(false); }
        if(passwordChangeErrorLabel!=null) { passwordChangeErrorLabel.setText(""); passwordChangeErrorLabel.setVisible(false); passwordChangeErrorLabel.setManaged(false); }
        if(favoriteGenresErrorLabel!=null) { favoriteGenresErrorLabel.setText(""); favoriteGenresErrorLabel.setVisible(false); favoriteGenresErrorLabel.setManaged(false); }
    }

    private void showProfileError(String message) {
        if (profileInfoErrorLabel != null) {
            profileInfoErrorLabel.setText(message);
            profileInfoErrorLabel.setVisible(true);
            profileInfoErrorLabel.setManaged(true);
        }
    }
    private void showPasswordError(String message) {
        if (passwordChangeErrorLabel != null) {
            passwordChangeErrorLabel.setText(message);
            passwordChangeErrorLabel.setVisible(true);
            passwordChangeErrorLabel.setManaged(true);
        }
    }
    private void showFavoriteGenresError(String message) {
        if (favoriteGenresErrorLabel != null) {
            favoriteGenresErrorLabel.setText(message);
            favoriteGenresErrorLabel.setVisible(true);
            favoriteGenresErrorLabel.setManaged(true);
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

    private void applyDialogStyles(DialogPane dialogPane) { // Nhận DialogPane
        try {
            URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
            if (cssUrl != null) {
                dialogPane.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) { System.err.println("WARN_DIALOG_CSS: Failed to load CSS for dialog: " + e.getMessage()); }
    }
}
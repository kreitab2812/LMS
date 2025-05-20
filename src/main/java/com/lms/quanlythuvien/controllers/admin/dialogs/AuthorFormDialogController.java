package com.lms.quanlythuvien.controllers.admin.dialogs;

import com.lms.quanlythuvien.models.item.Author;
import com.lms.quanlythuvien.services.library.AuthorManagementService;
// Import MainApp nếu cần dùng Alert có CSS chung
// import com.lms.quanlythuvien.MainApp;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable; // Thêm Initializable nếu cần
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType; // Cần cho Dialog
import javafx.scene.control.DialogPane; // Cần cho applyDialogStyles
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL; // Cần cho applyDialogStyles
import java.util.Optional; // Cần cho Optional<Author>
import java.util.ResourceBundle; // Thêm nếu dùng Initializable

public class AuthorFormDialogController implements Initializable {

    @FXML private Label dialogTitleLabel;
    @FXML private TextField nameField;
    @FXML private TextArea bioArea;
    @FXML private TextField yobField; // Year of Birth
    @FXML private TextField yodField; // Year of Death
    @FXML private TextField genderField;
    @FXML private TextField nationalityField;
    @FXML private TextField placeOfBirthField;
    @FXML private TextField avatarUrlField;
    @FXML private Label errorFormLabel;
    @FXML private Button saveAuthorButton;
    // @FXML private Button cancelAuthorButton; // Nút Cancel đã được ButtonBar xử lý

    private Stage dialogStage;
    private Author currentAuthor; // Tác giả đang được sửa, hoặc null nếu thêm mới
    private boolean saved = false; // Cờ để báo cho controller cha biết có lưu thành công không
    private AuthorManagementService authorService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        authorService = AuthorManagementService.getInstance();
        clearError(); // Ẩn lỗi ban đầu

        // Validate nút Save dựa trên trường tên (bắt buộc)
        if (nameField != null && saveAuthorButton != null) {
            saveAuthorButton.setDisable(nameField.getText().trim().isEmpty());
            nameField.textProperty().addListener((obs, oldVal, newVal) -> {
                saveAuthorButton.setDisable(newVal.trim().isEmpty());
            });
        }
        Platform.runLater(() -> nameField.requestFocus()); // Focus vào trường tên khi mở dialog
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * Thiết lập dữ liệu tác giả cho form (khi chỉnh sửa) hoặc để trống (khi thêm mới).
     * @param author Tác giả để chỉnh sửa, hoặc null nếu thêm mới.
     */
    public void setAuthorData(Author author) {
        this.currentAuthor = author;
        if (author != null) { // Chế độ sửa
            if (dialogTitleLabel != null) dialogTitleLabel.setText("Chỉnh Sửa Thông Tin Tác Giả");
            if (nameField != null) nameField.setText(author.getName());
            if (bioArea != null) bioArea.setText(author.getBiography());
            if (yobField != null && author.getYearOfBirth() != null) yobField.setText(String.valueOf(author.getYearOfBirth()));
            if (yodField != null && author.getYearOfDeath() != null) yodField.setText(String.valueOf(author.getYearOfDeath()));
            if (genderField != null) genderField.setText(author.getGender());
            if (nationalityField != null) nationalityField.setText(author.getNationality());
            if (placeOfBirthField != null) placeOfBirthField.setText(author.getPlaceOfBirth());
            if (avatarUrlField != null) avatarUrlField.setText(author.getAvatarUrl());
        } else { // Chế độ thêm mới
            if (dialogTitleLabel != null) dialogTitleLabel.setText("Thêm Tác Giả Mới");
            // Các trường sẽ trống (do prompt text trong FXML)
        }
        // Validate lại nút save sau khi điền dữ liệu
        if (nameField != null && saveAuthorButton != null) {
            saveAuthorButton.setDisable(nameField.getText().trim().isEmpty());
        }
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleSaveAuthor(ActionEvent event) {
        clearError();
        if (!validateInput()) {
            return;
        }

        String name = nameField.getText().trim();
        String biography = bioArea.getText().trim();
        Integer yearOfBirth = parseInteger(yobField.getText());
        Integer yearOfDeath = parseInteger(yodField.getText());
        String gender = genderField.getText().trim();
        String nationality = nationalityField.getText().trim();
        String placeOfBirth = placeOfBirthField.getText().trim();
        String avatarUrl = avatarUrlField.getText().trim();

        if (yearOfBirth != null && yearOfDeath != null && yearOfDeath < yearOfBirth) {
            showError("Năm mất không thể nhỏ hơn năm sinh.");
            return;
        }

        boolean successOperation;
        String successMessage;

        if (currentAuthor == null) { // Thêm mới
            Author newAuthor = new Author(name, biography, yearOfBirth, yearOfDeath, gender, nationality, placeOfBirth, avatarUrl);
            Optional<Author> addedAuthorOpt = authorService.addAuthor(newAuthor);
            successOperation = addedAuthorOpt.isPresent();
            if (successOperation) {
                this.currentAuthor = addedAuthorOpt.get(); // Lưu lại tác giả đã thêm (có ID)
                successMessage = "Đã thêm tác giả mới thành công: " + name;
            } else {
                showError("Lỗi khi thêm tác giả. Tên tác giả có thể đã tồn tại.");
                return;
            }
        } else { // Chỉnh sửa
            currentAuthor.setName(name);
            currentAuthor.setBiography(biography);
            currentAuthor.setYearOfBirth(yearOfBirth);
            currentAuthor.setYearOfDeath(yearOfDeath);
            currentAuthor.setGender(gender);
            currentAuthor.setNationality(nationality);
            currentAuthor.setPlaceOfBirth(placeOfBirth);
            currentAuthor.setAvatarUrl(avatarUrl);
            // AuthorManagementService.updateAuthor sẽ tự cập nhật `updatedAt`
            successOperation = authorService.updateAuthor(currentAuthor);
            if (successOperation) {
                successMessage = "Đã cập nhật thông tin tác giả thành công: " + name;
            } else {
                showError("Lỗi khi cập nhật tác giả. Tên mới có thể bị trùng hoặc không có thay đổi nào được ghi nhận.");
                return;
            }
        }

        if (successOperation) {
            saved = true; // Đánh dấu đã lưu thành công
            showAlert(Alert.AlertType.INFORMATION, "Thành công", successMessage);
            closeDialog();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeDialog();
    }

    private boolean validateInput() {
        if (nameField.getText().trim().isEmpty()) {
            showError("Tên tác giả không được để trống.");
            nameField.requestFocus();
            return false;
        }
        if (!yobField.getText().trim().isEmpty() && parseInteger(yobField.getText()) == null) {
            showError("Năm sinh phải là số nguyên hợp lệ (hoặc để trống).");
            yobField.requestFocus();
            return false;
        }
        if (!yodField.getText().trim().isEmpty() && parseInteger(yodField.getText()) == null) {
            showError("Năm mất phải là số nguyên hợp lệ (hoặc để trống).");
            yodField.requestFocus();
            return false;
        }
        return true;
    }

    private Integer parseInteger(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null; // Trả về null nếu không parse được
        }
    }

    private void showError(String message) {
        if (errorFormLabel != null) {
            errorFormLabel.setText(message);
            errorFormLabel.setVisible(true);
            errorFormLabel.setManaged(true);
        } else { // Fallback nếu label không có
            showAlert(Alert.AlertType.ERROR, "Lỗi Nhập Liệu", message);
        }
    }

    private void clearError() {
        if (errorFormLabel != null) {
            errorFormLabel.setText("");
            errorFormLabel.setVisible(false);
            errorFormLabel.setManaged(false);
        }
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        // Áp dụng CSS cho Alert
        if (dialogStage != null && dialogStage.getOwner() != null && dialogStage.getOwner().getScene() != null &&
                !dialogStage.getOwner().getScene().getStylesheets().isEmpty()) {
            alert.getDialogPane().getStylesheets().addAll(dialogStage.getOwner().getScene().getStylesheets());
        } else { // Fallback nếu không lấy được stylesheet từ owner
            try {
                URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
                if (cssUrl != null) {
                    alert.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
                }
            } catch (Exception e) { System.err.println("WARN_DIALOG_CSS: Failed to load default CSS for alert: " + e.getMessage()); }
        }
        alert.showAndWait();
    }
}
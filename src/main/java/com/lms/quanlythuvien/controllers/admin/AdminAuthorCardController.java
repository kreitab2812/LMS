package com.lms.quanlythuvien.controllers.admin;

import com.lms.quanlythuvien.models.item.Author; // Đổi tên model cho khớp
import com.lms.quanlythuvien.services.library.AuthorManagementService;
import com.lms.quanlythuvien.exceptions.DeletionRestrictedException;
import com.lms.quanlythuvien.utils.session.SessionManager;
import com.lms.quanlythuvien.MainApp; // Cần để lấy CSS

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog; // Cần cho applyDialogStyles
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
// import javafx.scene.layout.VBox; // Nếu root element có fx:id

import java.io.InputStream;
import java.net.URL;
import java.util.Optional;

public class AdminAuthorCardController {

    // @FXML private VBox adminAuthorCardRoot; // Chỉ cần nếu root element có fx:id và cần tương tác
    @FXML private ImageView avatarImageView;
    @FXML private Label nameLabel;
    @FXML private Label bookCountLabel; // Hiển thị số lượng tác phẩm
    // @FXML private Button viewDetailsButton; // Có thể không cần nếu click vào card là xem chi tiết
    @FXML private Button editAuthorButton;
    @FXML private Button deleteAuthorButton;

    private Author currentAuthor;
    private Image defaultAuthorAvatar;
    private AdminAuthorManagementController authorManagementController; // Parent controller

    private AuthorManagementService authorService;
    // BookManagementService không cần ở đây nữa nếu bookCount được truyền vào setData

    public AdminAuthorCardController() {
        authorService = AuthorManagementService.getInstance();
        loadDefaultAvatar();
    }

    private void loadDefaultAvatar() {
        try (InputStream defaultStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_author_avatar.png")) {
            if (defaultStream != null) {
                defaultAuthorAvatar = new Image(defaultStream);
            } else {
                System.err.println("ERROR_AACC: Default author avatar image not found.");
            }
        } catch (Exception e) {
            System.err.println("ERROR_AACC: Exception loading default author avatar: " + e.getMessage());
        }
    }

    // Sửa lại để nhận bookCount đã được tính toán trước từ parent
    public void setData(Author author, int bookCount, AdminAuthorManagementController parentController) {
        this.currentAuthor = author;
        this.authorManagementController = parentController;
        if (author == null) {
            nameLabel.setText("N/A");
            bookCountLabel.setText("");
            if(avatarImageView != null && defaultAuthorAvatar != null) avatarImageView.setImage(defaultAuthorAvatar);
            editAuthorButton.setDisable(true);
            deleteAuthorButton.setDisable(true);
            return;
        }

        nameLabel.setText(author.getName());
        loadAvatarImage(author.getAvatarUrl());

        bookCountLabel.setText("(" + bookCount + " tác phẩm)");
        editAuthorButton.setDisable(false);
        deleteAuthorButton.setDisable(false);

        // Xử lý click vào card để xem chi tiết
        // if (adminAuthorCardRoot != null) {
        //    adminAuthorCardRoot.setOnMouseClicked(event -> handleViewAuthorDetailsAction(null));
        // }
    }

    private void loadAvatarImage(String imageUrl) {
        if (avatarImageView == null) return;
        Image imageToSet = this.defaultAuthorAvatar;
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            String finalImageUrl = imageUrl.trim();
            if (finalImageUrl.startsWith("//")) finalImageUrl = "https:" + finalImageUrl;
            try {
                Image loadedImage = new Image(finalImageUrl, true);
                if (loadedImage.isError()) {
                    System.err.println("WARN_AACC_AVATAR: Error loading avatar from URL: " + finalImageUrl);
                } else {
                    imageToSet = loadedImage;
                }
            } catch (Exception e) {
                System.err.println("ERROR_AACC_AVATAR: Exception loading avatar from URL: " + finalImageUrl + ". Error: " + e.getMessage());
            }
        }
        avatarImageView.setImage(imageToSet);
    }

    @FXML
    void handleViewAuthorDetailsAction(ActionEvent event) { // Hoặc MouseEvent
        if (currentAuthor != null && authorManagementController != null && authorManagementController.getDashboardController() != null) {
            System.out.println("Admin views details for author: " + currentAuthor.getName());
            SessionManager.getInstance().setSelectedAuthor(currentAuthor);
            // SessionManager.getInstance().setAdminViewingAuthorDetail(true); // Cần thêm cờ này vào SessionManager nếu dùng
            authorManagementController.getDashboardController().loadAdminViewIntoCenter("user/AuthorDetailView.fxml");
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi Điều Hướng", "Không thể mở chi tiết tác giả.");
        }
    }

    @FXML
    void handleEditAuthorAction(ActionEvent event) {
        if (currentAuthor != null && authorManagementController != null) {
            authorManagementController.openAuthorFormDialog(currentAuthor);
        }
    }

    @FXML
    void handleDeleteAuthorAction(ActionEvent event) {
        if (currentAuthor == null || authorManagementController == null) return;

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Xác nhận Xóa Tác Giả");
        confirmation.setHeaderText("Xóa tác giả: " + currentAuthor.getName() + "?");
        confirmation.setContentText("Hành động này có thể không thành công nếu tác giả còn sách liên kết (do ràng buộc ON DELETE RESTRICT).");
        applyDialogStyles(confirmation);

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (authorService.deleteAuthor(currentAuthor.getId())) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã xóa tác giả: " + currentAuthor.getName());
                    authorManagementController.refreshAuthorsDisplay();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi Xóa", "Không thể xóa tác giả (có thể không tìm thấy).");
                }
            } catch (DeletionRestrictedException e) {
                showAlert(Alert.AlertType.WARNING, "Không thể xóa", e.getMessage());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi Hệ thống", "Lỗi không xác định khi xóa tác giả: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void applyDialogStyles(Dialog<?> dialog) {
        try {
            URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
            if (cssUrl != null) {
                dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) { System.err.println("WARN_DIALOG_CSS: Failed to load CSS for dialog: " + e.getMessage());}
    }
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyDialogStyles(alert);
        alert.showAndWait();
    }
}
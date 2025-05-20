package com.lms.quanlythuvien.controllers.admin;

import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.exceptions.DeletionRestrictedException;
import com.lms.quanlythuvien.utils.session.SessionManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Window;

import java.io.InputStream;
import java.net.URL;
import java.util.Optional;

public class AdminBookCardController {

    @FXML private ImageView coverImageView;
    @FXML private Label titleLabel;
    @FXML private Label authorsLabel;
    @FXML private Label isbnLabel;
    @FXML private Label quantityLabel;
    @FXML private Label borrowedCountLabel;
    @FXML private Button editBookButton;
    @FXML private Button deleteBookButton;
    @FXML private Button viewDetailsButton;
    @FXML private Button viewLoanHistoryButton;

    private Book currentBook;
    private Image defaultBookCoverImage;
    private AdminBookManagementController bookManagementController;
    private BookManagementService bookManagementService;

    public AdminBookCardController() {
        bookManagementService = BookManagementService.getInstance();
        loadDefaultCover();
    }

    private void loadDefaultCover() {
        try (InputStream defaultStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_book_cover.png")) {
            if (defaultStream != null) {
                defaultBookCoverImage = new Image(defaultStream);
            } else {
                System.err.println("ERROR_ABCC: Default book cover image not found.");
            }
        } catch (Exception e) {
            System.err.println("ERROR_ABCC: Exception loading default book cover: " + e.getMessage());
        }
    }

    public void setData(Book book, AdminBookManagementController parentController) {
        this.currentBook = book;
        this.bookManagementController = parentController;
        if (book == null) {
            titleLabel.setText("N/A");
            authorsLabel.setText("");
            isbnLabel.setText("");
            quantityLabel.setText("");
            borrowedCountLabel.setText("");
            if(coverImageView != null && defaultBookCoverImage != null) coverImageView.setImage(defaultBookCoverImage);
            editBookButton.setDisable(true);
            deleteBookButton.setDisable(true);
            if (viewDetailsButton != null) viewDetailsButton.setDisable(true);
            if (viewLoanHistoryButton != null) viewLoanHistoryButton.setDisable(true);
            return;
        }

        titleLabel.setText(book.getTitle());
        authorsLabel.setText(book.getAuthorsFormatted("N/A"));
        isbnLabel.setText("Mã: " + book.getIsbn13OrDefault("N/A"));
        quantityLabel.setText("SL: " + book.getAvailableQuantity() + "/" + book.getTotalQuantity());

        int currentlyBorrowed = bookManagementService.getBorrowedCountForBook(book.getInternalId());
        borrowedCountLabel.setText("Đang mượn: " + currentlyBorrowed);
        if (currentlyBorrowed > 0) {
            borrowedCountLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-weight:bold;");
        } else {
            borrowedCountLabel.setStyle("");
        }

        loadCoverImage(book.getThumbnailUrl());
        editBookButton.setDisable(false);
        deleteBookButton.setDisable(false);
        if (viewDetailsButton != null) viewDetailsButton.setDisable(false);
        if (viewLoanHistoryButton != null) viewLoanHistoryButton.setDisable(false);
    }

    private void loadCoverImage(String imageUrl) {
        if (coverImageView == null) return;
        Image imageToSet = this.defaultBookCoverImage;
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            String finalImageUrl = imageUrl.trim();
            if (finalImageUrl.startsWith("//")) finalImageUrl = "https:" + finalImageUrl;
            try {
                Image loadedImage = new Image(finalImageUrl, true);
                if (loadedImage.isError()) {
                    System.err.println("WARN_ABCC_COVER: Error loading image from URL: " + finalImageUrl + " - " + loadedImage.getException().getMessage());
                } else {
                    imageToSet = loadedImage;
                }
            } catch (Exception e) {
                System.err.println("ERROR_ABCC_COVER: Exception loading image from URL: " + finalImageUrl + ". Error: " + e.getMessage());
            }
        }
        coverImageView.setImage(imageToSet);
    }

    private void closeWindow() {
        Window window = titleLabel.getScene().getWindow();
        if (window != null) {
            window.hide();
        }
    }

    @FXML
    void handleViewDetailsAction(ActionEvent event) {
        if (currentBook != null && bookManagementController != null && bookManagementController.getDashboardController() != null) {
            SessionManager.getInstance().setSelectedBook(currentBook); // Dùng cho BookDetailView
            bookManagementController.getDashboardController().loadAdminViewIntoCenter("user/BookDetailView.fxml");
            closeWindow();
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi Điều Hướng", "Không thể mở chi tiết sách.");
        }
    }

    @FXML
    void handleViewLoanHistoryAction(ActionEvent event) {
        if (currentBook != null && bookManagementController != null && bookManagementController.getDashboardController() != null) {
            System.out.println("AdminBookCard: Requesting loan history for book: " + currentBook.getTitle());

            // SỬ DỤNG PHƯƠNG THỨC ĐÚNG CỦA SESSIONMANAGER
            SessionManager.getInstance().setSelectedBookForHistory(currentBook);

            bookManagementController.getDashboardController().loadAdminViewIntoCenter("admin/BookLoanHistoryView.fxml");
            closeWindow();
        } else {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Không thể xem lịch sử mượn sách.");
        }
    }

    @FXML
    void handleEditBookAction(ActionEvent event) {
        if (currentBook != null && bookManagementController != null) {
            closeWindow();
            bookManagementController.openBookFormDialog(currentBook);
        }
    }

    @FXML
    void handleDeleteBookAction(ActionEvent event) {
        if (currentBook == null || bookManagementController == null) return;

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Xác nhận Xóa Sách");
        confirmation.setHeaderText("Xóa sách: " + currentBook.getTitle() + "?");
        confirmation.setContentText("Bạn có chắc chắn muốn xóa cuốn sách này?");
        applyDialogStyles(confirmation);

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String identifierToDelete = currentBook.getId() != null ? currentBook.getId() : String.valueOf(currentBook.getInternalId());
                if (bookManagementService.deleteBookFromLibrary(identifierToDelete)) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã xóa sách thành công.");
                    closeWindow();
                    bookManagementController.refreshBookDisplay();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi Xóa Sách", "Không thể xóa sách.");
                }
            } catch (DeletionRestrictedException e) {
                showAlert(Alert.AlertType.WARNING, "Không thể xóa", e.getMessage());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi Hệ thống", "Lỗi không xác định: " + e.getMessage());
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
        } catch (Exception e) { System.err.println("WARN_DIALOG_CSS: Failed to load CSS for dialog (AdminBookCardController): " + e.getMessage()); }
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
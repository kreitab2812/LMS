package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.MainApp; // Để gọi loadScene cho chi tiết sách
import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.utils.session.SessionManager; // Để truyền thông tin sách được chọn

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;


import java.io.InputStream;

public class BookCardLibraryController {

    @FXML private VBox bookCardRoot;
    @FXML private ImageView coverImageView;
    @FXML private Label titleLabel;
    @FXML private Label authorsLabel;
    @FXML private Label availabilityLabel;
    // @FXML private Button requestButton; // Nếu có nút riêng

    private Book currentBook;
    private Image defaultBookCoverImage;
    // private UserLibraryController parentController; // Có thể không cần nếu chỉ điều hướng

    public BookCardLibraryController() {
        try (InputStream defaultStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_book_cover.png")) {
            if (defaultStream != null) {
                defaultBookCoverImage = new Image(defaultStream);
            }
        } catch (Exception e) {
            System.err.println("Error loading default book cover for library card: " + e.getMessage());
        }
    }

    public void setData(Book book) {
        this.currentBook = book;
        if (book == null) return;

        titleLabel.setText(book.getTitle());
        authorsLabel.setText(book.getAuthors() != null && !book.getAuthors().isEmpty() ? String.join(", ", book.getAuthors()) : "N/A");
        availabilityLabel.setText("Còn: " + book.getAvailableQuantity() + " / Tổng: " + book.getTotalQuantity());

        if (book.getAvailableQuantity() <= 0) {
            availabilityLabel.setStyle("-fx-text-fill: #D8000C;"); // Màu đỏ nếu hết sách
        } else {
            availabilityLabel.setStyle("-fx-text-fill: #007e3f;"); // Màu xanh nếu còn sách
        }

        loadCoverImage(book.getThumbnailUrl());
    }

    private void loadCoverImage(String imageUrl) {
        if (coverImageView == null) return;
        Image imageToSet = this.defaultBookCoverImage;
        if (imageUrl != null && !imageUrl.isEmpty()) {
            String finalImageUrl = imageUrl.startsWith("//") ? "https:" + imageUrl : imageUrl;
            try {
                Image loadedImage = new Image(finalImageUrl, true);
                if (!loadedImage.isError()) {
                    imageToSet = loadedImage;
                }
                loadedImage.errorProperty().addListener((obs, oldError, newError) -> {
                    if (newError) coverImageView.setImage(this.defaultBookCoverImage);
                });
                if(loadedImage.isError()) coverImageView.setImage(this.defaultBookCoverImage);
            } catch (Exception e) { /* Dùng ảnh mặc định */ }
        }
        coverImageView.setImage(imageToSet);
    }

    @FXML
    void handleCardClicked(MouseEvent event) {
        if (currentBook != null) {
            System.out.println("Book card clicked: " + currentBook.getTitle());
            // Lưu thông tin sách được chọn vào SessionManager (hoặc một cách khác)
            // để BookDetailController có thể lấy ra hiển thị
            SessionManager.getInstance().setSelectedBook(currentBook); // << CẦN THÊM PHƯƠNG THỨC NÀY VÀO SESSIONMANAGER
            MainApp.loadScene("user/BookDetailView.fxml"); // Chuyển đến màn hình chi tiết sách
        }
    }

    // @FXML void handleRequestBookAction(ActionEvent event) { ... } // Nếu có nút yêu cầu riêng
}
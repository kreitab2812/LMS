package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.utils.session.SessionManager;

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

    private Book currentBook;
    private Image defaultBookCoverImage;
    // --- SỬA ĐỔI Ở ĐÂY ---
    private Object parentNavigationContext; // Có thể là UserLibraryController hoặc MyBookshelfController

    public BookCardLibraryController() {
        try (InputStream defaultStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_book_cover.png")) {
            if (defaultStream != null) {
                defaultBookCoverImage = new Image(defaultStream);
            } else {
                System.err.println("ERROR_BOOK_CARD_LIB: Default book cover image not found.");
            }
        } catch (Exception e) {
            System.err.println("ERROR_BOOK_CARD_LIB: Exception loading default book cover: " + e.getMessage());
        }
    }

    // --- SỬA ĐỔI CHỮ KÝ PHƯƠNG THỨC setData ---
    public void setData(Book book, Object parentContext) {
        this.currentBook = book;
        this.parentNavigationContext = parentContext; // Gán parent controller mới

        if (book == null) {
            if (bookCardRoot != null) {
                bookCardRoot.setVisible(false);
                bookCardRoot.setManaged(false);
            }
            return;
        }
        if (bookCardRoot != null) {
            bookCardRoot.setVisible(true);
            bookCardRoot.setManaged(true);
        }

        titleLabel.setText(book.getTitleOrDefault("N/A")); // Sử dụng getTitleOrDefault
        authorsLabel.setText(book.getAuthorsFormatted("N/A")); // Sử dụng getAuthorsFormatted

        updateAvailabilityLabel();
        loadCoverImage(book.getThumbnailUrl());
    }

    public void updateAvailabilityLabel() {
        if (currentBook != null && availabilityLabel != null) {
            availabilityLabel.setText("Còn: " + currentBook.getAvailableQuantity() + "/" + currentBook.getTotalQuantity());
            availabilityLabel.getStyleClass().removeAll("availability-low", "availability-out", "availability-good");
            if (currentBook.getAvailableQuantity() <= 0) {
                availabilityLabel.getStyleClass().add("availability-out");
            } else if (currentBook.getAvailableQuantity() < 5) { // Ngưỡng "sắp hết" có thể tùy chỉnh
                availabilityLabel.getStyleClass().add("availability-low");
            } else {
                availabilityLabel.getStyleClass().add("availability-good");
            }
        }
    }

    private void loadCoverImage(String imageUrl) {
        if (coverImageView == null) return;
        Image imageToSet = this.defaultBookCoverImage;
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            String finalImageUrl = imageUrl.trim();
            if (finalImageUrl.startsWith("//")) finalImageUrl = "https:" + finalImageUrl; // Xử lý URL tương đối
            try {
                Image loadedImage = new Image(finalImageUrl, true); // true để tải nền
                if (loadedImage.isError()) {
                    System.err.println("WARN_BOOK_CARD_LIB_COVER: Error loading image from URL: " + finalImageUrl + " - " + loadedImage.getException().getMessage());
                    // Giữ ảnh mặc định nếu lỗi
                } else {
                    imageToSet = loadedImage;
                }
            } catch (Exception e) {
                System.err.println("ERROR_BOOK_CARD_LIB_COVER: Exception creating image from URL: " + finalImageUrl + ". Error: " + e.getMessage());
                // Giữ ảnh mặc định nếu có exception
            }
        }
        coverImageView.setImage(imageToSet);
    }

    @FXML
    void handleCardClicked(MouseEvent event) {
        if (currentBook != null && parentNavigationContext != null) {
            System.out.println("Book card library clicked: " + currentBook.getTitleOrDefault("N/A"));
            SessionManager.getInstance().setSelectedBook(currentBook); // Luôn set book vào session

            // --- SỬA ĐỔI LOGIC ĐIỀU HƯỚNG ---
            if (parentNavigationContext instanceof UserLibraryController) {
                ((UserLibraryController) parentNavigationContext).navigateToBookDetail();
            } else if (parentNavigationContext instanceof MyBookshelfController) {
                // MyBookshelfController có phương thức navigateToBookDetail(Book book)
                // nhưng vì đã setSelectedBook, có thể gọi một phương thức tương tự không cần tham số
                // hoặc MyBookshelfController cũng có thể có navigateToBookDetail() không tham số.
                // Tạm thời gọi trực tiếp với currentBook
                ((MyBookshelfController) parentNavigationContext).navigateToBookDetail(currentBook);
            } else {
                System.err.println("ERROR_BOOK_CARD_LIB_CLICK: Unknown parent controller type for navigation: " + parentNavigationContext.getClass().getName());
            }
        } else {
            System.err.println("ERROR_BOOK_CARD_LIB_CLICK: currentBook or parentNavigationContext is null.");
            if (currentBook == null) System.err.println(">> currentBook is null");
            if (parentNavigationContext == null) System.err.println(">> parentNavigationContext is null");
        }
    }
}
package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.transaction.BorrowingRecord;
import com.lms.quanlythuvien.models.transaction.LoanStatus;
import com.lms.quanlythuvien.services.transaction.BorrowingRecordService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox; // Thêm nếu dùng
import javafx.scene.layout.VBox;

import java.io.InputStream;
import java.net.URL; // Thêm import này
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class BookCardBorrowedController {

    @FXML private VBox bookCardRoot;
    @FXML private ImageView coverImageView;
    @FXML private Label titleLabel;
    @FXML private Label authorsLabel;
    @FXML private Label statusLabel;
    @FXML private Label borrowedOnLabel;
    @FXML private Label dueDateLabel;
    @FXML private Label daysRemainingLabel;
    @FXML private ProgressBar dueDateProgress;
    @FXML private Button returnButton;
    @FXML private HBox progressBox; // Thêm fx:id cho HBox chứa progress

    private BorrowingRecord currentRecord;
    private Book currentBook;
    private Image defaultBookCoverImage;
    private MyBookshelfController parentController;

    public BookCardBorrowedController() {
        try (InputStream defaultStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_book_cover.png")) {
            if (defaultStream != null) {
                defaultBookCoverImage = new Image(defaultStream);
            } else {
                System.err.println("ERROR_BCC: Default book cover image not found for card.");
            }
        } catch (Exception e) {
            System.err.println("ERROR_BCC: Exception loading default book cover for card: " + e.getMessage());
        }
    }

    public void setData(BorrowingRecord record, Book book) {
        this.currentRecord = record;
        this.currentBook = book;

        titleLabel.setText(book.getTitle());
        authorsLabel.setText(book.getAuthors() != null && !book.getAuthors().isEmpty() ? String.join(", ", book.getAuthors()) : "N/A");
        loadCoverImage(book.getThumbnailUrl());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        borrowedOnLabel.setText("Mượn ngày: " + record.getBorrowDate().format(formatter));
        dueDateLabel.setText("Hạn trả: " + record.getDueDate().format(formatter));

        updateStatusAndProgress(true); // true để hiển thị progress bar và nút trả

        returnButton.setVisible(true);
        returnButton.setManaged(true);
    }

    public void setDataForHistory(BorrowingRecord record, Book book) {
        this.currentRecord = record;
        this.currentBook = book;

        titleLabel.setText(book.getTitle());
        authorsLabel.setText(book.getAuthors() != null && !book.getAuthors().isEmpty() ? String.join(", ", book.getAuthors()) : "N/A");
        loadCoverImage(book.getThumbnailUrl());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        borrowedOnLabel.setText("Mượn ngày: " + record.getBorrowDate().format(formatter));

        if(record.getReturnDate() != null) {
            dueDateLabel.setText("Đã trả: " + record.getReturnDate().format(formatter));
        } else { // Fallback nếu vì lý do nào đó status là RETURNED nhưng returnDate null
            dueDateLabel.setText("Hạn trả: " + record.getDueDate().format(formatter));
        }

        updateStatusAndProgress(false); // false để ẩn progress bar và nút trả

        returnButton.setVisible(false);
        returnButton.setManaged(false);
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
                } else {
                    System.err.println("ERROR_BCC_LOAD_COVER: Error with image from URL (isError=true): " + finalImageUrl);
                }
                loadedImage.errorProperty().addListener((obs, oldError, newError) -> {
                    if (newError) {
                        System.err.println("ERROR_BCC_LOAD_COVER_ASYNC: Error loading image from URL: " + finalImageUrl);
                        coverImageView.setImage(this.defaultBookCoverImage);
                    }
                });
            } catch (Exception e) {
                System.err.println("CRITICAL_BCC_LOAD_COVER: Exception creating Image for URL: " + finalImageUrl + " - " + e.getMessage());
            }
        }
        coverImageView.setImage(imageToSet);
    }

    private void updateStatusAndProgress(boolean showProgressAndReturnButton) {
        if (currentRecord == null) return;
        LocalDate today = LocalDate.now();
        LocalDate dueDate = currentRecord.getDueDate();

        statusLabel.getStyleClass().removeAll("book-card-status-borrowed", "book-card-status-overdue", "book-card-status-returned");

        if (currentRecord.getStatus() == LoanStatus.RETURNED) {
            statusLabel.setText("ĐÃ TRẢ");
            statusLabel.getStyleClass().add("book-card-status-returned");
            if (daysRemainingLabel != null) daysRemainingLabel.setVisible(false);
            if (progressBox != null) { // Ẩn cả HBox chứa progress
                progressBox.setVisible(false);
                progressBox.setManaged(false);
            }
        } else if (currentRecord.isOverdue(today)) {
            statusLabel.setText("QUÁ HẠN");
            statusLabel.getStyleClass().add("book-card-status-overdue");
            long daysOverdue = ChronoUnit.DAYS.between(dueDate, today);
            if (daysRemainingLabel != null) {
                daysRemainingLabel.setText("Quá hạn " + daysOverdue + " ngày");
                daysRemainingLabel.setVisible(showProgressAndReturnButton);
            }
            if (progressBox != null) {
                progressBox.setVisible(showProgressAndReturnButton);
                progressBox.setManaged(showProgressAndReturnButton);
            }
            if (dueDateProgress != null && showProgressAndReturnButton) {
                dueDateProgress.setProgress(1.0);
                dueDateProgress.getStyleClass().add("progress-bar-overdue");
            }
        } else { // ACTIVE
            statusLabel.setText("ĐANG MƯỢN");
            statusLabel.getStyleClass().add("book-card-status-borrowed");
            long daysLeft = ChronoUnit.DAYS.between(today, dueDate);
            if (daysRemainingLabel != null) {
                daysRemainingLabel.setText(daysLeft >=0 ? "Còn " + daysLeft + " ngày" : "Quá hạn");
                daysRemainingLabel.setVisible(showProgressAndReturnButton);
            }

            if (progressBox != null) {
                progressBox.setVisible(showProgressAndReturnButton);
                progressBox.setManaged(showProgressAndReturnButton);
            }

            if (dueDateProgress != null && showProgressAndReturnButton) {
                long totalDaysOfLoan = ChronoUnit.DAYS.between(currentRecord.getBorrowDate(), dueDate);
                if (totalDaysOfLoan <= 0) totalDaysOfLoan = 1; // Tránh chia cho 0 hoặc số âm
                long daysPassed = ChronoUnit.DAYS.between(currentRecord.getBorrowDate(), today);
                double progress = (totalDaysOfLoan > 0) ? Math.max(0, (double) daysPassed / totalDaysOfLoan) : 0.0;
                dueDateProgress.setProgress(Math.min(progress, 1.0));
                dueDateProgress.getStyleClass().remove("progress-bar-overdue");
            }
        }

        if (returnButton != null) {
            returnButton.setVisible(showProgressAndReturnButton && currentRecord.getStatus() != LoanStatus.RETURNED);
            returnButton.setManaged(showProgressAndReturnButton && currentRecord.getStatus() != LoanStatus.RETURNED);
        }
    }

    public void setParentController(MyBookshelfController parentController) {
        this.parentController = parentController;
    }

    @FXML
    void handleReturnBookAction(ActionEvent event) {
        if (currentRecord == null || currentBook == null || parentController == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không đủ thông tin để xử lý trả sách.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Xác nhận Trả Sách");
        confirmation.setHeaderText("Bạn có chắc chắn muốn đánh dấu cuốn sách '" + currentBook.getTitle() + "' là đã trả?");
        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = BorrowingRecordService.getInstance().recordBookReturn(currentRecord.getRecordId(), LocalDate.now());
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã ghi nhận trả sách '" + currentBook.getTitle() + "'.");
                parentController.refreshBorrowedBooksTab(); // Yêu cầu parent controller tải lại danh sách
                // Không cần cập nhật UI của thẻ này nữa vì nó sẽ bị xóa và tạo lại bởi parentController
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể ghi nhận trả sách. Vui lòng thử lại.");
            }
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        try {
            URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
            if (cssUrl != null) {
                alert.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) {
            System.err.println("Failed to load CSS for alert: " + e.getMessage());
        }
        alert.showAndWait();
    }
}
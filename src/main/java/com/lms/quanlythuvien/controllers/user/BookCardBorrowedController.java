package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.MainApp; // Cho CSS
import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.transaction.BorrowingRecord;
import com.lms.quanlythuvien.models.transaction.LoanStatus;
import com.lms.quanlythuvien.services.transaction.BorrowingRecordService;
import com.lms.quanlythuvien.utils.session.SessionManager; // Để xem chi tiết sách

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent; // Cho phép click vào card để xem chi tiết
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class BookCardBorrowedController {

    @FXML private VBox bookCardRoot; // fx:id cho root card, có thể gán onMouseClicked="#handleCardClicked"
    @FXML private ImageView coverImageView;
    @FXML private Label titleLabel;
    @FXML private Label authorsLabel;
    @FXML private Label statusLabel;
    @FXML private Label borrowedOnLabel;
    @FXML private Label dueDateLabel;
    @FXML private Label daysRemainingLabel;
    @FXML private ProgressBar dueDateProgress;
    @FXML private Button returnButton; // fx:id cho nút trả sách, gán onAction="#handleReturnBookAction"
    @FXML private HBox progressBox;

    private BorrowingRecord currentRecord;
    private Book currentBook;
    private Image defaultBookCoverImage;
    private MyBookshelfController parentShelfController;
    private final DateTimeFormatter displayDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public BookCardBorrowedController() {
        try (InputStream defaultStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_book_cover.png")) {
            if (defaultStream != null) defaultBookCoverImage = new Image(defaultStream);
            else System.err.println("ERROR_BCBC: Default book cover image resource not found.");
        } catch (Exception e) { System.err.println("ERROR_BCBC: Exception loading default book cover: " + e.getMessage()); }
    }

    public void setDataForCurrentLoan(BorrowingRecord record, Book book, MyBookshelfController parentController) {
        this.currentRecord = record;
        this.currentBook = book;
        this.parentShelfController = parentController;
        commonSetDataUI(true);
    }

    public void setDataForLoanHistory(BorrowingRecord record, Book book, MyBookshelfController parentController) {
        this.currentRecord = record;
        this.currentBook = book;
        this.parentShelfController = parentController;
        commonSetDataUI(false);
    }

    private void commonSetDataUI(boolean isCurrentLoanView) {
        if (currentBook == null || currentRecord == null) {
            if(bookCardRoot != null) {bookCardRoot.setVisible(false); bookCardRoot.setManaged(false);}
            return;
        }
        if(bookCardRoot != null) {bookCardRoot.setVisible(true); bookCardRoot.setManaged(true);}

        titleLabel.setText(currentBook.getTitleOrDefault("N/A"));
        authorsLabel.setText(currentBook.getAuthorsFormatted("N/A"));
        loadCoverImageUI(currentBook.getThumbnailUrl());

        borrowedOnLabel.setText("Mượn: " + currentRecord.getBorrowDate().format(displayDateFormatter));

        if (currentRecord.getStatus() == LoanStatus.RETURNED && currentRecord.getReturnDate() != null) {
            dueDateLabel.setText("Đã trả: " + currentRecord.getReturnDate().format(displayDateFormatter));
        } else {
            dueDateLabel.setText("Hạn trả: " + currentRecord.getDueDate().format(displayDateFormatter));
        }
        updateStatusAndProgressDisplay(isCurrentLoanView);
    }

    private void loadCoverImageUI(String imageUrl) {
        if (coverImageView == null) return;
        Image imageToSet = this.defaultBookCoverImage;
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            String finalImageUrl = imageUrl.trim();
            if (finalImageUrl.startsWith("//")) finalImageUrl = "https:" + finalImageUrl;
            try {
                imageToSet = new Image(finalImageUrl, true); // true for background loading
                if (imageToSet.isError()) {
                    System.err.println("WARN_BCBC_COVER: Error loading image from URL: " + finalImageUrl);
                    imageToSet = this.defaultBookCoverImage;
                }
            } catch (Exception e) {
                System.err.println("ERROR_BCBC_COVER: Exception creating image from URL: " + finalImageUrl);
                imageToSet = this.defaultBookCoverImage;
            }
        }
        coverImageView.setImage(imageToSet);
    }

    private void updateStatusAndProgressDisplay(boolean showProgressElements) {
        if (currentRecord == null || statusLabel == null) return;

        LocalDate today = LocalDate.now();
        LocalDate dueDate = currentRecord.getDueDate();
        LoanStatus status = currentRecord.getStatus();

        statusLabel.getStyleClass().removeAll("status-active", "status-overdue", "status-returned", "status-due-soon");
        if(daysRemainingLabel != null) daysRemainingLabel.getStyleClass().removeAll("text-danger", "text-warning", "text-success", "text-neutral");
        if(dueDateProgress != null) dueDateProgress.getStyleClass().removeAll("progress-bar-danger", "progress-bar-warning", "progress-bar-success", "progress-bar-neutral");

        boolean progressVisible = showProgressElements;
        boolean returnButtonVisible = showProgressElements && status != LoanStatus.RETURNED;

        if (status == LoanStatus.RETURNED) {
            statusLabel.setText("ĐÃ TRẢ");
            statusLabel.getStyleClass().add("status-returned");
            if (daysRemainingLabel != null) daysRemainingLabel.setVisible(false);
            progressVisible = false;
        } else {
            long daysBetween = ChronoUnit.DAYS.between(today, dueDate);
            if (daysBetween < 0) {
                statusLabel.setText("QUÁ HẠN");
                statusLabel.getStyleClass().add("status-overdue");
                if (daysRemainingLabel != null) {
                    daysRemainingLabel.setText("Trễ " + Math.abs(daysBetween) + " ngày");
                    daysRemainingLabel.getStyleClass().add("text-danger");
                }
                if (dueDateProgress != null) {
                    dueDateProgress.setProgress(1.0);
                    dueDateProgress.getStyleClass().add("progress-bar-danger");
                }
            } else {
                statusLabel.setText("ĐANG MƯỢN");
                statusLabel.getStyleClass().add("status-active");
                if (daysRemainingLabel != null) {
                    daysRemainingLabel.setText("Còn " + daysBetween + " ngày");
                    if (daysBetween <= 3) {
                        statusLabel.setText("SẮP HẾT HẠN");
                        statusLabel.getStyleClass().remove("status-active");
                        statusLabel.getStyleClass().add("status-due-soon");
                        daysRemainingLabel.getStyleClass().add("text-warning");
                        if (dueDateProgress != null) dueDateProgress.getStyleClass().add("progress-bar-warning");
                    } else {
                        daysRemainingLabel.getStyleClass().add("text-success");
                        if (dueDateProgress != null) dueDateProgress.getStyleClass().add("progress-bar-success");
                    }
                }
                if (dueDateProgress != null) {
                    long totalLoanDuration = ChronoUnit.DAYS.between(currentRecord.getBorrowDate(), dueDate);
                    if (totalLoanDuration <= 0) totalLoanDuration = 1;
                    long daysPassed = ChronoUnit.DAYS.between(currentRecord.getBorrowDate(), today);
                    double progress = Math.max(0.0, Math.min(1.0, (double) daysPassed / totalLoanDuration));
                    dueDateProgress.setProgress(progress);
                }
            }
        }

        if (daysRemainingLabel != null) daysRemainingLabel.setVisible(showProgressElements && status != LoanStatus.RETURNED);
        if (progressBox != null) { progressBox.setVisible(progressVisible); progressBox.setManaged(progressVisible); }
        if (returnButton != null) { returnButton.setVisible(returnButtonVisible); returnButton.setManaged(returnButtonVisible); }
    }

    @FXML
    void handleReturnBookAction(ActionEvent event) {
        if (currentRecord == null || currentBook == null || parentShelfController == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Dữ liệu không hợp lệ để trả sách.");
            return;
        }
        if (currentRecord.getStatus() == LoanStatus.RETURNED) {
            showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Sách này đã được ghi nhận trả.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Xác Nhận Trả Sách");
        confirmation.setHeaderText("Trả sách: '" + currentBook.getTitleOrDefault("Sách không rõ") + "'?");
        confirmation.setContentText("Bạn có chắc chắn muốn thực hiện thao tác trả sách này?");
        applyDialogStyles(confirmation);
        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = BorrowingRecordService.getInstance().recordBookReturn(currentRecord.getRecordId(), LocalDate.now());
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Thành Công", "Đã ghi nhận trả sách '" + currentBook.getTitleOrDefault("N/A") + "'.");
                parentShelfController.refreshCurrentTabData();
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể ghi nhận trả sách. Vui lòng thử lại sau.");
            }
        }
    }

    @FXML
    void handleCardClicked(MouseEvent event) {
        if (currentBook != null && parentShelfController != null) {
            SessionManager.getInstance().setSelectedBook(currentBook);
            parentShelfController.navigateToBookDetail(currentBook); // Truyền Book object
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyDialogStyles(alert);
        alert.showAndWait();
    }

    private void applyDialogStyles(Dialog<?> dialog) {
        try {
            URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
            if (cssUrl != null) dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
        } catch (Exception e) { System.err.println("WARN_DIALOG_CSS: Failed to load CSS for dialog: " + e.getMessage());}
    }
}
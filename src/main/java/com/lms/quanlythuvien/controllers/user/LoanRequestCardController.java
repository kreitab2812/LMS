package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.transaction.BorrowingRequest;
import com.lms.quanlythuvien.services.transaction.BorrowingRequestService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.InputStream;
import java.net.URL; // Thêm nếu dùng cho showAlert
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class LoanRequestCardController {

    @FXML private ImageView bookCoverImageView;
    @FXML private Label bookTitleLabel;
    @FXML private Label authorsLabel;
    @FXML private Label requestStatusLabel;
    @FXML private Label requestDateLabel;
    @FXML private Label pickupDueDateLabel;
    @FXML private Label adminNotesLabel;
    @FXML private Button cancelRequestButton;

    private BorrowingRequest currentRequest;
    private Book requestedBook; // Lưu trữ thông tin sách để hiển thị
    private MyBookshelfController parentController;
    private Image defaultBookCoverImage;

    public LoanRequestCardController() {
        try (InputStream defaultStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_book_cover.png")) {
            if (defaultStream != null) {
                defaultBookCoverImage = new Image(defaultStream);
            }
        } catch (Exception e) {
            System.err.println("Error loading default book cover for request card: " + e.getMessage());
        }
    }

    public void setData(BorrowingRequest request, Book book, MyBookshelfController parentController) {
        this.currentRequest = request;
        this.requestedBook = book;
        this.parentController = parentController;

        if (book != null) {
            bookTitleLabel.setText(book.getTitle());
            authorsLabel.setText(book.getAuthors() != null && !book.getAuthors().isEmpty() ? String.join(", ", book.getAuthors()) : "N/A");
            loadCoverImage(book.getThumbnailUrl());
        } else {
            bookTitleLabel.setText("Sách không tìm thấy (ISBN: " + request.getBookIsbn13() + ")");
            authorsLabel.setText("N/A");
            bookCoverImageView.setImage(defaultBookCoverImage);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        requestDateLabel.setText("Ngày yêu cầu: " + request.getRequestDate().format(formatter));

        updateStatusDisplay();

        // Hiển thị/ẩn các trường tùy theo trạng thái
        adminNotesLabel.setText("Ghi chú Admin: " + (request.getAdminNotes() != null ? request.getAdminNotes() : "Không có"));
        adminNotesLabel.setVisible(request.getAdminNotes() != null && !request.getAdminNotes().isEmpty());
        adminNotesLabel.setManaged(request.getAdminNotes() != null && !request.getAdminNotes().isEmpty());

        if (request.getStatus() == BorrowingRequest.RequestStatus.APPROVED && request.getPickupDueDate() != null) {
            pickupDueDateLabel.setText("Hạn lấy sách: " + request.getPickupDueDate().format(formatter));
            pickupDueDateLabel.setVisible(true);
            pickupDueDateLabel.setManaged(true);
        } else {
            pickupDueDateLabel.setVisible(false);
            pickupDueDateLabel.setManaged(false);
        }

        // Nút hủy chỉ hiển thị khi trạng thái là PENDING (hoặc APPROVED nếu logic cho phép)
        boolean canCancel = request.getStatus() == BorrowingRequest.RequestStatus.PENDING;
        cancelRequestButton.setVisible(canCancel);
        cancelRequestButton.setManaged(canCancel);
    }

    private void loadCoverImage(String imageUrl) {
        if (bookCoverImageView == null) return;
        Image imageToSet = this.defaultBookCoverImage;
        if (imageUrl != null && !imageUrl.isEmpty()) {
            String finalImageUrl = imageUrl.startsWith("//") ? "https:" + imageUrl : imageUrl;
            try {
                Image loadedImage = new Image(finalImageUrl, true);
                if (!loadedImage.isError()) {
                    imageToSet = loadedImage;
                }
                loadedImage.errorProperty().addListener((obs, oldError, newError) -> {
                    if (newError) bookCoverImageView.setImage(this.defaultBookCoverImage);
                });
                if(loadedImage.isError()) bookCoverImageView.setImage(this.defaultBookCoverImage);
            } catch (Exception e) { /* Bỏ qua, dùng ảnh mặc định */ }
        }
        bookCoverImageView.setImage(imageToSet);
    }

    private void updateStatusDisplay() {
        requestStatusLabel.getStyleClass().removeAll("status-pending", "status-approved", "status-rejected", "status-completed", "status-canceled", "status-expired");
        requestStatusLabel.setText(currentRequest.getStatus().toString()); // Cần định nghĩa hàm toString() đẹp hơn cho Enum nếu muốn
        switch (currentRequest.getStatus()) {
            case PENDING:
                requestStatusLabel.getStyleClass().add("status-pending");
                // Cần dịch các giá trị Enum này sang tiếng Việt nếu muốn
                requestStatusLabel.setText("CHỜ DUYỆT");
                break;
            case APPROVED:
                requestStatusLabel.getStyleClass().add("status-approved");
                requestStatusLabel.setText("ĐÃ DUYỆT (Chờ lấy sách)");
                break;
            case REJECTED:
                requestStatusLabel.getStyleClass().add("status-rejected");
                requestStatusLabel.setText("BỊ TỪ CHỐI");
                break;
            case CANCELED_BY_USER:
                requestStatusLabel.getStyleClass().add("status-canceled");
                requestStatusLabel.setText("ĐÃ HỦY BỞI BẠN");
                break;
            case COMPLETED: // Yêu cầu đã được chuyển thành lượt mượn thành công
                requestStatusLabel.getStyleClass().add("status-completed");
                requestStatusLabel.setText("ĐÃ HOÀN TẤT (Đã mượn)");
                break;
            case EXPIRED:
                requestStatusLabel.getStyleClass().add("status-expired");
                requestStatusLabel.setText("ĐÃ HẾT HẠN LẤY SÁCH");
                break;
            default:
                requestStatusLabel.setText(currentRequest.getStatus().name());
                break;
        }
    }


    @FXML
    void handleCancelRequest(ActionEvent event) {
        if (currentRequest == null || parentController == null) return;
        if (currentRequest.getStatus() != BorrowingRequest.RequestStatus.PENDING) {
            showAlert(Alert.AlertType.WARNING, "Không thể hủy", "Chỉ có thể hủy các yêu cầu đang chờ duyệt.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Xác nhận Hủy Yêu Cầu");
        confirmation.setHeaderText("Bạn có chắc chắn muốn hủy yêu cầu mượn sách '" + (requestedBook != null ? requestedBook.getTitle() : currentRequest.getBookIsbn13()) + "'?");
        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = BorrowingRequestService.getInstance().cancelRequestByUser(currentRequest.getRequestId(), currentRequest.getUserId());
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã hủy yêu cầu mượn sách.");
                parentController.refreshLoanRequestsTab(); // Yêu cầu parent controller tải lại danh sách yêu cầu
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể hủy yêu cầu. Vui lòng thử lại.");
            }
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        // ... (hàm showAlert giữ nguyên)
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
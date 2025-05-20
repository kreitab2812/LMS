package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.MainApp; // Cho CSS
import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.transaction.BorrowingRequest;
import com.lms.quanlythuvien.services.transaction.BorrowingRequestService;
import com.lms.quanlythuvien.utils.session.SessionManager; // Để xem chi tiết sách

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog; // Cho applyDialogStyles
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent; // Cho click vào card
import javafx.scene.layout.VBox; // Cho root card

import java.io.InputStream;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class LoanRequestCardController {

    @FXML private VBox loanRequestCardRoot; // fx:id cho root card
    @FXML private ImageView bookCoverImageView;
    @FXML private Label bookTitleLabel;
    @FXML private Label authorsLabel;
    @FXML private Label requestStatusLabel;
    @FXML private Label requestDateLabel;
    @FXML private Label pickupDueDateLabel; // Ngày hết hạn lấy sách
    @FXML private Label adminNotesLabel;    // Ghi chú của admin khi từ chối/duyệt
    @FXML private Button cancelRequestButton;

    private BorrowingRequest currentRequest;
    private Book requestedBook;
    private MyBookshelfController parentShelfController; // Sửa tên cho nhất quán
    private Image defaultBookCoverImage;
    private final DateTimeFormatter displayDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");


    public LoanRequestCardController() {
        try (InputStream defaultStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_book_cover.png")) {
            if (defaultStream != null) {
                defaultBookCoverImage = new Image(defaultStream);
            } else {
                System.err.println("ERROR_LOAN_REQ_CARD: Default book cover image not found.");
            }
        } catch (Exception e) {
            System.err.println("ERROR_LOAN_REQ_CARD: Exception loading default book cover: " + e.getMessage());
        }
    }

    public void setData(BorrowingRequest request, Book book, MyBookshelfController parentController) {
        this.currentRequest = request;
        this.requestedBook = book; // Có thể null nếu sách không tìm thấy qua ISBN
        this.parentShelfController = parentController;

        if (book != null) {
            bookTitleLabel.setText(book.getTitleOrDefault("N/A"));
            authorsLabel.setText(book.getAuthorsFormatted("N/A"));
            loadCoverImageUI(book.getThumbnailUrl());
        } else {
            bookTitleLabel.setText("Sách không còn trong thư viện (ISBN: " + request.getBookIsbn13() + ")");
            authorsLabel.setText("Vui lòng hủy yêu cầu này.");
            bookCoverImageView.setImage(defaultBookCoverImage);
        }

        requestDateLabel.setText("Yêu cầu lúc: " + request.getRequestDate().format(displayDateFormatter));
        updateStatusDisplayAndControls();
    }

    private void loadCoverImageUI(String imageUrl) {
        if (bookCoverImageView == null) return;
        Image imageToSet = this.defaultBookCoverImage;
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            String finalImageUrl = imageUrl.trim();
            if (finalImageUrl.startsWith("//")) finalImageUrl = "https:" + finalImageUrl;
            try {
                imageToSet = new Image(finalImageUrl, true);
                if (imageToSet.isError()) {
                    System.err.println("WARN_LOAN_REQ_CARD_COVER: Error loading image: " + finalImageUrl);
                    imageToSet = this.defaultBookCoverImage;
                }
            } catch (Exception e) {
                System.err.println("ERROR_LOAN_REQ_CARD_COVER: Exception loading cover: " + finalImageUrl + ". " + e.getMessage());
                imageToSet = this.defaultBookCoverImage;
            }
        }
        bookCoverImageView.setImage(imageToSet);
    }

    private void updateStatusDisplayAndControls() {
        if (currentRequest == null || requestStatusLabel == null) return;

        requestStatusLabel.getStyleClass().removeAll("status-pending", "status-approved", "status-rejected", "status-completed", "status-canceled-user", "status-expired");
        String statusText = currentRequest.getStatus().name(); // Mặc định

        switch (currentRequest.getStatus()) {
            case PENDING:
                statusText = "CHỜ DUYỆT";
                requestStatusLabel.getStyleClass().add("status-pending");
                break;
            case APPROVED:
                statusText = "ĐÃ DUYỆT";
                requestStatusLabel.getStyleClass().add("status-approved");
                break;
            case REJECTED:
                statusText = "BỊ TỪ CHỐI";
                requestStatusLabel.getStyleClass().add("status-rejected");
                break;
            case CANCELED_BY_USER:
                statusText = "ĐÃ HỦY";
                requestStatusLabel.getStyleClass().add("status-canceled-user");
                break;
            case COMPLETED:
                statusText = "ĐÃ MƯỢN";
                requestStatusLabel.getStyleClass().add("status-completed");
                break;
            case EXPIRED:
                statusText = "HẾT HẠN LẤY";
                requestStatusLabel.getStyleClass().add("status-expired");
                break;
        }
        requestStatusLabel.setText(statusText);

        // Hiển thị ghi chú admin nếu có và trạng thái là REJECTED hoặc APPROVED
        boolean showAdminNotes = (currentRequest.getStatus() == BorrowingRequest.RequestStatus.REJECTED ||
                currentRequest.getStatus() == BorrowingRequest.RequestStatus.APPROVED) &&
                currentRequest.getAdminNotes() != null &&
                !currentRequest.getAdminNotes().trim().isEmpty();
        adminNotesLabel.setText("Admin: " + (showAdminNotes ? currentRequest.getAdminNotes() : ""));
        adminNotesLabel.setVisible(showAdminNotes);
        adminNotesLabel.setManaged(showAdminNotes);

        // Hiển thị hạn lấy sách nếu APPROVED
        boolean showPickupDue = currentRequest.getStatus() == BorrowingRequest.RequestStatus.APPROVED &&
                currentRequest.getPickupDueDate() != null;
        pickupDueDateLabel.setText("Hạn lấy sách: " + (showPickupDue ? currentRequest.getPickupDueDate().format(displayDateFormatter) : ""));
        pickupDueDateLabel.setVisible(showPickupDue);
        pickupDueDateLabel.setManaged(showPickupDue);

        // Nút hủy chỉ hiển thị khi trạng thái là PENDING
        boolean canCancel = currentRequest.getStatus() == BorrowingRequest.RequestStatus.PENDING;
        cancelRequestButton.setVisible(canCancel);
        cancelRequestButton.setManaged(canCancel);
    }


    @FXML
    void handleCancelRequest(ActionEvent event) {
        if (currentRequest == null || parentShelfController == null) return;
        if (currentRequest.getStatus() != BorrowingRequest.RequestStatus.PENDING) {
            showAlert(Alert.AlertType.INFORMATION, "Không Thể Hủy", "Chỉ có thể hủy các yêu cầu đang chờ duyệt.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Xác Nhận Hủy Yêu Cầu");
        String bookTitleForAlert = (requestedBook != null && requestedBook.getTitle() != null) ? requestedBook.getTitle() : "sách có ISBN " + currentRequest.getBookIsbn13();
        confirmation.setHeaderText("Hủy yêu cầu mượn sách: '" + bookTitleForAlert + "'?");
        confirmation.setContentText("Bạn có chắc chắn muốn hủy yêu cầu này không?");
        applyDialogStyles(confirmation);
        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Service nên chỉ cần requestId và userId của người hủy (là currentUser)
            boolean success = BorrowingRequestService.getInstance().cancelRequestByUser(currentRequest.getRequestId(), currentRequest.getUserId());
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Thành Công", "Đã hủy yêu cầu mượn sách.");
                parentShelfController.refreshCurrentTabData();
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể hủy yêu cầu. Vui lòng thử lại sau.");
                // Có thể yêu cầu đã được xử lý bởi admin, refresh lại để chắc chắn
                parentShelfController.refreshCurrentTabData();
            }
        }
    }

    @FXML
    void handleCardClicked(MouseEvent event) {
        if (requestedBook != null && parentShelfController != null) {
            // Điều hướng đến chi tiết sách, tương tự như các card sách khác
            parentShelfController.navigateToBookDetail(requestedBook);
        } else if (currentRequest != null) {
            System.out.println("INFO_LOAN_REQ_CARD_CLICK: Clicked on request for ISBN " + currentRequest.getBookIsbn13() + " but book details are not available.");
            // Có thể hiển thị thông báo sách không còn hoặc không thể xem chi tiết
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
        } catch (Exception e) { System.err.println("WARN_DIALOG_CSS: Failed to load CSS for dialog: " + e.getMessage()); }
    }
}
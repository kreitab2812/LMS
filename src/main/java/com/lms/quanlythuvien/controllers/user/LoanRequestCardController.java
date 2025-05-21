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
import java.time.LocalDate; // Thêm nếu chưa có
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class LoanRequestCardController {

    @FXML private VBox loanRequestCardRoot;
    @FXML private ImageView bookCoverImageView;
    @FXML private Label bookTitleLabel;
    @FXML private Label authorsLabel;
    @FXML private Label requestStatusLabel;
    @FXML private Label requestDateLabel;
    @FXML private Label pickupDueDateLabel;
    @FXML private Label adminNotesLabel;
    @FXML private Button cancelRequestButton;

    private BorrowingRequest currentRequest;
    private Book requestedBook;
    private MyBookshelfController parentShelfController;
    private Image defaultBookCoverImage;

    // --- SỬA LỖI Ở ĐÂY ---
    private final DateTimeFormatter displayDateOnlyFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    // --- KẾT THÚC SỬA LỖI ---


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
        this.requestedBook = book;
        this.parentShelfController = parentController;

        if (request == null) { // Thêm kiểm tra null cho request
            System.err.println("ERROR_LOAN_REQ_CARD:setData: BorrowingRequest is null.");
            // Có thể ẩn card hoặc hiển thị thông báo lỗi
            if(loanRequestCardRoot != null) loanRequestCardRoot.setVisible(false);
            return;
        }

        if (book != null) {
            bookTitleLabel.setText(book.getTitleOrDefault("N/A"));
            authorsLabel.setText(book.getAuthorsFormatted("N/A"));
            loadCoverImageUI(book.getThumbnailUrl());
        } else {
            bookTitleLabel.setText((request.getBookIsbn13() != null) ? "Sách (ISBN: " + request.getBookIsbn13() + ") không còn hoặc không tìm thấy." : "Thông tin sách không rõ");
            authorsLabel.setText("Vui lòng kiểm tra hoặc hủy yêu cầu này.");
            if(bookCoverImageView != null) bookCoverImageView.setImage(defaultBookCoverImage);
        }

        if (request.getRequestDate() != null) {
            requestDateLabel.setText("Yêu cầu lúc: " + request.getRequestDate().format(displayDateOnlyFormatter)); // Sử dụng formatter mới
        } else {
            requestDateLabel.setText("Yêu cầu lúc: N/A");
        }
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

        // Đảm bảo BorrowingRequest.RequestStatus có getDisplayName()
        BorrowingRequest.RequestStatus currentStatus = currentRequest.getStatus();
        String statusText = (currentStatus != null) ? currentStatus.getDisplayName() : "Không rõ";

        requestStatusLabel.getStyleClass().clear(); // Xóa hết style cũ
        requestStatusLabel.getStyleClass().add("status-label"); // Thêm class chung

        if (currentStatus != null) {
            switch (currentStatus) {
                case PENDING:
                    requestStatusLabel.getStyleClass().add("status-pending");
                    break;
                case APPROVED:
                    requestStatusLabel.getStyleClass().add("status-approved");
                    break;
                case REJECTED:
                    requestStatusLabel.getStyleClass().add("status-rejected");
                    break;
                case CANCELED_BY_USER:
                    requestStatusLabel.getStyleClass().add("status-canceled-user");
                    break;
                case COMPLETED:
                    requestStatusLabel.getStyleClass().add("status-completed");
                    break;
                case EXPIRED:
                    requestStatusLabel.getStyleClass().add("status-expired");
                    break;
                default:
                    requestStatusLabel.getStyleClass().add("status-unknown"); // Thêm class cho trạng thái không xác định
                    break;
            }
        } else {
            requestStatusLabel.getStyleClass().add("status-unknown");
        }
        requestStatusLabel.setText(statusText.toUpperCase());


        boolean showAdminNotes = (currentStatus == BorrowingRequest.RequestStatus.REJECTED ||
                currentStatus == BorrowingRequest.RequestStatus.APPROVED) &&
                currentRequest.getAdminNotes() != null &&
                !currentRequest.getAdminNotes().trim().isEmpty();

        if (adminNotesLabel != null) {
            adminNotesLabel.setText("Admin: " + (showAdminNotes ? currentRequest.getAdminNotes() : ""));
            adminNotesLabel.setVisible(showAdminNotes);
            adminNotesLabel.setManaged(showAdminNotes);
        }


        boolean showPickupDue = currentStatus == BorrowingRequest.RequestStatus.APPROVED &&
                currentRequest.getPickupDueDate() != null;
        if (pickupDueDateLabel != null) {
            pickupDueDateLabel.setText("Hạn lấy sách: " + (showPickupDue ? currentRequest.getPickupDueDate().format(displayDateOnlyFormatter) : "")); // Sử dụng formatter mới
            pickupDueDateLabel.setVisible(showPickupDue);
            pickupDueDateLabel.setManaged(showPickupDue);
        }

        boolean canCancel = currentStatus == BorrowingRequest.RequestStatus.PENDING;
        if (cancelRequestButton != null) {
            cancelRequestButton.setVisible(canCancel);
            cancelRequestButton.setManaged(canCancel);
        }
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
            boolean success = BorrowingRequestService.getInstance().cancelRequestByUser(currentRequest.getRequestId(), currentRequest.getUserId());
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Thành Công", "Đã hủy yêu cầu mượn sách.");
                parentShelfController.refreshCurrentTabData();
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể hủy yêu cầu. Vui lòng thử lại sau.");
                parentShelfController.refreshCurrentTabData();
            }
        }
    }

    @FXML
    void handleCardClicked(MouseEvent event) {
        if (requestedBook != null && parentShelfController != null) {
            parentShelfController.navigateToBookDetail(requestedBook);
        } else if (currentRequest != null) {
            System.out.println("INFO_LOAN_REQ_CARD_CLICK: Clicked on request for ISBN " + currentRequest.getBookIsbn13() + " but book details are not available.");
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
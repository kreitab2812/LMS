package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.MainApp; // Cần nếu có hành động điều hướng từ đây (hiện tại không có)
import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.models.transaction.BorrowingRequest;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.services.transaction.BorrowingRequestService;
import com.lms.quanlythuvien.utils.session.SessionManager;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Comparator;
import java.util.Optional;
import java.util.ResourceBundle;

public class BookDetailController implements Initializable {

    @FXML private Label detailTitleLabel;
    @FXML private ImageView detailCoverImageView;
    @FXML private Label detailAuthorsLabel;
    @FXML private Label detailPublisherLabel;
    @FXML private Label detailPublishedDateLabel;
    @FXML private Label detailIsbnLabel;
    @FXML private Label detailPageCountLabel;
    @FXML private Label detailCategoriesLabel;
    @FXML private TextArea detailDescriptionArea;
    @FXML private Label detailAvailableQuantityLabel;
    @FXML private Button requestBorrowButton;
    @FXML private Button closeDetailButton;

    private Book currentBook;
    private User currentUser;
    private BookManagementService bookManagementService;
    private BorrowingRequestService borrowingRequestService;
    private Image defaultBookCoverImage;

    // Constructor (không bắt buộc, nhưng có thể dùng để khởi tạo service nếu chưa phải Singleton hoàn toàn)
    public BookDetailController() {
        // Services được lấy bằng getInstance() nên không cần khởi tạo ở đây
        // nếu chúng đã được thiết kế là Singleton chuẩn.
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Khởi tạo các service
        bookManagementService = BookManagementService.getInstance();
        borrowingRequestService = BorrowingRequestService.getInstance();
        currentUser = SessionManager.getInstance().getCurrentUser(); // Lấy user hiện tại

        // Tải ảnh bìa mặc định
        try (InputStream defaultStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_book_cover.png")) {
            if (defaultStream != null) {
                defaultBookCoverImage = new Image(defaultStream);
            } else {
                System.err.println("ERROR_BDC_INIT: Default book cover image not found at specified path.");
            }
        } catch (Exception e) {
            System.err.println("ERROR_BDC_INIT: Exception loading default book cover: " + e.getMessage());
            e.printStackTrace();
        }

        // Không làm gì với currentBook ở đây nữa, vì nó sẽ được set qua setBookData()
        // Nếu currentUser là null, các hành động liên quan đến user (như gửi yêu cầu) sẽ bị ảnh hưởng.
        if (currentUser == null) {
            System.err.println("WARN_BDC_INIT: Current user is null. 'Request Borrow' button might be disabled or restricted.");
            if (requestBorrowButton != null) {
                requestBorrowButton.setText("Vui lòng đăng nhập");
                requestBorrowButton.setDisable(true);
            }
        }
    }

    /**
     * Thiết lập dữ liệu sách để hiển thị trên view.
     * Phương thức này sẽ được gọi từ controller đã load BookDetailView.fxml.
     * @param book Đối tượng Book cần hiển thị.
     */
    public void setBookData(Book book) {
        this.currentBook = book;
        if (book != null) {
            System.out.println("DEBUG_BDC_SET_BOOK: Displaying details for book: " + book.getTitle());
            detailTitleLabel.setText(book.getTitle() != null ? book.getTitle() : "N/A");
            detailAuthorsLabel.setText(book.getAuthors() != null && !book.getAuthors().isEmpty() ? String.join(", ", book.getAuthors()) : "N/A");
            detailPublisherLabel.setText(book.getPublisher() != null ? book.getPublisher() : "N/A");
            detailPublishedDateLabel.setText(book.getPublishedDate() != null ? book.getPublishedDate() : "N/A");

            String isbnText = "ISBN-13: " + (book.getIsbn13() != null ? book.getIsbn13() : "N/A");
            if (book.getIsbn10() != null && !book.getIsbn10().isEmpty()) {
                isbnText += " / ISBN-10: " + book.getIsbn10();
            }
            detailIsbnLabel.setText(isbnText);

            detailPageCountLabel.setText(book.getPageCount() != null ? String.valueOf(book.getPageCount()) + " trang" : "N/A");
            detailCategoriesLabel.setText(book.getCategories() != null && !book.getCategories().isEmpty() ? String.join("; ", book.getCategories()) : "N/A");
            detailDescriptionArea.setText(book.getDescription() != null ? book.getDescription() : "Không có mô tả.");
            detailDescriptionArea.setWrapText(true);
            detailDescriptionArea.setEditable(false); // Mô tả chỉ để đọc

            updateAvailabilityInfoAndRequestButton(); // Gọi hàm tổng hợp
            loadCoverImage(book.getThumbnailUrl());

        } else {
            System.err.println("ERROR_BDC_SET_BOOK: Book data provided to setBookData is null.");
            showAlert(Alert.AlertType.ERROR, "Lỗi Dữ liệu", "Không thể hiển thị chi tiết sách do thiếu dữ liệu.");
            // Cân nhắc đóng dialog nếu nó được mở dưới dạng dialog và không có dữ liệu
            Platform.runLater(this::closeDialog); // Đảm bảo chạy trên UI thread nếu setBookData được gọi từ luồng khác
        }
    }

    private void updateAvailabilityInfoAndRequestButton() {
        if (currentBook == null || detailAvailableQuantityLabel == null || requestBorrowButton == null) {
            return;
        }

        // Lấy thông tin sách mới nhất từ DB để đảm bảo số lượng chính xác
        // currentBook.getIsbn13() là ID chính để tham chiếu sách này
        Optional<Book> freshBookOpt = bookManagementService.findBookByIsbn13InLibrary(currentBook.getIsbn13());

        if (freshBookOpt.isPresent()) {
            this.currentBook = freshBookOpt.get(); // Cập nhật currentBook với thông tin mới nhất từ DB
        } else {
            // Sách không còn tìm thấy trong DB (có thể đã bị xóa bởi Admin khác)
            detailAvailableQuantityLabel.setText("Không xác định");
            detailAvailableQuantityLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
            requestBorrowButton.setDisable(true);
            requestBorrowButton.setText("Sách không tồn tại");
            System.err.println("WARN_BDC_AVAILABILITY: Book with ISBN " + currentBook.getIsbn13() + " not found in DB for availability update.");
            return;
        }

        int available = currentBook.getAvailableQuantity();
        detailAvailableQuantityLabel.setText(String.valueOf(available));

        if (currentUser == null) { // Nếu chưa đăng nhập
            requestBorrowButton.setText("Đăng nhập để yêu cầu");
            requestBorrowButton.setDisable(true);
            detailAvailableQuantityLabel.setStyle(""); // Reset style số lượng
            return;
        }

        if (available > 0) {
            detailAvailableQuantityLabel.setStyle("-fx-text-fill: #007e3f; -fx-font-weight: bold;");
            // Nút sẽ được bật/tắt bởi checkIfUserHasPendingRequestForThisBook
            checkIfUserHasPendingRequestForThisBook();
        } else {
            detailAvailableQuantityLabel.setStyle("-fx-text-fill: #D8000C; -fx-font-weight: bold;");
            requestBorrowButton.setDisable(true);
            requestBorrowButton.setText("Hết Sách");
        }
    }

    private void checkIfUserHasPendingRequestForThisBook() {
        // Hàm này chỉ nên được gọi nếu currentUser và currentBook không null, và sách còn hàng
        if (currentUser == null || currentBook == null || currentBook.getIsbn13() == null ||
                borrowingRequestService == null || requestBorrowButton == null || currentBook.getAvailableQuantity() <= 0) {
            // Nếu sách đã hết hàng, nút đã bị disable bởi updateAvailabilityInfo, không cần làm gì thêm
            if (currentBook != null && currentBook.getAvailableQuantity() <= 0) {
                return;
            }
            // Nếu các điều kiện khác không đáp ứng, có thể reset nút về trạng thái mặc định (nếu sách còn)
            if (requestBorrowButton != null && currentBook != null && currentBook.getAvailableQuantity() > 0) {
                requestBorrowButton.setDisable(false);
                requestBorrowButton.setText("Gửi Yêu Cầu Mượn (" + currentBook.getAvailableQuantity() + " còn lại)");
            }
            return;
        }

        // Lấy danh sách yêu cầu một lần để tối ưu
        List<BorrowingRequest> userRequests = borrowingRequestService.getRequestsByUserId(currentUser.getUserId());

        boolean hasPendingOrApproved = userRequests.stream()
                .anyMatch(req -> req.getBookIsbn13().equals(currentBook.getIsbn13()) &&
                        (req.getStatus() == BorrowingRequest.RequestStatus.PENDING ||
                                req.getStatus() == BorrowingRequest.RequestStatus.APPROVED));

        if (hasPendingOrApproved) {
            requestBorrowButton.setDisable(true);
            Optional<BorrowingRequest> latestRequestOpt = userRequests.stream()
                    .filter(req -> req.getBookIsbn13().equals(currentBook.getIsbn13()) &&
                            (req.getStatus() == BorrowingRequest.RequestStatus.PENDING || req.getStatus() == BorrowingRequest.RequestStatus.APPROVED))
                    .max(Comparator.comparing(BorrowingRequest::getRequestDate)); // Lấy yêu cầu mới nhất

            if (latestRequestOpt.isPresent()) {
                if (latestRequestOpt.get().getStatus() == BorrowingRequest.RequestStatus.PENDING) {
                    requestBorrowButton.setText("Đã Yêu Cầu (Chờ Duyệt)");
                } else if (latestRequestOpt.get().getStatus() == BorrowingRequest.RequestStatus.APPROVED) {
                    requestBorrowButton.setText("Đã Được Duyệt (Chờ Lấy)");
                }
            }
        } else {
            // Nếu không có yêu cầu nào, và sách còn hàng (đã kiểm tra ở updateAvailabilityInfo)
            requestBorrowButton.setDisable(false);
            requestBorrowButton.setText("Gửi Yêu Cầu Mượn (" + currentBook.getAvailableQuantity() + " còn lại)");
        }
    }

    private void loadCoverImage(String imageUrl) {
        if (detailCoverImageView == null) return;
        Image imageToSet = this.defaultBookCoverImage;
        if (imageUrl != null && !imageUrl.isEmpty()) {
            String finalImageUrl = imageUrl.startsWith("//") ? "https:" + imageUrl : imageUrl;
            try {
                Image loadedImage = new Image(finalImageUrl, true);
                if (!loadedImage.isError()) {
                    imageToSet = loadedImage;
                } else {
                    System.err.println("ERROR_BDC_LOAD_COVER: Error with image from URL (isError=true): " + finalImageUrl +
                            (loadedImage.getException() != null ? " - " + loadedImage.getException().getMessage() : ""));
                }
                // Listener lỗi nên được thêm trước khi gán ảnh để bắt lỗi tải ngầm
                loadedImage.errorProperty().addListener((obs, oldError, newError) -> {
                    if (newError && detailCoverImageView.getImage() == loadedImage) { // Chỉ set lại nếu ảnh đang hiển thị là ảnh lỗi
                        System.err.println("ERROR_BDC_LOAD_COVER_ASYNC: Error loading image from URL: " + finalImageUrl);
                        if (this.defaultBookCoverImage != null) {
                            detailCoverImageView.setImage(this.defaultBookCoverImage);
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println("CRITICAL_BDC_LOAD_COVER: Exception creating Image for URL: " + finalImageUrl + " - " + e.getMessage());
            }
        }
        detailCoverImageView.setImage(imageToSet);
    }

    @FXML
    void handleRequestBorrowAction(ActionEvent event) {
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Yêu cầu đăng nhập", "Vui lòng đăng nhập để thực hiện chức năng này.");
            return;
        }
        if (currentBook == null || currentBook.getIsbn13() == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không có thông tin sách hợp lệ để thực hiện yêu cầu.");
            return;
        }

        // Kiểm tra lại số lượng sách và trạng thái yêu cầu một lần nữa ngay trước khi gửi
        // để đảm bảo dữ liệu là mới nhất và tránh race condition.
        Optional<Book> freshBookOpt = bookManagementService.findBookByIsbn13InLibrary(currentBook.getIsbn13());
        if (freshBookOpt.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi Sách", "Sách không còn tồn tại trong thư viện.");
            Platform.runLater(this::closeDialog);
            return;
        }
        this.currentBook = freshBookOpt.get(); // Cập nhật lại currentBook

        if (currentBook.getAvailableQuantity() <= 0) {
            showAlert(Alert.AlertType.WARNING, "Hết Sách", "Xin lỗi, cuốn sách '" + currentBook.getTitle() + "' vừa hết hàng.");
            updateAvailabilityInfoAndRequestButton(); // Cập nhật lại UI
            return;
        }

        // Kiểm tra lại yêu cầu hiện có
        boolean hasExistingRequest = borrowingRequestService.getRequestsByUserId(currentUser.getUserId()).stream()
                .anyMatch(req -> req.getBookIsbn13().equals(currentBook.getIsbn13()) &&
                        (req.getStatus() == BorrowingRequest.RequestStatus.PENDING ||
                                req.getStatus() == BorrowingRequest.RequestStatus.APPROVED));

        if (hasExistingRequest) {
            showAlert(Alert.AlertType.INFORMATION, "Thông Báo", "Bạn đã có yêu cầu (đang chờ hoặc đã duyệt) cho cuốn sách này.");
            checkIfUserHasPendingRequestForThisBook(); // Cập nhật lại nút
            return;
        }


        System.out.println("DEBUG_BDC_REQUEST: User '" + currentUser.getUsername() + "' requests to borrow book: '" + currentBook.getTitle() + "'");
        Optional<BorrowingRequest> requestOpt = borrowingRequestService.addRequest(currentUser.getUserId(), currentBook.getIsbn13());

        if (requestOpt.isPresent()) {
            showAlert(Alert.AlertType.INFORMATION, "Yêu Cầu Đã Gửi",
                    "Yêu cầu mượn sách '" + currentBook.getTitle() + "' của bạn đã được gửi.\n" +
                            "Vui lòng đợi quản trị viên thư viện duyệt.");
        } else {
            // Service addRequest sẽ trả về Optional.empty() nếu user đã có yêu cầu hoặc sách không tồn tại/lỗi
            showAlert(Alert.AlertType.WARNING, "Không thể gửi yêu cầu",
                    "Yêu cầu mượn sách không thành công. \n" +
                            "Có thể bạn đã có yêu cầu đang chờ duyệt cho cuốn sách này, sách vừa hết hàng, hoặc có lỗi xảy ra.");
        }
        // Luôn cập nhật lại trạng thái nút sau khi thực hiện hành động
        updateAvailabilityInfoAndRequestButton();
    }

    @FXML
    void handleCloseDetailAction(ActionEvent event) {
        closeDialog();
    }

    private void closeDialog() {
        // Đảm bảo code này chạy trên UI thread nếu được gọi từ luồng khác
        if (Platform.isFxApplicationThread()) {
            tryCloseStage();
        } else {
            Platform.runLater(this::tryCloseStage);
        }
    }

    private void tryCloseStage() {
        if (closeDetailButton != null && closeDetailButton.getScene() != null && closeDetailButton.getScene().getWindow() instanceof Stage) {
            Stage stage = (Stage) closeDetailButton.getScene().getWindow();
            stage.close();
        } else {
            System.err.println("ERROR_BDC_CLOSE: Cannot get stage from closeDetailButton to close dialog.");
            // Nếu không lấy được stage từ nút, thử lấy từ một FXML element khác nếu có
            // Hoặc controller này cần một tham chiếu đến Stage của nó nếu nó luôn là dialog.
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
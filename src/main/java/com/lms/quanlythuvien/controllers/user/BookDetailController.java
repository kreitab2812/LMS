package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.models.transaction.BorrowingRequest;
import com.lms.quanlythuvien.models.item.BookReview;
import com.lms.quanlythuvien.models.system.Notification; // Thêm cho gửi thông báo
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.services.transaction.BorrowingRequestService;
import com.lms.quanlythuvien.services.user.FavoriteBookService;
import com.lms.quanlythuvien.services.library.BookReviewService;
import com.lms.quanlythuvien.services.system.NotificationService; // Thêm service
import com.lms.quanlythuvien.utils.session.SessionManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority; // <<< THÊM IMPORT
import javafx.scene.layout.VBox;
// Bỏ Stage nếu không dùng dialog riêng (view này được load vào StackPane)

import java.io.InputStream;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional; // <<< THÊM IMPORT
import java.util.ResourceBundle;
// Bỏ IntStream nếu không dùng

public class BookDetailController implements Initializable {

    //<editor-fold desc="FXML Injections - Basic Info & Actions">
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
    @FXML private Label shelfLocationLabel;
    @FXML private Button requestBorrowButton;
    @FXML private Button closeDetailButton;
    //</editor-fold>

    //<editor-fold desc="FXML Injections - Favorite & Review/Rating">
    @FXML private Button favoriteButton;
    @FXML private Label averageRatingDisplayLabel;
    @FXML private HBox averageRatingStarsPane;
    @FXML private ListView<BookReview> reviewsListView;
    @FXML private ComboBox<Integer> ratingComboBox;
    @FXML private TextArea newCommentTextArea;
    @FXML private Button submitReviewButton;
    @FXML private Label reviewErrorLabel;
    //</editor-fold>

    private Book currentBook;
    private User currentUser;
    private BookManagementService bookManagementService;
    private BorrowingRequestService borrowingRequestService;
    private FavoriteBookService favoriteBookService;
    private BookReviewService bookReviewService;
    private NotificationService notificationService;
    private Image defaultBookCoverImage;

    private UserDashboardController dashboardController; // Để điều hướng

    private final String HEART_EMPTY = "🤍"; // Chỉ icon
    private final String HEART_FULL = "❤️";   // Chỉ icon

    public BookDetailController() {
        // Constructor
    }

    public void setDashboardController(UserDashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bookManagementService = BookManagementService.getInstance();
        borrowingRequestService = BorrowingRequestService.getInstance();
        favoriteBookService = FavoriteBookService.getInstance();
        bookReviewService = BookReviewService.getInstance();
        notificationService = NotificationService.getInstance();
        currentUser = SessionManager.getInstance().getCurrentUser();

        loadDefaultBookCoverImage();
        setupRatingComboBox();
        setupReviewsListViewCellFactory();
        clearReviewError();

        Book bookFromSession = SessionManager.getInstance().getSelectedBook();
        // Không xóa book khỏi session ở đây, để UserDashboardController hoặc
        // nơi gọi quyết định khi nào xóa (ví dụ sau khi điều hướng đi)

        if (bookFromSession != null && bookFromSession.getIsbn13() != null && !bookFromSession.getIsbn13().isEmpty()) {
            Optional<Book> freshBookOpt = bookManagementService.findBookByIsbn13InLibrary(bookFromSession.getIsbn13());
            if (freshBookOpt.isPresent()) {
                setBookDataUI(freshBookOpt.get()); // Đổi tên hàm để rõ ràng hơn
            } else {
                handleMissingBookData("Sách (ISBN: " + bookFromSession.getIsbn13() + ") không còn tồn tại trong thư viện.");
            }
        } else {
            handleMissingBookData("Không có thông tin sách được chọn để hiển thị.");
        }
        updateControlsBasedOnLoginStatus();
    }

    private void loadDefaultBookCoverImage() {
        try (InputStream defaultStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_book_cover.png")) {
            if (defaultStream != null) {
                defaultBookCoverImage = new Image(defaultStream);
            } else {
                System.err.println("ERROR_BDC_INIT: Default book cover resource not found.");
            }
        } catch (Exception e) {
            System.err.println("ERROR_BDC_INIT: Exception loading default book cover: " + e.getMessage());
        }
    }

    private void setupRatingComboBox() {
        if (ratingComboBox != null) {
            ObservableList<Integer> ratings = FXCollections.observableArrayList(5, 4, 3, 2, 1);
            ratingComboBox.setItems(ratings);
            ratingComboBox.setPromptText("Sao");
        }
    }

    private void updateControlsBasedOnLoginStatus() {
        boolean isLoggedIn = (currentUser != null);
        if (requestBorrowButton != null) {
            if (!isLoggedIn) {
                requestBorrowButton.setText("Đăng nhập để mượn");
                requestBorrowButton.setDisable(true);
            }
            // Trạng thái disable/enable cụ thể sẽ do updateAvailabilityInfoAndRequestButtonStatus() xử lý
        }
        if (favoriteButton != null) favoriteButton.setDisable(!isLoggedIn);
        if (submitReviewButton != null) submitReviewButton.setDisable(!isLoggedIn);
        if (newCommentTextArea != null) {
            newCommentTextArea.setDisable(!isLoggedIn);
            newCommentTextArea.setPromptText(isLoggedIn ? "Viết bình luận của bạn..." : "Đăng nhập để bình luận.");
        }
        if (ratingComboBox != null) ratingComboBox.setDisable(!isLoggedIn);
    }

    // Hàm này được gọi sau khi đã có `currentBook` (bản mới nhất từ DB)
    public void setBookDataUI(Book bookToDisplay) {
        this.currentBook = bookToDisplay;
        if (currentBook == null) {
            handleMissingBookData("Dữ liệu sách không hợp lệ.");
            return;
        }

        System.out.println("DEBUG_BDC_SET_BOOK_UI: Displaying details for: " + currentBook.getTitleOrDefault("N/A"));

        detailTitleLabel.setText(currentBook.getTitleOrDefault("N/A"));
        detailAuthorsLabel.setText(currentBook.getAuthorsFormatted("N/A"));
        detailPublisherLabel.setText(currentBook.getPublisherOrDefault("N/A"));
        detailPublishedDateLabel.setText(currentBook.getPublishedDateOrDefault("N/A"));

        String isbnText = "ISBN-13: " + currentBook.getIsbn13OrDefault("N/A");
        if (currentBook.getIsbn10() != null && !currentBook.getIsbn10().isEmpty()) {
            isbnText += " / ISBN-10: " + currentBook.getIsbn10();
        }
        detailIsbnLabel.setText(isbnText);

        detailPageCountLabel.setText(currentBook.getPageCount() != null ? currentBook.getPageCount() + " trang" : "N/A");
        detailCategoriesLabel.setText(currentBook.getCategoriesFormatted("N/A"));
        detailDescriptionArea.setText(currentBook.getDescriptionOrDefault("Chưa có mô tả."));
        detailDescriptionArea.setWrapText(true);
        detailDescriptionArea.setEditable(false);
        shelfLocationLabel.setText(currentBook.getShelfLocationOrDefault("N/A"));

        loadCoverImageUI(currentBook.getThumbnailUrl());
        updateAvailabilityInfoAndRequestButtonStatus();
        updateFavoriteButtonStatus();
        loadBookReviewsAndRating();
    }

    private void handleMissingBookData(String message) {
        System.err.println("ERROR_BDC_MISSING_DATA: " + message);
        if (detailTitleLabel != null) detailTitleLabel.setText("Không Tìm Thấy Sách");
        if (detailDescriptionArea != null) detailDescriptionArea.setText(message);
        // Vô hiệu hóa các control khác
        if(requestBorrowButton != null) {requestBorrowButton.setDisable(true); requestBorrowButton.setText("Không có sách");}
        if(favoriteButton != null) favoriteButton.setDisable(true);
        if(submitReviewButton != null) submitReviewButton.setDisable(true);
        if(newCommentTextArea != null) newCommentTextArea.setDisable(true);
        if(ratingComboBox != null) ratingComboBox.setDisable(true);
        if(reviewsListView != null) reviewsListView.setPlaceholder(new Label(message));
        if(averageRatingDisplayLabel != null) averageRatingDisplayLabel.setText("");
        if(averageRatingStarsPane != null) averageRatingStarsPane.getChildren().clear();

        showAlert(Alert.AlertType.WARNING, "Lỗi Thông Tin", message);
        // Không tự động đóng view, để người dùng tự đóng bằng nút "Đóng"
    }

    private void updateAvailabilityInfoAndRequestButtonStatus() {
        if (currentBook == null || detailAvailableQuantityLabel == null || requestBorrowButton == null) return;
        int available = currentBook.getAvailableQuantity();
        detailAvailableQuantityLabel.setText(String.valueOf(available));
        detailAvailableQuantityLabel.getStyleClass().removeAll("availability-good", "availability-low", "availability-out");

        if (currentUser == null) { // Nếu chưa đăng nhập
            requestBorrowButton.setText("Đăng nhập để mượn");
            requestBorrowButton.setDisable(true);
            return;
        }

        if (available > 0) {
            detailAvailableQuantityLabel.getStyleClass().add("availability-good");
            checkIfUserHasPendingOrApprovedRequest();
        } else {
            detailAvailableQuantityLabel.getStyleClass().add("availability-out");
            requestBorrowButton.setText("Hết Sách");
            requestBorrowButton.setDisable(true);
        }
    }

    private void checkIfUserHasPendingOrApprovedRequest() {
        if (currentUser == null || currentBook == null || currentBook.getIsbn13() == null || borrowingRequestService == null || requestBorrowButton == null) {
            return;
        }
        // Chỉ thực hiện nếu sách còn hàng (đã kiểm tra ở hàm gọi)
        if (currentBook.getAvailableQuantity() <=0) return;

        List<BorrowingRequest> userRequests = borrowingRequestService.getRequestsByUserId(currentUser.getUserId());
        Optional<BorrowingRequest> activeRequestOpt = userRequests.stream()
                .filter(req -> currentBook.getIsbn13().equals(req.getBookIsbn13()) &&
                        (req.getStatus() == BorrowingRequest.RequestStatus.PENDING ||
                                req.getStatus() == BorrowingRequest.RequestStatus.APPROVED))
                .max(Comparator.comparing(BorrowingRequest::getRequestDate));

        if (activeRequestOpt.isPresent()) {
            requestBorrowButton.setDisable(true);
            BorrowingRequest.RequestStatus status = activeRequestOpt.get().getStatus();
            if (status == BorrowingRequest.RequestStatus.PENDING) requestBorrowButton.setText("Đang chờ duyệt");
            else if (status == BorrowingRequest.RequestStatus.APPROVED) requestBorrowButton.setText("Đã duyệt (Chờ lấy)");
        } else {
            requestBorrowButton.setDisable(false);
            requestBorrowButton.setText("Gửi Yêu Cầu Mượn");
        }
    }

    private void loadCoverImageUI(String imageUrl) {
        if (detailCoverImageView == null) return;
        Image imageToSet = this.defaultBookCoverImage;
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            String finalImageUrl = imageUrl.trim();
            if (finalImageUrl.startsWith("//")) finalImageUrl = "https:" + finalImageUrl;
            try {
                imageToSet = new Image(finalImageUrl, true);
                if (imageToSet.isError()) {
                    System.err.println("WARN_BDC_COVER: Error loading cover from: " + finalImageUrl);
                    imageToSet = this.defaultBookCoverImage;
                }
            } catch (Exception e) {
                System.err.println("ERROR_BDC_COVER: Exception loading cover: " + finalImageUrl + ". " + e.getMessage());
                imageToSet = this.defaultBookCoverImage;
            }
        }
        detailCoverImageView.setImage(imageToSet);
    }

    @FXML
    void handleFavoriteAction(ActionEvent event) {
        if (currentUser == null || currentBook == null || favoriteBookService == null) {
            showAlert(Alert.AlertType.WARNING, "Yêu Cầu Đăng Nhập", "Vui lòng đăng nhập để thao tác.");
            return;
        }
        if (currentBook.getInternalId() <= 0) {
            showAlert(Alert.AlertType.ERROR, "Lỗi Sách", "Sách không có ID hợp lệ.");
            return;
        }

        boolean isCurrentlyFavorite = favoriteBookService.isFavorite(currentUser.getUserId(), currentBook.getInternalId());
        boolean success;
        if (isCurrentlyFavorite) {
            success = favoriteBookService.removeFavorite(currentUser.getUserId(), currentBook.getInternalId());
            if (success) showAlert(Alert.AlertType.INFORMATION, "Yêu Thích", "Đã bỏ yêu thích sách '" + currentBook.getTitleOrDefault("N/A") + "'.");
            else showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể bỏ yêu thích.");
        } else {
            success = favoriteBookService.addFavorite(currentUser.getUserId(), currentBook.getInternalId());
            if (success) showAlert(Alert.AlertType.INFORMATION, "Yêu Thích", "Đã thêm '" + currentBook.getTitleOrDefault("N/A") + "' vào danh sách yêu thích.");
            else showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể thêm vào yêu thích.");
        }
        if (success) updateFavoriteButtonStatus();
    }

    private void updateFavoriteButtonStatus() {
        if (currentUser == null || currentBook == null || favoriteButton == null || favoriteBookService == null || currentBook.getInternalId() <= 0) {
            if (favoriteButton != null) favoriteButton.setDisable(true);
            return;
        }
        favoriteButton.setDisable(false);
        if (favoriteBookService.isFavorite(currentUser.getUserId(), currentBook.getInternalId())) {
            favoriteButton.setText(HEART_FULL);
            favoriteButton.getStyleClass().setAll("button", "favorite-button-active");
        } else {
            favoriteButton.setText(HEART_EMPTY);
            favoriteButton.getStyleClass().setAll("button", "favorite-button-inactive");
        }
    }

    private void loadBookReviewsAndRating() {
        if (currentBook == null || bookReviewService == null || reviewsListView == null || averageRatingDisplayLabel == null || averageRatingStarsPane == null) {
            if (averageRatingDisplayLabel!=null) averageRatingDisplayLabel.setText("Chưa có đánh giá");
            if (averageRatingStarsPane!=null) averageRatingStarsPane.getChildren().clear();
            if (reviewsListView!=null) reviewsListView.setPlaceholder(new Label("Chưa có đánh giá nào."));
            return;
        }
        int bookInternalId = currentBook.getInternalId();
        if (bookInternalId <= 0) {
            averageRatingDisplayLabel.setText("Sách không hợp lệ.");
            reviewsListView.setPlaceholder(new Label("Sách không hợp lệ."));
            averageRatingStarsPane.getChildren().clear();
            return;
        }

        Optional<Double> avgRatingOpt = bookReviewService.getAverageRatingForBook(bookInternalId);
        List<BookReview> reviews = bookReviewService.getReviewsByBookInternalId(bookInternalId);

        if (avgRatingOpt.isPresent()) {
            averageRatingDisplayLabel.setText(String.format("%.1f/5 (%d đánh giá)", avgRatingOpt.get(), reviews.size()));
            displayRatingStars(averageRatingStarsPane, avgRatingOpt.get());
        } else {
            averageRatingDisplayLabel.setText(reviews.isEmpty() ? "Chưa có đánh giá" : String.format("0.0/5 (%d đánh giá)", reviews.size()));
            displayRatingStars(averageRatingStarsPane, 0);
        }

        if (reviews.isEmpty()) {
            reviewsListView.setPlaceholder(new Label("Chưa có đánh giá nào. Hãy là người đầu tiên!"));
        }
        reviews.sort(Comparator.comparing(BookReview::getReviewDate).reversed());
        reviewsListView.setItems(FXCollections.observableArrayList(reviews)); // Gán cho ListView
    }

    private void displayRatingStars(HBox starsPane, double rating) {
        starsPane.getChildren().clear();
        int roundedRating = (int) Math.round(rating);
        for (int i = 1; i <= 5; i++) {
            Label starLabel = new Label(i <= roundedRating ? "⭐" : "☆");
            starLabel.getStyleClass().add("rating-star-display");
            starsPane.getChildren().add(starLabel);
        }
    }

    private void setupReviewsListViewCellFactory() {
        reviewsListView.setCellFactory(lv -> new ListCell<BookReview>() {
            private final VBox content = new VBox(8);
            private final HBox authorAndRatingBox = new HBox(10);
            private final Label authorNameLabel = new Label();
            private final HBox reviewStarsDisplay = new HBox(2);
            private final Label reviewDateLabel = new Label();
            private final Label commentTextLabel = new Label();
            {
                authorNameLabel.getStyleClass().add("review-cell-author");
                reviewDateLabel.getStyleClass().add("review-cell-date");
                commentTextLabel.getStyleClass().add("review-cell-comment");
                commentTextLabel.setWrapText(true);
                commentTextLabel.setMaxWidth(Double.MAX_VALUE);
                VBox.setVgrow(commentTextLabel, Priority.ALWAYS); // <<< Priority cần import

                authorAndRatingBox.setAlignment(Pos.CENTER_LEFT);
                authorAndRatingBox.getChildren().addAll(authorNameLabel, new Label("-"), reviewStarsDisplay);

                content.getChildren().addAll(authorAndRatingBox, reviewDateLabel, commentTextLabel);
                content.setPadding(new Insets(10));
                content.getStyleClass().add("review-cell-content");
            }
            @Override
            protected void updateItem(BookReview review, boolean empty) {
                super.updateItem(review, empty);
                if (empty || review == null) {
                    setGraphic(null);
                } else {
                    authorNameLabel.setText(review.getUserUsername() != null ? review.getUserUsername() : "Người dùng ẩn danh");
                    reviewDateLabel.setText("vào lúc " + review.getFormattedReviewDate());
                    reviewStarsDisplay.getChildren().clear();
                    if (review.getRating() > 0) {
                        for (int i = 1; i <= 5; i++) {
                            Label star = new Label(i <= review.getRating() ? "★" : "☆");
                            star.getStyleClass().add("review-star-small-cell");
                            reviewStarsDisplay.getChildren().add(star);
                        }
                    } else {
                        Label noRatingText = new Label("(chưa đánh giá sao)");
                        noRatingText.getStyleClass().add("no-rating-text");
                        reviewStarsDisplay.getChildren().add(noRatingText);
                    }
                    commentTextLabel.setText(review.getCommentText());
                    setGraphic(content);
                }
            }
        });
    }

    @FXML
    void handleSubmitReviewAction(ActionEvent event) {
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Yêu Cầu Đăng Nhập", "Bạn cần đăng nhập để gửi đánh giá.");
            return;
        }
        if (currentBook == null || currentBook.getInternalId() <= 0) {
            showAlert(Alert.AlertType.ERROR, "Lỗi Sách", "Sách không hợp lệ để đánh giá.");
            return;
        }
        Integer rating = ratingComboBox.getValue();
        String comment = newCommentTextArea.getText().trim();
        if (rating == null && comment.isEmpty()) {
            setReviewError("Vui lòng chọn số sao hoặc viết bình luận."); return;
        }
        int ratingValue = (rating != null) ? rating : 0;
        if (ratingValue == 0 && comment.isEmpty() && rating == null) {
            setReviewError("Vui lòng chọn số sao hoặc viết bình luận."); return;
        }
        clearReviewError();

        BookReview newReview = new BookReview(currentBook.getInternalId(), currentUser.getUserId(), ratingValue, comment);
        Optional<BookReview> submittedReviewOpt = bookReviewService.addReview(newReview);

        if (submittedReviewOpt.isPresent()) {
            showAlert(Alert.AlertType.INFORMATION, "Gửi Thành Công", "Cảm ơn bạn đã gửi đánh giá!");
            newCommentTextArea.clear();
            ratingComboBox.getSelectionModel().clearSelection();
            ratingComboBox.setPromptText("Chọn sao");
            loadBookReviewsAndRating();
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi Gửi Đánh Giá", "Không thể gửi đánh giá. Bạn có thể đã đánh giá sách này hoặc có lỗi xảy ra.");
        }
    }

    private void setReviewError(String message) {
        if (reviewErrorLabel != null) {
            reviewErrorLabel.setText(message);
            reviewErrorLabel.setVisible(true);
            reviewErrorLabel.setManaged(true);
        }
    }
    private void clearReviewError() {
        if (reviewErrorLabel != null) {
            reviewErrorLabel.setText("");
            reviewErrorLabel.setVisible(false);
            reviewErrorLabel.setManaged(false);
        }
    }

    @FXML
    void handleRequestBorrowAction(ActionEvent event) {
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Yêu Cầu Đăng Nhập", "Vui lòng đăng nhập để mượn sách.");
            return;
        }
        if (currentBook == null || currentBook.getIsbn13() == null || currentBook.getIsbn13().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi Sách", "Sách không hợp lệ để yêu cầu mượn.");
            return;
        }

        // Luôn lấy thông tin sách mới nhất từ DB trước khi thực hiện hành động
        Optional<Book> freshBookOpt = bookManagementService.findBookByIsbn13InLibrary(currentBook.getIsbn13());
        if (freshBookOpt.isEmpty()) {
            handleMissingBookData("Sách này không còn tồn tại.");
            return;
        }
        this.currentBook = freshBookOpt.get(); // Cập nhật currentBook với dữ liệu mới nhất

        if (currentBook.getAvailableQuantity() <= 0) {
            showAlert(Alert.AlertType.INFORMATION, "Hết Sách", "Sách '" + currentBook.getTitleOrDefault("N/A") + "' hiện đã hết hàng.");
            updateAvailabilityInfoAndRequestButtonStatus(); // Cập nhật UI
            return;
        }

        // Kiểm tra lại xem user có request đang chờ/đã duyệt cho sách này không
        // (logic này được gọi bên trong updateAvailabilityInfoAndRequestButtonStatus)
        // Nếu nút requestBorrowButton bị disable sau khi cập nhật, nghĩa là không thể gửi yêu cầu
        if (requestBorrowButton.isDisable()) {
            showAlert(Alert.AlertType.INFORMATION, "Thông Báo", requestBorrowButton.getText()); // Hiển thị lý do từ text của nút
            return;
        }

        Optional<BorrowingRequest> requestOpt = borrowingRequestService.addRequest(currentUser.getUserId(), currentBook.getIsbn13());
        if (requestOpt.isPresent()) {
            showAlert(Alert.AlertType.INFORMATION, "Yêu Cầu Thành Công", "Đã gửi yêu cầu mượn sách '" + currentBook.getTitleOrDefault("N/A") + "'. Vui lòng chờ duyệt.");
            // Gửi thông báo cho Admin
            notificationService.createNotification(
                    null, // userId là null cho thông báo hệ thống/admin
                    "Người dùng '" + currentUser.getUsername() + "' yêu cầu mượn sách: " + currentBook.getTitleOrDefault("N/A") + " (ISBN: " + currentBook.getIsbn13() + ")",
                    Notification.NotificationType.NEW_LOAN_REQUEST, // Đảm bảo Type này tồn tại
                    requestOpt.get().getRequestId(), // relatedItemId
                    null // actionLink
            );
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi Yêu Cầu", "Không thể gửi yêu cầu. Bạn có thể đã có yêu cầu cho sách này.");
        }
        updateAvailabilityInfoAndRequestButtonStatus(); // Cập nhật lại trạng thái nút
    }

    @FXML
    void handleCloseDetailAction(ActionEvent event) {
        navigateBackToPreviousView();
    }

    private void navigateBackToPreviousView() {
        if (this.dashboardController != null) {
            this.dashboardController.loadViewIntoCenter("UserLibraryView.fxml"); // Hoặc view trước đó
            SessionManager.getInstance().setSelectedBook(null); // Xóa sách khỏi session
        } else {
            System.err.println("WARN_BDC_CLOSE: dashboardController is null. Cannot navigate back.");
            // Fallback an toàn: có thể không làm gì hoặc hiển thị thông báo
            showAlert(Alert.AlertType.INFORMATION, "Thông Báo", "Vui lòng sử dụng thanh điều hướng.");
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
            if (cssUrl != null) {
                dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) { System.err.println("WARN_DIALOG_CSS: Failed to load CSS for dialog: " + e.getMessage()); }
    }
}
package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.models.item.Book; // <<< THÊM IMPORT
import com.lms.quanlythuvien.models.system.Notification;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.library.BookManagementService; // <<< THÊM IMPORT
import com.lms.quanlythuvien.services.system.NotificationService;
import com.lms.quanlythuvien.utils.session.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.Optional; // <<< THÊM IMPORT
import java.util.ResourceBundle;

public class NotificationsController implements Initializable {

    @FXML private Button refreshNotificationsButton;
    @FXML private Button markAllAsReadButton;
    @FXML private Button deleteReadNotificationsButton;
    @FXML private ListView<Notification> notificationsListView;

    private NotificationService notificationService;
    private User currentUser;
    private ObservableList<Notification> observableNotificationList;

    private UserDashboardController dashboardController;

    public void setDashboardController(UserDashboardController dashboardController) {
        this.dashboardController = dashboardController;
        // Cập nhật badge ngay khi controller này được dashboard chuẩn bị
        updateUnreadCountInSidebar();
    }

    public NotificationsController() {
        notificationService = NotificationService.getInstance();
        currentUser = SessionManager.getInstance().getCurrentUser();
        observableNotificationList = FXCollections.observableArrayList();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        notificationsListView.setItems(observableNotificationList);
        setupNotificationCellFactory();

        if (currentUser != null) {
            loadNotifications();
        } else {
            notificationsListView.setPlaceholder(new Label("Vui lòng đăng nhập để xem thông báo."));
            disableActionButtons(true);
        }
    }

    private void disableActionButtons(boolean disable) {
        if (markAllAsReadButton != null) markAllAsReadButton.setDisable(disable);
        if (refreshNotificationsButton != null) refreshNotificationsButton.setDisable(disable);
        if (deleteReadNotificationsButton != null) deleteReadNotificationsButton.setDisable(disable);
    }

    private void loadNotifications() {
        if (currentUser == null) {
            observableNotificationList.clear();
            notificationsListView.setPlaceholder(new Label("Vui lòng đăng nhập."));
            disableActionButtons(true);
            return;
        }
        disableActionButtons(false);
        List<Notification> userNotifications = notificationService.getNotificationsForUser(currentUser.getUserId(), false); // Lấy cả đã đọc và chưa đọc
        observableNotificationList.setAll(userNotifications); // setAll sẽ tự động refresh ListView

        if (userNotifications.isEmpty()) {
            notificationsListView.setPlaceholder(new Label("Bạn không có thông báo nào."));
        }
        updateUnreadCountInSidebar();
    }

    private void setupNotificationCellFactory() {
        notificationsListView.setCellFactory(listView -> new ListCell<Notification>() {
            private final HBox hbox = new HBox(10);
            private final Label emojiIconLabel = new Label();
            private final VBox textContainer = new VBox(3);
            private final Label messageLabel = new Label();
            private final Label timestampLabel = new Label();
            // private final Region spacer = new Region(); // Có thể không cần spacer nếu không có nút action riêng trong cell
            // private final Button actionButton = new Button();

            {
                emojiIconLabel.setMinWidth(25);
                emojiIconLabel.setMinHeight(25);
                emojiIconLabel.setAlignment(Pos.CENTER);
                emojiIconLabel.getStyleClass().add("notification-emoji-icon");

                messageLabel.setWrapText(true);
                messageLabel.getStyleClass().add("notification-message-text");
                timestampLabel.getStyleClass().add("notification-timestamp-text");

                VBox.setVgrow(messageLabel, Priority.ALWAYS);
                HBox.setHgrow(textContainer, Priority.ALWAYS);
                textContainer.getChildren().addAll(messageLabel, timestampLabel);

                // HBox.setHgrow(spacer, Priority.ALWAYS);
                // actionButton.setVisible(false); // Mặc định ẩn nút action
                // actionButton.getStyleClass().add("secondary-button-small");

                hbox.getChildren().addAll(emojiIconLabel, textContainer /*, spacer, actionButton*/);
                hbox.setAlignment(Pos.CENTER_LEFT);
                hbox.setPadding(new Insets(10));
                hbox.getStyleClass().add("notification-cell-hbox");

                this.setOnMouseClicked(event -> {
                    Notification item = getItem();
                    if (item != null) {
                        if (!item.isRead()) {
                            if (notificationService.markAsRead(item.getId())) {
                                item.setRead(true);
                                updateItemStyleBasedOnReadStatus(item); // Chỉ cập nhật style
                                // Giảm số lượng badge ngay lập tức thay vì load lại toàn bộ list
                                updateUnreadCountInSidebar();
                                // Không cần gọi updateItem(item, false) vì thuộc tính isRead của item đã thay đổi,
                                // và nếu cell được tái sử dụng, updateItem sẽ được gọi.
                                // Hoặc có thể gọi this.getListView().refresh() để làm mới toàn bộ listview.
                            }
                        }
                        // Xử lý actionLink
                        if (item.getActionLink() != null && !item.getActionLink().isEmpty()) {
                            handleNotificationAction(item);
                        }
                    }
                });
            }

            private void updateItemStyleBasedOnReadStatus(Notification item) {
                getStyleClass().removeAll("notification-cell-unread", "notification-cell-read");
                if (item != null && !item.isRead()) {
                    getStyleClass().add("notification-cell-unread");
                } else {
                    getStyleClass().add("notification-cell-read");
                }
            }

            @Override
            protected void updateItem(Notification item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    messageLabel.setText(item.getMessage());
                    timestampLabel.setText(item.getFormattedCreatedAt());
                    String emoji = "🔔"; // Default
                    switch (item.getType()) {
                        case INFO: emoji = "ℹ️"; break;
                        case SUCCESS: case LOAN_APPROVED_USER: emoji = "✅"; break;
                        case WARNING: case LOAN_DUE_SOON_USER: emoji = "⚠️"; break;
                        case ERROR: case LOAN_REJECTED_USER: emoji = "❌"; break;
                        case USER_YoutubeED: emoji = "💬"; break;
                        case DONATION_PROCESSED_USER: emoji = "🎁"; break;
                        // Thêm các NotificationType khác của User ở đây
                        default: emoji = "🔔";
                    }
                    emojiIconLabel.setText(emoji);
                    updateItemStyleBasedOnReadStatus(item);
                    setGraphic(hbox);
                }
            }
        });
    }

    private void handleNotificationAction(Notification notification) {
        if (dashboardController == null || notification.getActionLink() == null) return;

        String action = notification.getActionLink().toUpperCase(); // Viết hoa để so sánh dễ hơn
        String relatedId = notification.getRelatedItemId();

        System.out.println("INFO_NOTIF_ACTION: Handling action: " + action + ", RelatedID: " + relatedId);

        switch (action) {
            case "VIEW_BOOK_DETAIL":
                if (relatedId != null && !relatedId.isEmpty()) {
                    // Book ID có thể là ISBN hoặc internalId. Ưu tiên tìm theo ISBN trước.
                    Optional<Book> bookOpt = BookManagementService.getInstance().findBookByIsbn13InLibrary(relatedId);
                    if (bookOpt.isEmpty() && relatedId.matches("\\d+")) { // Nếu không phải ISBN và là số, thử tìm bằng internalId
                        try {
                            bookOpt = BookManagementService.getInstance().findBookByInternalId(Integer.parseInt(relatedId));
                        } catch (NumberFormatException e) {
                            System.err.println("ERROR_NOTIF_ACTION: relatedId " + relatedId + " is not a valid integer for internalId.");
                        }
                    }

                    if (bookOpt.isPresent()) { // Sửa: dùng isPresent()
                        SessionManager.getInstance().setSelectedBook(bookOpt.get()); // Sửa: dùng get()
                        dashboardController.loadViewIntoCenter("user/BookDetailView.fxml");
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Không Tìm Thấy", "Không tìm thấy sách liên quan (ID/ISBN: " + relatedId + "). Sách có thể đã bị xóa.");
                    }
                } else {
                    showAlert(Alert.AlertType.WARNING, "Thiếu Thông Tin", "Thông báo này không có ID sách liên quan.");
                }
                break;
            case "VIEW_MY_LOANS":
                dashboardController.loadViewIntoCenter("MyBookshelfView.fxml");
                SessionManager.getInstance().setTargetMyBookshelfTab("BORROWED_TAB_ID"); // Cần định nghĩa ID cho tab
                break;
            case "VIEW_MY_REQUESTS":
                dashboardController.loadViewIntoCenter("MyBookshelfView.fxml");
                SessionManager.getInstance().setTargetMyBookshelfTab("REQUESTS_TAB_ID");
                break;
            // Thêm các case khác
            default:
                System.out.println("INFO_NOTIF_ACTION: No specific handler for action: " + action);
                break;
        }
    }


    @FXML
    void handleRefreshNotifications(ActionEvent event) {
        if (currentUser != null) loadNotifications();
        else showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng đăng nhập.");
    }

    @FXML
    void handleMarkAllAsRead(ActionEvent event) {
        if (currentUser != null) {
            if (notificationService.markAllAsRead(currentUser.getUserId())) { // markAllAsRead giờ trả về boolean
                showAlert(Alert.AlertType.INFORMATION, "Hoàn tất", "Tất cả thông báo đã được đánh dấu đã đọc.");
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Không có thông báo mới nào để đánh dấu.");
            }
            loadNotifications(); // Luôn tải lại
        } else {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng đăng nhập.");
        }
    }

    @FXML
    void handleDeleteReadNotificationsAction(ActionEvent event) {
        if (currentUser != null) {
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION, "Xóa tất cả thông báo đã đọc?", ButtonType.YES, ButtonType.NO);
            confirmDialog.setTitle("Xác nhận Xóa");
            confirmDialog.setHeaderText(null);
            applyDialogStyles(confirmDialog);
            Optional<ButtonType> result = confirmDialog.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.YES) { // Sửa: dùng isPresent() và get()
                if (notificationService.deleteReadNotifications(currentUser.getUserId())) {
                    showAlert(Alert.AlertType.INFORMATION, "Hoàn tất", "Đã xóa các thông báo đã đọc.");
                } else {
                    showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Không có thông báo đã đọc nào để xóa.");
                }
                loadNotifications();
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng đăng nhập.");
        }
    }

    private void updateUnreadCountInSidebar() {
        if (currentUser != null && dashboardController != null) {
            int unreadCount = notificationService.getUnreadNotificationCount(currentUser.getUserId());
            dashboardController.updateNotificationBadgeOnSidebar(unreadCount); // Gọi hàm đã thêm ở UserDashboardController
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
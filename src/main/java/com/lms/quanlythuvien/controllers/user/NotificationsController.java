package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.system.Notification;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.library.BookManagementService;
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
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList; // <<< ĐÃ THÊM IMPORT
import java.util.List;
import java.util.Optional;
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
        updateUnreadCountInSidebar();
    }

    public NotificationsController() {
        notificationService = NotificationService.getInstance();
        observableNotificationList = FXCollections.observableArrayList();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.currentUser = SessionManager.getInstance().getCurrentUser();

        notificationsListView.setItems(observableNotificationList);
        setupNotificationCellFactory();

        if (currentUser != null) {
            loadNotifications();
            disableActionButtons(false);
        } else {
            notificationsListView.setPlaceholder(new Label("Vui lòng đăng nhập để xem thông báo."));
            disableActionButtons(true);
        }
    }

    public void onViewActivated() {
        System.out.println("DEBUG_UserNotifications: View Activated.");
        this.currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            loadNotifications();
            disableActionButtons(false);
        } else {
            observableNotificationList.clear();
            notificationsListView.setPlaceholder(new Label("Vui lòng đăng nhập để xem thông báo."));
            disableActionButtons(true);
            updateUnreadCountInSidebar(); // Cập nhật lại badge
        }
    }

    private void disableActionButtons(boolean disable) {
        if (markAllAsReadButton != null) markAllAsReadButton.setDisable(disable);
        if (refreshNotificationsButton != null) refreshNotificationsButton.setDisable(disable);
        if (deleteReadNotificationsButton != null) deleteReadNotificationsButton.setDisable(disable);
    }

    private void loadNotifications() {
        if (currentUser == null) {
            System.out.println("DEBUG_UserNotifications: Cannot load notifications, current user is null.");
            observableNotificationList.clear();
            notificationsListView.setPlaceholder(new Label("Vui lòng đăng nhập."));
            disableActionButtons(true);
            updateUnreadCountInSidebar();
            return;
        }
        disableActionButtons(false);
        System.out.println("DEBUG_UserNotifications: Loading notifications for user: " + currentUser.getUserId());
        List<Notification> userNotifications = notificationService.getNotificationsForUser(currentUser.getUserId(), false);

        if (userNotifications == null) {
            System.err.println("ERROR_UserNotifications: Notification service returned null list for user: " + currentUser.getUserId());
            userNotifications = new ArrayList<>(); // Khởi tạo list rỗng để tránh NullPointerException
        }

        observableNotificationList.setAll(userNotifications);

        if (userNotifications.isEmpty()) {
            notificationsListView.setPlaceholder(new Label("Bạn không có thông báo nào."));
        } else {
            notificationsListView.setPlaceholder(null);
        }
        System.out.println("DEBUG_UserNotifications: Loaded " + userNotifications.size() + " notifications.");
        updateUnreadCountInSidebar();
    }

    private void setupNotificationCellFactory() {
        notificationsListView.setCellFactory(listView -> new ListCell<Notification>() {
            private final HBox hbox = new HBox(10);
            private final Label emojiIconLabel = new Label();
            private final VBox textContainer = new VBox(3);
            private final Label messageLabel = new Label();
            private final Label timestampLabel = new Label();

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

                hbox.getChildren().addAll(emojiIconLabel, textContainer);
                hbox.setAlignment(Pos.CENTER_LEFT);
                hbox.setPadding(new Insets(10, 15, 10, 15));
                hbox.getStyleClass().add("notification-cell-hbox");

                this.setOnMouseClicked(event -> {
                    Notification item = getItem();
                    if (item != null) {
                        if (!item.isRead()) {
                            if (notificationService.markAsRead(item.getId())) {
                                item.setRead(true);
                                updateItemStyleBasedOnReadStatus(item);
                                updateUnreadCountInSidebar();
                            }
                        }
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
                    getStyleClass().removeAll("notification-cell-unread", "notification-cell-read");
                } else {
                    messageLabel.setText(item.getMessage());
                    timestampLabel.setText(item.getFormattedCreatedAt()); // Sử dụng phương thức từ model

                    String emoji = "🔔";
                    Notification.NotificationType type = item.getType();
                    if (type != null) {
                        switch (type) {
                            case INFO: emoji = "ℹ️"; break;
                            case SUCCESS:
                            case LOAN_APPROVED_USER: emoji = "✅"; break;
                            case WARNING:
                            case LOAN_DUE_SOON_USER: emoji = "⚠️"; break;
                            case ERROR:
                            case LOAN_REJECTED_USER: emoji = "❌"; break;
                            case USER_YoutubeED: emoji = "💬"; break; // Đã sửa từ USER_YoutubeED
                            case DONATION_PROCESSED_USER: emoji = "🎁"; break;
                            case ACCOUNT_LOCKED_USER: emoji = "🔒"; break;
                            case FINE_ISSUED_USER: emoji = "💰"; break;
                            default: emoji = "🔔";
                        }
                    }
                    emojiIconLabel.setText(emoji);
                    updateItemStyleBasedOnReadStatus(item);
                    setGraphic(hbox);
                }
            }
        });
    }

    private void handleNotificationAction(Notification notification) {
        if (dashboardController == null || notification.getActionLink() == null) {
            System.err.println("ERROR_UserNotifications_Action: DashboardController is null or actionLink is null for notification ID: " + notification.getId());
            return;
        }

        String action = notification.getActionLink().toUpperCase();
        String relatedId = notification.getRelatedItemId();

        System.out.println("INFO_UserNotifications_Action: Handling action: " + action + ", RelatedID: " + relatedId);

        switch (action) {
            case "VIEW_BOOK_DETAIL":
                if (relatedId != null && !relatedId.isEmpty()) {
                    Optional<Book> bookOpt = BookManagementService.getInstance().findBookByIsbn13InLibrary(relatedId);
                    if (bookOpt.isEmpty() && relatedId.matches("\\d+")) {
                        try {
                            bookOpt = BookManagementService.getInstance().findBookByInternalId(Integer.parseInt(relatedId));
                        } catch (NumberFormatException e) {
                            System.err.println("ERROR_UserNotifications_Action: relatedId " + relatedId + " is not a valid integer for internalId.");
                        }
                    }

                    if (bookOpt.isPresent()) {
                        SessionManager.getInstance().setSelectedBook(bookOpt.get());
                        dashboardController.loadViewIntoCenter("user/BookDetailView.fxml");
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Không Tìm Thấy", "Không tìm thấy sách liên quan (ID/ISBN: " + relatedId + ").");
                    }
                } else {
                    showAlert(Alert.AlertType.WARNING, "Thiếu Thông Tin", "Thông báo này không có ID sách liên quan.");
                }
                break;
            case "VIEW_MY_LOANS":
                dashboardController.navigateToMyBookshelf("borrowed"); // Sử dụng định danh tab
                break;
            case "VIEW_MY_REQUESTS":
                dashboardController.navigateToMyBookshelf("requests"); // Sử dụng định danh tab
                break;
            default:
                System.out.println("INFO_UserNotifications_Action: No specific handler for action: " + action);
                break;
        }
    }

    @FXML
    void handleRefreshNotifications(ActionEvent event) {
        System.out.println("DEBUG_UserNotifications: Refresh button clicked.");
        if (currentUser != null) loadNotifications();
        else showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng đăng nhập để làm mới thông báo.");
    }

    @FXML
    void handleMarkAllAsRead(ActionEvent event) {
        if (currentUser != null) {
            if (notificationService.markAllAsRead(currentUser.getUserId())) {
                showAlert(Alert.AlertType.INFORMATION, "Hoàn tất", "Tất cả thông báo đã được đánh dấu đã đọc.");
                loadNotifications();
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Không có thông báo mới nào hoặc không thể thực hiện.");
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng đăng nhập.");
        }
    }

    @FXML
    void handleDeleteReadNotificationsAction(ActionEvent event) {
        if (currentUser != null) {
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION,
                    "Bạn có chắc chắn muốn xóa tất cả thông báo ĐÃ ĐỌC không?",
                    ButtonType.YES, ButtonType.NO);
            confirmDialog.setTitle("Xác Nhận Xóa");
            confirmDialog.setHeaderText(null);
            if (confirmDialog.getDialogPane() != null) applyDialogStyles(confirmDialog.getDialogPane());
            Optional<ButtonType> result = confirmDialog.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.YES) {
                if (notificationService.deleteReadNotifications(currentUser.getUserId())) {
                    showAlert(Alert.AlertType.INFORMATION, "Hoàn tất", "Đã xóa các thông báo đã đọc.");
                } else {
                    showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Không có thông báo đã đọc nào để xóa hoặc có lỗi xảy ra.");
                }
                loadNotifications();
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng đăng nhập.");
        }
    }

    private void updateUnreadCountInSidebar() {
        if (dashboardController != null && currentUser != null) { // Đảm bảo currentUser cũng không null
            int unreadCount = notificationService.getUnreadNotificationCount(currentUser.getUserId());
            System.out.println("DEBUG_UserNotifications: Updating unread count for sidebar: " + unreadCount);
            dashboardController.updateNotificationBadgeOnSidebar(unreadCount);
        } else {
            System.out.println("DEBUG_UserNotifications: Cannot update unread count, currentUser or dashboardController is null.");
            if(dashboardController != null) dashboardController.updateNotificationBadgeOnSidebar(0);
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (alert.getDialogPane() != null) {
            applyDialogStyles(alert.getDialogPane());
        }
        alert.showAndWait();
    }

    private void applyDialogStyles(DialogPane dialogPane) {
        try {
            URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
            if (cssUrl != null) {
                dialogPane.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.err.println("WARN_UserNotifications_DIALOG_CSS: CSS file not found for dialogs.");
            }
        } catch (Exception e) {
            System.err.println("WARN_UserNotifications_DIALOG_CSS: Failed to load CSS for dialog: " + e.getMessage());
        }
    }
}
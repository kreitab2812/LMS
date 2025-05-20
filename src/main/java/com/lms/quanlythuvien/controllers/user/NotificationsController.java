package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.models.system.Notification;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.system.NotificationService;
import com.lms.quanlythuvien.utils.session.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
// Bỏ import ImageView và Image nếu không dùng nữa
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// import java.io.InputStream; // Không cần nữa nếu không load ảnh
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class NotificationsController implements Initializable {

    @FXML private Button refreshNotificationsButton;
    @FXML private Button markAllAsReadButton;
    @FXML private Button deleteReadNotificationsButton;
    @FXML private ListView<Notification> notificationsListView;

    private NotificationService notificationService;
    private User currentUser;
    private ObservableList<Notification> observableNotificationList;

    // Không cần các biến Image cho icon nữa
    // private Image infoIcon, successIcon, warningIcon, errorIcon, loanIcon;

    public NotificationsController() {
        notificationService = NotificationService.getInstance();
        currentUser = SessionManager.getInstance().getCurrentUser();
        observableNotificationList = FXCollections.observableArrayList();
        // Không cần gọi loadIcons() nữa
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        notificationsListView.setItems(observableNotificationList);
        setupNotificationCellFactory();

        if (currentUser != null) {
            loadNotifications();
        } else {
            notificationsListView.setPlaceholder(new Label("Vui lòng đăng nhập để xem thông báo."));
            if (markAllAsReadButton != null) markAllAsReadButton.setDisable(true);
            if (refreshNotificationsButton != null) refreshNotificationsButton.setDisable(true);
            if (deleteReadNotificationsButton != null) deleteReadNotificationsButton.setDisable(true);
        }
    }

    // Bỏ phương thức loadIcons() và loadImage()

    private void loadNotifications() {
        if (currentUser == null) return;
        List<Notification> userNotifications = notificationService.getNotificationsForUser(currentUser.getUserId(), false);
        observableNotificationList.setAll(userNotifications);
        notificationsListView.refresh();
        updateUnreadCountInSidebar();
    }

    private void setupNotificationCellFactory() {
        notificationsListView.setCellFactory(listView -> new ListCell<Notification>() {
            private final HBox hbox = new HBox(10); // Spacing giữa các thành phần
            private final Label emojiIconLabel = new Label(); // Dùng Label cho Emoji
            private final VBox textContainer = new VBox(3);
            private final Label messageLabel = new Label();
            private final Label timestampLabel = new Label();

            // Khối khởi tạo instance
            {
                emojiIconLabel.setMinWidth(24); // Đảm bảo có không gian cho emoji
                emojiIconLabel.setAlignment(Pos.CENTER);
                emojiIconLabel.setStyle("-fx-font-size: 1.5em;"); // Kích thước Emoji

                messageLabel.setWrapText(true);
                timestampLabel.getStyleClass().add("notification-timestamp");

                VBox.setVgrow(messageLabel, Priority.ALWAYS);
                HBox.setHgrow(textContainer, Priority.ALWAYS);
                textContainer.getChildren().addAll(messageLabel, timestampLabel);
                // Thêm emojiLabel vào HBox
                hbox.getChildren().addAll(emojiIconLabel, textContainer);
                hbox.setAlignment(Pos.CENTER_LEFT);
                hbox.setPadding(new Insets(8, 10, 8, 10));

                this.setOnMouseClicked(event -> {
                    Notification item = getItem();
                    if (item != null && !item.isRead()) {
                        if (notificationService.markAsRead(item.getId())) {
                            item.setRead(true);
                            updateItemStyle(item);
                            updateUnreadCountInSidebar();
                        }
                    }
                    if (item != null && item.getActionLink() != null && !item.getActionLink().isEmpty()){
                        System.out.println("Notification action link clicked: " + item.getActionLink());
                        // TODO: Xử lý actionLink (ví dụ: điều hướng)
                        // Ví dụ: if ("view_loan_details".equals(item.getActionLink())) {
                        //     MainApp.loadScene("user/LoanDetailsView.fxml", item.getRelatedItemId());
                        // }
                    }
                });
            }

            private void updateItemStyle(Notification item) {
                if (item != null && !item.isRead()) {
                    messageLabel.setStyle("-fx-font-weight: bold;");
                    // Có thể thêm styleClass cho toàn bộ HBox nếu chưa đọc
                    // hbox.getStyleClass().add("unread-notification-cell");
                    // hbox.getStyleClass().remove("read-notification-cell");
                } else {
                    messageLabel.setStyle("-fx-font-weight: normal;");
                    // hbox.getStyleClass().remove("unread-notification-cell");
                    // hbox.getStyleClass().add("read-notification-cell");
                }
            }

            @Override
            protected void updateItem(Notification item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    messageLabel.setText(item.getMessage());
                    timestampLabel.setText(item.getFormattedCreatedAt());

                    String emoji = "ℹ️"; // Default INFO emoji
                    switch (item.getType()) {
                        case INFO:           emoji = "ℹ️"; break;
                        case SUCCESS:        emoji = "✅"; break;
                        case WARNING:        emoji = "⚠️"; break;
                        case ERROR:          emoji = "❌"; break;
                        case LOAN_APPROVED:  emoji = "👍"; break; // Hoặc 🎉
                        case LOAN_REJECTED:  emoji = "👎"; break;
                        case LOAN_DUE_SOON:  emoji = "⏰"; break;
                        case LOAN_OVERDUE:   emoji = "❗"; break;
                        // case NEW_BOOK_MATCHING_PREFERENCE: emoji = "📖"; break; // Nếu có type này
                        default: emoji = "ℹ️";
                    }
                    emojiIconLabel.setText(emoji);

                    updateItemStyle(item);
                    setGraphic(hbox);
                }
            }
        });
    }

    @FXML
    void handleRefreshNotifications(ActionEvent event) {
        if (currentUser != null) {
            loadNotifications();
            showAlert(Alert.AlertType.INFORMATION, "Làm mới", "Đã cập nhật danh sách thông báo.");
        } else {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Không thể làm mới, vui lòng đăng nhập.");
        }
    }

    @FXML
    void handleMarkAllAsRead(ActionEvent event) {
        if (currentUser != null) {
            boolean anyUpdated = notificationService.markAllAsRead(currentUser.getUserId());
            loadNotifications(); // Luôn tải lại để refresh UI
            if (anyUpdated) {
                showAlert(Alert.AlertType.INFORMATION, "Hoàn tất", "Tất cả thông báo đã được đánh dấu là đã đọc.");
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Không có thông báo mới nào để đánh dấu hoặc đã đọc tất cả.");
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Không thể thực hiện, vui lòng đăng nhập.");
        }
    }

    @FXML
    void handleDeleteReadNotificationsAction(ActionEvent event) {
        if (currentUser != null) {
            boolean anyDeleted = notificationService.deleteReadNotifications(currentUser.getUserId());
            if (anyDeleted) {
                loadNotifications(); // Tải lại để cập nhật UI
                showAlert(Alert.AlertType.INFORMATION, "Hoàn tất", "Đã xóa các thông báo đã đọc.");
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Không có thông báo đã đọc nào để xóa hoặc có lỗi xảy ra.");
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Không thể thực hiện, vui lòng đăng nhập.");
        }
    }

    private void updateUnreadCountInSidebar() {
        if (currentUser != null) {
            int unreadCount = notificationService.getUnreadNotificationCount(currentUser.getUserId());
            System.out.println("DEBUG_NOTIF_CTRL: Unread notifications for user " + currentUser.getUserId() + ": " + unreadCount);
            // TODO: Cập nhật một Label hoặc Badge trên nút "Thông báo" ở UserDashboardController
            // Ví dụ: ((UserDashboardController) MainApp.getControllerForCurrentScene()).updateNotificationBadge(unreadCount);
            // Điều này đòi hỏi cách lấy controller của UserDashboard một cách an toàn.
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
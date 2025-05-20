package com.lms.quanlythuvien.controllers.admin;

import com.lms.quanlythuvien.models.system.Notification;
import com.lms.quanlythuvien.models.system.Notification.NotificationType;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminNotificationsController implements Initializable {
    @FXML private ListView<Notification> adminNotificationsListView;
    @FXML private Button refreshAdminNotificationsButton;
    @FXML private Button markAllAdminNotificationsReadButton;

    private NotificationService notificationService;
    private User currentAdmin;
    private ObservableList<Notification> adminObservableNotifications;

    public AdminNotificationsController() {
        notificationService = NotificationService.getInstance();
        currentAdmin = SessionManager.getInstance().getCurrentUser();
        adminObservableNotifications = FXCollections.observableArrayList();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        adminNotificationsListView.setItems(adminObservableNotifications);
        setupAdminNotificationCellFactory();

        if (currentAdmin != null) {
            loadAdminNotifications();
        } else {
            adminNotificationsListView.setPlaceholder(new Label("Không thể tải thông báo. Phiên làm việc Quản trị viên không hợp lệ."));
            System.err.println("ERROR_ANC_INIT: currentAdmin is null in AdminNotificationsController.");
        }
    }

    private void loadAdminNotifications() {
        if (currentAdmin == null) {
            adminObservableNotifications.clear();
            adminNotificationsListView.setPlaceholder(new Label("Yêu cầu đăng nhập với quyền Quản trị viên."));
            return;
        }

        // Gọi phương thức mới (sẽ được thêm vào NotificationService)
        List<Notification> fetchedNotifications = notificationService.getNotificationsForAdmin(true); // true = unreadOnly

        if (fetchedNotifications != null) {
            adminObservableNotifications.setAll(fetchedNotifications);
        } else {
            adminObservableNotifications.clear();
            System.err.println("ERROR_ANC_LOAD: Fetched admin notifications list is null.");
        }

        if (adminObservableNotifications.isEmpty()) {
            adminNotificationsListView.setPlaceholder(new Label("Không có thông báo mới nào cho quản trị viên."));
        }
        System.out.println("DEBUG_ANC_LOAD: Loaded " + adminObservableNotifications.size() + " admin notifications.");
    }

    private void setupAdminNotificationCellFactory() {
        adminNotificationsListView.setCellFactory(lv -> new ListCell<Notification>() {
            private final HBox hbox = new HBox(10);
            private final Label emojiIconLabel = new Label();
            private final VBox textContainer = new VBox(3);
            private final Label messageLabel = new Label();
            private final Label timestampLabel = new Label();
            private final Region spacer = new Region();
            private final Button actionButton = new Button();

            {
                emojiIconLabel.setMinWidth(24);
                emojiIconLabel.setAlignment(Pos.CENTER);
                emojiIconLabel.setStyle("-fx-font-size: 1.4em;");
                messageLabel.setWrapText(true);
                messageLabel.getStyleClass().add("notification-message");
                timestampLabel.getStyleClass().add("notification-timestamp");
                textContainer.getChildren().addAll(messageLabel, timestampLabel);
                VBox.setVgrow(messageLabel, Priority.ALWAYS);
                actionButton.setVisible(false);
                actionButton.getStyleClass().add("secondary-button-small");
                HBox.setHgrow(textContainer, Priority.ALWAYS);
                HBox.setHgrow(spacer, Priority.ALWAYS);
                hbox.getChildren().addAll(emojiIconLabel, textContainer, spacer, actionButton);
                hbox.setAlignment(Pos.CENTER_LEFT);
                hbox.setPadding(new Insets(8, 10, 8, 10));
            }

            @Override
            protected void updateItem(Notification item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    messageLabel.setText(item.getMessage());
                    timestampLabel.setText(item.getFormattedCreatedAt());
                    getStyleClass().removeAll("notification-unread", "notification-read",
                            "notification-info", "notification-success",
                            "notification-warning", "notification-error");
                    if (!item.isRead()) getStyleClass().add("notification-unread");
                    else getStyleClass().add("notification-read");

                    String emoji = "";
                    switch (item.getType()) {
                        case NEW_LOAN_REQUEST: // <<< Sửa thành NEW_LOAN_REQUEST
                            emoji = "📨"; getStyleClass().add("notification-info"); break;
                        case NEW_USER_QUESTION: // <<< Sửa thành NEW_USER_QUESTION
                            emoji = "❓"; getStyleClass().add("notification-info"); break;
                        case USER_LOAN_OVERDUE_ADMIN: // (Nếu cậu đã thêm type này)
                            emoji = "⏰"; getStyleClass().add("notification-warning"); break;
                        case LOAN_OVERDUE:
                            emoji = "❗"; getStyleClass().add("notification-error"); break;
                        case LOAN_APPROVED:
                            emoji = "👍"; getStyleClass().add("notification-success"); break;
                        default:
                            emoji = "ℹ️"; getStyleClass().add("notification-info"); break;
                    }
                    emojiIconLabel.setText(emoji);

                    actionButton.setVisible(true);
                    if (!item.isRead()) {
                        actionButton.setText("Đã xem");
                        actionButton.setDisable(false);
                        actionButton.setOnAction(e -> {
                            if (notificationService.markAsRead(item.getId())) {
                                item.setRead(true);
                                updateItem(item, false);
                                // Cân nhắc: Gọi lại loadAdminNotifications() để cập nhật số lượng, hoặc
                                // chỉ cập nhật trạng thái của item này trong ObservableList.
                                // Để đơn giản, có thể gọi loadAdminNotifications()
                                // nhưng sẽ hiệu quả hơn nếu chỉ cập nhật item.
                            }
                        });
                    } else {
                        actionButton.setText("Đã xem");
                        actionButton.setDisable(true);
                    }
                    setGraphic(hbox);
                }
            }
        });
    }

    @FXML private void handleRefreshNotifications(ActionEvent event) {
        System.out.println("DEBUG_ANC_REFRESH: Refreshing admin notifications.");
        loadAdminNotifications();
    }

    @FXML private void handleMarkAllAsRead(ActionEvent event) {
        System.out.println("DEBUG_ANC_MARK_ALL_READ: Attempting to mark all admin notifications as read.");
        if (currentAdmin != null) {
            // Gọi phương thức mới (sẽ được thêm vào NotificationService)
            if (notificationService.markAllAdminNotificationsAsRead()) {
                System.out.println("INFO_ANC_MARK_ALL_READ: All admin notifications marked as read successfully.");
            } else {
                System.err.println("WARN_ANC_MARK_ALL_READ: Could not mark all admin notifications as read or no unread notifications found.");
            }
            loadAdminNotifications();
        } else {
            System.err.println("ERROR_ANC_MARK_ALL_READ: No current admin to mark notifications for.");
        }
    }
}
package com.lms.quanlythuvien.controllers.admin;

import com.lms.quanlythuvien.models.system.Notification;
import com.lms.quanlythuvien.models.user.User; // Thêm nếu cần User
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
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminNotificationsController implements Initializable {
    @FXML private ListView<Notification> adminNotificationsListView;
    @FXML private Button refreshAdminNotificationsButton;
    @FXML private Button markAllAdminNotificationsReadButton;
    // @FXML private Button deleteReadAdminNotificationsButton; // Bạn có thể thêm nút này nếu muốn

    private NotificationService notificationService;
    private User currentAdmin;
    private ObservableList<Notification> adminObservableNotifications;

    private AdminDashboardController dashboardController;

    public void setDashboardController(AdminDashboardController dashboardController) {
        this.dashboardController = dashboardController;
        // updateUnreadCountInAdminSidebar(); // Nếu có badge cho admin
    }

    public AdminNotificationsController() {
        notificationService = NotificationService.getInstance();
        adminObservableNotifications = FXCollections.observableArrayList();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.currentAdmin = SessionManager.getInstance().getCurrentUser();

        adminNotificationsListView.setItems(adminObservableNotifications);
        setupAdminNotificationCellFactory();

        if (currentAdmin != null && currentAdmin.getRole() == User.Role.ADMIN) {
            loadAdminNotifications();
            if(refreshAdminNotificationsButton!=null) refreshAdminNotificationsButton.setDisable(false);
            if(markAllAdminNotificationsReadButton!=null) markAllAdminNotificationsReadButton.setDisable(false);
        } else {
            adminObservableNotifications.clear();
            adminNotificationsListView.setPlaceholder(new Label("Không thể tải thông báo. Vui lòng đăng nhập với quyền Quản trị viên."));
            System.err.println("ERROR_ANC_INIT: currentAdmin is null or not an ADMIN in AdminNotificationsController.");
            if(refreshAdminNotificationsButton!=null) refreshAdminNotificationsButton.setDisable(true);
            if(markAllAdminNotificationsReadButton!=null) markAllAdminNotificationsReadButton.setDisable(true);
        }
    }

    public void onViewActivated() {
        System.out.println("DEBUG_AdminNotifications: View Activated.");
        this.currentAdmin = SessionManager.getInstance().getCurrentUser();
        if (currentAdmin != null && currentAdmin.getRole() == User.Role.ADMIN) {
            loadAdminNotifications();
        } else {
            adminObservableNotifications.clear();
            adminNotificationsListView.setPlaceholder(new Label("Vui lòng đăng nhập lại với quyền Quản trị viên."));
        }
    }

    private void loadAdminNotifications() {
        if (currentAdmin == null) {
            adminObservableNotifications.clear();
            adminNotificationsListView.setPlaceholder(new Label("Yêu cầu đăng nhập với quyền Quản trị viên."));
            // updateUnreadCountInAdminSidebar(); // Cập nhật badge thành 0
            return;
        }
        System.out.println("DEBUG_ANC_LOAD: Loading admin notifications (unread only)...");
        // Giả sử getNotificationsForAdmin(true) chỉ lấy thông báo CHƯA ĐỌC
        List<Notification> fetchedNotifications = notificationService.getNotificationsForAdmin(true);

        if (fetchedNotifications == null) {
            System.err.println("ERROR_ANC_LOAD: Fetched admin notifications list is null from service.");
            fetchedNotifications = new ArrayList<>();
        }

        adminObservableNotifications.setAll(fetchedNotifications);

        if (adminObservableNotifications.isEmpty()) {
            adminNotificationsListView.setPlaceholder(new Label("Không có thông báo mới nào cho quản trị viên."));
        } else {
            adminNotificationsListView.setPlaceholder(null);
        }
        System.out.println("DEBUG_ANC_LOAD: Loaded " + adminObservableNotifications.size() + " unread admin notifications.");
        // updateUnreadCountInAdminSidebar();
    }

    private void setupAdminNotificationCellFactory() {
        adminNotificationsListView.setCellFactory(lv -> new ListCell<Notification>() {
            private final HBox hbox = new HBox(10);
            private final Label emojiIconLabel = new Label();
            private final VBox textContainer = new VBox(3);
            private final Label messageLabel = new Label();
            private final Label timestampLabel = new Label();
            private final Button actionButton = new Button("Đã xem");

            {
                emojiIconLabel.setMinWidth(25); // Đảm bảo đủ rộng
                emojiIconLabel.setMinHeight(25);
                emojiIconLabel.setAlignment(Pos.CENTER);
                emojiIconLabel.getStyleClass().add("notification-emoji-icon");

                messageLabel.setWrapText(true);
                messageLabel.getStyleClass().add("notification-message-text");
                timestampLabel.getStyleClass().add("notification-timestamp-text");

                textContainer.getChildren().addAll(messageLabel, timestampLabel);
                VBox.setVgrow(messageLabel, Priority.ALWAYS);
                HBox.setHgrow(textContainer, Priority.ALWAYS); // Cho textContainer chiếm không gian

                actionButton.getStyleClass().add("secondary-button-small");
                actionButton.setOnAction(e -> {
                    Notification item = getItem();
                    if (item != null && !item.isRead()) {
                        if (notificationService.markAsRead(item.getId())) {
                            // Khi đánh dấu đã đọc, và view này chỉ hiển thị unread, item đó sẽ biến mất khi load lại
                            loadAdminNotifications();
                            // updateUnreadCountInAdminSidebar();
                        }
                    }
                });

                hbox.getChildren().addAll(emojiIconLabel, textContainer, actionButton);
                hbox.setAlignment(Pos.CENTER_LEFT);
                hbox.setPadding(new Insets(10, 15, 10, 15));
                hbox.getStyleClass().add("notification-cell-hbox");

                this.setOnMouseClicked(event -> {
                    Notification item = getItem();
                    if (item != null) {
                        if (!item.isRead()) { // Tự động đánh dấu đã đọc khi click
                            if (notificationService.markAsRead(item.getId())) {
                                item.setRead(true); // Chỉ cập nhật model, list sẽ tự refresh khi loadAdminNotifications() được gọi
                                // Không cần updateItemStyle ở đây nếu loadAdminNotifications() sẽ được gọi
                            }
                        }
                        if (item.getActionLink() != null && !item.getActionLink().isEmpty()) {
                            handleAdminNotificationAction(item);
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
                    timestampLabel.setText(item.getFormattedCreatedAt());
                    updateItemStyleBasedOnReadStatus(item); // Cập nhật style dựa trên trạng thái read/unread

                    actionButton.setDisable(item.isRead()); // Disable nút nếu đã đọc

                    String emoji = "🔔";
                    Notification.NotificationType type = item.getType();
                    if (type != null) {
                        switch (type) {
                            case NEW_LOAN_REQUEST:
                                emoji = "📨"; break;
                            case NEW_USER_QUESTION:
                                emoji = "❓"; break;
                            case USER_LOAN_OVERDUE_ADMIN:
                                emoji = "⏰"; break;
                            case NEW_DONATION_REQUEST: // Đảm bảo type này tồn tại trong Enum NotificationType
                                emoji = "🎁"; break;
                            case BOOK_STOCK_LOW_ADMIN:
                                emoji = "📉"; break;
                            case SYSTEM_ALERT:
                                emoji = "📢"; break;
                            // Thêm các NotificationType khác của Admin
                            default:
                                emoji = "ℹ️"; break;
                        }
                    }
                    emojiIconLabel.setText(emoji);
                    setGraphic(hbox);
                }
            }
        });
    }

    private void handleAdminNotificationAction(Notification notification) {
        if (dashboardController == null || notification.getActionLink() == null || notification.getActionLink().isEmpty()) {
            System.err.println("ERROR_AdminNotif_Action: DashboardController or actionLink is null/empty for notif ID: " + notification.getId());
            return;
        }

        String action = notification.getActionLink().toUpperCase();
        String relatedId = notification.getRelatedItemId();
        System.out.println("INFO_AdminNotif_Action: Handling admin action: " + action + ", RelatedID: " + relatedId);

        switch (action) {
            case "VIEW_LOAN_REQUESTS_TAB": // Ví dụ: điều hướng đến tab duyệt yêu cầu trong LoanManagement
                // SessionManager.getInstance().setTargetAdminLoanManagementTab("PENDING_REQUESTS_TAB_ID"); // Cần định nghĩa ID
                dashboardController.loadAdminViewIntoCenter("LoanManagementView.fxml"); // AdminLoanManagementController sẽ đọc session
                // (Cần AdminLoanManagementController hỗ trợ chọn tab từ session)
                break;
            case "VIEW_USER_QUESTIONS_TAB": // Ví dụ: điều hướng đến quản lý FAQ
                // SessionManager.getInstance().setTargetFAQManagementFocus(relatedId);
                dashboardController.loadAdminViewIntoCenter("AdminFAQManagementView.fxml");
                break;
            case "VIEW_DONATION_REQUESTS_TAB": // Ví dụ
                dashboardController.loadAdminViewIntoCenter("AdminDonationManagementView.fxml");
                break;
            // Thêm các case khác cho admin
            default:
                System.out.println("INFO_AdminNotif_Action: No specific admin handler for action: " + action);
                break;
        }
    }

    @FXML private void handleRefreshNotifications(ActionEvent event) {
        System.out.println("DEBUG_ANC_REFRESH: Refreshing admin notifications.");
        if (currentAdmin != null) loadAdminNotifications();
    }

    @FXML private void handleMarkAllAsRead(ActionEvent event) {
        System.out.println("DEBUG_ANC_MARK_ALL_READ: Attempting to mark all admin notifications as read.");
        if (currentAdmin != null) {
            if (notificationService.markAllAdminNotificationsAsRead()) {
                showAlert(Alert.AlertType.INFORMATION, "Hoàn tất", "Tất cả thông báo mới đã được đánh dấu đã xem.");
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Không có thông báo mới nào hoặc không thể thực hiện.");
            }
            loadAdminNotifications(); // Tải lại, danh sách sẽ trống vì chỉ lấy unread
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể thực hiện. Vui lòng đăng nhập lại.");
        }
    }

    // private void updateUnreadCountInAdminSidebar() {
    //     if (currentAdmin != null && dashboardController != null) {
    //         // int unreadCount = notificationService.getUnreadAdminNotificationCount();
    //         // dashboardController.updateAdminNotificationBadge(unreadCount);
    //     }
    // }

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
                System.err.println("WARN_ANC_DIALOG_CSS: CSS file not found for dialogs.");
            }
        } catch (Exception e) {
            System.err.println("WARN_ANC_DIALOG_CSS: Failed to load CSS for dialog: " + e.getMessage());
        }
    }
}
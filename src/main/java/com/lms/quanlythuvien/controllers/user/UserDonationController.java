package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.models.transaction.DonationRequest;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.services.system.NotificationService;
import com.lms.quanlythuvien.models.system.Notification; // Đã có import
import com.lms.quanlythuvien.services.transaction.DonationRequestService;
import com.lms.quanlythuvien.utils.session.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox; // Import VBox nếu dùng trong ListCell

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList; // Đã có
import java.util.Arrays;    // Đã có
import java.util.Collections;
import java.util.Comparator;  // Đã có
import java.util.HashSet;
import java.util.List;
import java.util.Optional;    // Đã có
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class UserDonationController implements Initializable {

    @FXML private TextField bookNameField;
    @FXML private TextField authorNameField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> languageComboBox;
    @FXML private TextArea reasonTextArea;
    @FXML private Button submitDonationButton;
    @FXML private Label donationFormErrorLabel;
    @FXML private ListView<DonationRequest> donationHistoryListView;

    private User currentUser;
    private DonationRequestService donationRequestService;
    private BookManagementService bookManagementService;
    private NotificationService notificationService;

    private ObservableList<DonationRequest> userDonationHistory;
    private final DateTimeFormatter displayDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private UserDashboardController dashboardController; // Thêm để có thể gọi lại dashboard

    public void setDashboardController(UserDashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        donationRequestService = DonationRequestService.getInstance();
        bookManagementService = BookManagementService.getInstance();
        notificationService = NotificationService.getInstance();
        userDonationHistory = FXCollections.observableArrayList();

        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi Người Dùng", "Vui lòng đăng nhập để sử dụng chức năng này.");
            disableForm(true);
            if (donationHistoryListView != null) donationHistoryListView.setPlaceholder(new Label("Vui lòng đăng nhập."));
            return;
        }
        disableForm(false);
        populateCategoryComboBox();
        populateLanguageComboBox();
        setupDonationHistoryListView();
        loadDonationHistory();
        clearError();
    }

    private void disableForm(boolean disable) {
        if (bookNameField != null) bookNameField.setDisable(disable);
        if (authorNameField != null) authorNameField.setDisable(disable);
        if (categoryComboBox != null) categoryComboBox.setDisable(disable);
        if (languageComboBox != null) languageComboBox.setDisable(disable);
        if (reasonTextArea != null) reasonTextArea.setDisable(disable);
        if (submitDonationButton != null) submitDonationButton.setDisable(disable);
    }

    private void populateCategoryComboBox() {
        if (categoryComboBox == null) return;
        Set<String> categories = new HashSet<>();
        List<String> distinctCategories = bookManagementService.getAllDistinctCategories();
        if (distinctCategories != null && !distinctCategories.isEmpty()) { // Kiểm tra null và rỗng
            categories.addAll(distinctCategories);
        }
        if (categories.isEmpty()) { // Chỉ thêm mặc định nếu sau khi lấy từ service vẫn rỗng
            categories.addAll(Arrays.asList("Văn học", "Kinh tế", "Khoa học", "Lịch sử", "Thiếu nhi", "Ngoại ngữ", "Kỹ năng", "Truyện tranh", "Giáo trình"));
        }

        ObservableList<String> categoryOptions = FXCollections.observableArrayList(new ArrayList<>(categories));
        Collections.sort(categoryOptions, String.CASE_INSENSITIVE_ORDER);
        categoryComboBox.setItems(categoryOptions);
        categoryComboBox.setPromptText("-- Chọn hoặc nhập thể loại --");
        categoryComboBox.setEditable(true);
    }

    private void populateLanguageComboBox() {
        if (languageComboBox == null) return;
        ObservableList<String> languages = FXCollections.observableArrayList("Tiếng Việt", "Tiếng Anh", "Tiếng Pháp", "Tiếng Nhật", "Tiếng Hàn", "Ngôn ngữ khác");
        languageComboBox.setItems(languages);
        languageComboBox.setPromptText("-- Chọn ngôn ngữ --");
        languageComboBox.setValue("Tiếng Việt");
    }

    private void setupDonationHistoryListView() {
        if (donationHistoryListView == null) return;
        donationHistoryListView.setItems(userDonationHistory);
        donationHistoryListView.setCellFactory(listView -> new ListCell<DonationRequest>() {
            private final VBox vbox = new VBox(5);
            private final Label bookInfoLabel = new Label();
            private final Label dateAndStatusLabel = new Label();
            private final Label reasonLabel = new Label();
            private final Label adminNotesLabel = new Label();
            {
                bookInfoLabel.getStyleClass().add("donation-history-book");
                dateAndStatusLabel.getStyleClass().add("donation-history-status");
                reasonLabel.getStyleClass().add("donation-history-reason");
                adminNotesLabel.getStyleClass().add("donation-history-adminnote");
                reasonLabel.setWrapText(true);
                adminNotesLabel.setWrapText(true);
                vbox.getChildren().addAll(bookInfoLabel, dateAndStatusLabel, reasonLabel, adminNotesLabel);
                vbox.setPadding(new Insets(10));
                vbox.getStyleClass().add("list-cell-container");
            }
            @Override
            protected void updateItem(DonationRequest item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    bookInfoLabel.setText("Sách: " + item.getBookName() +
                            (item.getAuthorName()!=null && !item.getAuthorName().isEmpty() ?
                                    " - Tác giả: " + item.getAuthorName() : ""));
                    String statusText = item.getStatus().name();
                    String statusStyleClass = "status-default"; // Class CSS mặc định
                    switch (item.getStatus()) {
                        case PENDING_APPROVAL: statusText = "Chờ duyệt"; statusStyleClass = "status-pending"; break;
                        case APPROVED_PENDING_RECEIPT: statusText = "Đã duyệt (chờ nhận)"; statusStyleClass = "status-approved"; break;
                        case COMPLETED: statusText = "Đã hoàn tất"; statusStyleClass = "status-completed"; break;
                        case REJECTED: statusText = "Bị từ chối"; statusStyleClass = "status-rejected"; break;
                        case CANCELED_BY_USER: statusText = "Bạn đã hủy"; statusStyleClass = "status-canceled-user"; break;
                        default: statusText = item.getStatus().toString(); // Giữ nguyên nếu có trạng thái lạ
                    }
                    dateAndStatusLabel.setText("Gửi: " + item.getRequestDate().format(displayDateFormatter) + " - Trạng thái: " + statusText);
                    dateAndStatusLabel.getStyleClass().setAll("donation-history-status", statusStyleClass); // Dùng setAll để ghi đè class cũ

                    reasonLabel.setText("Lý do quyên góp: " + (item.getReasonForContribution() != null && !item.getReasonForContribution().isEmpty() ? item.getReasonForContribution() : "(không có)"));
                    reasonLabel.setVisible(item.getReasonForContribution() != null && !item.getReasonForContribution().isEmpty());
                    reasonLabel.setManaged(reasonLabel.isVisible());

                    adminNotesLabel.setText("Phản hồi từ Admin: " + (item.getAdminNotes() != null && !item.getAdminNotes().isEmpty() ? item.getAdminNotes() : "(chưa có)"));
                    adminNotesLabel.setVisible(item.getAdminNotes() != null && !item.getAdminNotes().isEmpty());
                    adminNotesLabel.setManaged(adminNotesLabel.isVisible());
                    setGraphic(vbox);
                }
            }
        });
    }

    private void loadDonationHistory() {
        if (currentUser == null) return;
        List<DonationRequest> history = donationRequestService.getRequestsByUserId(currentUser.getUserId());
        history.sort(Comparator.comparing(DonationRequest::getRequestDate).reversed());
        userDonationHistory.setAll(history);
        if (donationHistoryListView != null) {
            donationHistoryListView.setPlaceholder(new Label(history.isEmpty() ? "Bạn chưa có lịch sử quyên góp nào." : ""));
        }
    }

    @FXML
    void handleSubmitDonationAction(ActionEvent event) {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Bạn cần đăng nhập để quyên góp sách.");
            return;
        }
        String bookName = bookNameField.getText().trim();
        String authorName = authorNameField.getText().trim();
        String category = categoryComboBox.getEditor().getText().trim();
        if (category.isEmpty() && categoryComboBox.getValue() != null) {
            category = categoryComboBox.getValue();
        }
        String language = languageComboBox.getValue(); // Ngôn ngữ có thể là null nếu không chọn
        String reason = reasonTextArea.getText().trim();

        if (bookName.isEmpty()) { showError("Tên sách là bắt buộc."); return; }
        if (category == null || category.isEmpty()) { showError("Thể loại sách là bắt buộc."); return; }
        clearError();

        DonationRequest newRequest = new DonationRequest(
                currentUser.getUserId(), bookName, authorName, category, language, reason);

        Optional<DonationRequest> createdRequestOpt = donationRequestService.createRequest(newRequest);
        if (createdRequestOpt.isPresent()) {
            showAlert(Alert.AlertType.INFORMATION, "Thành Công", "Yêu cầu quyên góp của bạn đã được gửi. Ban quản trị thư viện xin chân thành cảm ơn!");

            // Thông báo cho Admin (cần đảm bảo NotificationType.NEW_DONATION_REQUEST_ADMIN đã được định nghĩa)
            notificationService.createNotification(
                    null,
                    "Yêu cầu quyên góp mới từ user '" + currentUser.getUsername() + "': Sách '" + bookName + "'.",
                    Notification.NotificationType.NEW_DONATION_REQUEST_ADMIN,
                    createdRequestOpt.get().getRequestId(),
                    null
            );
            clearForm();
            loadDonationHistory();
        } else {
            showAlert(Alert.AlertType.ERROR, "Thất Bại", "Không thể gửi yêu cầu quyên góp. Vui lòng thử lại sau.");
        }
    }

    private void clearForm() {
        if (bookNameField != null) bookNameField.clear();
        if (authorNameField != null) authorNameField.clear();
        if (categoryComboBox != null) {categoryComboBox.getSelectionModel().select(null); categoryComboBox.getEditor().clear(); categoryComboBox.setPromptText("-- Chọn hoặc nhập thể loại --");}
        if (languageComboBox != null) {languageComboBox.getSelectionModel().select("Tiếng Việt");} // Reset về mặc định
        if (reasonTextArea != null) reasonTextArea.clear();
        clearError();
    }

    private void showError(String message) {
        if (donationFormErrorLabel != null) {
            donationFormErrorLabel.setText(message);
            donationFormErrorLabel.setVisible(true);
            donationFormErrorLabel.setManaged(true);
        }
    }

    private void clearError() {
        if (donationFormErrorLabel != null) {
            donationFormErrorLabel.setText("");
            donationFormErrorLabel.setVisible(false);
            donationFormErrorLabel.setManaged(false);
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyDialogStyles(alert.getDialogPane()); // Sửa: truyền DialogPane
        alert.showAndWait();
    }

    // Sửa: applyDialogStyles nhận DialogPane
    private void applyDialogStyles(DialogPane dialogPane) {
        try {
            URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
            if (cssUrl != null) {
                dialogPane.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) { System.err.println("WARN_DIALOG_CSS: Failed to load CSS for dialog: " + e.getMessage()); }
    }
}
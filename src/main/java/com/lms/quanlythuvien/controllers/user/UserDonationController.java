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
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate; // Thêm import này nếu cần
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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

    private UserDashboardController dashboardController;

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
        if (distinctCategories != null && !distinctCategories.isEmpty()) {
            categories.addAll(distinctCategories);
        }
        // Thêm các thể loại mặc định nếu danh sách từ service rỗng hoặc để bổ sung
        if (categories.isEmpty()) { // Hoặc bạn có thể luôn thêm các mục này
            categories.addAll(Arrays.asList("Văn học", "Kinh tế", "Khoa học", "Lịch sử", "Thiếu nhi", "Ngoại ngữ", "Kỹ năng", "Truyện tranh", "Giáo trình", "Khác"));
        }

        ObservableList<String> categoryOptions = FXCollections.observableArrayList(new ArrayList<>(categories));
        Collections.sort(categoryOptions, String.CASE_INSENSITIVE_ORDER);
        categoryComboBox.setItems(categoryOptions);
        categoryComboBox.setPromptText("-- Chọn hoặc nhập thể loại --");
        categoryComboBox.setEditable(true); // Cho phép người dùng nhập thể loại mới
    }

    private void populateLanguageComboBox() {
        if (languageComboBox == null) return;
        ObservableList<String> languages = FXCollections.observableArrayList("Tiếng Việt", "Tiếng Anh", "Tiếng Pháp", "Tiếng Nhật", "Tiếng Hàn", "Ngôn ngữ khác");
        languageComboBox.setItems(languages);
        languageComboBox.setPromptText("-- Chọn ngôn ngữ --");
        languageComboBox.setValue("Tiếng Việt"); // Ngôn ngữ mặc định
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
                vbox.getStyleClass().add("list-cell-container"); // Class chung cho cell
            }
            @Override
            protected void updateItem(DonationRequest item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null); // Đảm bảo text cũng được xóa
                } else {
                    bookInfoLabel.setText("Sách: " + item.getBookName() +
                            (item.getAuthorName()!=null && !item.getAuthorName().isEmpty() ?
                                    " - Tác giả: " + item.getAuthorName() : ""));

                    String statusText = "Không rõ"; // Mặc định
                    String statusStyleClass = "status-unknown";
                    if (item.getStatus() != null) {
                        statusText = item.getStatus().getDisplayName(); // Sử dụng getDisplayName
                        switch (item.getStatus()) {
                            case PENDING_APPROVAL: statusStyleClass = "status-pending"; break;
                            case APPROVED_PENDING_RECEIPT: statusStyleClass = "status-approved"; break;
                            case COMPLETED: statusStyleClass = "status-completed"; break;
                            case REJECTED: statusStyleClass = "status-rejected"; break;
                            case CANCELED_BY_USER: statusStyleClass = "status-canceled-user"; break;
                            default: statusStyleClass = "status-unknown";
                        }
                    }
                    dateAndStatusLabel.setText("Gửi: " + (item.getRequestDate() != null ? item.getRequestDate().format(displayDateFormatter) : "N/A") +
                            " - Trạng thái: " + statusText);
                    dateAndStatusLabel.getStyleClass().setAll("donation-history-status", statusStyleClass);

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
        // Sắp xếp lịch sử theo ngày yêu cầu giảm dần (mới nhất lên đầu)
        history.sort(Comparator.comparing(DonationRequest::getRequestDate, Comparator.nullsLast(Comparator.reverseOrder())));
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
        String category = categoryComboBox.getEditor().getText().trim(); // Lấy text từ editor nếu người dùng nhập mới
        if (category.isEmpty() && categoryComboBox.getValue() != null && !categoryComboBox.getValue().equals(categoryComboBox.getPromptText())) {
            category = categoryComboBox.getValue(); // Nếu editor rỗng và có giá trị được chọn, lấy giá trị đó
        }
        String language = languageComboBox.getValue();
        String reason = reasonTextArea.getText().trim();

        if (bookName.isEmpty()) { showError("Tên sách là bắt buộc."); return; }
        if (category == null || category.isEmpty() || category.equals(categoryComboBox.getPromptText())) {
            showError("Thể loại sách là bắt buộc."); return;
        }
        if (language == null || language.equals(languageComboBox.getPromptText())) { // Ngôn ngữ cũng nên được chọn
            showError("Vui lòng chọn ngôn ngữ cho sách."); return;
        }
        clearError();

        DonationRequest newRequest = new DonationRequest(
                currentUser.getUserId(), bookName, authorName, category, language, reason);

        Optional<DonationRequest> createdRequestOpt = donationRequestService.createRequest(newRequest);
        if (createdRequestOpt.isPresent()) {
            DonationRequest createdRequest = createdRequestOpt.get(); // Lấy đối tượng request đã tạo
            showAlert(Alert.AlertType.INFORMATION, "Thành Công", "Yêu cầu quyên góp của bạn đã được gửi. Ban quản trị thư viện xin chân thành cảm ơn!");

            // --- SỬA LỖI Ở ĐÂY ---
            // Sử dụng NotificationType.NEW_DONATION_REQUEST (đã có displayName)
            // userId là null vì đây là thông báo cho hệ thống/admin, không phải cho người dùng cụ thể này
            notificationService.createNotification(
                    null, // Hoặc một ID đặc biệt cho Admin nếu cần phân loại
                    "Yêu cầu quyên góp mới từ user '" + currentUser.getUsername() + "': Sách '" + bookName + "'.",
                    Notification.NotificationType.NEW_DONATION_REQUEST, // Sử dụng hằng số đúng
                    createdRequest.getRequestId(), // ID của yêu cầu quyên góp vừa tạo
                    "VIEW_DONATION_REQUESTS_TAB" // Ví dụ actionLink để admin điều hướng
            );
            // --- KẾT THÚC SỬA LỖI ---

            clearForm();
            loadDonationHistory(); // Tải lại lịch sử để hiển thị yêu cầu mới
        } else {
            showAlert(Alert.AlertType.ERROR, "Thất Bại", "Không thể gửi yêu cầu quyên góp. Vui lòng thử lại sau.");
        }
    }

    private void clearForm() {
        if (bookNameField != null) bookNameField.clear();
        if (authorNameField != null) authorNameField.clear();
        if (categoryComboBox != null) {
            categoryComboBox.getSelectionModel().clearSelection(); // Xóa lựa chọn
            categoryComboBox.getEditor().clear(); // Xóa text trong editor
            categoryComboBox.setPromptText("-- Chọn hoặc nhập thể loại --"); // Đặt lại prompt text
        }
        if (languageComboBox != null) {
            languageComboBox.setValue("Tiếng Việt"); // Reset về giá trị mặc định
        }
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
        if (alert.getDialogPane() != null) { // Kiểm tra null trước khi áp dụng
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
                System.err.println("WARN_UserDonation_CSS: CSS file not found for dialogs.");
            }
        } catch (Exception e) {
            System.err.println("WARN_UserDonation_CSS: Failed to load CSS for dialog: " + e.getMessage());
        }
    }
}
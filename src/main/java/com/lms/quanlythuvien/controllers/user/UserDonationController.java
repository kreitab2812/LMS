package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.models.transaction.DonationRequest;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.transaction.DonationRequestService;
import com.lms.quanlythuvien.utils.session.SessionManager;
// Import BookManagementService nếu cần lấy danh sách thể loại từ sách hiện có
import com.lms.quanlythuvien.services.library.BookManagementService;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;


import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Arrays; // Ví dụ cho ngôn ngữ
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Optional;
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
    private BookManagementService bookManagementService; // Để lấy danh sách thể loại

    private ObservableList<DonationRequest> userDonationHistory;

    public UserDonationController() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        donationRequestService = DonationRequestService.getInstance();
        bookManagementService = BookManagementService.getInstance(); // Khởi tạo
        userDonationHistory = FXCollections.observableArrayList();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (currentUser == null) {
            // Xử lý trường hợp user chưa đăng nhập
            showAlert(Alert.AlertType.ERROR, "Lỗi Người Dùng", "Vui lòng đăng nhập để sử dụng chức năng này.");
            // Vô hiệu hóa form
            if (bookNameField != null) bookNameField.setDisable(true);
            if (authorNameField != null) authorNameField.setDisable(true);
            if (categoryComboBox != null) categoryComboBox.setDisable(true);
            if (languageComboBox != null) languageComboBox.setDisable(true);
            if (reasonTextArea != null) reasonTextArea.setDisable(true);
            if (submitDonationButton != null) submitDonationButton.setDisable(true);
            if (donationHistoryListView != null) donationHistoryListView.setPlaceholder(new Label("Vui lòng đăng nhập."));
            return;
        }

        populateCategoryComboBox();
        populateLanguageComboBox();
        setupDonationHistoryListView();
        loadDonationHistory();
    }

    private void populateCategoryComboBox() {
        // Lấy danh sách thể loại từ tất cả sách trong thư viện (ví dụ)
        // Hoặc cậu có thể có một danh sách thể loại cố định
        Set<String> categories = bookManagementService.getAllBooksInLibrary().stream()
                .flatMap(book -> book.getCategories().stream())
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        ObservableList<String> categoryOptions = FXCollections.observableArrayList(categories.stream().sorted().collect(Collectors.toList()));
        if (categoryComboBox != null) {
            categoryComboBox.setItems(categoryOptions);
            categoryComboBox.setPromptText("-- Chọn thể loại --");
        }
    }

    private void populateLanguageComboBox() {
        // Ví dụ danh sách ngôn ngữ cố định
        ObservableList<String> languages = FXCollections.observableArrayList("Tiếng Việt", "Tiếng Anh", "Tiếng Pháp", "Tiếng Nhật", "Khác");
        if (languageComboBox != null) {
            languageComboBox.setItems(languages);
            languageComboBox.setPromptText("-- Chọn ngôn ngữ --");
        }
    }

    private void setupDonationHistoryListView() {
        if (donationHistoryListView == null) return;
        donationHistoryListView.setItems(userDonationHistory);
        donationHistoryListView.setCellFactory(listView -> new ListCell<DonationRequest>() {
            private VBox vbox = new VBox(5);
            private Label bookInfoLabel = new Label();
            private Label dateAndStatusLabel = new Label();
            private Label reasonLabel = new Label();
            private Label adminNotesLabel = new Label();

            {
                bookInfoLabel.getStyleClass().add("donation-history-book");
                dateAndStatusLabel.getStyleClass().add("donation-history-status");
                reasonLabel.getStyleClass().add("donation-history-reason");
                adminNotesLabel.getStyleClass().add("donation-history-adminnote");

                reasonLabel.setWrapText(true);
                adminNotesLabel.setWrapText(true);

                vbox.getChildren().addAll(bookInfoLabel, dateAndStatusLabel, reasonLabel, adminNotesLabel);
                vbox.setPadding(new Insets(8));
            }

            @Override
            protected void updateItem(DonationRequest item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    bookInfoLabel.setText("Sách: " + item.getBookName() + " - Tác giả: " + item.getAuthorName());
                    String statusText = item.getStatus().toString(); // Cần dịch nếu muốn
                    switch (item.getStatus()) {
                        case PENDING_APPROVAL: statusText = "Chờ duyệt"; break;
                        case APPROVED_PENDING_RECEIPT: statusText = "Đã duyệt (Chờ nhận sách)"; break;
                        case COMPLETED: statusText = "Đã hoàn tất"; break;
                        case REJECTED: statusText = "Bị từ chối"; break;
                        case CANCELED_BY_USER: statusText = "Bạn đã hủy"; break;
                    }
                    dateAndStatusLabel.setText("Ngày gửi: " + item.getRequestDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " - Trạng thái: " + statusText);

                    reasonLabel.setText("Lý do: " + (item.getReasonForContribution() != null ? item.getReasonForContribution() : "Không có"));
                    reasonLabel.setVisible(item.getReasonForContribution() != null && !item.getReasonForContribution().isEmpty());
                    reasonLabel.setManaged(item.getReasonForContribution() != null && !item.getReasonForContribution().isEmpty());

                    adminNotesLabel.setText("Ghi chú Admin: " + (item.getAdminNotes() != null ? item.getAdminNotes() : "Không có"));
                    adminNotesLabel.setVisible(item.getAdminNotes() != null && !item.getAdminNotes().isEmpty());
                    adminNotesLabel.setManaged(item.getAdminNotes() != null && !item.getAdminNotes().isEmpty());

                    setGraphic(vbox);
                }
            }
        });
    }

    private void loadDonationHistory() {
        if (currentUser == null) return;
        List<DonationRequest> history = donationRequestService.getRequestsByUserId(currentUser.getUserId());
        userDonationHistory.setAll(history);
        if (donationHistoryListView != null) {
            donationHistoryListView.refresh();
            if (history.isEmpty()) {
                donationHistoryListView.setPlaceholder(new Label("Bạn chưa có đăng ký quyên góp nào."));
            }
        }
    }

    @FXML
    void handleSubmitDonationAction(ActionEvent event) {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Bạn cần đăng nhập để thực hiện chức năng này.");
            return;
        }

        String bookName = bookNameField.getText().trim();
        String authorName = authorNameField.getText().trim();
        String category = categoryComboBox.getValue();
        String language = languageComboBox.getValue();
        String reason = reasonTextArea.getText().trim();

        if (bookName.isEmpty() || authorName.isEmpty() || category == null) {
            showError("Vui lòng điền đầy đủ thông tin Tên sách, Tác giả và Thể loại.");
            return;
        }
        clearError();

        DonationRequest newRequest = new DonationRequest(
                currentUser.getUserId(),
                bookName,
                authorName,
                category,
                language, // Có thể null
                reason    // Có thể null
        );

        Optional<DonationRequest> createdRequestOpt = donationRequestService.createRequest(newRequest);
        if (createdRequestOpt.isPresent()) {
            showAlert(Alert.AlertType.INFORMATION, "Thành Công", "Yêu cầu quyên góp của bạn đã được gửi. Cảm ơn sự đóng góp của bạn!");
            clearForm();
            loadDonationHistory(); // Làm mới lịch sử
        } else {
            showAlert(Alert.AlertType.ERROR, "Thất Bại", "Không thể gửi yêu cầu quyên góp. Vui lòng thử lại.");
        }
    }

    private void clearForm() {
        if (bookNameField != null) bookNameField.clear();
        if (authorNameField != null) authorNameField.clear();
        if (categoryComboBox != null) categoryComboBox.getSelectionModel().clearSelection();
        if (languageComboBox != null) languageComboBox.getSelectionModel().clearSelection();
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
        try {
            URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
            if (cssUrl != null) {
                alert.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) { /* Bỏ qua nếu không load được CSS cho Alert */ }
        alert.showAndWait();
    }
}
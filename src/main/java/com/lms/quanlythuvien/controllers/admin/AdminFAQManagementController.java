package com.lms.quanlythuvien.controllers.admin;

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.models.system.Notification;
import com.lms.quanlythuvien.models.system.UserQuestion;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.system.NotificationService;
import com.lms.quanlythuvien.services.user.UserQuestionService;
import com.lms.quanlythuvien.utils.session.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos; // <<< THÊM IMPORT NÀY
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminFAQManagementController implements Initializable {

    @FXML private TableView<UserQuestion> questionsTableView;
    @FXML private TableColumn<UserQuestion, String> questionTextColumn;
    @FXML private TableColumn<UserQuestion, String> questionUserColumn;
    @FXML private TableColumn<UserQuestion, String> questionDateColumn;
    @FXML private TableColumn<UserQuestion, UserQuestion.QuestionStatus> questionStatusColumn;

    @FXML private TextArea questionDisplayArea;
    @FXML private TextArea answerTextArea;
    @FXML private CheckBox publishAsFAQCheckBox;
    @FXML private Button saveAnswerButton;
    @FXML private Button rejectQuestionButton;
    @FXML private Button refreshQuestionsButton;
    @FXML private Label faqManagementErrorLabel;

    private UserQuestionService userQuestionService;
    private NotificationService notificationService;
    private User currentAdmin;
    private UserQuestion selectedQuestion;
    private ObservableList<UserQuestion> observableQuestionsList;
    private AdminDashboardController dashboardController;

    public void setDashboardController(AdminDashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    public AdminFAQManagementController() {
        userQuestionService = UserQuestionService.getInstance();
        notificationService = NotificationService.getInstance();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentAdmin = SessionManager.getInstance().getCurrentUser();
        if (currentAdmin == null || currentAdmin.getRole() != User.Role.ADMIN) {
            showAlert(Alert.AlertType.ERROR, "Lỗi Phân Quyền", "Bạn không có quyền truy cập mục này.");
            disableControls(true);
            return;
        }
        disableControls(false);

        observableQuestionsList = FXCollections.observableArrayList();
        setupQuestionsTable();
        questionsTableView.setItems(observableQuestionsList);

        questionsTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            this.selectedQuestion = newSelection;
            displaySelectedQuestionDetails(newSelection);
        });

        answerTextArea.textProperty().addListener((obs, oldVal, newVal) -> validateSaveButton());

        saveAnswerButton.setDisable(true);
        rejectQuestionButton.setDisable(true);
        loadPendingQuestions();
    }

    private void disableControls(boolean disable) {
        if (questionsTableView != null) questionsTableView.setDisable(disable);
        if (questionDisplayArea != null) questionDisplayArea.setEditable(!disable); // Chỉ setEditable
        if (answerTextArea != null) answerTextArea.setEditable(!disable); // Chỉ setEditable
        if (publishAsFAQCheckBox != null) publishAsFAQCheckBox.setDisable(disable);
        if (saveAnswerButton != null) saveAnswerButton.setDisable(disable);
        if (rejectQuestionButton != null) rejectQuestionButton.setDisable(disable);
        if (refreshQuestionsButton != null) refreshQuestionsButton.setDisable(disable);
    }

    private void setupQuestionsTable() {
        questionTextColumn.setCellValueFactory(new PropertyValueFactory<>("questionText"));
        questionDateColumn.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getQuestionDate();
            return new SimpleStringProperty(date != null ? date.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")) : "N/A");
        });
        questionStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        questionUserColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getUserFullNameOrDefault(
                        cellData.getValue().getUserId() != null ? cellData.getValue().getUserId() : "N/A"))
        );
        questionTextColumn.setCellFactory(ApplicationHelper.createWrappingTextCellFactory());
    }

    @FXML
    private void handleRefreshQuestionsAction(ActionEvent event) {
        loadPendingQuestions();
        showAlert(Alert.AlertType.INFORMATION, "Làm mới", "Danh sách câu hỏi đã được cập nhật.");
    }

    private void loadPendingQuestions() {
        List<UserQuestion> pending = userQuestionService.getPendingQuestions();
        observableQuestionsList.setAll(pending);
        questionsTableView.setPlaceholder(new Label("Không có câu hỏi nào đang chờ trả lời."));
        clearSelectionAndFields();
    }

    private void displaySelectedQuestionDetails(UserQuestion question) {
        if (question != null) {
            questionDisplayArea.setText(question.getQuestionTextOrDefault(""));
            answerTextArea.setText(question.getAnswerTextOrDefault(""));
            publishAsFAQCheckBox.setSelected(question.getStatus() == UserQuestion.QuestionStatus.PUBLISHED_AS_FAQ);

            boolean canEditAnswer = (question.getStatus() == UserQuestion.QuestionStatus.PENDING_REVIEW ||
                    question.getStatus() == UserQuestion.QuestionStatus.ANSWERED);
            boolean canPublish = (question.getStatus() == UserQuestion.QuestionStatus.ANSWERED && question.getAnswerText() != null && !question.getAnswerText().trim().isEmpty());
            boolean canUnpublish = (question.getStatus() == UserQuestion.QuestionStatus.PUBLISHED_AS_FAQ);

            answerTextArea.setEditable(canEditAnswer);
            publishAsFAQCheckBox.setDisable(!(canPublish || canUnpublish));
            rejectQuestionButton.setDisable(question.getStatus() != UserQuestion.QuestionStatus.PENDING_REVIEW);
        } else {
            clearSelectionAndFields();
        }
        validateSaveButton();
    }

    private void clearSelectionAndFields(){
        if(questionsTableView != null) questionsTableView.getSelectionModel().clearSelection();
        if(questionDisplayArea != null) questionDisplayArea.clear();
        if(answerTextArea != null) answerTextArea.clear();
        if(publishAsFAQCheckBox != null) publishAsFAQCheckBox.setSelected(false);
        if(saveAnswerButton != null) saveAnswerButton.setDisable(true);
        if(rejectQuestionButton != null) rejectQuestionButton.setDisable(true);
        selectedQuestion = null;
        clearError(); // <<< Gọi phương thức này
    }

    private void validateSaveButton() {
        boolean disable = true;
        if (selectedQuestion != null && !answerTextArea.getText().trim().isEmpty()) {
            if (selectedQuestion.getStatus() == UserQuestion.QuestionStatus.PENDING_REVIEW ||
                    selectedQuestion.getStatus() == UserQuestion.QuestionStatus.ANSWERED) {
                disable = false;
            }
        }
        if (saveAnswerButton != null) saveAnswerButton.setDisable(disable);
    }

    @FXML
    private void handleSaveAnswerAction(ActionEvent event) {
        if (selectedQuestion == null || currentAdmin == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn một câu hỏi để xử lý.");
            return;
        }
        String answer = answerTextArea.getText().trim();
        if (answer.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập nội dung câu trả lời.");
            return;
        }
        boolean publishAsFaq = publishAsFAQCheckBox.isSelected();
        boolean answerUpdatedSuccessfully = userQuestionService.answerQuestion(selectedQuestion.getId(), answer, currentAdmin.getUserId());

        if (answerUpdatedSuccessfully) {
            boolean publishStatusChanged = false;
            if (publishAsFaq) {
                publishStatusChanged = userQuestionService.publishFAQ(selectedQuestion.getId());
            } else {
                if (selectedQuestion.getStatus() == UserQuestion.QuestionStatus.PUBLISHED_AS_FAQ ||
                        (selectedQuestion.isPublic() && selectedQuestion.getStatus() == UserQuestion.QuestionStatus.ANSWERED) ) {
                    publishStatusChanged = userQuestionService.unpublishFAQ(selectedQuestion.getId());
                } else {
                    publishStatusChanged = true;
                }
            }
            if (publishStatusChanged) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã lưu và cập nhật câu hỏi.");
                notificationService.createNotification(
                        selectedQuestion.getUserId(),
                        "Câu hỏi '" + truncate(selectedQuestion.getQuestionTextOrDefault(""), 30) + "' đã được xử lý.",
                        Notification.NotificationType.USER_YoutubeED,
                        selectedQuestion.getId(), null
                );
            } else {
                showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Đã lưu câu trả lời, nhưng có lỗi khi cập nhật trạng thái publish FAQ.");
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể lưu câu trả lời.");
        }
        loadPendingQuestions();
    }

    @FXML
    private void handleRejectQuestionAction(ActionEvent event) {
        if (selectedQuestion == null || currentAdmin == null || selectedQuestion.getStatus() != UserQuestion.QuestionStatus.PENDING_REVIEW) {
            showAlert(Alert.AlertType.WARNING, "Không hợp lệ", "Vui lòng chọn một câu hỏi đang chờ duyệt để từ chối.");
            return;
        }
        TextInputDialog reasonDialog = new TextInputDialog("Không phù hợp/Đã có trong FAQ.");
        reasonDialog.setTitle("Từ chối Câu Hỏi");
        reasonDialog.setHeaderText("Từ chối câu hỏi của: " + selectedQuestion.getUserFullNameOrDefault(selectedQuestion.getUserId()));
        reasonDialog.setContentText("Nhập lý do từ chối (*):");
        applyDialogStyles(reasonDialog.getDialogPane()); // Sửa: truyền DialogPane
        Optional<String> reasonOpt = reasonDialog.showAndWait();

        if (reasonOpt.isPresent() && !reasonOpt.get().trim().isEmpty()) {
            String reason = reasonOpt.get().trim();
            if (userQuestionService.rejectQuestion(selectedQuestion.getId(), reason, currentAdmin.getUserId())) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã từ chối câu hỏi.");
                notificationService.createNotification(
                        selectedQuestion.getUserId(),
                        "Câu hỏi '" + truncate(selectedQuestion.getQuestionTextOrDefault(""), 30) + "' đã bị từ chối. Lý do: " + reason,
                        Notification.NotificationType.USER_YoutubeED,
                        selectedQuestion.getId(), null
                );
                loadPendingQuestions();
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể từ chối câu hỏi.");
            }
        } else if (reasonOpt.isPresent()){
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập lý do từ chối.");
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, Math.min(text.length(), maxLength - 3)) + "...";
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyDialogStyles(alert.getDialogPane()); // Sửa: truyền DialogPane
        alert.showAndWait();
    }

    private void applyDialogStyles(DialogPane dialogPane) { // Sửa: nhận DialogPane
        try {
            URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
            if (cssUrl != null) {
                dialogPane.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) { System.err.println("WARN_DIALOG_CSS: Failed to load CSS for dialog: " + e.getMessage()); }
    }

    // Thêm phương thức clearError nếu nó chưa có hoặc bị xóa nhầm
    private void clearError() {
        if (faqManagementErrorLabel != null) {
            faqManagementErrorLabel.setText("");
            faqManagementErrorLabel.setVisible(false);
            faqManagementErrorLabel.setManaged(false);
        }
    }


    // Helper class cho TableCell để wrap text
    public static class ApplicationHelper {
        public static Callback<TableColumn<UserQuestion, String>, TableCell<UserQuestion, String>> createWrappingTextCellFactory() {
            return column -> new TableCell<>() {
                private final Label label = new Label();
                {
                    label.setWrapText(true);
                    label.prefWidthProperty().bind(column.widthProperty().subtract(15));
                    setGraphic(label);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY); // <<< ContentDisplay cần import
                    setAlignment(Pos.TOP_LEFT); // <<< Pos cần import
                }
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        label.setText(null);
                    } else {
                        label.setText(item);
                    }
                }
            };
        }
    }
}
package com.lms.quanlythuvien.controllers.common;

import com.lms.quanlythuvien.models.system.FAQItem; // FAQ tĩnh cũ
import com.lms.quanlythuvien.models.system.UserQuestion; // Model mới cho câu hỏi của user
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.user.UserQuestionService; // Service mới
import com.lms.quanlythuvien.services.user.UserService; // Để lấy tên user nếu cần
import com.lms.quanlythuvien.utils.session.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.layout.Region;
import javafx.scene.text.TextFlow;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class FAQController implements Initializable {

    @FXML private Accordion faqAccordion; // Cho FAQ chung
    @FXML private TextArea userQuestionTextArea;
    @FXML private Button submitQuestionButton;
    @FXML private Label askQuestionErrorLabel;
    @FXML private ListView<UserQuestion> myQuestionsListView; // Hiển thị câu hỏi của user

    private List<FAQItem> staticFaqList; // Danh sách FAQ tĩnh (nếu vẫn giữ)
    private UserQuestionService userQuestionService;
    private UserService userService; // Để lấy tên user nếu cần hiển thị chi tiết
    private User currentUser;
    private ObservableList<UserQuestion> myQuestionsObservableList;

    public FAQController() {
        userQuestionService = UserQuestionService.getInstance();
        userService = UserService.getInstance(); // Khởi tạo
        currentUser = SessionManager.getInstance().getCurrentUser();
        myQuestionsObservableList = FXCollections.observableArrayList();
        initializeStaticFAQs(); // Khởi tạo danh sách FAQ tĩnh
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (faqAccordion != null) {
            populateStaticFAQ();
        }
        if (myQuestionsListView != null) {
            myQuestionsListView.setItems(myQuestionsObservableList);
            setupMyQuestionsCellFactory();
            if (currentUser != null) {
                loadMyQuestions();
            } else {
                userQuestionTextArea.setDisable(true);
                submitQuestionButton.setDisable(true);
                myQuestionsListView.setPlaceholder(new Label("Vui lòng đăng nhập để xem và đặt câu hỏi."));
            }
        }
        clearAskQuestionError();
    }

    // Trong FAQController.java
    private void initializeStaticFAQs() {
        staticFaqList = new ArrayList<>();

        // Ưu tiên lấy FAQ đã được Admin công khai từ UserQuestionService
        List<UserQuestion> publicFaqs = userQuestionService.getPublicFAQItems();
        for (UserQuestion uq : publicFaqs) {
            staticFaqList.add(new FAQItem(uq.getQuestionText(), uq.getAnswerText() != null ? uq.getAnswerText() : "Chưa có câu trả lời."));
        }

        // Nếu không có FAQ nào từ service (hoặc muốn bổ sung thêm FAQ tĩnh cố định)
        // thì thêm các FAQ tĩnh ở đây.
        // Cậu có thể quyết định chỉ thêm nếu publicFaqs rỗng, hoặc luôn thêm các câu này.
        // Ví dụ: Luôn thêm các câu này, hoặc chỉ thêm nếu publicFaqs.size() < một_số_nào_đó

        // Danh sách FAQ tĩnh tạm thời (giống phiên bản đầu)
        staticFaqList.add(new FAQItem("Làm thế nào để tôi đăng ký tài khoản thư viện?",
                "Bạn có thể đăng ký tài khoản trực tuyến qua mục 'Đăng ký' trên ứng dụng hoặc trang web của thư viện. Vui lòng điền đầy đủ thông tin theo mẫu và làm theo các bước hướng dẫn. Sau khi đăng ký, bạn có thể cần xác thực email (nếu có)."));
        staticFaqList.add(new FAQItem("Tôi có thể mượn tối đa bao nhiêu cuốn sách và trong bao lâu?",
                "Hiện tại, mỗi sinh viên được phép mượn tối đa 5 cuốn sách trong vòng 14 ngày. Giảng viên có thể có chính sách mượn khác. Vui lòng xem chi tiết trong 'Quy định thư viện'."));
        staticFaqList.add(new FAQItem("Làm sao để gia hạn sách đã mượn?",
                "Tính năng gia hạn sách trực tuyến hiện đang được phát triển. Trước mắt, bạn vui lòng đến quầy thủ thư để yêu cầu gia hạn trước ngày hết hạn, với điều kiện sách đó không có ai khác đang đặt trước."));
        staticFaqList.add(new FAQItem("Thư viện có phạt nếu trả sách muộn không?",
                "Có. Theo quy định, việc trả sách muộn sẽ bị tính phí phạt theo ngày. Ngoài ra, nếu quá hạn quá lâu, tài khoản của bạn có thể bị tạm khóa. Chi tiết về mức phạt được nêu rõ trong 'Quy định thư viện'."));
        staticFaqList.add(new FAQItem("Tôi có thể tìm kiếm sách như thế nào?",
                "Bạn có thể sử dụng thanh tìm kiếm ở đầu trang, chọn tìm theo 'Tiêu đề sách', 'Tên tác giả', 'ISBN', hoặc 'Thể loại sách'. Nhập từ khóa và nhấn 'Tìm'. Trong mục 'Thư Viện', bạn cũng có thể lọc sách theo thể loại."));
        staticFaqList.add(new FAQItem("Làm thế nào để gửi yêu cầu mượn sách trực tuyến?",
                "Trong mục 'Thư Viện', sau khi tìm thấy cuốn sách bạn muốn, nếu sách còn trong kho, bạn sẽ thấy nút 'Gửi Yêu Cầu Mượn'. Nhấn vào đó và yêu cầu của bạn sẽ được gửi đến thủ thư để duyệt. Bạn có thể theo dõi trạng thái yêu cầu trong 'Tủ sách của tôi' -> 'Yêu cầu mượn'."));
        staticFaqList.add(new FAQItem("Tôi muốn quyên góp sách cho thư viện thì làm thế nào?",
                "Thật tuyệt vời! Bạn có thể vào mục 'Quyên góp' trên ứng dụng, điền thông tin về cuốn sách bạn muốn quyên góp và gửi đăng ký. Thư viện sẽ xem xét và liên hệ lại với bạn. Chúng tôi rất trân trọng mọi đóng góp."));

        // Có thể thêm một câu chào mừng nếu không có publicFaqs nào
        if (publicFaqs.isEmpty() && staticFaqList.isEmpty()) { // Nếu không có gì cả
            staticFaqList.add(new FAQItem("Chào mừng đến với Thư viện UET-VNU!", "Đây là nơi bạn có thể tìm thấy nhiều tài liệu học tập và nghiên cứu bổ ích. Hãy đặt câu hỏi nếu bạn cần hỗ trợ!"));
        }
    }

    private void populateStaticFAQ() {
        if (faqAccordion == null) return;
        faqAccordion.getPanes().clear();

        if (staticFaqList.isEmpty() && currentUser == null) { // Nếu không có FAQ tĩnh và chưa đăng nhập
            Label placeholderLabel = new Label("Hiện chưa có câu hỏi thường gặp nào. Vui lòng đăng nhập để đặt câu hỏi của bạn.");
            TitledPane placeholderPane = new TitledPane("Thông báo", placeholderLabel);
            faqAccordion.getPanes().add(placeholderPane);
            return;
        }
        if (staticFaqList.isEmpty() && currentUser != null) { // Chưa có FAQ tĩnh nhưng đã đăng nhập
            Label placeholderLabel = new Label("Hiện chưa có câu hỏi thường gặp nào. Bạn có thể là người đầu tiên đặt câu hỏi!");
            TitledPane placeholderPane = new TitledPane("Thông báo", placeholderLabel);
            faqAccordion.getPanes().add(placeholderPane);
            // Không return, vì user có thể đặt câu hỏi của họ
        }


        for (FAQItem item : staticFaqList) {
            Text answerTextNode = new Text(item.getAnswer());
            answerTextNode.getStyleClass().add("faq-answer-text");
            TextFlow answerTextFlow = new TextFlow(answerTextNode);
            answerTextFlow.setPadding(new Insets(10));

            ScrollPane answerScrollPane = new ScrollPane(answerTextFlow);
            answerScrollPane.setFitToWidth(true);
            answerScrollPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
            answerScrollPane.setMinHeight(50);
            answerScrollPane.setMaxHeight(200);
            answerScrollPane.getStyleClass().add("faq-answer-scrollpane");
            answerScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

            TitledPane titledPane = new TitledPane(item.getQuestion(), answerScrollPane);
            titledPane.setAnimated(true);
            titledPane.getStyleClass().add("faq-titled-pane");
            faqAccordion.getPanes().add(titledPane);
        }
    }

    private void setupMyQuestionsCellFactory() {
        myQuestionsListView.setCellFactory(lv -> new ListCell<UserQuestion>() {
            private VBox contentBox = new VBox(5);
            private Label questionTextLabel = new Label();
            private Label questionDateLabel = new Label();
            private Label statusLabel = new Label();
            private TextFlow answerTextFlow = new TextFlow(); // Để hiển thị câu trả lời (nếu có)
            private Label answerDateLabel = new Label();

            {
                questionTextLabel.getStyleClass().add("my-question-text"); // Style riêng
                questionTextLabel.setWrapText(true);
                questionDateLabel.getStyleClass().add("my-question-timestamp");
                statusLabel.getStyleClass().add("my-question-status");
                answerTextFlow.getStyleClass().add("my-question-answer");
                answerDateLabel.getStyleClass().add("my-question-timestamp");

                contentBox.getChildren().addAll(questionTextLabel, questionDateLabel, statusLabel, answerTextFlow, answerDateLabel);
                contentBox.setPadding(new Insets(10));
                // Thêm Separator giữa các mục
                this.setGraphic(contentBox);
                this.setStyle("-fx-border-color: #e0e0e0 transparent transparent transparent; -fx-border-width: 1;");

            }

            @Override
            protected void updateItem(UserQuestion item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    questionTextLabel.setText("Hỏi: " + item.getQuestionText());
                    questionDateLabel.setText("Ngày hỏi: " + item.getFormattedQuestionDate());

                    String statusDisplay = item.getStatus().toString();
                    // Có thể dịch các trạng thái sang tiếng Việt
                    switch (item.getStatus()) {
                        case PENDING_REVIEW: statusDisplay = "Chờ duyệt"; break;
                        case ANSWERED: statusDisplay = "Đã trả lời (chưa công khai)"; break;
                        case PUBLISHED_AS_FAQ: statusDisplay = "Đã trả lời & Công khai"; break;
                        case REJECTED: statusDisplay = "Bị từ chối"; break;
                    }
                    statusLabel.setText("Trạng thái: " + statusDisplay);

                    if (item.getAnswerText() != null && !item.getAnswerText().isEmpty()) {
                        Text answerContent = new Text("Trả lời: " + item.getAnswerText());
                        answerTextFlow.getChildren().setAll(answerContent);
                        answerTextFlow.setVisible(true);
                        answerTextFlow.setManaged(true);
                        answerDateLabel.setText("Ngày trả lời: " + (item.getAnswerDate() != null ? item.getFormattedAnswerDate() : "N/A"));
                        answerDateLabel.setVisible(true);
                        answerDateLabel.setManaged(true);
                    } else {
                        answerTextFlow.getChildren().clear();
                        answerTextFlow.setVisible(false);
                        answerTextFlow.setManaged(false);
                        answerDateLabel.setVisible(false);
                        answerDateLabel.setManaged(false);
                    }
                    setGraphic(contentBox);
                }
            }
        });
    }

    private void loadMyQuestions() {
        if (currentUser == null) return;
        List<UserQuestion> myQuestions = userQuestionService.getQuestionsByUserId(currentUser.getUserId());
        myQuestionsObservableList.setAll(myQuestions);
        if (myQuestionsListView!=null) myQuestionsListView.refresh();
    }

    @FXML
    void handleSubmitUserQuestion(ActionEvent event) {
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Yêu Cầu Đăng Nhập", "Bạn cần đăng nhập để gửi câu hỏi.");
            return;
        }
        String question = userQuestionTextArea.getText().trim();
        if (question.isEmpty()) {
            showAskQuestionError("Vui lòng nhập nội dung câu hỏi của bạn.");
            return;
        }
        if (question.length() < 10) { // Ví dụ kiểm tra độ dài tối thiểu
            showAskQuestionError("Câu hỏi của bạn quá ngắn. Vui lòng mô tả chi tiết hơn.");
            return;
        }
        clearAskQuestionError();

        UserQuestion newQuestion = new UserQuestion(currentUser.getUserId(), question);
        Optional<UserQuestion> submittedOpt = userQuestionService.submitQuestion(newQuestion);

        if (submittedOpt.isPresent()) {
            showAlert(Alert.AlertType.INFORMATION, "Gửi Thành Công", "Câu hỏi của bạn đã được gửi. Chúng tôi sẽ sớm phản hồi.");
            userQuestionTextArea.clear();
            loadMyQuestions(); // Load lại danh sách câu hỏi của tôi
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể gửi câu hỏi của bạn lúc này. Vui lòng thử lại sau.");
        }
    }

    private void showAskQuestionError(String message) {
        if (askQuestionErrorLabel != null) {
            askQuestionErrorLabel.setText(message);
            askQuestionErrorLabel.setVisible(true);
            askQuestionErrorLabel.setManaged(true);
        }
    }
    private void clearAskQuestionError() {
        if (askQuestionErrorLabel != null) {
            askQuestionErrorLabel.setText("");
            askQuestionErrorLabel.setVisible(false);
            askQuestionErrorLabel.setManaged(false);
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        // ... (giữ nguyên hàm showAlert)
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        try {
            URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
            if (cssUrl != null) {
                alert.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) { /* Bỏ qua */ }
        alert.showAndWait();
    }
}
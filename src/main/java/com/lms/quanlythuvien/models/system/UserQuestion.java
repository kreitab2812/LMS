package com.lms.quanlythuvien.models.system;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class UserQuestion {

    public enum QuestionStatus {
        PENDING_REVIEW,  // Chờ Admin xem xét
        ANSWERED,        // Đã được Admin trả lời (chưa công khai)
        PUBLISHED_AS_FAQ, // Đã được trả lời và công khai thành FAQ
        REJECTED          // Bị Admin từ chối (ví dụ: không phù hợp, đã có FAQ)
    }

    private String id;
    private String userId;
    private String userFullName; // Sẽ được UserQuestionService điền vào
    private String questionText;
    private LocalDateTime questionDate;
    private String answerText;
    private String answeredByAdminId;
    private String adminFullName; // Sẽ được UserQuestionService điền vào
    private LocalDateTime answerDate;
    private QuestionStatus status;
    private boolean isPublic;

    // Constructor khi User gửi câu hỏi mới
    public UserQuestion(String userId, String questionText) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.questionText = questionText;
        this.questionDate = LocalDateTime.now();
        this.status = QuestionStatus.PENDING_REVIEW;
        this.isPublic = false;
    }

    // Constructor đầy đủ (khi load từ DB hoặc tạo trong service)
    public UserQuestion(String id, String userId, String userFullName, String questionText, LocalDateTime questionDate,
                        String answerText, String answeredByAdminId, String adminFullName, LocalDateTime answerDate,
                        QuestionStatus status, boolean isPublic) {
        this.id = id;
        this.userId = userId;
        this.userFullName = userFullName;
        this.questionText = questionText;
        this.questionDate = questionDate;
        this.answerText = answerText;
        this.answeredByAdminId = answeredByAdminId;
        this.adminFullName = adminFullName;
        this.answerDate = answerDate;
        this.status = status;
        this.isPublic = isPublic;
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getUserFullName() { return userFullName; }
    public String getQuestionText() { return questionText; }
    public LocalDateTime getQuestionDate() { return questionDate; }
    public String getAnswerText() { return answerText; }
    public String getAnsweredByAdminId() { return answeredByAdminId; }
    public String getAdminFullName() { return adminFullName; }
    public LocalDateTime getAnswerDate() { return answerDate; }
    public QuestionStatus getStatus() { return status; }
    public boolean isPublic() { return isPublic; }

    // Setters
    public void setUserFullName(String userFullName) { this.userFullName = userFullName;}
    public void setAnswerText(String answerText) { this.answerText = answerText; }
    public void setAnsweredByAdminId(String answeredByAdminId) { this.answeredByAdminId = answeredByAdminId; }
    public void setAdminFullName(String adminFullName) { this.adminFullName = adminFullName;}
    public void setAnswerDate(LocalDateTime answerDate) { this.answerDate = answerDate; }
    public void setStatus(QuestionStatus status) { this.status = status; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; } // Tên tham số 'aPublic' vẫn giữ như cũ

    // --- Phương thức tiện ích ---
    public String getFormattedQuestionDate() {
        if (questionDate == null) return "N/A";
        return questionDate.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
    }

    public String getFormattedAnswerDate() {
        if (answerDate == null) return "N/A";
        return answerDate.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
    }

    public String getQuestionTextOrDefault(String defaultValue) {
        return (this.questionText != null && !this.questionText.trim().isEmpty()) ? this.questionText.trim() : defaultValue;
    }

    public String getAnswerTextOrDefault(String defaultValue) {
        return (this.answerText != null && !this.answerText.trim().isEmpty()) ? this.answerText.trim() : defaultValue;
    }

    public String getUserFullNameOrDefault(String defaultValue) {
        return (this.userFullName != null && !this.userFullName.trim().isEmpty()) ? this.userFullName.trim() : defaultValue;
    }

    public String getAdminFullNameOrDefault(String defaultValue) {
        return (this.adminFullName != null && !this.adminFullName.trim().isEmpty()) ? this.adminFullName.trim() : defaultValue;
    }

    @Override
    public String toString() {
        // Dùng cho việc hiển thị tóm tắt trong ListView/ComboBox nếu cần
        return "Hỏi: " + getQuestionTextOrDefault("(Chưa có nội dung)") +
                " (User: " + getUserFullNameOrDefault(getUserId()) +
                ", Status: " + getStatus() + ")";
    }
}
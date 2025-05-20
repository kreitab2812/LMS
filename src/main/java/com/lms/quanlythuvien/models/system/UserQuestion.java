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

    private String id;                  // UUID
    private String userId;              // ID của người đặt câu hỏi
    private String userFullName;        // (Tùy chọn) Tên người hỏi để hiển thị cho Admin
    private String questionText;
    private LocalDateTime questionDate;
    private String answerText;          // Câu trả lời từ Admin (nullable)
    private String answeredByAdminId;   // ID của Admin đã trả lời (nullable)
    private String adminFullName;       // (Tùy chọn) Tên Admin trả lời
    private LocalDateTime answerDate;       // Ngày trả lời (nullable)
    private QuestionStatus status;
    private boolean isPublic;           // Admin quyết định có công khai câu hỏi này thành FAQ không

    // Constructor khi User gửi câu hỏi mới
    public UserQuestion(String userId, String questionText) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.questionText = questionText;
        this.questionDate = LocalDateTime.now();
        this.status = QuestionStatus.PENDING_REVIEW;
        this.isPublic = false; // Mặc định là chưa công khai
    }

    // Constructor đầy đủ (khi load từ DB)
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
    public String getUserFullName() { return userFullName; } // Cần join để lấy hoặc set sau
    public String getQuestionText() { return questionText; }
    public LocalDateTime getQuestionDate() { return questionDate; }
    public String getAnswerText() { return answerText; }
    public String getAnsweredByAdminId() { return answeredByAdminId; }
    public String getAdminFullName() { return adminFullName; } // Cần join hoặc set sau
    public LocalDateTime getAnswerDate() { return answerDate; }
    public QuestionStatus getStatus() { return status; }
    public boolean isPublic() { return isPublic; }

    // Setters (chủ yếu cho Admin cập nhật)
    public void setUserFullName(String userFullName) { this.userFullName = userFullName;}
    public void setAnswerText(String answerText) { this.answerText = answerText; }
    public void setAnsweredByAdminId(String answeredByAdminId) { this.answeredByAdminId = answeredByAdminId; }
    public void setAdminFullName(String adminFullName) { this.adminFullName = adminFullName;}
    public void setAnswerDate(LocalDateTime answerDate) { this.answerDate = answerDate; }
    public void setStatus(QuestionStatus status) { this.status = status; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }

    public String getFormattedQuestionDate() {
        if (questionDate == null) return "";
        return questionDate.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
    }
    public String getFormattedAnswerDate() {
        if (answerDate == null) return "";
        return answerDate.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
    }
}
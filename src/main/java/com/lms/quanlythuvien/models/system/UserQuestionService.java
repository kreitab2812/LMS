package com.lms.quanlythuvien.services.system;

import com.lms.quanlythuvien.models.system.UserQuestion;
import com.lms.quanlythuvien.utils.database.DatabaseManager;
// Cần import thêm User và UserService để lấy tên người dùng/admin nếu muốn
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.user.UserService;


import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserQuestionService {
    private static UserQuestionService instance;
    // private UserService userService; // Để lấy tên người dùng

    private UserQuestionService() {
        // this.userService = UserService.getInstance();
    }

    public static synchronized UserQuestionService getInstance() {
        if (instance == null) {
            instance = new UserQuestionService();
        }
        return instance;
    }

    private UserQuestion mapResultSetToUserQuestion(ResultSet rs) throws SQLException {
        UserQuestion uq = new UserQuestion(
                rs.getString("id"),
                rs.getString("userId"),
                null, // userFullName sẽ được lấy sau nếu cần
                rs.getString("questionText"),
                LocalDateTime.parse(rs.getString("questionDate"), DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                rs.getString("answerText"),
                rs.getString("answeredByAdminId"),
                null, // adminFullName sẽ được lấy sau nếu cần
                rs.getString("answerDate") != null ? LocalDateTime.parse(rs.getString("answerDate"), DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                UserQuestion.QuestionStatus.valueOf(rs.getString("status")),
                rs.getInt("isPublic") == 1
        );
        // Lấy tên người dùng và tên admin (tùy chọn, có thể làm ở controller để giảm query trong service)
        // Ví dụ:
        // Optional<User> userOpt = userService.findUserById(uq.getUserId());
        // userOpt.ifPresent(user -> uq.setUserFullName(user.getUsername())); // Hoặc getFullName() nếu có
        // if (uq.getAnsweredByAdminId() != null) {
        //     Optional<User> adminOpt = userService.findUserById(uq.getAnsweredByAdminId());
        //     adminOpt.ifPresent(admin -> uq.setAdminFullName(admin.getUsername()));
        // }
        return uq;
    }

    public Optional<UserQuestion> submitQuestion(UserQuestion question) {
        String sql = "INSERT INTO UserQuestions (id, userId, questionText, questionDate, status, isPublic) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, question.getId());
            pstmt.setString(2, question.getUserId());
            pstmt.setString(3, question.getQuestionText());
            pstmt.setString(4, question.getQuestionDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.setString(5, question.getStatus().name());
            pstmt.setInt(6, question.isPublic() ? 1 : 0);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                return Optional.of(question);
            }
        } catch (SQLException e) {
            System.err.println("ERROR_UQS_SUBMIT: Could not submit question: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<UserQuestion> getQuestionsByUserId(String userId) {
        List<UserQuestion> questions = new ArrayList<>();
        String sql = "SELECT * FROM UserQuestions WHERE userId = ? ORDER BY questionDate DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapResultSetToUserQuestion(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_UQS_GET_BY_USER: Could not get questions for user " + userId + ": " + e.getMessage());
        }
        return questions;
    }

    // Lấy các câu hỏi đã được công khai làm FAQ
    public List<UserQuestion> getPublicFAQItems() {
        List<UserQuestion> faqItems = new ArrayList<>();
        String sql = "SELECT * FROM UserQuestions WHERE isPublic = 1 AND status = 'PUBLISHED_AS_FAQ' ORDER BY answerDate DESC, questionDate DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement(); // Hoặc PreparedStatement nếu cần
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                faqItems.add(mapResultSetToUserQuestion(rs));
            }
        } catch (SQLException e) {
            System.err.println("ERROR_UQS_GET_PUBLIC: Could not get public FAQ items: " + e.getMessage());
        }
        return faqItems;
    }

    // Các phương thức cho Admin (sẽ làm sau):
    // getPendingQuestions(), answerQuestion(questionId, answerText, adminId), markAsPublicFAQ(questionId), etc.
}
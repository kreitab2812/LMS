package com.lms.quanlythuvien.services.user;

import com.lms.quanlythuvien.models.system.UserQuestion;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.utils.database.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class UserQuestionService {
    private static UserQuestionService instance;
    private final UserService userService;
    private static final DateTimeFormatter DB_DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private UserQuestionService() {
        this.userService = UserService.getInstance();
    }

    public static synchronized UserQuestionService getInstance() {
        if (instance == null) {
            instance = new UserQuestionService();
        }
        return instance;
    }

    private UserQuestion mapResultSetToBasicUserQuestion(ResultSet rs) throws SQLException {
        String answerDateStr = rs.getString("answerDate");
        return new UserQuestion(
                rs.getString("id"),
                rs.getString("userId"),
                null, // userFullName
                rs.getString("questionText"),
                LocalDateTime.parse(rs.getString("questionDate"), DB_DATETIME_FORMATTER),
                rs.getString("answerText"),
                rs.getString("answeredByAdminId"),
                null, // adminFullName
                answerDateStr != null ? LocalDateTime.parse(answerDateStr, DB_DATETIME_FORMATTER) : null,
                UserQuestion.QuestionStatus.valueOf(rs.getString("status").toUpperCase()),
                rs.getInt("isPublic") == 1
        );
    }

    private void populateUserDetails(List<UserQuestion> questions) {
        if (questions == null || questions.isEmpty()) return;
        Set<String> userIdsToFetch = new HashSet<>();
        for (UserQuestion q : questions) {
            if (q.getUserId() != null) userIdsToFetch.add(q.getUserId());
            if (q.getAnsweredByAdminId() != null) userIdsToFetch.add(q.getAnsweredByAdminId());
        }
        if (userIdsToFetch.isEmpty()) return;
        Map<String, User> usersMap = userService.getUsersMapByIds(userIdsToFetch);
        for (UserQuestion q : questions) {
            if (q.getUserId() != null && usersMap.containsKey(q.getUserId())) {
                q.setUserFullName(usersMap.get(q.getUserId()).getUsername());
            }
            if (q.getAnsweredByAdminId() != null && usersMap.containsKey(q.getAnsweredByAdminId())) {
                q.setAdminFullName(usersMap.get(q.getAnsweredByAdminId()).getUsername());
            }
        }
    }

    public Optional<UserQuestion> submitQuestion(UserQuestion question) {
        if (question == null || question.getUserId() == null || question.getQuestionText() == null || question.getQuestionText().trim().isEmpty()) {
            System.err.println("ERROR_UQS_SUBMIT: Invalid question data.");
            return Optional.empty();
        }
        String sql = "INSERT INTO UserQuestions (id, userId, questionText, questionDate, status, isPublic) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, question.getId());
            pstmt.setString(2, question.getUserId());
            pstmt.setString(3, question.getQuestionText());
            pstmt.setString(4, question.getQuestionDate().format(DB_DATETIME_FORMATTER));
            pstmt.setString(5, question.getStatus().name()); // Sẽ là PENDING_REVIEW từ constructor
            pstmt.setInt(6, question.isPublic() ? 1 : 0);   // Sẽ là false (0) từ constructor

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_UQS_SUBMIT: Question submitted. ID: " + question.getId());
                // Không cần populate userFullName ở đây vì object gốc đã có
                return Optional.of(question);
            }
        } catch (SQLException e) {
            System.err.println("ERROR_UQS_SUBMIT: Could not submit question for user " + question.getUserId() + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<UserQuestion> getQuestionsByUserId(String userId) {
        List<UserQuestion> questions = new ArrayList<>();
        if (userId == null || userId.trim().isEmpty()) return questions;
        String sql = "SELECT * FROM UserQuestions WHERE userId = ? ORDER BY questionDate DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapResultSetToBasicUserQuestion(rs));
                }
            }
            if (!questions.isEmpty()) populateUserDetails(questions);
        } catch (SQLException e) {
            System.err.println("ERROR_UQS_GET_BY_USER: Could not get questions for user " + userId + ": " + e.getMessage());
        }
        return questions;
    }

    /**
     * Lấy tất cả các câu hỏi từ người dùng, đã được populate thông tin người dùng.
     * @return Danh sách tất cả UserQuestion.
     */
    public List<UserQuestion> getAllQuestions() {
        List<UserQuestion> allQuestions = new ArrayList<>();
        String sql = "SELECT * FROM UserQuestions ORDER BY questionDate DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                allQuestions.add(mapResultSetToBasicUserQuestion(rs));
            }
            if (!allQuestions.isEmpty()) {
                populateUserDetails(allQuestions);
            }
        } catch (SQLException e) {
            System.err.println("ERROR_UQS_GET_ALL: Could not get all user questions: " + e.getMessage());
        }
        return allQuestions;
    }


    public List<UserQuestion> getPublicFAQItems() {
        List<UserQuestion> faqItems = new ArrayList<>();
        String sql = "SELECT * FROM UserQuestions WHERE isPublic = 1 AND status = ? ORDER BY answerDate DESC, questionDate DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, UserQuestion.QuestionStatus.PUBLISHED_AS_FAQ.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    faqItems.add(mapResultSetToBasicUserQuestion(rs));
                }
            }
            if (!faqItems.isEmpty()) populateUserDetails(faqItems);
        } catch (SQLException e) {
            System.err.println("ERROR_UQS_GET_PUBLIC: Could not get public FAQ items: " + e.getMessage());
        }
        return faqItems;
    }

    public List<UserQuestion> getPendingQuestions() {
        List<UserQuestion> pendingQuestions = new ArrayList<>();
        String sql = "SELECT * FROM UserQuestions WHERE status = ? ORDER BY questionDate ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, UserQuestion.QuestionStatus.PENDING_REVIEW.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    pendingQuestions.add(mapResultSetToBasicUserQuestion(rs));
                }
            }
            if (!pendingQuestions.isEmpty()) populateUserDetails(pendingQuestions);
        } catch (SQLException e) {
            System.err.println("ERROR_UQS_GET_PENDING: Could not get pending questions: " + e.getMessage());
        }
        return pendingQuestions;
    }

    public Optional<UserQuestion> findQuestionById(String questionId) {
        if (questionId == null || questionId.trim().isEmpty()) return Optional.empty();
        String sql = "SELECT * FROM UserQuestions WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, questionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    UserQuestion uq = mapResultSetToBasicUserQuestion(rs);
                    populateUserDetails(List.of(uq)); // Populate cho một câu hỏi này
                    return Optional.of(uq);
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR_UQS_FIND_BY_ID: Could not find question " + questionId + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean answerQuestion(String questionId, String answerText, String answeredByAdminId) {
        if (questionId == null || answerText == null || answerText.trim().isEmpty() || answeredByAdminId == null) {
            System.err.println("ERROR_UQS_ANSWER: Invalid parameters for answering question.");
            return false;
        }
        // Câu hỏi có thể đang ở PENDING_REVIEW hoặc đã ANSWERED (nếu admin muốn sửa câu trả lời)
        String sql = "UPDATE UserQuestions SET answerText = ?, answeredByAdminId = ?, answerDate = ?, status = ? " +
                "WHERE id = ? AND status IN (?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, answerText);
            pstmt.setString(2, answeredByAdminId);
            pstmt.setString(3, LocalDateTime.now().format(DB_DATETIME_FORMATTER));
            pstmt.setString(4, UserQuestion.QuestionStatus.ANSWERED.name()); // Luôn set là ANSWERED khi có câu trả lời
            pstmt.setString(5, questionId);
            pstmt.setString(6, UserQuestion.QuestionStatus.PENDING_REVIEW.name());
            pstmt.setString(7, UserQuestion.QuestionStatus.ANSWERED.name()); // Cho phép sửa câu trả lời đã có

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_UQS_ANSWER: Question " + questionId + " answered/updated by admin " + answeredByAdminId);
                return true;
            } else {
                System.err.println("WARN_UQS_ANSWER: Question " + questionId + " not found or not in an editable state (PENDING_REVIEW or ANSWERED).");
            }
        } catch (SQLException e) {
            System.err.println("ERROR_UQS_ANSWER: Could not answer/update question " + questionId + ": " + e.getMessage());
        }
        return false;
    }

    public boolean publishFAQ(String questionId) {
        Optional<UserQuestion> questionOpt = findQuestionById(questionId);
        if (questionOpt.isEmpty() || questionOpt.get().getStatus() == UserQuestion.QuestionStatus.PENDING_REVIEW ||
                questionOpt.get().getAnswerText() == null || questionOpt.get().getAnswerText().trim().isEmpty()) {
            System.err.println("WARN_UQS_PUBLISH: Question " + questionId + " not found, not answered, or has no answer text. Cannot publish.");
            return false;
        }
        // Chỉ publish câu hỏi đã được trả lời (status = ANSWERED hoặc đã là PUBLISHED_AS_FAQ nếu muốn cập nhật lại)
        String sql = "UPDATE UserQuestions SET isPublic = 1, status = ? WHERE id = ? AND status IN (?,?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, UserQuestion.QuestionStatus.PUBLISHED_AS_FAQ.name());
            pstmt.setString(2, questionId);
            pstmt.setString(3, UserQuestion.QuestionStatus.ANSWERED.name());
            pstmt.setString(4, UserQuestion.QuestionStatus.PUBLISHED_AS_FAQ.name());


            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_UQS_PUBLISH: Question " + questionId + " published/re-published as FAQ.");
                return true;
            } else {
                System.err.println("WARN_UQS_PUBLISH: Question " + questionId + " not updated. Might already be in desired state or not found.");
            }
        } catch (SQLException e) {
            System.err.println("ERROR_UQS_PUBLISH: Could not publish FAQ " + questionId + ": " + e.getMessage());
        }
        return false;
    }

    public boolean unpublishFAQ(String questionId) {
        // Chỉ unpublish câu hỏi đang là PUBLISHED_AS_FAQ, đưa về trạng thái ANSWERED
        String sql = "UPDATE UserQuestions SET isPublic = 0, status = ? WHERE id = ? AND status = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, UserQuestion.QuestionStatus.ANSWERED.name());
            pstmt.setString(2, questionId);
            pstmt.setString(3, UserQuestion.QuestionStatus.PUBLISHED_AS_FAQ.name());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG_UQS_UNPUBLISH: Question " + questionId + " unpublished from FAQ, status set to ANSWERED.");
                return true;
            } else {
                System.err.println("WARN_UQS_UNPUBLISH: Question " + questionId + " not found or not currently a published FAQ.");
            }
        } catch (SQLException e) {
            System.err.println("ERROR_UQS_UNPUBLISH: Could not unpublish FAQ " + questionId + ": " + e.getMessage());
        }
        return false;
    }


    public boolean rejectQuestion(String questionId, String adminNotes, String adminId) {
        Optional<UserQuestion> questionOpt = findQuestionById(questionId);
        if (questionOpt.isEmpty() || questionOpt.get().getStatus() != UserQuestion.QuestionStatus.PENDING_REVIEW) {
            System.err.println("WARN_UQS_REJECT: Question " + questionId + " not found or not in PENDING_REVIEW state.");
            return false;
        }

        String sql = "UPDATE UserQuestions SET status = ?, adminNotes = ?, answeredByAdminId = ?, answerDate = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, UserQuestion.QuestionStatus.REJECTED.name());
            pstmt.setString(2, adminNotes != null ? adminNotes.trim() : "Câu hỏi không phù hợp hoặc đã có.");
            pstmt.setString(3, adminId);
            pstmt.setString(4, LocalDateTime.now().format(DB_DATETIME_FORMATTER));
            pstmt.setString(5, questionId);

            int affectedRows = pstmt.executeUpdate();
            if(affectedRows > 0){
                System.out.println("DEBUG_UQS_REJECT: Question " + questionId + " rejected by admin " + adminId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("ERROR_UQS_REJECT: Could not reject question " + questionId + ": " + e.getMessage());
        }
        return false;
    }
}
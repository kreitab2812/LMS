package com.lms.quanlythuvien.models.item; // Hoặc package phù hợp

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class BookReview {
    private String reviewId;
    private int bookInternalId;
    private String userId;
    private String userUsername; // Để hiển thị tên người review
    private int rating; // Ví dụ: 1 đến 5, 0 nếu chỉ có bình luận
    private String commentText;
    private LocalDateTime reviewDate;

    // Constructor khi tạo mới
    public BookReview(int bookInternalId, String userId, int rating, String commentText) {
        this.reviewId = UUID.randomUUID().toString();
        this.bookInternalId = bookInternalId;
        this.userId = userId;
        this.rating = rating;
        this.commentText = commentText;
        this.reviewDate = LocalDateTime.now();
    }

    // Constructor đầy đủ (khi load từ DB)
    public BookReview(String reviewId, int bookInternalId, String userId, String userUsername, int rating, String commentText, LocalDateTime reviewDate) {
        this.reviewId = reviewId;
        this.bookInternalId = bookInternalId;
        this.userId = userId;
        this.userUsername = userUsername; // Sẽ được set sau khi join hoặc lấy từ UserService
        this.rating = rating;
        this.commentText = commentText;
        this.reviewDate = reviewDate;
    }

    // Getters
    public String getReviewId() { return reviewId; }
    public int getBookInternalId() { return bookInternalId; }
    public String getUserId() { return userId; }
    public String getUserUsername() { return userUsername; }
    public int getRating() { return rating; }
    public String getCommentText() { return commentText; }
    public LocalDateTime getReviewDate() { return reviewDate; }

    // Setters
    public void setUserUsername(String userUsername) { this.userUsername = userUsername; }
    public void setRating(int rating) { this.rating = rating;}
    public void setCommentText(String commentText) { this.commentText = commentText;}


    public String getFormattedReviewDate() {
        if (reviewDate == null) return "";
        return reviewDate.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
    }
}
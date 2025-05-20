package com.lms.quanlythuvien.services.user; // Hoặc package phù hợp

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserFavoriteBook {
    private String userId;
    private int bookInternalId; // Tham chiếu đến Books.internalId
    private LocalDateTime favoritedAt;

    // Constructor khi tạo mới
    public UserFavoriteBook(String userId, int bookInternalId) {
        this.userId = userId;
        this.bookInternalId = bookInternalId;
        this.favoritedAt = LocalDateTime.now();
    }

    // Constructor khi load từ DB
    public UserFavoriteBook(String userId, int bookInternalId, LocalDateTime favoritedAt) {
        this.userId = userId;
        this.bookInternalId = bookInternalId;
        this.favoritedAt = favoritedAt;
    }

    // Getters
    public String getUserId() { return userId; }
    public int getBookInternalId() { return bookInternalId; }
    public LocalDateTime getFavoritedAt() { return favoritedAt; }

    public String getFormattedFavoritedAt() {
        if (favoritedAt == null) return "";
        return favoritedAt.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
    }

    // Setters (ít khi cần, vì favoritedAt được đặt khi tạo)
    public void setFavoritedAt(LocalDateTime favoritedAt) { this.favoritedAt = favoritedAt; }
}
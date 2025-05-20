package com.lms.quanlythuvien.models.item;

import java.util.List;

public abstract class Document {
    protected int internalId;
    protected String id; // Thường là ID từ API, ví dụ Google Books ID
    protected String title;
    protected List<String> authors;
    protected String publisher;
    protected String publishedDate; // Có thể là "2023" hoặc "2023-05-15"
    protected String description;
    protected List<String> categories; // Thể loại
    protected String thumbnailUrl; // URL ảnh bìa nhỏ
    protected String infoLink; // Link đến trang chi tiết (ví dụ trên Google Books)

    public Document(String id, String title, List<String> authors, String publisher, String publishedDate, String description, List<String> categories, String thumbnailUrl, String infoLink) {
        this.id = id;
        this.title = title;
        this.authors = authors;
        this.publisher = publisher;
        this.publishedDate = publishedDate;
        this.description = description;
        this.categories = categories;
        this.thumbnailUrl = thumbnailUrl;
        this.infoLink = infoLink;
    }

    // --- Getters (Giữ nguyên như của bạn) ---
    public int getInternalId() { return internalId; }
    public String getId() { return id; }
    public String getTitle() { return title; }
    public List<String> getAuthors() { return authors; }
    public String getPublisher() { return publisher; }
    public String getPublishedDate() { return publishedDate; }
    public String getDescription() { return description; }
    public List<String> getCategories() { return categories; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getInfoLink() { return infoLink; }

    // --- Setters cho các thuộc tính của Document ---
    public void setId(String id) { this.id = id; }

    public void setInternalId(int internalId) { this.internalId = internalId; }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void setInfoLink(String infoLink) {
        this.infoLink = infoLink;
    }

    // Phương thức trừu tượng
    public abstract void displayInfo();

    @Override
    public String toString() {
        return "Document{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", authors=" + (authors != null ? String.join(", ", authors) : "N/A") +
                '}';
    }
}
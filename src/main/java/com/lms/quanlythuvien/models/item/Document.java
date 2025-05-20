package com.lms.quanlythuvien.models.item;

import java.util.List;
import java.util.ArrayList;

public abstract class Document {
    protected String id; // Sẽ dùng để lưu ISBN13 cho Book, hoặc ID chính cho các Document khác
    protected int internalId;
    protected String customDisplayId; // ID hiển thị tùy chỉnh
    protected String title;
    protected List<String> authors;
    protected String publisher;
    protected String publishedDate;
    protected String description;
    protected List<String> categories;
    protected String thumbnailUrl;
    protected String infoLink;

    public Document(String id, String title, List<String> authors, String publisher,
                    String publishedDate, String description, List<String> categories,
                    String thumbnailUrl, String infoLink) {
        this.id = id;
        this.title = title;
        this.authors = (authors != null) ? new ArrayList<>(authors) : new ArrayList<>();
        this.publisher = publisher;
        this.publishedDate = publishedDate;
        this.description = description;
        this.categories = (categories != null) ? new ArrayList<>(categories) : new ArrayList<>();
        this.thumbnailUrl = thumbnailUrl;
        this.infoLink = infoLink;
    }

    // Getters
    public String getId() { return id; }
    public int getInternalId() { return internalId; }
    public String getCustomDisplayId() { return customDisplayId; }
    public String getTitle() { return title; }
    public List<String> getAuthors() { return authors; }
    public String getPublisher() { return publisher; }
    public String getPublishedDate() { return publishedDate; }
    public String getDescription() { return description; }
    public List<String> getCategories() { return categories; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getInfoLink() { return infoLink; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setInternalId(int internalId) { this.internalId = internalId; }
    public void setCustomDisplayId(String customDisplayId) { this.customDisplayId = customDisplayId; }
    public void setTitle(String title) { this.title = title; }
    public void setAuthors(List<String> authors) { this.authors = (authors != null) ? new ArrayList<>(authors) : new ArrayList<>(); }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public void setPublishedDate(String publishedDate) { this.publishedDate = publishedDate; }
    public void setDescription(String description) { this.description = description; }
    public void setCategories(List<String> categories) { this.categories = (categories != null) ? new ArrayList<>(categories) : new ArrayList<>();}
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public void setInfoLink(String infoLink) { this.infoLink = infoLink; }

    public abstract void displayInfo();
}
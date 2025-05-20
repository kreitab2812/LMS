package com.lms.quanlythuvien.models.item;

import java.util.List;
import java.util.ArrayList; // Đã có
import java.util.stream.Collectors; // Thêm cho join list

public class Book extends Document {
    private String isbn10;
    // isbn13 giờ được quản lý bởi trường 'id' của lớp Document cha (super.getId())
    private Integer pageCount;
    private Double averageRating;
    private Integer ratingsCount;
    private int totalQuantity;
    private int availableQuantity;
    private String shelfLocation;
    private String qrCodeData;

    // Constructor chính
    public Book(String isbn13_as_id, String title, List<String> authors, String publisher,
                String publishedDate, String description, List<String> categories,
                String thumbnailUrl, String infoLink,
                String isbn10, /*Bỏ isbn13 thừa ở đây*/ Integer pageCount,
                Double averageRating, Integer ratingsCount, int initialQuantity) {
        super(isbn13_as_id, title, authors, publisher, publishedDate, description, categories, thumbnailUrl, infoLink);
        this.isbn10 = isbn10;
        this.pageCount = pageCount;
        this.averageRating = averageRating;
        this.ratingsCount = ratingsCount;
        this.totalQuantity = initialQuantity;
        this.availableQuantity = initialQuantity; // Ban đầu số lượng có sẵn bằng tổng số lượng
        // shelfLocation và qrCodeData có thể được set sau
    }

    // Constructor phụ (nếu dùng)
    public Book(String isbn13_as_id, String title, List<String> authors, String publisher,
                String publishedDate, String description, List<String> categories,
                String thumbnailUrl, String infoLink,
                String isbn10, Integer pageCount,
                Double averageRating, Integer ratingsCount) {
        this(isbn13_as_id, title, authors, publisher, publishedDate, description, categories,
                thumbnailUrl, infoLink, isbn10, pageCount, averageRating, ratingsCount, 0);
    }


    // --- Getters ---
    public String getIsbn10() { return isbn10; }
    public String getIsbn13() { return super.getId(); } // Lấy từ id của Document
    public Integer getPageCount() { return pageCount; }
    public Double getAverageRating() { return averageRating; }
    public Integer getRatingsCount() { return ratingsCount; }
    public int getTotalQuantity() { return totalQuantity; }
    public int getAvailableQuantity() { return availableQuantity; }
    public String getShelfLocation() { return shelfLocation; }
    public String getQrCodeData() { return qrCodeData; }

    // --- Setters ---
    public void setIsbn10(String isbn10) { this.isbn10 = isbn10; }
    // public void setIsbn13(String isbn13) { super.setId(isbn13); } // Không cần nếu ID là không đổi sau khi tạo
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
    public void setRatingsCount(Integer ratingsCount) { this.ratingsCount = ratingsCount; }
    public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }
    public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }
    public void setShelfLocation(String shelfLocation) { this.shelfLocation = shelfLocation; }
    public void setQrCodeData(String qrCodeData) { this.qrCodeData = qrCodeData; }

    // --- Các phương thức tiện ích "OrDefault" và "Formatted" ---
    public String getTitleOrDefault(String defaultValue) {
        return (super.getTitle() != null && !super.getTitle().isEmpty()) ? super.getTitle() : defaultValue;
    }

    public String getAuthorsFormatted(String defaultValue) {
        List<String> authors = super.getAuthors();
        return (authors != null && !authors.isEmpty()) ? String.join(", ", authors) : defaultValue;
    }

    public String getPublisherOrDefault(String defaultValue) {
        return (super.getPublisher() != null && !super.getPublisher().isEmpty()) ? super.getPublisher() : defaultValue;
    }

    public String getPublishedDateOrDefault(String defaultValue) {
        return (super.getPublishedDate() != null && !super.getPublishedDate().isEmpty()) ? super.getPublishedDate() : defaultValue;
    }

    public String getIsbn13OrDefault(String defaultValue) {
        return (super.getId() != null && !super.getId().isEmpty()) ? super.getId() : defaultValue;
    }

    public String getCategoriesFormatted(String defaultValue) {
        List<String> categories = super.getCategories();
        return (categories != null && !categories.isEmpty()) ? String.join("; ", categories) : defaultValue;
    }

    public String getDescriptionOrDefault(String defaultValue) {
        return (super.getDescription() != null && !super.getDescription().isEmpty()) ? super.getDescription() : defaultValue;
    }

    public String getShelfLocationOrDefault(String defaultValue) {
        return (this.shelfLocation != null && !this.shelfLocation.isEmpty()) ? this.shelfLocation : defaultValue;
    }


    // --- Các phương thức khác ---
    public boolean borrowBook() {
        if (this.availableQuantity > 0) {
            this.availableQuantity--;
            return true;
        }
        return false;
    }

    public void returnBook() {
        if (this.availableQuantity < this.totalQuantity) { // Chỉ tăng nếu chưa đạt max
            this.availableQuantity++;
        }
    }

    @Override
    public void displayInfo() {
        System.out.println("Book Title: " + getTitleOrDefault("N/A"));
        System.out.println("Authors: " + getAuthorsFormatted("N/A"));
        System.out.println("ISBN-13 (ID): " + getIsbn13OrDefault("N/A"));
        System.out.println("Internal DB ID: " + getInternalId()); // Giả sử Document có getInternalId()
        System.out.println("QR Data: " + (qrCodeData != null ? qrCodeData : "N/A"));
        System.out.println("Total Quantity: " + totalQuantity);
        System.out.println("Available Quantity: " + availableQuantity);
    }

    @Override
    public String toString() {
        return "Book{" +
                "internalId=" + getInternalId() +
                ", isbn13(id)='" + getId() + '\'' +
                ", title='" + getTitle() + '\'' +
                ", authors=" + getAuthorsFormatted("N/A") +
                ", qrCodeData='" + qrCodeData + '\'' +
                ", totalQuantity=" + totalQuantity +
                ", availableQuantity=" + availableQuantity +
                '}';
    }
}
package com.lms.quanlythuvien.models;

import java.util.List;

public class Book extends Document {
    private String isbn10;
    private String isbn13;
    private Integer pageCount;
    private Double averageRating; // Điểm đánh giá trung bình
    private Integer ratingsCount;  // Số lượt đánh giá
    private int totalQuantity; // Tổng số lượng trong kho (ĐỔI TÊN TỪ quantityInStock)
    private int availableQuantity; // Số lượng hiện có sẵn để cho mượn (THÊM MỚI)
    private String shelfLocation; // Vị trí trên kệ

    public Book(String id, String title, List<String> authors, String publisher,
                String publishedDate, String description, List<String> categories,
                String thumbnailUrl, String infoLink,
                String isbn10, String isbn13, Integer pageCount,
                Double averageRating, Integer ratingsCount, int initialQuantity) { // Thêm initialQuantity
        super(id, title, authors, publisher, publishedDate, description, categories, thumbnailUrl, infoLink);
        this.isbn10 = isbn10;
        this.isbn13 = isbn13;
        this.pageCount = pageCount;
        this.averageRating = averageRating;
        this.ratingsCount = ratingsCount;
        this.totalQuantity = initialQuantity; // Gán tổng số lượng
        this.availableQuantity = initialQuantity; // Ban đầu, số lượng có sẵn bằng tổng số lượng
        this.shelfLocation = "N/A"; // Giá trị mặc định
    }

    // Constructor cũ có thể giữ lại hoặc điều chỉnh nếu cần
    public Book(String id, String title, List<String> authors, String publisher,
                String publishedDate, String description, List<String> categories,
                String thumbnailUrl, String infoLink,
                String isbn10, String isbn13, Integer pageCount,
                Double averageRating, Integer ratingsCount) {
        this(id, title, authors, publisher, publishedDate, description, categories, thumbnailUrl, infoLink, isbn10, isbn13, pageCount, averageRating, ratingsCount, 0); // Gọi constructor chính với initialQuantity = 0
    }


    // --- Getters ---
    public String getIsbn10() { return isbn10; }
    public String getIsbn13() { return isbn13; }
    public Integer getPageCount() { return pageCount; }
    public Double getAverageRating() { return averageRating; }
    public Integer getRatingsCount() { return ratingsCount; }
    public int getTotalQuantity() { return totalQuantity; } // Đổi tên getter
    public int getAvailableQuantity() { return availableQuantity; } // Getter mới
    public String getShelfLocation() { return shelfLocation; }

    // --- Setters ---
    public void setIsbn10(String isbn10) { this.isbn10 = isbn10; }
    public void setIsbn13(String isbn13) { this.isbn13 = isbn13; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
    public void setRatingsCount(Integer ratingsCount) { this.ratingsCount = ratingsCount; }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
        // Cân nhắc: có nên cập nhật availableQuantity ở đây không?
        // Ví dụ: nếu tổng số lượng giảm xuống thấp hơn số đang cho mượn thì sẽ xử lý thế nào?
        // Tạm thời, có thể để việc điều chỉnh availableQuantity cho logic nghiệp vụ mượn/trả.
        // Hoặc, nếu thay đổi totalQuantity, availableQuantity cũng nên được điều chỉnh một cách hợp lý.
        // Ví dụ đơn giản: this.availableQuantity = Math.min(this.availableQuantity, this.totalQuantity);
        // và nếu this.totalQuantity tăng lên, this.availableQuantity có thể tăng theo phần chênh lệch.
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public void setShelfLocation(String shelfLocation) { this.shelfLocation = shelfLocation; }

    // Phương thức để giảm số lượng có sẵn khi mượn sách
    public boolean borrowBook() {
        if (this.availableQuantity > 0) {
            this.availableQuantity--;
            return true; // Mượn thành công
        }
        return false; // Không đủ sách để mượn
    }

    // Phương thức để tăng số lượng có sẵn khi trả sách
    public void returnBook() {
        if (this.availableQuantity < this.totalQuantity) { // Đảm bảo không vượt quá tổng số lượng
            this.availableQuantity++;
        }
        // Cần có logic kiểm tra nếu availableQuantity > totalQuantity (trường hợp lỗi)
    }


    // --- Kế thừa và override setters từ Document ---
    @Override
    public void setTitle(String title) { super.setTitle(title); }
    @Override
    public void setAuthors(List<String> authors) { super.setAuthors(authors); }
    @Override
    public void setPublisher(String publisher) { super.setPublisher(publisher); }
    @Override
    public void setPublishedDate(String publishedDate) { super.setPublishedDate(publishedDate); }
    @Override
    public void setDescription(String description) { super.setDescription(description); }
    @Override
    public void setCategories(List<String> categories) { super.setCategories(categories); }
    @Override
    public void setThumbnailUrl(String thumbnailUrl) { super.setThumbnailUrl(thumbnailUrl); }

    @Override
    public void displayInfo() {
        System.out.println("Book Title: " + getTitle());
        System.out.println("Authors: " + (getAuthors() != null ? String.join(", ", getAuthors()) : "N/A"));
        System.out.println("ISBN-13: " + (isbn13 != null ? isbn13 : "N/A"));
        System.out.println("Total Quantity: " + totalQuantity);
        System.out.println("Available Quantity: " + availableQuantity);
    }

    @Override
    public String toString() {
        return "Book{" +
                "id='" + getId() + '\'' +
                ", title='" + getTitle() + '\'' +
                ", authors=" + (getAuthors() != null ? String.join(", ", getAuthors()) : "N/A") +
                ", isbn13='" + (isbn13 != null ? isbn13 : "N/A") + '\'' +
                ", totalQuantity=" + totalQuantity +
                ", availableQuantity=" + availableQuantity +
                '}';
    }
}
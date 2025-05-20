package com.lms.quanlythuvien.models.item; // Hoặc package models phù hợp của cậu

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects; // Thêm import cho Objects

public class Author {
    private int id; // ID tự tăng từ database
    private String name;
    private String biography;
    private Integer yearOfBirth;
    private Integer yearOfDeath; // Có thể null
    private String gender; // Ví dụ: "Nam", "Nữ", "Khác"
    private String nationality; // Quốc tịch
    private String placeOfBirth;
    private String avatarUrl; // Đường dẫn đến ảnh đại diện
    private String createdAt;
    private String updatedAt;

    // Constructor để tạo mới (trước khi có ID và timestamps từ DB)
    public Author(String name, String biography, Integer yearOfBirth, Integer yearOfDeath, String gender, String nationality, String placeOfBirth, String avatarUrl) {
        this.name = name;
        this.biography = biography;
        this.yearOfBirth = yearOfBirth;
        this.yearOfDeath = yearOfDeath;
        this.gender = gender;
        this.nationality = nationality;
        this.placeOfBirth = placeOfBirth;
        this.avatarUrl = avatarUrl;
        // createdAt và updatedAt sẽ được gán khi lưu vào DB hoặc trong service
    }

    // Constructor đầy đủ (khi load từ DB, bao gồm id và timestamps)
    public Author(int id, String name, String biography, Integer yearOfBirth, Integer yearOfDeath, String gender, String nationality, String placeOfBirth, String avatarUrl, String createdAt, String updatedAt) {
        this.id = id;
        this.name = name;
        this.biography = biography;
        this.yearOfBirth = yearOfBirth;
        this.yearOfDeath = yearOfDeath;
        this.gender = gender;
        this.nationality = nationality;
        this.placeOfBirth = placeOfBirth;
        this.avatarUrl = avatarUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getBiography() { return biography; }
    public Integer getYearOfBirth() { return yearOfBirth; }
    public Integer getYearOfDeath() { return yearOfDeath; }
    public String getGender() { return gender; }
    public String getNationality() { return nationality; }
    public String getPlaceOfBirth() { return placeOfBirth; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setBiography(String biography) { this.biography = biography; }
    public void setYearOfBirth(Integer yearOfBirth) { this.yearOfBirth = yearOfBirth; }
    public void setYearOfDeath(Integer yearOfDeath) { this.yearOfDeath = yearOfDeath; }
    public void setGender(String gender) { this.gender = gender; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public void setPlaceOfBirth(String placeOfBirth) { this.placeOfBirth = placeOfBirth; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return name != null ? name : "N/A";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Author author = (Author) o;
        // Nếu ID > 0 (đã có từ DB), so sánh bằng ID. Nếu không, so sánh bằng tên.
        if (id > 0 && author.id > 0) {
            return id == author.id;
        }
        return Objects.equals(name, author.name); // Dùng Objects.equals để xử lý null
    }

    @Override
    public int hashCode() {
        // Nếu ID > 0, dùng ID. Nếu không, dùng tên.
        if (id > 0) {
            return Objects.hash(id);
        }
        return Objects.hash(name);
    }
}
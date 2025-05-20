package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.MainApp; // Để điều hướng
import com.lms.quanlythuvien.models.item.Author; // Model Tác giả
import com.lms.quanlythuvien.utils.session.SessionManager; // Để truyền Author được chọn

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.io.InputStream;

public class AuthorCardController {

    @FXML private VBox authorCardRoot;
    @FXML private ImageView avatarImageView;
    @FXML private Label nameLabel;
    @FXML private Label nationalityLabel;

    private Author currentAuthor;
    private Image defaultAvatarImage;
    // private UserLibraryController parentLibraryController; // Nếu cần gọi lại parent

    public AuthorCardController() {
        try (InputStream defaultStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_author_avatar.png")) { // Tạo ảnh default cho tác giả
            if (defaultStream != null) {
                defaultAvatarImage = new Image(defaultStream);
            } else {
                System.err.println("ERROR_ACC: Default author avatar image not found.");
            }
        } catch (Exception e) {
            System.err.println("ERROR_ACC: Exception loading default author avatar: " + e.getMessage());
        }
    }

    public void setData(Author author) {
        this.currentAuthor = author;
        if (author == null) return;

        nameLabel.setText(author.getName() != null ? author.getName() : "Chưa có tên");

        if (author.getNationality() != null && !author.getNationality().isEmpty()) {
            nationalityLabel.setText(author.getNationality());
            nationalityLabel.setVisible(true);
            nationalityLabel.setManaged(true);
        } else {
            nationalityLabel.setVisible(false);
            nationalityLabel.setManaged(false);
        }

        loadAvatarImage(author.getAvatarUrl());
    }

    private void loadAvatarImage(String imageUrl) {
        if (avatarImageView == null) return;
        Image imageToSet = this.defaultAvatarImage;
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Giả sử imageUrl là đường dẫn tương đối trong resources hoặc URL tuyệt đối
            try {
                // Nếu imageUrl là URL web
                if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                    Image loadedImage = new Image(imageUrl, true); // true cho background loading
                    if (!loadedImage.isError()) {
                        imageToSet = loadedImage;
                    }
                    loadedImage.errorProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal) avatarImageView.setImage(this.defaultAvatarImage);
                    });
                    if(loadedImage.isError()) avatarImageView.setImage(this.defaultAvatarImage);

                } else { // Nếu là đường dẫn file local (ví dụ trong resources)
                    InputStream stream = getClass().getResourceAsStream(imageUrl);
                    if (stream != null) {
                        imageToSet = new Image(stream);
                        stream.close();
                    }
                }
            } catch (Exception e) {
                System.err.println("ERROR_ACC_LOAD_AVATAR: Failed to load avatar: " + imageUrl + " - " + e.getMessage());
                // imageToSet vẫn là defaultAvatarImage
            }
        }
        avatarImageView.setImage(imageToSet);
    }

    @FXML
    void handleCardClicked(MouseEvent event) {
        if (currentAuthor != null) {
            System.out.println("Author card clicked: " + currentAuthor.getName());
            SessionManager.getInstance().setSelectedAuthor(currentAuthor); // << CẦN THÊM PHƯƠNG THỨC NÀY VÀO SESSIONMANAGER
            MainApp.loadScene("user/AuthorDetailView.fxml"); // << CẦN TẠO VIEW NÀY
        }
    }
}
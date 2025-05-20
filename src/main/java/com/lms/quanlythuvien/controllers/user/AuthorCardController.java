package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.models.item.Author;
import com.lms.quanlythuvien.utils.session.SessionManager;

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
    private UserLibraryController parentLibraryController; // Tham chiếu đến UserLibraryController

    public AuthorCardController() {
        try (InputStream defaultStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_author_avatar.png")) {
            if (defaultStream != null) {
                defaultAvatarImage = new Image(defaultStream);
            } else {
                System.err.println("ERROR_AUTHOR_CARD: Default author avatar not found.");
            }
        } catch (Exception e) {
            System.err.println("ERROR_AUTHOR_CARD: Exception loading default author avatar: " + e.getMessage());
        }
    }

    public void setData(Author author, UserLibraryController parentController) {
        this.currentAuthor = author;
        this.parentLibraryController = parentController;

        if (author == null) {
            if (authorCardRoot != null) {
                authorCardRoot.setVisible(false);
                authorCardRoot.setManaged(false);
            }
            return;
        }
        if (authorCardRoot != null) {
            authorCardRoot.setVisible(true);
            authorCardRoot.setManaged(true);
        }

        nameLabel.setText(author.getName() != null ? author.getName() : "N/A");
        String nationality = author.getNationality();
        if (nationality != null && !nationality.isEmpty()) {
            nationalityLabel.setText("Quốc tịch: " + nationality);
            nationalityLabel.setVisible(true);
            nationalityLabel.setManaged(true);
        } else {
            nationalityLabel.setText("Quốc tịch: N/A");
            // Giữ lại label nhưng với text N/A, hoặc ẩn đi tùy ý
            // nationalityLabel.setVisible(false);
            // nationalityLabel.setManaged(false);
        }
        loadAvatarImage(author.getAvatarUrl());
    }

    private void loadAvatarImage(String imageUrl) {
        if (avatarImageView == null) return;
        Image imageToSet = this.defaultAvatarImage;
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            String finalImageUrl = imageUrl.trim();
            if (finalImageUrl.startsWith("//")) finalImageUrl = "https:" + finalImageUrl;
            try {
                Image loadedImage = new Image(finalImageUrl, true); // true for background loading
                if (loadedImage.isError()) {
                    System.err.println("WARN_AUTHOR_CARD_AVATAR: Error loading image from URL: " + finalImageUrl);
                } else {
                    imageToSet = loadedImage;
                }
            } catch (Exception e) {
                System.err.println("ERROR_AUTHOR_CARD_AVATAR: Exception loading avatar from URL: " + finalImageUrl + ". " + e.getMessage());
            }
        }
        avatarImageView.setImage(imageToSet);
    }

    @FXML
    void handleCardClicked(MouseEvent event) {
        if (currentAuthor != null && parentLibraryController != null) {
            System.out.println("Author card clicked: " + currentAuthor.getName());
            SessionManager.getInstance().setSelectedAuthor(currentAuthor);
            parentLibraryController.navigateToAuthorDetail(); // Gọi phương thức của parent controller
        } else {
            System.err.println("ERROR_AUTHOR_CARD_CLICK: currentAuthor or parentLibraryController is null.");
        }
    }
}
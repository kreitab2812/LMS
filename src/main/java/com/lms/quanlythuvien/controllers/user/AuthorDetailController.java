package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.models.item.Author;
import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.services.library.AuthorManagementService;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.utils.session.SessionManager;
import com.lms.quanlythuvien.controllers.admin.AdminDashboardController; // THÊM IMPORT NÀY

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
// import javafx.stage.Stage; // Không cần trực tiếp nếu chỉ điều hướng qua dashboard

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AuthorDetailController implements Initializable {

    @FXML private Label authorNameHeaderLabel;
    @FXML private ImageView authorAvatarImageView;
    @FXML private Label authorNameLabel;
    @FXML private Label authorDobLabel;
    @FXML private Label authorDodLabel;
    @FXML private Label authorGenderLabel;
    @FXML private Label authorNationalityLabel;
    @FXML private Label authorPlaceOfBirthLabel;
    @FXML private TextArea authorBiographyArea;
    @FXML private TilePane authorBooksTilePane;
    @FXML private ScrollPane authorBooksScrollPane;
    @FXML private Label noBooksByAuthorLabel;
    @FXML private Button closeAuthorDetailButton;

    private Author currentAuthor;
    private BookManagementService bookManagementService;
    private AuthorManagementService authorManagementService;
    private Image defaultAuthorAvatar;

    // --- SỬA ĐỔI ĐỂ HỖ TRỢ CẢ HAI DASHBOARD ---
    private UserDashboardController userDashboardController;
    private AdminDashboardController adminDashboardController; // THÊM TRƯỜNG NÀY
    // --- KẾT THÚC SỬA ĐỔI ---

    // Setter cho UserDashboardController (giữ nguyên)
    public void setDashboardController(UserDashboardController dashboardController) {
        this.userDashboardController = dashboardController;
        this.adminDashboardController = null; // Đảm bảo chỉ một dashboard được active
    }

    // --- THÊM SETTER CHO ADMIN DASHBOARD CONTROLLER ---
    public void setAdminDashboardController(AdminDashboardController dashboardController) {
        this.adminDashboardController = dashboardController;
        this.userDashboardController = null; // Đảm bảo chỉ một dashboard được active
    }
    // --- KẾT THÚC THÊM SETTER ---

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bookManagementService = BookManagementService.getInstance();
        authorManagementService = AuthorManagementService.getInstance();
        loadDefaultAuthorAvatar();

        // Nên kiểm tra cờ isAdminViewingAuthorDetail từ SessionManager để biết ai đang gọi
        // boolean isAdminView = SessionManager.getInstance().isAdminViewingAuthorDetail();

        Author authorFromSession = SessionManager.getInstance().getSelectedAuthor();

        if (authorFromSession != null && authorFromSession.getId() > 0) {
            Optional<Author> freshAuthorOpt = authorManagementService.findAuthorById(authorFromSession.getId());
            if (freshAuthorOpt.isPresent()) {
                this.currentAuthor = freshAuthorOpt.get();
                Platform.runLater(() -> {
                    populateAuthorDetails();
                    loadBooksByAuthor();
                });
            } else {
                handleMissingAuthorData("Không tìm thấy thông tin cập nhật cho tác giả ID: " + authorFromSession.getId() + ".");
            }
        } else {
            handleMissingAuthorData("Không có thông tin tác giả hợp lệ được chọn.");
        }

        if (authorBooksTilePane != null) {
            authorBooksTilePane.setPadding(new Insets(10));
            authorBooksTilePane.setHgap(10);
            authorBooksTilePane.setVgap(10);
        }
        if (authorBooksScrollPane != null) {
            authorBooksScrollPane.setFitToWidth(true);
        }
    }

    private void loadDefaultAuthorAvatar() {
        try (InputStream defaultStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_author_avatar.png")) {
            if (defaultStream != null) {
                defaultAuthorAvatar = new Image(defaultStream);
            } else {
                System.err.println("ERROR_AUTHOR_DETAIL: Default author avatar image not found.");
            }
        } catch (Exception e) {
            System.err.println("ERROR_AUTHOR_DETAIL: Exception loading default author avatar: " + e.getMessage());
        }
    }

    private void handleMissingAuthorData(String message) {
        System.err.println("ERROR_AUTHOR_DETAIL_DATA: " + message);
        if (authorNameHeaderLabel != null) authorNameHeaderLabel.setText("Lỗi Thông Tin");
        if (authorNameLabel != null) authorNameLabel.setText(message);
        if (authorBiographyArea != null) {
            authorBiographyArea.setText("Không thể hiển thị thông tin chi tiết.");
            authorBiographyArea.setEditable(false);
        }
        if (authorBooksTilePane != null) authorBooksTilePane.getChildren().clear();
        if (noBooksByAuthorLabel != null) {
            noBooksByAuthorLabel.setText(message);
            noBooksByAuthorLabel.setVisible(true);
            noBooksByAuthorLabel.setManaged(true);
        }
        if(authorDobLabel!=null) authorDobLabel.setText("N/A");
        if(authorDodLabel!=null) authorDodLabel.setText("N/A");
        // ...
        if (closeAuthorDetailButton != null) {
            // Không cần setOnAction ở đây nữa nếu FXML đã có onAction
            // closeAuthorDetailButton.setOnAction(e -> navigateBackToPreviousView());
        }
    }

    private void populateAuthorDetails() {
        if (currentAuthor == null) {
            handleMissingAuthorData("Dữ liệu tác giả không có sẵn.");
            return;
        }

        String authorName = currentAuthor.getName() != null ? currentAuthor.getName() : "Chưa rõ";
        authorNameHeaderLabel.setText("Thông tin chi tiết: " + authorName);
        authorNameLabel.setText(authorName);

        authorDobLabel.setText((currentAuthor.getYearOfBirth() != null ? String.valueOf(currentAuthor.getYearOfBirth()) : "Chưa rõ"));
        authorDodLabel.setText((currentAuthor.getYearOfDeath() != null ? String.valueOf(currentAuthor.getYearOfDeath()) : (currentAuthor.getYearOfBirth()!=null ? "Còn sống" : "Chưa rõ")));
        authorGenderLabel.setText((currentAuthor.getGender() != null && !currentAuthor.getGender().isEmpty() ? currentAuthor.getGender() : "Chưa rõ"));
        authorNationalityLabel.setText((currentAuthor.getNationality() != null && !currentAuthor.getNationality().isEmpty() ? currentAuthor.getNationality() : "Chưa rõ"));
        authorPlaceOfBirthLabel.setText((currentAuthor.getPlaceOfBirth() != null && !currentAuthor.getPlaceOfBirth().isEmpty() ? currentAuthor.getPlaceOfBirth() : "Chưa rõ"));
        authorBiographyArea.setText(currentAuthor.getBiography() != null && !currentAuthor.getBiography().isEmpty() ? currentAuthor.getBiography() : "Chưa có thông tin tiểu sử.");
        authorBiographyArea.setEditable(false);
        authorBiographyArea.setWrapText(true);
        loadAvatarImage(currentAuthor.getAvatarUrl());
    }

    private void loadAvatarImage(String imageUrl) {
        if (authorAvatarImageView == null) return;
        Image imageToSet = this.defaultAuthorAvatar;
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            String finalImageUrl = imageUrl.trim();
            if (finalImageUrl.startsWith("//")) finalImageUrl = "https:" + finalImageUrl;
            try {
                Image loadedImage = new Image(finalImageUrl, true);
                if (loadedImage.isError()) {
                    System.err.println("WARN_AUTHOR_DETAIL_AVATAR: Error loading avatar from URL: " + finalImageUrl);
                } else {
                    imageToSet = loadedImage;
                }
            } catch (Exception e) {
                System.err.println("ERROR_AUTHOR_DETAIL_AVATAR: Exception loading avatar: " + finalImageUrl + ". " + e.getMessage());
            }
        }
        authorAvatarImageView.setImage(imageToSet);
    }

    private void loadBooksByAuthor() {
        if (currentAuthor == null || authorBooksTilePane == null || bookManagementService == null || noBooksByAuthorLabel == null) {
            if (noBooksByAuthorLabel != null) {
                noBooksByAuthorLabel.setText("Không thể tải sách của tác giả.");
                noBooksByAuthorLabel.setVisible(true);
                noBooksByAuthorLabel.setManaged(true);
            }
            return;
        }
        authorBooksTilePane.getChildren().clear();

        List<Book> books = bookManagementService.getBooksByAuthorId(currentAuthor.getId());

        if (books == null || books.isEmpty()) {
            noBooksByAuthorLabel.setText((currentAuthor.getName() != null ? currentAuthor.getName() : "Tác giả này") + " chưa có đầu sách nào trong thư viện.");
            noBooksByAuthorLabel.setVisible(true);
            noBooksByAuthorLabel.setManaged(true);
        } else {
            noBooksByAuthorLabel.setVisible(false);
            noBooksByAuthorLabel.setManaged(false);
            for (Book book : books) {
                try {
                    // Cần xác định controller cha phù hợp cho BookCardLibraryController
                    // Nếu admin xem, có thể không cần truyền userDashboardController
                    // Nếu user xem, thì userDashboardController là cần thiết
                    boolean isAdminViewing = SessionManager.getInstance().isAdminViewingAuthorDetail();

                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/quanlythuvien/fxml/user/BookCardLibraryView.fxml"));
                    Node bookCardNode = loader.load();
                    BookCardLibraryController cardController = loader.getController();

                    if (!isAdminViewing && this.userDashboardController != null) {
                        // Nếu là User đang xem và có userDashboardController, thì truyền nó vào
                        // Giả sử BookCardLibraryController có phương thức setDashboardController
                        // cardController.setDashboardController(this.userDashboardController);
                    }
                    // Hoặc BookCardLibraryController tự lấy dashboard từ SessionManager nếu cần
                    cardController.setData(book, null); // Truyền null cho parent controller (LibraryController) nếu không cần
                    authorBooksTilePane.getChildren().add(bookCardNode);
                } catch (IOException e) {
                    System.err.println("ERROR_AUTHOR_DETAIL_LOAD_CARD: Failed to load BookCardLibraryView for book '" + book.getTitle() + "': " + e.getMessage());
                }
            }
        }
    }

    private void navigateBackToPreviousView() {
        // --- SỬA LOGIC ĐIỀU HƯỚNG ---
        if (this.adminDashboardController != null) {
            System.out.println("DEBUG_AUTHOR_DETAIL_CLOSE: Admin navigating back to Author Management.");
            this.adminDashboardController.loadAdminViewIntoCenter("AdminAuthorManagementView.fxml");
            SessionManager.getInstance().setSelectedAuthor(null); // Xóa tác giả đã chọn
            SessionManager.getInstance().setAdminViewingAuthorDetail(false); // Reset cờ
        } else if (this.userDashboardController != null) {
            System.out.println("DEBUG_AUTHOR_DETAIL_CLOSE: User navigating back to Library or previous.");
            // User thường quay lại UserLibraryView hoặc một view cụ thể đã được đặt trước đó
            this.userDashboardController.loadViewIntoCenter("UserLibraryView.fxml");
            SessionManager.getInstance().setSelectedAuthor(null);
        } else {
            System.err.println("WARN_AUTHOR_DETAIL_CLOSE: Both userDashboardController and adminDashboardController are null. Cannot navigate back programmatically.");
            showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Không thể tự động quay lại. Vui lòng sử dụng thanh điều hướng.");
        }
        // --- KẾT THÚC SỬA LOGIC ---
    }

    @FXML
    void handleCloseAuthorDetailAction(ActionEvent event) {
        navigateBackToPreviousView();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyDialogStyles(alert);
        alert.showAndWait();
    }

    private void applyDialogStyles(Dialog<?> dialog) {
        try {
            URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
            if (cssUrl != null) {
                dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) { System.err.println("WARN_DIALOG_CSS: Failed to load CSS for dialog: " + e.getMessage()); }
    }
}
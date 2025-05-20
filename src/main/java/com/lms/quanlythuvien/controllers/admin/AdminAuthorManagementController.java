package com.lms.quanlythuvien.controllers.admin;

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.exceptions.DeletionRestrictedException;
import com.lms.quanlythuvien.models.item.Author;
import com.lms.quanlythuvien.services.library.AuthorManagementService;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.utils.session.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane; // Cho dialog code-built
import javafx.scene.layout.TilePane; // Sử dụng TilePane
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class AdminAuthorManagementController implements Initializable {

    @FXML private TextField authorSearchField;
    @FXML private Button addAuthorButton; // Nút thêm tác giả chung
    @FXML private TilePane authorsTilePane; // Thay TableView bằng TilePane

    private AuthorManagementService authorService;
    private BookManagementService bookManagementService;

    // private ObservableList<Author> observableAuthorList; // Có thể không cần nếu load thẳng vào TilePane
    private Map<Integer, Integer> authorBookCountsMap = new HashMap<>(); // Cache số sách

    private AdminDashboardController dashboardController;

    public void setDashboardController(AdminDashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    public AdminAuthorManagementController() {
        authorService = AuthorManagementService.getInstance();
        bookManagementService = BookManagementService.getInstance();
        // observableAuthorList = FXCollections.observableArrayList();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Thiết lập cho TilePane nếu cần (ví dụ: padding, hgap, vgap đã có thể set trong FXML)
        if (authorsTilePane != null) {
            authorsTilePane.setPadding(new Insets(15));
            authorsTilePane.setHgap(15);
            authorsTilePane.setVgap(15);
        }
        loadAuthorsToDisplay(authorService.getAllAuthors()); // Load tất cả ban đầu

        authorSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            handleSearchAuthorAction(null); // Gọi search khi text thay đổi
        });
    }

    private void updateBookCountsForAuthors(List<Author> authorsToList) {
        authorBookCountsMap.clear();
        if (authorsToList == null || authorsToList.isEmpty()) {
            return;
        }
        Set<Integer> authorIds = authorsToList.stream().map(Author::getId).collect(Collectors.toSet());

        // YÊU CẦU: BookManagementService cần có phương thức này để tối ưu
        // authorBookCountsMap = bookManagementService.getBookCountsForMultipleAuthors(authorIds);
        // Nếu chưa có, dùng cách tạm thời (N+1):
        for (Author author : authorsToList) {
            authorBookCountsMap.put(author.getId(), bookManagementService.getBooksByAuthorId(author.getId()).size());
        }
    }

    private void loadAuthorsToDisplay(List<Author> authors) {
        if (authorsTilePane == null) return;
        authorsTilePane.getChildren().clear();

        if (authors == null || authors.isEmpty()) {
            Label placeholder = new Label("Không có tác giả nào để hiển thị.");
            // placeholder.getStyleClass().add("placeholder-text-tilepane"); // Thêm style nếu cần
            authorsTilePane.getChildren().add(placeholder);
            return;
        }

        updateBookCountsForAuthors(authors); // Cập nhật map số sách

        for (Author author : authors) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/quanlythuvien/fxml/admin/AdminAuthorCardView.fxml"));
                Node authorCardNode = loader.load();
                AdminAuthorCardController cardController = loader.getController();
                // Truyền số sách đã được tính toán vào card
                cardController.setData(author, authorBookCountsMap.getOrDefault(author.getId(), 0), this);
                authorsTilePane.getChildren().add(authorCardNode);
            } catch (IOException e) {
                System.err.println("Error loading AdminAuthorCardView.fxml for author " + author.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleSearchAuthorAction(ActionEvent event) {
        String keyword = authorSearchField.getText().trim();
        List<Author> searchedAuthors = authorService.searchAuthors(keyword);
        loadAuthorsToDisplay(searchedAuthors);
    }

    @FXML
    private void handleClearAuthorSearchAction(ActionEvent event) {
        authorSearchField.clear();
        loadAuthorsToDisplay(authorService.getAllAuthors());
    }

    @FXML
    private void handleAddAuthorAction(ActionEvent event) {
        openAuthorFormDialog(null);
    }

    // --- PHƯƠNG THỨC PUBLIC ĐỂ CARD CONTROLLER HOẶC MODULE KHÁC GỌI ---
    public AdminDashboardController getDashboardController() {
        return this.dashboardController;
    }

    public void openAuthorFormDialog(Author existingAuthor) {
        // Giữ lại logic tạo dialog bằng code Java cho nhất quán với Book dialog
        Optional<Author> authorOpt = showAuthorFormDialogInCode(existingAuthor, (Stage) addAuthorButton.getScene().getWindow());
        authorOpt.ifPresent(author -> {
            boolean success;
            String actionVerb = (existingAuthor == null) ? "thêm" : "cập nhật";
            String successMessage = "Đã " + actionVerb + " tác giả thành công: " + author.getName();
            String errorMessage = "Không thể " + actionVerb + " tác giả. ";

            if (existingAuthor == null) {
                Optional<Author> addedAuthor = authorService.addAuthor(author);
                success = addedAuthor.isPresent();
                if (!success) errorMessage += "Tên tác giả có thể đã tồn tại.";
            } else {
                author.setId(existingAuthor.getId()); // Đảm bảo ID không đổi khi sửa
                success = authorService.updateAuthor(author);
                if (!success) errorMessage += "Tên tác giả mới có thể bị trùng.";
            }

            if (success) {
                refreshAuthorsDisplay();
                showAlert(Alert.AlertType.INFORMATION, "Thành công", successMessage);
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", errorMessage);
            }
        });
    }

    public void refreshAuthorsDisplay() {
        // Tải lại dựa trên từ khóa tìm kiếm hiện tại, hoặc tải lại tất cả nếu không có từ khóa
        handleSearchAuthorAction(null);
    }

    public void navigateToAuthorDetails(Author author) {
        if (dashboardController != null && author != null) {
            SessionManager.getInstance().setSelectedAuthor(author);
            // SessionManager.getInstance().setAdminViewingAuthorDetail(true); // Cần cờ này trong SessionManager
            dashboardController.loadAdminViewIntoCenter("user/AuthorDetailView.fxml");
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi Điều Hướng", "Không thể mở chi tiết tác giả.");
        }
    }
    // --- KẾT THÚC PHƯƠNG THỨC PUBLIC ---

    // Dialog tạo bằng code, tương tự như showBookDialog nhưng cho Author
    private Optional<Author> showAuthorFormDialogInCode(Author existingAuthor, Stage ownerStage) {
        Dialog<Author> dialog = new Dialog<>();
        dialog.setTitle(existingAuthor == null ? "Thêm Tác Giả Mới" : "Chỉnh Sửa Thông Tin Tác Giả");
        dialog.setHeaderText(existingAuthor == null ? "Nhập thông tin tác giả:" : "Cập nhật thông tin cho: " + existingAuthor.getName());
        dialog.initOwner(ownerStage);
        dialog.initModality(Modality.WINDOW_MODAL);
        applyDialogStyles(dialog);

        ButtonType saveButtonType = ButtonType.OK;
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField nameField = new TextField(); nameField.setPromptText("Tên tác giả (*)");
        TextArea bioArea = new TextArea(); bioArea.setPromptText("Tiểu sử"); bioArea.setWrapText(true); bioArea.setPrefRowCount(3);
        TextField yobField = new TextField(); yobField.setPromptText("Năm sinh (YYYY)");
        TextField yodField = new TextField(); yodField.setPromptText("Năm mất (YYYY, để trống nếu còn sống)");
        TextField genderField = new TextField(); genderField.setPromptText("Giới tính (Nam/Nữ/Khác)");
        TextField nationalityField = new TextField(); nationalityField.setPromptText("Quốc tịch");
        TextField placeOfBirthField = new TextField(); placeOfBirthField.setPromptText("Nơi sinh");
        TextField avatarUrlField = new TextField(); avatarUrlField.setPromptText("URL ảnh đại diện");

        grid.add(new Label("Tên (*):"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Tiểu sử:"), 0, 1); grid.add(bioArea, 1, 1);
        grid.add(new Label("Năm sinh:"), 0, 2); grid.add(yobField, 1, 2);
        grid.add(new Label("Năm mất:"), 0, 3); grid.add(yodField, 1, 3);
        grid.add(new Label("Giới tính:"), 0, 4); grid.add(genderField, 1, 4);
        grid.add(new Label("Quốc tịch:"), 0, 5); grid.add(nationalityField, 1, 5);
        grid.add(new Label("Nơi sinh:"), 0, 6); grid.add(placeOfBirthField, 1, 6);
        grid.add(new Label("URL Avatar:"), 0, 7); grid.add(avatarUrlField, 1, 7);

        Node saveButtonNode = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButtonNode.setDisable(true);
        nameField.textProperty().addListener((obs, oldVal, newVal) -> saveButtonNode.setDisable(newVal.trim().isEmpty()));

        if (existingAuthor != null) {
            nameField.setText(existingAuthor.getName());
            bioArea.setText(existingAuthor.getBiography());
            if(existingAuthor.getYearOfBirth() != null) yobField.setText(String.valueOf(existingAuthor.getYearOfBirth()));
            if(existingAuthor.getYearOfDeath() != null) yodField.setText(String.valueOf(existingAuthor.getYearOfDeath()));
            genderField.setText(existingAuthor.getGender());
            nationalityField.setText(existingAuthor.getNationality());
            placeOfBirthField.setText(existingAuthor.getPlaceOfBirth());
            avatarUrlField.setText(existingAuthor.getAvatarUrl());
            saveButtonNode.setDisable(nameField.getText().trim().isEmpty());
        }
        dialog.getDialogPane().setContent(grid);
        Platform.runLater(nameField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Thiếu thông tin", "Tên tác giả không được để trống.");
                    return null;
                }
                Integer yob = null, yod = null;
                try {
                    if (!yobField.getText().trim().isEmpty()) yob = Integer.parseInt(yobField.getText().trim());
                    if (!yodField.getText().trim().isEmpty()) yod = Integer.parseInt(yodField.getText().trim());
                    if (yob != null && yod != null && yod < yob) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi logic", "Năm mất không thể nhỏ hơn năm sinh.");
                        return null;
                    }
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Năm sinh/mất phải là số nguyên.");
                    return null;
                }

                Author authorResult;
                if (existingAuthor != null) {
                    authorResult = existingAuthor;
                    authorResult.setName(name); // Cho phép sửa tên
                } else {
                    // Tạo mới với constructor đầy đủ (hoặc constructor chỉ nhận tên nếu Author model có)
                    authorResult = new Author(name, bioArea.getText().trim(), yob, yod,
                            genderField.getText().trim(), nationalityField.getText().trim(),
                            placeOfBirthField.getText().trim(), avatarUrlField.getText().trim());
                }
                // Cập nhật các trường còn lại
                authorResult.setBiography(bioArea.getText().trim());
                authorResult.setYearOfBirth(yob);
                authorResult.setYearOfDeath(yod);
                authorResult.setGender(genderField.getText().trim());
                authorResult.setNationality(nationalityField.getText().trim());
                authorResult.setPlaceOfBirth(placeOfBirthField.getText().trim());
                authorResult.setAvatarUrl(avatarUrlField.getText().trim());
                return authorResult;
            }
            return null;
        });
        return dialog.showAndWait();
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
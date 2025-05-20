package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.utils.session.SessionManager;
// Import các controller con cần thiết
import com.lms.quanlythuvien.controllers.common.FAQController;
import com.lms.quanlythuvien.controllers.user.UserProfileController;
import com.lms.quanlythuvien.controllers.user.UserLibraryController;
import com.lms.quanlythuvien.controllers.user.MyBookshelfController;
import com.lms.quanlythuvien.controllers.user.BookDetailController;
import com.lms.quanlythuvien.controllers.user.AuthorDetailController;
import com.lms.quanlythuvien.controllers.user.NotificationsController;
import com.lms.quanlythuvien.controllers.user.UserDonationController;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;

import java.io.File; // Thêm cho loadUserAvatarForTopBar
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class UserDashboardController implements Initializable {

    //<editor-fold desc="FXML Injections - Top Bar">
    @FXML private ComboBox<String> searchTypeComboBoxUser;
    @FXML private TextField searchInputFieldUser;
    @FXML private Button searchButtonUser;
    @FXML private ImageView userAvatarTopBar;
    @FXML private Label topBarUsernameLabel;
    @FXML private Label topBarUserExtraInfoLabel;
    @FXML private MenuButton userActionsMenuButton;
    @FXML private MenuItem viewProfileMenuItem;
    @FXML private MenuItem settingsMenuItem;
    @FXML private MenuItem logoutMenuItemUser;
    //</editor-fold>

    //<editor-fold desc="FXML Injections - Left Sidebar">
    @FXML private Button navTrangChuButton;
    @FXML private Button navThongBaoButton;
    @FXML private Button navTuSachCuaToiButton;
    @FXML private Button navThuVienButton;
    @FXML private Button navQuyenGopButton;
    @FXML private Button navHoiDapButton;
    @FXML private Button navLienHeAdminButton;
    //</editor-fold>

    @FXML private StackPane mainContentAreaUser;

    private User currentUser;
    private Button currentActiveSidebarButton = null;
    private List<Button> sidebarButtons;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("DEBUG_UDC: Initializing UserDashboardController...");
        currentUser = SessionManager.getInstance().getCurrentUser();

        if (currentUser == null || currentUser.getRole() != User.Role.USER) {
            System.err.println("ERROR_UDC: No current user or not a USER. Redirecting to login.");
            SessionManager.getInstance().clearSession();
            MainApp.loadScene("common/LoginScreen.fxml");
            return;
        }

        SessionManager.getInstance().registerController("UserDashboardController", this);
        initializeSidebarButtons();
        populateTopBarUserInfo();
        setupSearchComboBox();

        if (searchInputFieldUser != null) {
            searchInputFieldUser.setOnKeyPressed(this::handleSearchFieldEnter);
        }
        if (searchButtonUser != null && searchInputFieldUser != null && searchInputFieldUser.isFocused()) {
            searchButtonUser.setDefaultButton(true);
        }

        handleNavTrangChu(null);
        System.out.println("DEBUG_UDC: UserDashboardController initialized.");
    }

    private void handleSearchFieldEnter(KeyEvent event) {
        if (event.getCode().toString().equals("ENTER")) {
            handleGlobalSearchUserAction(null);
        }
    }

    private void initializeSidebarButtons() {
        sidebarButtons = Arrays.asList(
                navTrangChuButton, navThongBaoButton, navTuSachCuaToiButton,
                navThuVienButton, navQuyenGopButton, navHoiDapButton, navLienHeAdminButton
        );
    }

    private void setupSearchComboBox() {
        if (searchTypeComboBoxUser != null) {
            ObservableList<String> searchTypes = FXCollections.observableArrayList(
                    "Tất cả", "Tiêu đề sách", "Tên tác giả", "ISBN", "Thể loại sách"
            );
            searchTypeComboBoxUser.setItems(searchTypes);
            searchTypeComboBoxUser.setValue("Tất cả");
        }
    }

    public void populateTopBarUserInfo() {
        if (currentUser == null) { // Lấy lại nếu chưa có hoặc đã bị clear
            currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                System.err.println("ERROR_UDC_POPULATE_TOP_BAR: Current user is null even after trying to get from session.");
                return;
            }
        }
        if (topBarUsernameLabel != null) {
            topBarUsernameLabel.setText(currentUser.getFullNameOrDefault(currentUser.getUsernameOrDefault("Người dùng")));
        }
        if (topBarUserExtraInfoLabel != null) {
            topBarUserExtraInfoLabel.setText(currentUser.getEmailOrDefault("Chưa có email"));
        }
        loadUserAvatarForTopBar();
    }

    private void loadUserAvatarForTopBar() {
        if (userAvatarTopBar == null) return;
        Image imageToSet = null;
        String avatarPath = (currentUser != null) ? currentUser.getAvatarUrl() : null;

        if (avatarPath != null && !avatarPath.isEmpty()) {
            try {
                File avatarFile = new File(avatarPath);
                if (avatarFile.exists() && avatarFile.isFile() && avatarFile.canRead()) {
                    imageToSet = new Image(avatarFile.toURI().toString(), true);
                } else {
                    imageToSet = new Image(avatarPath, true);
                }
                if (imageToSet.isError()) {
                    System.err.println("WARN_UDC_AVATAR: Error loading user avatar image from path/URL: " + avatarPath);
                    imageToSet = null;
                }
            } catch (Exception e) {
                System.err.println("ERROR_UDC_AVATAR: Exception loading user avatar: " + avatarPath + ". Error: " + e.getMessage());
                imageToSet = null;
            }
        }
        if (imageToSet == null) {
            try (InputStream defaultStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_avatar.png")) {
                if (defaultStream != null) imageToSet = new Image(defaultStream);
                else System.err.println("ERROR_UDC_AVATAR: Default avatar resource not found.");
            } catch (Exception e) { System.err.println("ERROR_UDC_AVATAR: Failed to load default avatar: " + e.getMessage());}
        }
        userAvatarTopBar.setImage(imageToSet);
    }

    public void loadViewIntoCenter(String fxmlFilename) {
        if (mainContentAreaUser == null) {
            System.err.println("ERROR_UDC_LOADVIEW: mainContentAreaUser is null. Cannot load: " + fxmlFilename);
            return;
        }
        try {
            String basePath = "/com/lms/quanlythuvien/fxml/";
            String fullFxmlPath = fxmlFilename.startsWith("common/") || fxmlFilename.startsWith("user/") || fxmlFilename.startsWith("admin/") ?
                    basePath + fxmlFilename : basePath + "user/" + fxmlFilename;

            URL fxmlUrl = getClass().getResource(fullFxmlPath);
            if (fxmlUrl == null) {
                System.err.println("ERROR_UDC_LOADVIEW: FXML not found: " + fullFxmlPath);
                mainContentAreaUser.getChildren().setAll(new Label("Lỗi: Không tìm thấy giao diện " + fxmlFilename));
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Node view = loader.load();
            mainContentAreaUser.getChildren().setAll(view);
            Object controller = loader.getController();

            // Truyền tham chiếu dashboard cho các controller con
            if (controller instanceof UserLibraryController) ((UserLibraryController) controller).setDashboardController(this);
            else if (controller instanceof MyBookshelfController) ((MyBookshelfController) controller).setDashboardController(this);
            else if (controller instanceof UserProfileController) ((UserProfileController) controller).setDashboardController(this);
            else if (controller instanceof BookDetailController) ((BookDetailController) controller).setDashboardController(this);
            else if (controller instanceof AuthorDetailController) ((AuthorDetailController) controller).setDashboardController(this);
            else if (controller instanceof NotificationsController) ((NotificationsController) controller).setDashboardController(this);
            else if (controller instanceof UserDonationController) ((UserDonationController) controller).setDashboardController(this);
            else if (controller instanceof FAQController) { /* ((FAQController) controller).setDashboardController(this); */ } // FAQ có thể không cần

            System.out.println("DEBUG_UDC_LOADVIEW: Successfully loaded " + fxmlFilename);

        } catch (IOException e) {
            System.err.println("ERROR_UDC_LOADVIEW_IO: IOException loading " + fxmlFilename + ": " + e.getMessage());
            mainContentAreaUser.getChildren().setAll(new Label("Lỗi I/O: " + fxmlFilename));
        } catch (Exception e) {
            System.err.println("CRITICAL_UDC_LOADVIEW: Unexpected error loading " + fxmlFilename + ": " + e.getMessage());
            e.printStackTrace();
            mainContentAreaUser.getChildren().setAll(new Label("Lỗi không mong muốn: " + fxmlFilename));
        }
    }

    private void setActiveSidebarButton(Button activeButton) {
        if (currentActiveSidebarButton != null && currentActiveSidebarButton != activeButton) {
            currentActiveSidebarButton.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("active"), false);
        }
        // Chỉ set active nếu nút đó là một trong các nút sidebar đã định nghĩa
        if (activeButton != null && sidebarButtons.contains(activeButton)) {
            activeButton.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("active"), true);
            currentActiveSidebarButton = activeButton;
        } else { // Nếu không có nút nào được chọn (ví dụ: xem profile từ top bar, hoặc nút không thuộc sidebar)
            if (currentActiveSidebarButton != null) {
                currentActiveSidebarButton.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("active"), false);
            }
            currentActiveSidebarButton = null;
        }
    }

    @FXML
    private void handleGlobalSearchUserAction(ActionEvent event) {
        String query = (searchInputFieldUser.getText() != null) ? searchInputFieldUser.getText().trim() : "";
        String type = searchTypeComboBoxUser.getValue();
        if (type == null || type.isEmpty()) type = "Tất cả";

        if (query.isEmpty() && !("Tất cả".equalsIgnoreCase(type) || "Tên tác giả".equalsIgnoreCase(type) || "Thể loại sách".equalsIgnoreCase(type))) {
            showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Vui lòng nhập từ khóa tìm kiếm.");
            return;
        }
        System.out.println("DEBUG_UDC_SEARCH: Global search. Query: [" + query + "], Type: [" + type + "]");
        SessionManager.getInstance().setGlobalSearchQuery(query);
        SessionManager.getInstance().setGlobalSearchType(type);
        loadViewIntoCenter("UserLibraryView.fxml");
        setActiveSidebarButton(navThuVienButton);
    }

    @FXML private void handleViewProfileAction(ActionEvent event) { loadViewIntoCenter("UserProfileView.fxml"); setActiveSidebarButton(null); }
    @FXML private void handleSettingsAction(ActionEvent event) {
        // Hiện tại UserProfileView đảm nhận cả chức năng "Cài đặt"
        loadViewIntoCenter("UserProfileView.fxml"); setActiveSidebarButton(null);
        System.out.println("INFO_UDC_SETTINGS: Settings clicked, navigating to User Profile.");
    }
    @FXML private void handleLogoutUserAction(ActionEvent event) { SessionManager.getInstance().clearSession(); MainApp.loadScene("common/LoginScreen.fxml"); }

    @FXML private void handleNavTrangChu(ActionEvent event) { loadViewIntoCenter("UserHomeContentView.fxml"); setActiveSidebarButton(navTrangChuButton); }
    @FXML private void handleNavThongBao(ActionEvent event) { loadViewIntoCenter("NotificationsView.fxml"); setActiveSidebarButton(navThongBaoButton); }
    @FXML private void handleNavTuSachCuaToi(ActionEvent event) { loadViewIntoCenter("MyBookshelfView.fxml"); setActiveSidebarButton(navTuSachCuaToiButton); }
    @FXML private void handleNavThuVien(ActionEvent event) {
        SessionManager.getInstance().setGlobalSearchQuery(""); // Đặt query rỗng
        SessionManager.getInstance().setGlobalSearchType("Tất cả"); // Đặt type mặc định
        loadViewIntoCenter("UserLibraryView.fxml"); setActiveSidebarButton(navThuVienButton);
    }
    @FXML private void handleNavQuyenGop(ActionEvent event) { loadViewIntoCenter("UserDonationView.fxml"); setActiveSidebarButton(navQuyenGopButton); }
    @FXML private void handleNavHoiDap(ActionEvent event) { loadViewIntoCenter("common/FAQView.fxml"); setActiveSidebarButton(navHoiDapButton); }
    @FXML private void handleNavLienHeAdmin(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Liên hệ Quản trị viên", "Mọi thắc mắc hoặc hỗ trợ, vui lòng gửi email đến: uet.library.contact@vnu.edu.vn");
    }

    public void updateNotificationBadgeOnSidebar(int count) {
        if (navThongBaoButton != null) {
            String baseText = "🔔  Thông báo";
            navThongBaoButton.getStyleClass().remove("new-notifications-badge"); // Xóa class cũ trước
            if (count > 0) {
                navThongBaoButton.setText(baseText + " (" + count + ")");
                navThongBaoButton.getStyleClass().add("new-notifications-badge"); // Thêm class mới
            } else {
                navThongBaoButton.setText(baseText);
            }
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyDialogStyles(alert.getDialogPane());
        alert.showAndWait();
    }

    private void applyDialogStyles(DialogPane dialogPane) {
        try {
            URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
            if (cssUrl != null) dialogPane.getStylesheets().add(cssUrl.toExternalForm());
        } catch (Exception e) { System.err.println("WARN_DIALOG_CSS: Failed to load CSS: " + e.getMessage()); }
    }
}
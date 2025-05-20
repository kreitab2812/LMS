package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.utils.session.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;

public class UserDashboardController implements Initializable {

    //<editor-fold desc="FXML Injections - Top Bar">
    @FXML private ComboBox<String> searchTypeComboBoxUser;
    @FXML private TextField searchInputFieldUser;
    @FXML private Button searchButtonUser;
    @FXML private ImageView userAvatarTopBar;
    @FXML private Label topBarUsernameLabel;
    @FXML private Label topBarUserExtraInfoLabel; // Đã khớp với FXML ở Turn #49
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

    @FXML private StackPane mainContentAreaUser; // Vùng chứa nội dung chính

    private User currentUser;

    public UserDashboardController() {
        // Constructor
        System.out.println("DEBUG_UDC: UserDashboardController constructor called.");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("DEBUG_UDC: Initializing UserDashboardController...");
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            System.out.println("DEBUG_UDC: Current user retrieved: " + currentUser.getUsername());
            populateTopBarUserInfo();
            // Load nội dung trang chủ (UserHomeContentView) mặc định
            loadViewIntoCenter("UserHomeContentView.fxml");
        } else {
            System.err.println("ERROR_UDC: No current user in session. Redirecting to login screen.");
            MainApp.loadScene("common/LoginScreen.fxml");
            return;
        }
        setupSearchComboBox();
        System.out.println("DEBUG_UDC: UserDashboardController initialized.");
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

    private void populateTopBarUserInfo() {
        if (currentUser == null) return;
        if (topBarUsernameLabel != null) topBarUsernameLabel.setText(currentUser.getUsername());
        if (topBarUserExtraInfoLabel != null) {
            topBarUserExtraInfoLabel.setText("Vai trò: " + currentUser.getRole().toString());
        }
        loadDefaultAvatarForTopBar();
    }

    private void loadDefaultAvatarForTopBar() {
        if (userAvatarTopBar == null) return;
        try (InputStream defaultStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_avatar.png")) {
            if (defaultStream != null) {
                userAvatarTopBar.setImage(new Image(defaultStream));
            } else { System.err.println("ERROR_UDC_AVATAR: Default avatar for top bar not found.");}
        } catch (Exception e) { System.err.println("ERROR_UDC_AVATAR: Failed to load default avatar for top bar: " + e.getMessage());}
    }

    private void loadViewIntoCenter(String fxmlFilename) {
        if (mainContentAreaUser == null) {
            System.err.println("ERROR_UDC_LOADVIEW: mainContentAreaUser (StackPane) is null. Cannot load view: " + fxmlFilename);
            return;
        }
        try {
            String basePath = "/com/lms/quanlythuvien/fxml/";
            String fullFxmlPath;

            if (fxmlFilename.startsWith("admin/") || fxmlFilename.startsWith("common/") || fxmlFilename.startsWith("user/")) {
                fullFxmlPath = basePath + fxmlFilename;
            } else {
                fullFxmlPath = basePath + "user/" + fxmlFilename; // Mặc định vào package 'user'
            }

            System.out.println("DEBUG_UDC_LOADVIEW: Attempting to load FXML: " + fullFxmlPath);

            URL fxmlUrl = getClass().getResource(fullFxmlPath);
            if (fxmlUrl == null) {
                System.err.println("ERROR_UDC_LOADVIEW: Cannot find FXML file: " + fullFxmlPath + " (tried from " + getClass().getName() + ")");
                showAlert(Alert.AlertType.ERROR, "Lỗi Tải Giao Diện", "Không tìm thấy file giao diện: " + fxmlFilename + "\nKiểm tra đường dẫn: " + fullFxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Node view = loader.load();
            mainContentAreaUser.getChildren().setAll(view);

            Object controller = loader.getController();
            if (view != null) view.setUserData(controller); // Lưu controller để có thể truy cập nếu cần

            // Gọi onViewActivated nếu controller của view mới load có phương thức đó
            if (controller instanceof UserLibraryController) {
                System.out.println("DEBUG_UDC_LOADVIEW: Loaded UserLibraryController, calling onViewActivated...");
                ((UserLibraryController) controller).onViewActivated();
            } else if (controller instanceof UserHomeContentController) {
                System.out.println("DEBUG_UDC_LOADVIEW: Loaded UserHomeContentController.");
                // ((UserHomeContentController) controller).onViewActivated(); // Nếu UserHomeContentController cũng có hàm này
            } else if (controller instanceof NotificationsController){
                System.out.println("DEBUG_UDC_LOADVIEW: Loaded NotificationsController.");
                // ((NotificationsController) controller).onViewActivated(); // Nếu có
            } // Thêm cho các controller khác nếu chúng cần xử lý khi được active

            System.out.println("DEBUG_UDC_LOADVIEW: Successfully loaded " + fxmlFilename + " into mainContentAreaUser.");

        } catch (IOException e) {
            System.err.println("ERROR_UDC_LOADVIEW: IOException while loading FXML " + fxmlFilename + ": " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi Tải Giao Diện", "Có lỗi I/O xảy ra khi tải giao diện: " + fxmlFilename);
        } catch (Exception e) { // Bắt các lỗi không mong muốn khác
            System.err.println("CRITICAL_UDC_LOADVIEW: Unexpected error loading FXML " + fxmlFilename + ": " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi Nghiêm Trọng", "Có lỗi không mong muốn xảy ra khi tải giao diện.");
        }
    }

    //<editor-fold desc="Action Handlers - Top Bar">
    @FXML
    private void handleGlobalSearchUserAction(ActionEvent event) {
        String query = searchInputFieldUser.getText() != null ? searchInputFieldUser.getText().trim() : "";
        String type = searchTypeComboBoxUser.getValue();
        if (type == null) type = "Tất cả";

        if (query.isEmpty() && !("Tất cả".equalsIgnoreCase(type) || "Tên tác giả".equalsIgnoreCase(type) || "Thể loại sách".equalsIgnoreCase(type))) {
            showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Vui lòng nhập từ khóa tìm kiếm cho loại đã chọn.");
            return;
        }

        System.out.println("DEBUG_UDC_SEARCH: Global search. Query: [" + query + "], Type: [" + type + "]");

        SessionManager.getInstance().setGlobalSearchQuery(query);
        SessionManager.getInstance().setGlobalSearchType(type);

        loadViewIntoCenter("UserLibraryView.fxml");
    }

    @FXML
    private void handleViewProfileAction(ActionEvent event) {
        System.out.println("DEBUG_UDC_NAV: Navigating to UserProfileView.fxml");
        loadViewIntoCenter("UserProfileView.fxml");
    }

    @FXML
    private void handleSettingsAction(ActionEvent event) {
        System.out.println("DEBUG_UDC_NAV: Navigating to UserSettingsView.fxml");
        loadViewIntoCenter("UserSettingsView.fxml");
    }

    @FXML
    private void handleLogoutUserAction(ActionEvent event) {
        System.out.println("DEBUG_UDC_LOGOUT: Logging out user.");
        SessionManager.getInstance().clearSession();
        MainApp.loadScene("common/LoginScreen.fxml");
    }
    //</editor-fold>

    //<editor-fold desc="Action Handlers - Left Sidebar">
    @FXML private void handleNavTrangChu(ActionEvent event) {
        System.out.println("DEBUG_UDC_NAV: Navigating to UserHomeContentView.fxml");
        loadViewIntoCenter("UserHomeContentView.fxml");
    }
    @FXML private void handleNavThongBao(ActionEvent event) {
        System.out.println("DEBUG_UDC_NAV: Navigating to NotificationsView.fxml");
        loadViewIntoCenter("NotificationsView.fxml");
    }
    @FXML private void handleNavTuSachCuaToi(ActionEvent event) {
        System.out.println("DEBUG_UDC_NAV: Navigating to MyBookshelfView.fxml");
        loadViewIntoCenter("MyBookshelfView.fxml");
    }
    @FXML private void handleNavThuVien(ActionEvent event) {
        System.out.println("DEBUG_UDC_NAV: Navigating to UserLibraryView.fxml (clearing previous search)");
        SessionManager.getInstance().setGlobalSearchQuery(null);
        SessionManager.getInstance().setGlobalSearchType(null);
        loadViewIntoCenter("UserLibraryView.fxml");
    }
    @FXML private void handleNavQuyenGop(ActionEvent event) {
        System.out.println("DEBUG_UDC_NAV: Navigating to UserDonationView.fxml");
        loadViewIntoCenter("UserDonationView.fxml");
    }
    @FXML private void handleNavHoiDap(ActionEvent event) {
        System.out.println("DEBUG_UDC_NAV: Navigating to FAQView.fxml");
        loadViewIntoCenter("common/FAQView.fxml"); // Load từ package common
    }
    @FXML private void handleNavLienHeAdmin(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Liên hệ Admin", "Vui lòng liên hệ quản trị viên qua email: uet.library.contact@vnu.edu.vn");
    }
    //</editor-fold>

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        // ... (giữ nguyên)
        Alert alert = new Alert(alertType);
        // ...
        alert.showAndWait();
    }
}
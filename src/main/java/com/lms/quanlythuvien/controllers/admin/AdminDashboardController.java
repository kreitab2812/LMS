package com.lms.quanlythuvien.controllers.admin;

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.utils.session.SessionManager;
// Import các controller con
import com.lms.quanlythuvien.controllers.user.BookDetailController; // Vẫn giữ nếu admin có thể xem BookDetailView
import com.lms.quanlythuvien.controllers.user.AuthorDetailController; // Controller cho chi tiết tác giả

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    //<editor-fold desc="FXML Injections - Top Bar">
    @FXML private ComboBox<String> adminGlobalSearchTypeComboBox;
    @FXML private TextField adminGlobalSearchField;
    @FXML private Button adminGlobalSearchButton;
    @FXML private ImageView adminAvatarTopBar;
    @FXML private Label adminUsernameLabelTopBar;
    @FXML private Label adminRoleLabelTopBar;
    @FXML private MenuButton adminActionsMenuButton;
    @FXML private MenuItem adminViewProfileMenuItem;
    @FXML private MenuItem adminSettingsMenuItem;
    @FXML private MenuItem adminLogoutMenuItem;
    //</editor-fold>

    //<editor-fold desc="FXML Injections - Left Sidebar">
    @FXML private Button navAdminHomeButton;
    @FXML private Button navAdminNotificationsButton;
    @FXML private Button navAdminBookManagementButton;
    @FXML private Button navAdminAuthorManagementButton;
    @FXML private Button navAdminUserManagementButton;
    @FXML private Button navAdminLoanManagementButton;
    @FXML private Button navAdminDonationManagementButton;
    @FXML private Button navAdminFAQManagementButton;
    //</editor-fold>

    @FXML private StackPane adminMainContentArea;

    private User currentAdmin;
    private Button currentActiveSidebarButton = null;
    private List<Button> sidebarButtons;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("DEBUG_ADC_INIT: AdminDashboardController initialize() started.");
        currentAdmin = SessionManager.getInstance().getCurrentUser();

        if (currentAdmin != null && currentAdmin.getRole() == User.Role.ADMIN) {
            SessionManager.getInstance().registerController("AdminDashboardController", this);
            initializeSidebarButtons();
            populateTopBarAdminInfo();
            setupAdminGlobalSearchComboBox();
            if (adminGlobalSearchField != null) {
                adminGlobalSearchField.setOnKeyPressed(event -> {
                    if (event.getCode().toString().equals("ENTER") && adminGlobalSearchButton != null && !adminGlobalSearchButton.isDisable()) {
                        handleAdminGlobalSearch(new ActionEvent(adminGlobalSearchButton, null));
                    }
                });
            }
            handleAdminNavHome(null); // Load trang chủ admin mặc định
        } else {
            System.err.println("ERROR_ADC_INIT: No admin user or not ADMIN role. Redirecting to login.");
            SessionManager.getInstance().clearSession();
            MainApp.loadScene("common/LoginScreen.fxml");
        }
        System.out.println("DEBUG_ADC_INIT: AdminDashboardController initialize() finished.");
    }

    private void initializeSidebarButtons() {
        sidebarButtons = Arrays.asList(
                navAdminHomeButton, navAdminNotificationsButton, navAdminBookManagementButton,
                navAdminAuthorManagementButton, navAdminUserManagementButton,
                navAdminLoanManagementButton, navAdminDonationManagementButton, navAdminFAQManagementButton
        );
    }

    private void setupAdminGlobalSearchComboBox() {
        if (adminGlobalSearchTypeComboBox != null) {
            ObservableList<String> searchTypes = FXCollections.observableArrayList(
                    "Tất cả (Sách)", "Sách (Tiêu đề/ISBN)", "Tác giả", "Người dùng (Username/Email)"
            );
            adminGlobalSearchTypeComboBox.setItems(searchTypes);
            adminGlobalSearchTypeComboBox.setValue("Tất cả (Sách)");
        }
    }

    private void populateTopBarAdminInfo() {
        if (currentAdmin == null) return;
        if (adminUsernameLabelTopBar != null) {
            adminUsernameLabelTopBar.setText(currentAdmin.getFullNameOrDefault(currentAdmin.getUsernameOrDefault("Admin")));
        }
        if (adminRoleLabelTopBar != null) {
            adminRoleLabelTopBar.setText("Quyền: " + currentAdmin.getRole().toString());
        }
        loadAdminAvatarForTopBar();
    }

    public void refreshTopBarAdminInfo() {
        System.out.println("DEBUG_ADC: Refreshing top bar admin info...");
        this.currentAdmin = SessionManager.getInstance().getCurrentUser();
        if (this.currentAdmin != null && this.currentAdmin.getRole() == User.Role.ADMIN) {
            populateTopBarAdminInfo();
        } else {
            System.err.println("ERROR_ADC_REFRESH_TOP_BAR: Current admin is null or not an admin after session refresh.");
        }
    }

    private void loadAdminAvatarForTopBar() {
        if (adminAvatarTopBar == null) return;
        Image imageToSet = null; String avatarPath = (currentAdmin != null) ? currentAdmin.getAvatarUrl() : null;
        if (avatarPath != null && !avatarPath.isEmpty()) {
            try {
                File avatarFile = new File(avatarPath);
                if (avatarFile.exists() && avatarFile.isFile() && avatarFile.canRead()) {
                    imageToSet = new Image(avatarFile.toURI().toString(), true);
                } else {
                    try { // Thử load như một URL nếu không phải file
                        new URL(avatarPath).toURI();
                        imageToSet = new Image(avatarPath, true);
                    } catch (Exception urlEx) {
                        System.err.println("WARN_ADC_AVATAR: Avatar path is not a valid file or URL: " + avatarPath);
                    }
                }
                if (imageToSet != null && imageToSet.isError()) {
                    System.err.println("ERROR_ADC_AVATAR: Error loading image from path: " + avatarPath + " - " + imageToSet.getException().getMessage());
                    imageToSet = null;
                }
            } catch (Exception e) {
                System.err.println("ERROR_ADC_AVATAR: Exception processing avatar path: " + avatarPath + " - " + e.getMessage());
                imageToSet = null;
            }
        }
        if (imageToSet == null) {
            try (InputStream defaultStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_avatar.png")) {
                if (defaultStream != null) {
                    imageToSet = new Image(defaultStream);
                } else {
                    System.err.println("ERROR_ADC_AVATAR: Default admin avatar resource not found.");
                }
            } catch (Exception e) { System.err.println("ERROR_ADC_AVATAR: Failed to load default admin avatar: " + e.getMessage());}
        }
        adminAvatarTopBar.setImage(imageToSet);
    }

    public void loadAdminViewIntoCenter(String fxmlFilename) {
        if (adminMainContentArea == null) {
            System.err.println("ERROR_ADC_LOADVIEW: adminMainContentArea is null.");
            return;
        }
        try {
            String basePath = "/com/lms/quanlythuvien/fxml/";
            // Kiểm tra xem fxmlFilename đã bao gồm prefix package (common/, user/, admin/) chưa
            String fullFxmlPath;
            if (fxmlFilename.startsWith("common/") || fxmlFilename.startsWith("user/") || fxmlFilename.startsWith("admin/")) {
                fullFxmlPath = basePath + fxmlFilename;
            } else {
                // Nếu không, mặc định là view của admin
                fullFxmlPath = basePath + "admin/" + fxmlFilename;
            }

            URL fxmlUrl = getClass().getResource(fullFxmlPath);
            if (fxmlUrl == null) {
                System.err.println("ERROR_ADC_LOADVIEW: FXML not found: " + fullFxmlPath);
                adminMainContentArea.getChildren().setAll(new Label("Lỗi: Không tìm thấy " + fxmlFilename));
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Node view = loader.load();
            adminMainContentArea.getChildren().setAll(view);
            Object controller = loader.getController();

            // Truyền dashboard controller cho các view con của admin
            if (controller instanceof AdminHomeContentController) {
                // AdminHomeContentController không cần tham chiếu ngược lại dashboard nếu nó không thực hiện điều hướng
            } else if (controller instanceof AdminBookManagementController) {
                ((AdminBookManagementController) controller).setDashboardController(this);
            } else if (controller instanceof AdminAuthorManagementController) {
                ((AdminAuthorManagementController) controller).setDashboardController(this);
            } else if (controller instanceof AdminUserManagementController) {
                ((AdminUserManagementController) controller).setDashboardController(this);
            } else if (controller instanceof AdminLoanManagementController) {
                ((AdminLoanManagementController) controller).setDashboardController(this);
            } else if (controller instanceof AdminProfileController) {
                ((AdminProfileController) controller).setDashboardController(this);
            } else if (controller instanceof AdminDonationManagementController) {
                ((AdminDonationManagementController) controller).setDashboardController(this);
            } else if (controller instanceof AdminFAQManagementController) {
                ((AdminFAQManagementController) controller).setDashboardController(this);
            }
            // --- THÊM XỬ LÝ CHO AUTHOR DETAIL CONTROLLER ---
            else if (controller instanceof AuthorDetailController) {
                // Khi Admin mở AuthorDetailView (là view của user nhưng admin có thể xem)
                ((AuthorDetailController) controller).setAdminDashboardController(this);
                // Đặt cờ trong SessionManager để AuthorDetailController biết admin đang xem
                SessionManager.getInstance().setAdminViewingAuthorDetail(true);
            }
            // --- KẾT THÚC THÊM ---
            // (Tương tự, nếu admin có thể xem BookDetailView của user)
            else if (controller instanceof BookDetailController) {
                // ((BookDetailController) controller).setAdminDashboardController(this); // Nếu BookDetailController cũng cần logic tương tự
                SessionManager.getInstance().setAdminViewingBookDetail(true);
            }


            System.out.println("DEBUG_ADC_LOADVIEW: Successfully loaded " + fullFxmlPath + " into adminMainContentArea.");
        } catch (IOException e) {
            System.err.println("ERROR_ADC_LOADVIEW_IO: Failed to load FXML: " + fxmlFilename + ". Message: " + e.getMessage());
            e.printStackTrace();
            adminMainContentArea.getChildren().setAll(new Label("Lỗi I/O khi tải: " + fxmlFilename));
        } catch (Exception e) {
            System.err.println("CRITICAL_ADC_LOADVIEW: Unexpected error loading FXML: " + fxmlFilename + ". Message: " + e.getMessage());
            e.printStackTrace();
            adminMainContentArea.getChildren().setAll(new Label("Lỗi không mong muốn khi tải: " + fxmlFilename));
        }
    }

    private void setActiveSidebarButton(Button activeButton) {
        // Xóa trạng thái active của nút cũ (nếu có và khác nút mới)
        if (currentActiveSidebarButton != null && currentActiveSidebarButton != activeButton) {
            currentActiveSidebarButton.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("active"), false);
        }
        // Đặt trạng thái active cho nút mới (nếu có và thuộc danh sách sidebar)
        if (activeButton != null && sidebarButtons.contains(activeButton)) {
            activeButton.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("active"), true);
            currentActiveSidebarButton = activeButton;
        } else {
            // Nếu không có nút nào được active (ví dụ: xem profile không thuộc sidebar)
            // hoặc nút không thuộc sidebar, thì xóa active của nút hiện tại (nếu có)
            if (currentActiveSidebarButton != null && activeButton == null) { // Chỉ xóa nếu activeButton là null
                currentActiveSidebarButton.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("active"), false);
            }
            currentActiveSidebarButton = null; // Đặt lại currentActiveSidebarButton
        }
    }

    @FXML
    private void handleAdminGlobalSearch(ActionEvent event) {
        String query = (adminGlobalSearchField != null) ? adminGlobalSearchField.getText().trim() : "";
        String type = (adminGlobalSearchTypeComboBox != null && adminGlobalSearchTypeComboBox.getValue() != null) ?
                adminGlobalSearchTypeComboBox.getValue() : "Tất cả (Sách)"; // Mặc định tìm sách
        if (query.isEmpty() && !"Tất cả (Sách)".equals(type)) { // "Tất cả (Sách)" có thể không cần query
            showAlert(Alert.AlertType.INFORMATION, "Thiếu thông tin", "Vui lòng nhập từ khóa tìm kiếm.");
            return;
        }
        System.out.println("ADMIN GLOBAL SEARCH: Query='" + query + "', Type='" + type + "'");
        SessionManager.getInstance().setGlobalSearchQuery(query);
        SessionManager.getInstance().setGlobalSearchType(type);

        // Điều hướng đến view tương ứng và đặt active button
        switch (type) {
            case "Sách (Tiêu đề/ISBN)":
            case "Tất cả (Sách)": // "Tất cả (Sách)" cũng mở BookManagementView, có thể filter hoặc không
                loadAdminViewIntoCenter("BookManagementView.fxml");
                setActiveSidebarButton(navAdminBookManagementButton);
                break;
            case "Tác giả":
                loadAdminViewIntoCenter("AdminAuthorManagementView.fxml");
                setActiveSidebarButton(navAdminAuthorManagementButton);
                break;
            case "Người dùng (Username/Email)":
                loadAdminViewIntoCenter("UserManagementView.fxml");
                setActiveSidebarButton(navAdminUserManagementButton);
                break;
            default: // Mặc định trở về quản lý sách
                loadAdminViewIntoCenter("BookManagementView.fxml");
                setActiveSidebarButton(navAdminBookManagementButton);
                break;
        }
    }

    @FXML private void handleAdminNavProfile(ActionEvent event) {
        loadAdminViewIntoCenter("AdminProfileView.fxml");
        setActiveSidebarButton(null); // Profile không phải là mục trên sidebar chính
    }
    @FXML private void handleAdminNavSettings(ActionEvent event) {
        // loadAdminViewIntoCenter("AdminSettingsView.fxml"); // Khi có view cài đặt
        setActiveSidebarButton(null);
        showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Chức năng Cài đặt Hệ thống sẽ được phát triển sau.");
    }
    @FXML private void handleAdminLogoutAction(ActionEvent event) {
        SessionManager.getInstance().clearSession();
        MainApp.loadScene("common/LoginScreen.fxml");
    }

    @FXML private void handleAdminNavHome(ActionEvent event) {
        loadAdminViewIntoCenter("AdminHomeContentView.fxml");
        setActiveSidebarButton(navAdminHomeButton);
    }
    @FXML private void handleAdminNavNotifications(ActionEvent event) {
        loadAdminViewIntoCenter("AdminNotificationsView.fxml");
        setActiveSidebarButton(navAdminNotificationsButton);
    }
    @FXML private void handleAdminNavBookManagement(ActionEvent event) {
        SessionManager.getInstance().setGlobalSearchQuery(null); // Xóa query cũ khi vào trực tiếp
        SessionManager.getInstance().setGlobalSearchType(null);
        loadAdminViewIntoCenter("BookManagementView.fxml");
        setActiveSidebarButton(navAdminBookManagementButton);
    }
    @FXML private void handleAdminNavAuthorManagement(ActionEvent event) {
        SessionManager.getInstance().setGlobalSearchQuery(null);
        SessionManager.getInstance().setGlobalSearchType(null);
        loadAdminViewIntoCenter("AdminAuthorManagementView.fxml");
        setActiveSidebarButton(navAdminAuthorManagementButton);
    }
    @FXML private void handleAdminNavUserManagement(ActionEvent event) {
        SessionManager.getInstance().setGlobalSearchQuery(null);
        SessionManager.getInstance().setGlobalSearchType(null);
        loadAdminViewIntoCenter("UserManagementView.fxml");
        setActiveSidebarButton(navAdminUserManagementButton);
    }
    @FXML private void handleAdminNavLoanManagement(ActionEvent event) {
        loadAdminViewIntoCenter("LoanManagementView.fxml");
        setActiveSidebarButton(navAdminLoanManagementButton);
    }
    @FXML private void handleAdminNavDonationManagement(ActionEvent event) {
        loadAdminViewIntoCenter("AdminDonationManagementView.fxml");
        setActiveSidebarButton(navAdminDonationManagementButton);
    }
    @FXML private void handleAdminNavFAQManagement(ActionEvent event) {
        loadAdminViewIntoCenter("AdminFAQManagementView.fxml");
        setActiveSidebarButton(navAdminFAQManagementButton);
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
            if (cssUrl != null) {
                dialogPane.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) { System.err.println("WARN_DIALOG_CSS: Failed to load CSS for AdminDashboard dialogs: " + e.getMessage()); }
    }
}
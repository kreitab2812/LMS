package com.lms.quanlythuvien.controllers.admin;

import com.lms.quanlythuvien.MainApp; // Để lấy CSS cho dialog
import com.lms.quanlythuvien.exceptions.DeletionRestrictedException;
import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.transaction.BorrowingRecord;
import com.lms.quanlythuvien.models.transaction.LoanStatus;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.services.transaction.BorrowingRecordService;
import com.lms.quanlythuvien.services.user.UserService;
import com.lms.quanlythuvien.utils.security.PasswordUtils;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap; // Cho map cache
import java.util.List;
import java.util.Map;   // Cho map cache
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;   // Cho map cache
import java.util.stream.Collectors; // Cho map cache

public class AdminUserManagementController implements Initializable {

    @FXML private TextField userFilterField;
    @FXML private Button addUserButton;
    @FXML private Button editUserButton;
    @FXML private Button deleteUserButton;
    @FXML private TableView<User> usersTableView;
    @FXML private TableColumn<User, String> userIdColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, User.Role> roleColumn;
    @FXML private TableColumn<User, Integer> activeLoansCountColumn;
    @FXML private TableColumn<User, Integer> reputationScoreColumn; // Thêm cột điểm uy tín
    @FXML private TableColumn<User, String> accountStatusColumn; // Thêm cột trạng thái tài khoản

    @FXML private VBox userDetailsPane;
    @FXML private Label selectedUserDetailsTitleLabel;
    @FXML private TableView<BorrowingRecord> userLoanHistoryTableView;
    @FXML private TableColumn<BorrowingRecord, Integer> historyRecordIdColumn;
    @FXML private TableColumn<BorrowingRecord, String> historyBookTitleColumn;
    @FXML private TableColumn<BorrowingRecord, LocalDate> historyBorrowDateColumn;
    @FXML private TableColumn<BorrowingRecord, LocalDate> historyDueDateColumn;
    @FXML private TableColumn<BorrowingRecord, LocalDate> historyReturnDateColumn;
    @FXML private TableColumn<BorrowingRecord, LoanStatus> historyStatusColumn;

    private UserService userService;
    private BorrowingRecordService borrowingRecordService;
    private BookManagementService bookManagementService;

    private ObservableList<User> observableUserList;
    private FilteredList<User> filteredUserList;
    private ObservableList<BorrowingRecord> observableLoanHistoryList;

    private User selectedUserForDetails;
    private Map<String, Integer> userActiveLoanCountsMap = new HashMap<>(); // Cache số lượt mượn active
    private Map<Integer, String> bookTitlesMap = new HashMap<>(); // Cache tên sách cho lịch sử

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = UserService.getInstance();
        bookManagementService = BookManagementService.getInstance();
        borrowingRecordService = BorrowingRecordService.getInstance();

        observableUserList = FXCollections.observableArrayList();
        observableLoanHistoryList = FXCollections.observableArrayList();

        setupUsersTableColumns();
        setupUserLoanHistoryTableColumns();

        usersTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            this.selectedUserForDetails = newSelection;
            boolean userSelected = (newSelection != null);
            editUserButton.setDisable(!userSelected);
            deleteUserButton.setDisable(!userSelected);

            if (userSelected) {
                selectedUserDetailsTitleLabel.setText("Lịch sử mượn của: " + newSelection.getUsername());
                loadUserLoanHistory(newSelection);
                userDetailsPane.setVisible(true);
                userDetailsPane.setManaged(true);
            } else {
                selectedUserDetailsTitleLabel.setText("Chi tiết và Lịch sử mượn");
                observableLoanHistoryList.clear();
                userDetailsPane.setVisible(false);
                userDetailsPane.setManaged(false);
            }
        });

        userFilterField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterUsersTable(newValue);
        });

        loadUsersAndLoanCounts(); // Load cả số lượt mượn
        userDetailsPane.setVisible(false);
        userDetailsPane.setManaged(false);
        editUserButton.setDisable(true);
        deleteUserButton.setDisable(true);
    }

    private void setupUsersTableColumns() {
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        reputationScoreColumn.setCellValueFactory(new PropertyValueFactory<>("reputationScore"));
        accountStatusColumn.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            return new SimpleStringProperty(user.isAccountLocked() ? "Đã khóa" : "Hoạt động");
        });


        activeLoansCountColumn.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            return new SimpleIntegerProperty(userActiveLoanCountsMap.getOrDefault(user.getUserId(), 0)).asObject();
        });
    }

    private void setupUserLoanHistoryTableColumns() {
        historyRecordIdColumn.setCellValueFactory(new PropertyValueFactory<>("recordId"));
        historyBorrowDateColumn.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        historyDueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        historyReturnDateColumn.setCellValueFactory(new PropertyValueFactory<>("returnDate"));
        historyStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        historyBookTitleColumn.setCellValueFactory(cellData -> {
            BorrowingRecord record = cellData.getValue();
            return new SimpleStringProperty(bookTitlesMap.getOrDefault(record.getBookInternalId(), "Sách ID: " + record.getBookInternalId()));
        });
    }

    private void loadUsersAndLoanCounts() {
        List<User> allUsers = userService.getAllUsers();
        if (allUsers != null && !allUsers.isEmpty()) {
            Set<String> userIds = allUsers.stream().map(User::getUserId).collect(Collectors.toSet());
            // TODO: BorrowingRecordService cần có hàm getActiveLoanCountsForMultipleUsers(Set<String> userIds)
            // Tạm thời vẫn N+1 ở đây cho việc đếm:
            userActiveLoanCountsMap.clear();
            for (User user : allUsers) {
                userActiveLoanCountsMap.put(user.getUserId(), borrowingRecordService.getLoansByUserId(user.getUserId(), true).size());
            }
        } else {
            userActiveLoanCountsMap.clear();
        }
        observableUserList.setAll(allUsers != null ? allUsers : new ArrayList<>());
        if (filteredUserList == null) {
            filteredUserList = new FilteredList<>(observableUserList, p -> true);
            usersTableView.setItems(filteredUserList);
        }
        usersTableView.setPlaceholder(new Label(allUsers.isEmpty() ? "Chưa có người dùng nào." : "Không tìm thấy người dùng khớp."));
        usersTableView.refresh();
    }

    private void loadUserLoanHistory(User user) {
        if (user == null) {
            observableLoanHistoryList.clear();
            return;
        }
        List<BorrowingRecord> userHistory = borrowingRecordService.getLoansByUserId(user.getUserId(), false);
        userHistory.sort(Comparator.comparing(BorrowingRecord::getBorrowDate, Comparator.nullsLast(Comparator.reverseOrder())));

        // Tối ưu N+1 cho tên sách trong lịch sử
        bookTitlesMap.clear();
        if (!userHistory.isEmpty()) {
            Set<Integer> bookInternalIdsInHistory = userHistory.stream()
                    .map(BorrowingRecord::getBookInternalId)
                    .collect(Collectors.toSet());
            // Sử dụng phương thức getBooksByInternalIds đã tạo trong BookManagementService
            List<Book> booksInHistory = bookManagementService.getBooksByInternalIds(bookInternalIdsInHistory);
            for (Book book : booksInHistory) {
                bookTitlesMap.put(book.getInternalId(), book.getTitle());
            }
        }
        observableLoanHistoryList.setAll(userHistory);
        userLoanHistoryTableView.setItems(observableLoanHistoryList);
        userLoanHistoryTableView.refresh();
        userLoanHistoryTableView.setPlaceholder(new Label(userHistory.isEmpty() ? "Người dùng này chưa có lịch sử mượn sách." : ""));

    }

    private void filterUsersTable(String searchText) {
        if (filteredUserList == null) return;
        final String lowerCaseFilter = (searchText == null) ? "" : searchText.toLowerCase().trim();
        filteredUserList.setPredicate(user -> {
            if (lowerCaseFilter.isEmpty()) return true;
            if (user.getUsername().toLowerCase().contains(lowerCaseFilter)) return true;
            if (user.getEmail().toLowerCase().contains(lowerCaseFilter)) return true;
            if (user.getUserId().toLowerCase().contains(lowerCaseFilter)) return true;
            if (user.getFullName() != null && user.getFullName().toLowerCase().contains(lowerCaseFilter)) return true;
            return false;
        });
    }

    @FXML
    void handleAddUserAction(ActionEvent event) {
        showUserFormDialog(null);
    }

    @FXML
    void handleEditUserAction(ActionEvent event) {
        User selectedUser = usersTableView.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn một người dùng để sửa.");
            return;
        }
        showUserFormDialog(selectedUser);
    }

    @FXML
    void handleDeleteUserAction(ActionEvent event) {
        User selectedUser = usersTableView.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn một người dùng để xóa.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Xác nhận Xóa");
        confirmation.setHeaderText("Xóa người dùng: " + selectedUser.getUsername() + "?");
        confirmation.setContentText("Bạn có chắc chắn muốn xóa người dùng này?");
        applyDialogStyles(confirmation);
        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (userService.deleteUser(selectedUser.getUserId())) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã xóa người dùng: " + selectedUser.getUsername());
                    loadUsersAndLoanCounts(); // Tải lại cả số sách
                    if (userDetailsPane.isVisible() && this.selectedUserForDetails != null && this.selectedUserForDetails.getUserId().equals(selectedUser.getUserId())) {
                        userDetailsPane.setVisible(false); userDetailsPane.setManaged(false);
                        selectedUserDetailsTitleLabel.setText("Chi tiết và Lịch sử mượn");
                        observableLoanHistoryList.clear();
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xóa người dùng '" + selectedUser.getUsername() + "'.");
                }
            } catch (DeletionRestrictedException e) {
                showAlert(Alert.AlertType.ERROR, "Không thể xóa", e.getMessage());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi Hệ thống", "Lỗi khi xóa người dùng: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void showUserFormDialog(User existingUser) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(existingUser == null ? "Thêm Người Dùng Mới" : "Chỉnh Sửa Thông Tin Người Dùng");
        dialog.setHeaderText(existingUser == null ? "Nhập thông tin người dùng:" : "Chỉnh sửa thông tin cho: " + existingUser.getUsername());
        applyDialogStyles(dialog);

        ButtonType saveButtonType = ButtonType.OK;
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField usernameField = new TextField(); usernameField.setPromptText("Tên đăng nhập (*)");
        TextField emailField = new TextField(); emailField.setPromptText("Email (*)");
        PasswordField passwordField = new PasswordField(); passwordField.setPromptText(existingUser == null ? "Mật khẩu (*, ít nhất 7 ký tự)" : "Mật khẩu mới (để trống nếu không đổi)");
        TextField fullNameField = new TextField(); fullNameField.setPromptText("Họ và tên đầy đủ");
        // Thêm các trường khác nếu cần: dateOfBirth, address, phoneNumber, avatarUrl, introduction

        grid.add(new Label("Tên đăng nhập (*):"), 0, 0); grid.add(usernameField, 1, 0);
        grid.add(new Label("Email (*):"), 0, 1); grid.add(emailField, 1, 1);
        grid.add(new Label(existingUser == null ? "Mật khẩu (*):" : "Mật khẩu mới:"), 0, 2); grid.add(passwordField, 1, 2);
        grid.add(new Label("Họ tên:"), 0, 3); grid.add(fullNameField, 1, 3);
        // Thêm các hàng cho các trường khác

        Node saveButtonNode = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButtonNode.setDisable(true);

        Runnable validateFields = () -> {
            boolean disable = usernameField.getText().trim().isEmpty() || emailField.getText().trim().isEmpty() ||
                    !emailField.getText().trim().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
            String passText = passwordField.getText();
            if (existingUser == null && (passText.isEmpty() || passText.length() < 7)) disable = true;
            else if (existingUser != null && !passText.isEmpty() && passText.length() < 7) disable = true;
            saveButtonNode.setDisable(disable);
        };

        usernameField.textProperty().addListener((obs, o, n) -> validateFields.run());
        emailField.textProperty().addListener((obs, o, n) -> validateFields.run());
        passwordField.textProperty().addListener((obs, o, n) -> validateFields.run());

        if (existingUser != null) {
            usernameField.setText(existingUser.getUsername());
            emailField.setText(existingUser.getEmail());
            fullNameField.setText(existingUser.getFullName());
            // Điền các trường khác
        }
        validateFields.run(); // Kiểm tra lần đầu
        Platform.runLater(usernameField::requestFocus);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String username = usernameField.getText().trim();
                String email = emailField.getText().trim();
                String rawPassword = passwordField.getText();

                // Tạo đối tượng User từ constructor tối thiểu cho AuthController
                User userFromDialog;
                if (existingUser == null) { // Thêm mới
                    String hashedPassword = PasswordUtils.hashPassword(rawPassword);
                    // Constructor này trong User.java tự tạo UUID và thời gian
                    userFromDialog = new User(username, email, hashedPassword, User.Role.USER);
                } else { // Sửa
                    userFromDialog = existingUser; // Dùng lại existingUser để giữ ID và các trường không sửa đổi
                    userFromDialog.setUsername(username);
                    userFromDialog.setEmail(email);
                    if (!rawPassword.isEmpty()) {
                        userFromDialog.setPasswordHash(PasswordUtils.hashPassword(rawPassword));
                    }
                }
                // Gán các trường mở rộng
                userFromDialog.setFullName(fullNameField.getText().trim());
                // Gán các trường khác...
                return userFromDialog;
            }
            return null;
        });

        Optional<User> result = dialog.showAndWait();
        result.ifPresent(userFromDialog -> {
            boolean success;
            if (existingUser == null) {
                Optional<User> addedUser = userService.addUser(userFromDialog); // addUser sẽ tạo ID YYYYMM-NNNN và các giá trị mặc định khác
                success = addedUser.isPresent();
                if (success) showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã thêm người dùng mới: " + userFromDialog.getUsername());
                else showAlert(Alert.AlertType.ERROR, "Lỗi Thêm", "Không thể thêm người dùng. Username hoặc Email có thể đã tồn tại.");
            } else {
                success = userService.updateUser(userFromDialog); // updateUser sẽ cập nhật updatedAt
                if (success) showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật người dùng: " + userFromDialog.getUsername());
                else showAlert(Alert.AlertType.ERROR, "Lỗi Cập nhật", "Không thể cập nhật người dùng.");
            }
            if (success) loadUsersAndLoanCounts();
        });
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

    private AdminDashboardController dashboardController;

    public void setDashboardController(AdminDashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }
}
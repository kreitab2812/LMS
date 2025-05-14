package com.lms.quanlythuvien.controllers;

import com.lms.quanlythuvien.models.Book;
import com.lms.quanlythuvien.models.BorrowingRecord;
import com.lms.quanlythuvien.models.LoanStatus;
import com.lms.quanlythuvien.models.User;
import com.lms.quanlythuvien.services.BookManagementService;
import com.lms.quanlythuvien.services.BorrowingRecordService;
import com.lms.quanlythuvien.services.UserService;
import com.lms.quanlythuvien.utils.PasswordUtils;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.Node; // Đảm bảo import này
import javafx.scene.control.cell.PropertyValueFactory;
// import javafx.scene.control.TextInputDialog; // Có vẻ không dùng
// import javafx.scene.control.ComboBox; // Có vẻ không dùng ComboBox cho Role trong dialog này
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.geometry.Insets;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class UserManagementController implements Initializable {

    //<editor-fold desc="FXML Injections">
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

    @FXML private VBox userDetailsPane;
    @FXML private Label selectedUserDetailsTitleLabel;
    @FXML private TableView<BorrowingRecord> userLoanHistoryTableView;
    @FXML private TableColumn<BorrowingRecord, String> historyBookTitleColumn;
    @FXML private TableColumn<BorrowingRecord, LocalDate> historyBorrowDateColumn;
    @FXML private TableColumn<BorrowingRecord, LocalDate> historyDueDateColumn;
    @FXML private TableColumn<BorrowingRecord, LocalDate> historyReturnDateColumn;
    @FXML private TableColumn<BorrowingRecord, LoanStatus> historyStatusColumn;
    //</editor-fold>

    // Services - Sử dụng Singleton
    private UserService userService;
    private BorrowingRecordService borrowingRecordService;
    private BookManagementService bookManagementService;
    private User selectedUserForDetails;

    // Data for TableViews
    private ObservableList<User> observableUserList;
    private FilteredList<User> filteredUserList;
    private ObservableList<BorrowingRecord> observableLoanHistoryList;

    // Không cần selectedUserForDetails nữa vì TableView selection listener đã xử lý
    // private User selectedUserForDetails;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("DEBUG_UMC_INIT: UserManagementController initialize() started.");
        // LẤY INSTANCE SINGLETON CỦA CÁC SERVICE
        userService = UserService.getInstance();
        bookManagementService = BookManagementService.getInstance();
        borrowingRecordService = BorrowingRecordService.getInstance();
        System.out.println("DEBUG_UMC_INIT: Services retrieved/instantiated.");
        System.out.println("DEBUG_UMC_INIT: UserService instance: " + userService.hashCode());
        System.out.println("DEBUG_UMC_INIT: BookManagementService instance: " + bookManagementService.hashCode());
        System.out.println("DEBUG_UMC_INIT: BorrowingRecordService instance: " + borrowingRecordService.hashCode());

        // (Tùy chọn) Gọi initializeSampleData cho BorrowingRecordService nếu muốn
        // và đảm bảo nó chỉ chạy một lần khi cần (có thể đã gọi ở MainApp hoặc controller chính)
        // borrowingRecordService.initializeSampleData();


        observableUserList = FXCollections.observableArrayList();
        observableLoanHistoryList = FXCollections.observableArrayList();

        setupUsersTableColumns();
        setupUserLoanHistoryTableColumns();

        usersTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            // selectedUserForDetails = newSelection; // Không cần gán lại ở đây nếu chỉ dùng newSelection
            boolean userSelected = (newSelection != null);
            editUserButton.setDisable(!userSelected);
            deleteUserButton.setDisable(!userSelected);

            if (userSelected) {
                selectedUserDetailsTitleLabel.setText("Chi tiết và Lịch sử mượn của: " + newSelection.getUsername());
                loadUserLoanHistory(newSelection);
                userDetailsPane.setVisible(true);
                userDetailsPane.setManaged(true);
            } else {
                selectedUserDetailsTitleLabel.setText("Chi tiết và Lịch sử mượn của: (Chưa chọn người dùng)");
                observableLoanHistoryList.clear(); // Xóa lịch sử cũ
                userDetailsPane.setVisible(false);
                userDetailsPane.setManaged(false);
            }
        });

        userFilterField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterUsersTable(newValue);
        });

        loadUsers(); // Tải danh sách user ban đầu
        userDetailsPane.setVisible(false);
        userDetailsPane.setManaged(false);
        System.out.println("DEBUG_UMC_INIT: UserManagementController initialize() finished.");
    }

    private void setupUsersTableColumns() {
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));

        activeLoansCountColumn.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            if (user != null && borrowingRecordService != null) { // Thêm kiểm tra borrowingRecordService != null
                int count = borrowingRecordService.getActiveLoansByUserId(user.getUserId()).size();
                return new SimpleIntegerProperty(count).asObject();
            }
            return new SimpleIntegerProperty(0).asObject();
        });
    }

    private void setupUserLoanHistoryTableColumns() {
        historyBorrowDateColumn.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        historyDueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        historyReturnDateColumn.setCellValueFactory(new PropertyValueFactory<>("returnDate"));
        historyStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        historyBookTitleColumn.setCellValueFactory(cellData -> {
            BorrowingRecord record = cellData.getValue();
            if (record != null && bookManagementService != null) { // Thêm kiểm tra bookManagementService != null
                Optional<Book> bookOpt = bookManagementService.findBookByIdInLibrary(record.getBookId());
                return new SimpleStringProperty(bookOpt.map(Book::getTitle).orElse("Sách không xác định"));
            }
            return new SimpleStringProperty("");
        });
    }

    private void loadUsers() {
        if (userService == null) {
            System.err.println("ERROR_UMC_LOAD_USERS: userService is null!");
            return;
        }
        // Lấy tất cả user có vai trò USER
        List<User> regularUsers = userService.getUsersByRole(User.Role.USER);
        // Hoặc nếu muốn hiển thị cả Admin (thường không cần trong màn hình quản lý user thường):
        // List<User> allUsers = userService.getAllUsers();

        observableUserList.setAll(regularUsers);
        if (filteredUserList == null) { // Khởi tạo filteredUserList nếu chưa có
            filteredUserList = new FilteredList<>(observableUserList, p -> true);
            usersTableView.setItems(filteredUserList);
        } else {
            // Nếu đã có, chỉ cần set lại predicate để nó cập nhật từ observableUserList mới
            // Hoặc đơn giản là set lại items nếu không muốn giữ filter cũ
            usersTableView.setItems(null); // Xóa items cũ để tránh lỗi
            usersTableView.layout();       // Yêu cầu layout lại
            usersTableView.setItems(filteredUserList); // Đặt lại
        }
        usersTableView.refresh(); // Đảm bảo bảng được cập nhật
        System.out.println("DEBUG_UMC_LOAD_USERS: Users loaded. Count: " + observableUserList.size());
    }

    private void loadUserLoanHistory(User selectedUser) {
        if (selectedUser == null) {
            observableLoanHistoryList.clear();
            System.out.println("DEBUG_UMC_LOAD_HISTORY: No user selected, history cleared.");
            return;
        }
        if (borrowingRecordService == null) {
            System.err.println("ERROR_UMC_LOAD_HISTORY: borrowingRecordService is null!");
            observableLoanHistoryList.clear();
            return;
        }
        // Lấy toàn bộ lịch sử mượn của user này
        List<BorrowingRecord> userHistory = borrowingRecordService.getAllLoans().stream()
                .filter(record -> record.getUserId().equals(selectedUser.getUserId()))
                .sorted((r1, r2) -> r2.getBorrowDate().compareTo(r1.getBorrowDate())) // Sắp xếp mới nhất lên đầu
                .collect(Collectors.toList());

        observableLoanHistoryList.setAll(userHistory);
        userLoanHistoryTableView.setItems(observableLoanHistoryList);
        userLoanHistoryTableView.refresh();
        System.out.println("DEBUG_UMC_LOAD_HISTORY: Loan history loaded for user " + selectedUser.getUsername() + ". Count: " + userHistory.size());
    }

    private void filterUsersTable(String searchText) {
        if (filteredUserList == null) return;
        if (searchText == null || searchText.isEmpty()) {
            filteredUserList.setPredicate(p -> true);
        } else {
            String lowerCaseFilter = searchText.toLowerCase();
            filteredUserList.setPredicate(user ->
                    (user.getUsername() != null && user.getUsername().toLowerCase().contains(lowerCaseFilter)) ||
                            (user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerCaseFilter)) ||
                            (user.getUserId() != null && user.getUserId().toLowerCase().contains(lowerCaseFilter)) // Thêm tìm theo userId nếu muốn
            );
        }
    }

    @FXML
    void handleAddUserAction(ActionEvent event) {
        System.out.println("DEBUG_UMC_ADD_ACTION: Add User button clicked.");
        showUserFormDialog(null); // Gọi dialog, null nghĩa là thêm mới
    }

    @FXML
    void handleEditUserAction(ActionEvent event) {
        User selectedUser = usersTableView.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn Người dùng", "Vui lòng chọn một người dùng để sửa thông tin.");
            return;
        }
        System.out.println("DEBUG_UMC_EDIT_ACTION: Edit User button clicked for: " + selectedUser.getUsername());
        showUserFormDialog(selectedUser); // Gọi dialog, truyền user để edit
    }

    @FXML
    void handleDeleteUserAction(ActionEvent event) {
        User selectedUser = usersTableView.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn Người dùng", "Vui lòng chọn một người dùng để xóa.");
            return;
        }
        System.out.println("DEBUG_UMC_DELETE_ACTION: Delete User button clicked for: " + selectedUser.getUsername());

        if (borrowingRecordService == null || userService == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi Hệ thống", "Không thể kiểm tra hoặc xóa người dùng do lỗi service.");
            return;
        }

        List<BorrowingRecord> activeLoans = borrowingRecordService.getActiveLoansByUserId(selectedUser.getUserId());
        if (!activeLoans.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Không thể xóa", "Người dùng '" + selectedUser.getUsername() + "' đang có " + activeLoans.size() + " cuốn sách mượn chưa trả. Không thể xóa.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Xác nhận Xóa Người dùng");
        confirmation.setHeaderText("Xóa người dùng: " + selectedUser.getUsername() + " (ID: " + selectedUser.getUserId() + ")");
        confirmation.setContentText("Bạn có chắc chắn muốn xóa người dùng này không? Hành động này sẽ xóa vĩnh viễn người dùng và không thể hoàn tác.");
        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean deleted = userService.deleteUser(selectedUser.getUserId());
            if (deleted) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã xóa người dùng: " + selectedUser.getUsername());
                loadUsers(); // Tải lại danh sách
                // Nếu userDetailsPane đang hiển thị thông tin của user vừa xóa, thì ẩn nó đi
                if (userDetailsPane.isVisible() && selectedUserForDetails != null && selectedUserForDetails.getUserId().equals(selectedUser.getUserId())) {
                    userDetailsPane.setVisible(false);
                    userDetailsPane.setManaged(false);
                    selectedUserDetailsTitleLabel.setText("Chi tiết và Lịch sử mượn của: (Chưa chọn người dùng)");
                    observableLoanHistoryList.clear();
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xóa người dùng '" + selectedUser.getUsername() + "'. Người dùng có thể không tồn tại hoặc là tài khoản quản trị mặc định.");
            }
        }
    }

    private void showUserFormDialog(User existingUser) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(existingUser == null ? "Thêm Người dùng Mới" : "Chỉnh sửa Thông tin Người dùng");
        dialog.setHeaderText(existingUser == null ? "Nhập thông tin cho người dùng mới." : "Chỉnh sửa thông tin cho: " + existingUser.getUsername());

        URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
        if (cssUrl != null) {
            dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
        }

        ButtonType saveButtonType = ButtonType.OK;
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10)); // Giảm padding phải một chút

        TextField usernameField = new TextField();
        usernameField.setPromptText("Tên đăng nhập");
        TextField emailField = new TextField();
        emailField.setPromptText("Địa chỉ email");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(existingUser == null ? "Mật khẩu*" : "Mật khẩu mới (để trống nếu không đổi)");

        grid.add(new Label("Tên đăng nhập*:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Email*:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label(existingUser == null ? "Mật khẩu*:" : "Mật khẩu mới:"), 0, 2);
        grid.add(passwordField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        Node saveButtonNode = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButtonNode.setDisable(true);

        Runnable validateFields = () -> {
            boolean disable = usernameField.getText().trim().isEmpty() ||
                    emailField.getText().trim().isEmpty() ||
                    !emailField.getText().trim().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
            if (existingUser == null && passwordField.getText().isEmpty()) {
                disable = true;
            }
            // Nếu là sửa và mật khẩu được nhập, kiểm tra độ dài
            if (existingUser != null && !passwordField.getText().isEmpty() && passwordField.getText().length() < 7) {
                disable = true; // Hoặc hiển thị lỗi cụ thể hơn
            }
            // Nếu là thêm mới, kiểm tra độ dài mật khẩu
            if (existingUser == null && !passwordField.getText().isEmpty() && passwordField.getText().length() < 7) {
                disable = true;
            }
            saveButtonNode.setDisable(disable);
        };

        usernameField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        emailField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());

        if (existingUser != null) {
            usernameField.setText(existingUser.getUsername());
            emailField.setText(existingUser.getEmail());
        }
        Platform.runLater(usernameField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String username = usernameField.getText().trim();
                String email = emailField.getText().trim();
                String rawPassword = passwordField.getText(); // Không trim, mật khẩu có thể có khoảng trắng ở đầu/cuối

                if (username.isEmpty() || email.isEmpty() || !email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi Nhập liệu", "Tên đăng nhập và Email (đúng định dạng) là bắt buộc.");
                    return null;
                }

                if (existingUser == null) { // Thêm mới
                    if (rawPassword.isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi Nhập liệu", "Mật khẩu là bắt buộc khi thêm người dùng mới.");
                        return null;
                    }
                    if (rawPassword.length() < 7) { // Kiểm tra độ dài mật khẩu
                        showAlert(Alert.AlertType.ERROR, "Lỗi Nhập liệu", "Mật khẩu phải có ít nhất 7 ký tự.");
                        return null;
                    }
                    if (userService.isUsernameTaken(username)) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi Trùng lặp", "Tên đăng nhập '" + username + "' đã tồn tại.");
                        return null;
                    }
                    if (userService.isEmailTaken(email)) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi Trùng lặp", "Email '" + email + "' đã được đăng ký.");
                        return null;
                    }
                    String hashedPassword = PasswordUtils.hashPassword(rawPassword);
                    return new User(username, email, hashedPassword, User.Role.USER);
                } else { // Sửa
                    if (!existingUser.getUsername().equalsIgnoreCase(username) && userService.isUsernameTaken(username)) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi Trùng lặp", "Tên đăng nhập '" + username + "' đã được sử dụng bởi người dùng khác.");
                        return null;
                    }
                    if (!existingUser.getEmail().equalsIgnoreCase(email) && userService.isEmailTaken(email)) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi Trùng lặp", "Email '" + email + "' đã được đăng ký bởi người dùng khác.");
                        return null;
                    }
                    existingUser.setUsername(username);
                    existingUser.setEmail(email);
                    if (!rawPassword.isEmpty()) {
                        if (rawPassword.length() < 7) {
                            showAlert(Alert.AlertType.ERROR, "Lỗi Nhập liệu", "Mật khẩu mới phải có ít nhất 7 ký tự.");
                            return null;
                        }
                        existingUser.setPasswordHash(PasswordUtils.hashPassword(rawPassword));
                    }
                    return existingUser;
                }
            }
            return null;
        });

        Optional<User> result = dialog.showAndWait();
        result.ifPresent(user -> {
            boolean success = false;
            if (existingUser == null) {
                success = userService.addUser(user);
                if (success) showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã thêm người dùng mới: " + user.getUsername());
                else showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể thêm người dùng. Username hoặc Email có thể đã tồn tại.");
            } else {
                success = userService.updateUser(user);
                if (success) showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật thông tin người dùng: " + user.getUsername());
                else showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật thông tin người dùng.");
            }
            if (success) loadUsers();
        });
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
        if (cssUrl != null) {
            alert.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
        }
        alert.showAndWait();
    }
}
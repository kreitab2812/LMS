package com.lms.quanlythuvien.controllers; // NHỚ ĐỔI CHO ĐÚNG PACKAGE CỦA CẬU

import com.lms.quanlythuvien.models.Book;
import com.lms.quanlythuvien.models.BorrowingRecord;
import com.lms.quanlythuvien.models.LoanStatus;
import com.lms.quanlythuvien.models.User;
import com.lms.quanlythuvien.services.BookManagementService;
import com.lms.quanlythuvien.services.BorrowingRecordService;
import com.lms.quanlythuvien.services.UserService;
import com.lms.quanlythuvien.utils.PasswordUtils; // Cần cho việc hash mật khẩu khi thêm user

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
import javafx.scene.Node;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ComboBox; // Nếu dùng ComboBox cho Role
import javafx.scene.layout.GridPane; // Cho dialog
import javafx.scene.layout.VBox;
import javafx.application.Platform; // Cho dialog thêm/sửa
import javafx.geometry.Insets;     // Cho dialog

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class UserManagementController implements Initializable {

    //<editor-fold desc="FXML Injections from UserManagementView.fxml">
    @FXML private TextField userFilterField;
    @FXML private Button addUserButton;
    @FXML private Button editUserButton;
    @FXML private Button deleteUserButton;
    @FXML private TableView<User> usersTableView;
    @FXML private TableColumn<User, String> userIdColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, User.Role> roleColumn;
    @FXML private TableColumn<User, Integer> activeLoansCountColumn; // Số sách đang mượn

    @FXML private VBox userDetailsPane; // Pane chi tiết, ban đầu ẩn
    @FXML private Label selectedUserDetailsTitleLabel;
    @FXML private TableView<BorrowingRecord> userLoanHistoryTableView;
    @FXML private TableColumn<BorrowingRecord, String> historyBookTitleColumn;
    @FXML private TableColumn<BorrowingRecord, LocalDate> historyBorrowDateColumn;
    @FXML private TableColumn<BorrowingRecord, LocalDate> historyDueDateColumn;
    @FXML private TableColumn<BorrowingRecord, LocalDate> historyReturnDateColumn;
    @FXML private TableColumn<BorrowingRecord, LoanStatus> historyStatusColumn;
    //</editor-fold>

    // Services
    private UserService userService;
    private BorrowingRecordService borrowingRecordService;
    private BookManagementService bookManagementService; // Cần để lấy tên sách cho lịch sử mượn

    // Data for TableViews
    private ObservableList<User> observableUserList;
    private FilteredList<User> filteredUserList;
    private ObservableList<BorrowingRecord> observableLoanHistoryList;

    private User selectedUserForDetails;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bookManagementService = new BookManagementService(); // Khởi tạo BookManagementService
        userService = new UserService();
        borrowingRecordService = new BorrowingRecordService(); // Giả sử constructor của BorrowingRecordService cần 2 service kia

        observableUserList = FXCollections.observableArrayList();
        observableLoanHistoryList = FXCollections.observableArrayList();

        setupUsersTableColumns();
        setupUserLoanHistoryTableColumns(); // Thiết lập cột cho bảng lịch sử

        // Listener cho việc chọn User trong bảng chính
        usersTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selectedUserForDetails = newSelection;
            boolean userSelected = (newSelection != null);
            editUserButton.setDisable(!userSelected);
            deleteUserButton.setDisable(!userSelected);

            if (userSelected) {
                selectedUserDetailsTitleLabel.setText("Chi tiết và Lịch sử mượn của: " + newSelection.getUsername());
                loadUserLoanHistory(newSelection);
                userDetailsPane.setVisible(true);
                userDetailsPane.setManaged(true);
            } else {
                selectedUserDetailsTitleLabel.setText("Chi tiết và Lịch sử mượn của: N/A");
                observableLoanHistoryList.clear();
                userDetailsPane.setVisible(false);
                userDetailsPane.setManaged(false);
            }
        });

        // Listener cho ô lọc user
        userFilterField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterUsersTable(newValue);
        });

        // Tải danh sách user ban đầu (ví dụ: chỉ user thường)
        loadUsers();
        userDetailsPane.setVisible(false); // Ẩn phần chi tiết ban đầu
        userDetailsPane.setManaged(false);
    }

    private void setupUsersTableColumns() {
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));

        // Cột hiển thị số sách đang mượn (tính toán)
        activeLoansCountColumn.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            if (user != null) {
                // Lấy danh sách ID các lượt mượn active của user
                // List<String> activeLoanIds = user.getActiveLoanRecordIds();
                // return new SimpleIntegerProperty(activeLoanIds != null ? activeLoanIds.size() : 0).asObject();
                // HOẶC gọi service để lấy số lượng chính xác hơn dựa trên trạng thái trong BorrowingRecord
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
            if (record != null) {
                Optional<Book> bookOpt = bookManagementService.findBookByIdInLibrary(record.getBookId());
                return new SimpleStringProperty(bookOpt.map(Book::getTitle).orElse("Sách không xác định"));
            }
            return new SimpleStringProperty("");
        });
    }


    private void loadUsers() {
        // Lấy tất cả user, hoặc chỉ user có vai trò USER
        // List<User> allUsers = userService.getAllUsers();
        // Hiện tại, AuthService quản lý admin, UserService quản lý user thường (cần hợp nhất sau)
        // Tạm thời lấy hết user từ UserService (nếu có)
        List<User> regularUsers = userService.getAllUsers().stream()
                .filter(user -> user.getRole() == User.Role.USER)
                .collect(Collectors.toList());
        observableUserList.setAll(regularUsers);
        filteredUserList = new FilteredList<>(observableUserList, p -> true);
        usersTableView.setItems(filteredUserList);
        usersTableView.refresh();
        System.out.println("Users loaded: " + observableUserList.size());
    }

    private void loadUserLoanHistory(User selectedUser) {
        if (selectedUser == null) {
            observableLoanHistoryList.clear();
            return;
        }
        // Lấy toàn bộ lịch sử mượn của user này từ BorrowingRecordService
        // Cần một phương thức trong BorrowingRecordService, ví dụ: getLoansByUserId(String userId)
        // Tạm thời, nếu BorrowingRecordService.getAllLoans() trả về tất cả, ta lọc ở đây:
        List<BorrowingRecord> userHistory = borrowingRecordService.getAllLoans().stream()
                .filter(record -> record.getUserId().equals(selectedUser.getUserId()))
                .sorted((r1, r2) -> r2.getBorrowDate().compareTo(r1.getBorrowDate())) // Sắp xếp mới nhất lên đầu
                .collect(Collectors.toList());

        observableLoanHistoryList.setAll(userHistory);
        userLoanHistoryTableView.setItems(observableLoanHistoryList);
        userLoanHistoryTableView.refresh();
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
                            (user.getUserId().toLowerCase().contains(lowerCaseFilter))
            );
        }
    }

    @FXML
    void handleAddUserAction(ActionEvent event) {
        // TODO: Hiển thị dialog để nhập thông tin user mới
        // Lấy username, email, password
        // Hash password: String hashedPassword = PasswordUtils.hashPassword(rawPassword);
        // User newUser = new User(username, email, hashedPassword, User.Role.USER); // Mặc định là USER
        // if (userService.addUser(newUser)) { showAlert... loadUsers(); } else { showAlert... }
        showUserFormDialog(null); // Gọi dialog, null nghĩa là thêm mới
        System.out.println("Add User button clicked - NOT FULLY IMPLEMENTED YET");
    }

    @FXML
    void handleEditUserAction(ActionEvent event) {
        User selectedUser = usersTableView.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn User", "Vui lòng chọn một người dùng để sửa.");
            return;
        }
        // TODO: Hiển thị dialog với thông tin user đã chọn để sửa
        // User updatedUser = ... (lấy từ dialog)
        // if (userService.updateUser(updatedUser)) { showAlert... loadUsers(); } else { showAlert... }
        showUserFormDialog(selectedUser); // Gọi dialog, truyền user để edit
        System.out.println("Edit User button clicked for: " + selectedUser.getUsername() + " - NOT FULLY IMPLEMENTED YET");
    }

    @FXML
    void handleDeleteUserAction(ActionEvent event) {
        User selectedUser = usersTableView.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn User", "Vui lòng chọn một người dùng để xóa.");
            return;
        }

        // Kiểm tra xem user có đang mượn sách không
        List<BorrowingRecord> activeLoans = borrowingRecordService.getActiveLoansByUserId(selectedUser.getUserId());
        if (!activeLoans.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Không thể xóa", "Người dùng '" + selectedUser.getUsername() + "' đang có sách mượn (" + activeLoans.size() + " cuốn). Không thể xóa.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Xác nhận xóa");
        confirmation.setHeaderText("Xóa người dùng: " + selectedUser.getUsername());
        confirmation.setContentText("Bạn có chắc chắn muốn xóa người dùng này không? Hành động này không thể hoàn tác.");
        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            // TODO: Gọi userService.deleteUser(selectedUser.getUserId()); (cần tạo phương thức này trong UserService)
            // Tạm thời mô phỏng việc xóa khỏi list nếu dùng ArrayList thuần
            boolean deleted = userService.deleteUser(selectedUser.getUserId()); // Giả sử đã có phương thức này

            if (deleted) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã xóa người dùng: " + selectedUser.getUsername());
                loadUsers(); // Tải lại danh sách
                userDetailsPane.setVisible(false); // Ẩn chi tiết
                userDetailsPane.setManaged(false);
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xóa người dùng.");
            }
        }
        System.out.println("Delete User button clicked for: " + selectedUser.getUsername());
    }

    // --- Dialog cho Thêm/Sửa User ---
    private void showUserFormDialog(User existingUser) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(existingUser == null ? "Thêm Người dùng Mới" : "Chỉnh sửa Thông tin Người dùng");
        dialog.setHeaderText(existingUser == null ? "Nhập thông tin cho người dùng mới." : "Chỉnh sửa thông tin cho: " + existingUser.getUsername());

        ButtonType saveButtonType = ButtonType.OK;
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        PasswordField passwordField = new PasswordField(); // Chỉ hiển thị khi thêm mới
        passwordField.setPromptText("Mật khẩu (để trống nếu không đổi)");

        // (Tùy chọn) ComboBox cho Role nếu muốn admin có thể gán role
        // ComboBox<User.Role> roleComboBox = new ComboBox<>(FXCollections.observableArrayList(User.Role.values()));
        // roleComboBox.setValue(User.Role.USER); // Mặc định

        grid.add(new Label("Username*:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Email*:"), 0, 1);
        grid.add(emailField, 1, 1);
        if (existingUser == null) { // Chỉ yêu cầu mật khẩu khi thêm mới
            grid.add(new Label("Mật khẩu*:"), 0, 2);
            grid.add(passwordField, 1, 2);
            // grid.add(new Label("Vai trò:"), 0, 3);
            // grid.add(roleComboBox, 1, 3);
        } else { // Khi sửa, có thể có trường "Mật khẩu mới" riêng nếu muốn đổi
            grid.add(new Label("Mật khẩu mới:"), 0, 2);
            grid.add(passwordField, 1, 2); // Để trống nếu không đổi
            // roleComboBox.setValue(existingUser.getRole());
            // grid.add(new Label("Vai trò:"), 0, 3);
            // grid.add(roleComboBox, 1, 3);
        }

        dialog.getDialogPane().setContent(grid);

        // Enable/disable nút Save tùy theo việc nhập liệu
        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);
        Runnable validateFields = () -> {
            boolean disable = usernameField.getText().trim().isEmpty() || emailField.getText().trim().isEmpty();
            if (existingUser == null && passwordField.getText().isEmpty()) { // Mật khẩu là bắt buộc khi thêm mới
                disable = true;
            }
            saveButton.setDisable(disable);
        };
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        emailField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        if (existingUser == null) {
            passwordField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        }


        if (existingUser != null) {
            usernameField.setText(existingUser.getUsername());
            emailField.setText(existingUser.getEmail());
            // Không điền mật khẩu cũ vào form sửa
        }

        Platform.runLater(usernameField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String username = usernameField.getText().trim();
                String email = emailField.getText().trim();
                String rawPassword = passwordField.getText(); // Lấy mật khẩu (có thể rỗng khi sửa)
                // User.Role role = roleComboBox.getValue();

                // Validate lần cuối
                if (username.isEmpty() || email.isEmpty() || !email.contains("@")) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi Nhập liệu", "Username và Email (đúng định dạng) là bắt buộc.");
                    return null; // Giữ dialog mở
                }

                if (existingUser == null) { // Thêm mới
                    if (rawPassword.isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi Nhập liệu", "Mật khẩu là bắt buộc khi thêm người dùng mới.");
                        return null;
                    }
                    if (userService.isUsernameTaken(username)) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi Trùng lặp", "Username '" + username + "' đã tồn tại.");
                        return null;
                    }
                    if (userService.isEmailTaken(email)) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi Trùng lặp", "Email '" + email + "' đã được đăng ký.");
                        return null;
                    }
                    String hashedPassword = PasswordUtils.hashPassword(rawPassword);
                    return new User(username, email, hashedPassword, User.Role.USER); // Mặc định USER
                } else { // Sửa
                    // Kiểm tra nếu username/email thay đổi, nó có bị trùng với user khác không
                    if (!existingUser.getUsername().equalsIgnoreCase(username) && userService.isUsernameTaken(username)) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi Trùng lặp", "Username '" + username + "' đã tồn tại.");
                        return null;
                    }
                    if (!existingUser.getEmail().equalsIgnoreCase(email) && userService.isEmailTaken(email)) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi Trùng lặp", "Email '" + email + "' đã được đăng ký bởi người dùng khác.");
                        return null;
                    }
                    existingUser.setUsername(username);
                    existingUser.setEmail(email);
                    if (!rawPassword.isEmpty()) { // Chỉ cập nhật pass nếu người dùng nhập pass mới
                        existingUser.setPasswordHash(PasswordUtils.hashPassword(rawPassword));
                    }
                    // existingUser.setRole(role); // Cẩn thận khi cho phép đổi role
                    return existingUser;
                }
            }
            return null;
        });

        Optional<User> result = dialog.showAndWait();
        result.ifPresent(user -> {
            boolean success;
            if (existingUser == null) { // Thêm mới
                success = userService.addUser(user);
                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã thêm người dùng mới: " + user.getUsername());
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể thêm người dùng mới. Username hoặc Email có thể đã tồn tại.");
                }
            } else { // Sửa
                success = userService.updateUser(user); // Cần tạo phương thức này trong UserService
                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật thông tin người dùng: " + user.getUsername());
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật thông tin người dùng.");
                }
            }
            if(success) loadUsers();
        });
    }


    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
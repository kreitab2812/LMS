package com.lms.quanlythuvien.controllers.admin;

import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.transaction.BorrowingRecord;
import com.lms.quanlythuvien.models.transaction.LoanStatus;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.services.transaction.BorrowingRecordService;
import com.lms.quanlythuvien.services.user.UserService;
import com.lms.quanlythuvien.utils.security.PasswordUtils;
import com.lms.quanlythuvien.exceptions.DeletionRestrictedException; // << THÊM IMPORT

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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.util.Comparator; // << THÊM IMPORT
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class UserManagementController implements Initializable {

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
    // Các cột cho userLoanHistoryTableView
    @FXML private TableColumn<BorrowingRecord, Integer> historyRecordIdColumn; // << THÊM CỘT NÀY NẾU CẦN HIỂN THỊ ID LƯỢT MƯỢN
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = UserService.getInstance();
        bookManagementService = BookManagementService.getInstance();
        borrowingRecordService = BorrowingRecordService.getInstance();

        // borrowingRecordService.initializeSampleData(); // <<--- LOẠI BỎ, seeding DB làm riêng

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
                selectedUserDetailsTitleLabel.setText("Chi tiết và Lịch sử mượn của: " + newSelection.getUsername());
                loadUserLoanHistory(newSelection); // Load lịch sử cho user được chọn
                userDetailsPane.setVisible(true);
                userDetailsPane.setManaged(true);
            } else {
                selectedUserDetailsTitleLabel.setText("Chi tiết và Lịch sử mượn của: (Chưa chọn người dùng)");
                observableLoanHistoryList.clear();
                userDetailsPane.setVisible(false);
                userDetailsPane.setManaged(false);
            }
        });

        userFilterField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterUsersTable(newValue);
        });

        loadUsers();
        userDetailsPane.setVisible(false); // Ẩn ban đầu
        userDetailsPane.setManaged(false);
    }

    private void setupUsersTableColumns() {
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));

        activeLoansCountColumn.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            if (user != null) {
                // Sử dụng getLoansByUserId(userId, activeOnly = true)
                int count = borrowingRecordService.getLoansByUserId(user.getUserId(), true).size();
                return new SimpleIntegerProperty(count).asObject();
            }
            return new SimpleIntegerProperty(0).asObject();
        });
    }

    private void setupUserLoanHistoryTableColumns() {
        // Nếu cậu thêm cột historyRecordIdColumn vào FXML:
        // historyRecordIdColumn.setCellValueFactory(new PropertyValueFactory<>("recordId")); // recordId giờ là int

        historyBorrowDateColumn.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        historyDueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        historyReturnDateColumn.setCellValueFactory(new PropertyValueFactory<>("returnDate"));
        historyStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        historyBookTitleColumn.setCellValueFactory(cellData -> {
            BorrowingRecord record = cellData.getValue();
            if (record != null) {
                // Sử dụng getBookInternalId() và findBookByInternalId()
                Optional<Book> bookOpt = bookManagementService.findBookByInternalId(record.getBookInternalId());
                return new SimpleStringProperty(bookOpt.map(Book::getTitle).orElse("Sách ID nội bộ: " + record.getBookInternalId()));
            }
            return new SimpleStringProperty("");
        });
    }

    private void loadUsers() {
        List<User> allUsers = userService.getAllUsers(); // Lấy tất cả user (bao gồm cả admin nếu muốn hiển thị)
        // Hoặc chỉ lấy USER: List<User> regularUsers = userService.getUsersByRole(User.Role.USER);
        observableUserList.setAll(allUsers);

        if (filteredUserList == null) {
            filteredUserList = new FilteredList<>(observableUserList, p -> true);
            usersTableView.setItems(filteredUserList);
        }
        usersTableView.refresh();
    }

    private void loadUserLoanHistory(User user) {
        if (user == null) {
            observableLoanHistoryList.clear();
            return;
        }
        // Lấy tất cả các lượt mượn (cả active và returned) của user này
        List<BorrowingRecord> userHistory = borrowingRecordService.getLoansByUserId(user.getUserId(), false); // false để lấy tất cả
        // Sắp xếp theo ngày mượn mới nhất lên đầu
        userHistory.sort(Comparator.comparing(BorrowingRecord::getBorrowDate).reversed());

        observableLoanHistoryList.setAll(userHistory);
        userLoanHistoryTableView.setItems(observableLoanHistoryList); // Gán vào TableView
        userLoanHistoryTableView.refresh();
    }

    private void filterUsersTable(String searchText) {
        if (filteredUserList == null) return;
        final String lowerCaseFilter = (searchText == null) ? "" : searchText.toLowerCase().trim();

        filteredUserList.setPredicate(user -> {
            if (lowerCaseFilter.isEmpty()) {
                return true;
            }
            if (user.getUsername().toLowerCase().contains(lowerCaseFilter)) return true;
            if (user.getEmail().toLowerCase().contains(lowerCaseFilter)) return true;
            if (user.getUserId().toLowerCase().contains(lowerCaseFilter)) return true; // userId là String
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
            showAlert(Alert.AlertType.WARNING, "Chưa chọn Người dùng", "Vui lòng chọn một người dùng để sửa.");
            return;
        }
        showUserFormDialog(selectedUser);
    }

    @FXML
    void handleDeleteUserAction(ActionEvent event) {
        User selectedUser = usersTableView.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn Người dùng", "Vui lòng chọn một người dùng để xóa.");
            return;
        }

        // Bước kiểm tra trước khi gọi service đã được tích hợp vào UserService.deleteUser
        // Giờ đây UserService.deleteUser sẽ ném DeletionRestrictedException nếu không xóa được
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Xác nhận Xóa Người dùng");
        confirmation.setHeaderText("Xóa người dùng: " + selectedUser.getUsername() + " (ID: " + selectedUser.getUserId() + ")");
        confirmation.setContentText("Bạn có chắc chắn muốn xóa người dùng này? Hành động này sẽ xóa vĩnh viễn người dùng và không thể hoàn tác (trừ khi họ đang có sách mượn).");

        URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
        if (cssUrl != null) {
            confirmation.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
        }

        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean deleted = userService.deleteUser(selectedUser.getUserId()); // Phương thức này giờ ném exception
                if (deleted) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã xóa người dùng: " + selectedUser.getUsername());
                    loadUsers();
                    if (userDetailsPane.isVisible() && this.selectedUserForDetails != null && this.selectedUserForDetails.getUserId().equals(selectedUser.getUserId())) {
                        userDetailsPane.setVisible(false);
                        userDetailsPane.setManaged(false);
                        selectedUserDetailsTitleLabel.setText("Chi tiết và Lịch sử mượn của: (Chưa chọn người dùng)");
                        observableLoanHistoryList.clear();
                    }
                } else {
                    // Trường hợp này ít xảy ra nếu deleteUser ném exception cho các lỗi ràng buộc
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xóa người dùng '" + selectedUser.getUsername() + "'.");
                }
            } catch (DeletionRestrictedException e) {
                showAlert(Alert.AlertType.ERROR, "Không thể xóa", e.getMessage());
            } catch (Exception e) { // Bắt các lỗi không lường trước khác
                showAlert(Alert.AlertType.ERROR, "Lỗi Hệ thống", "Đã xảy ra lỗi khi cố gắng xóa người dùng: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void showUserFormDialog(User existingUser) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(existingUser == null ? "Thêm Người dùng Mới" : "Chỉnh sửa Thông tin Người dùng");
        // ... (Phần còn lại của showUserFormDialog giữ nguyên như cũ, vì nó đã hoạt động với UserService đã refactor)
        // Chỉ cần đảm bảo các lời gọi isUsernameTaken, isEmailTaken, addUser, updateUser là đúng
        // và PasswordUtils.hashPassword được sử dụng.
        // Constructor User(...) được dùng khi tạo user mới (trước khi addUser) là User(username, email, hashedPassword, role)
        // Constructor này sẽ tự tạo UUID cho userId, nhưng UserService.addUser sẽ tạo ID mới (YYYYMM-NNNN) cho DB.

        URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
        if (cssUrl != null) {
            dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
        }

        ButtonType saveButtonType = ButtonType.OK;
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Tên đăng nhập");
        TextField emailField = new TextField();
        emailField.setPromptText("Địa chỉ email (ví dụ: user@example.com)");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(existingUser == null ? "Mật khẩu* (ít nhất 7 ký tự)" : "Mật khẩu mới (để trống nếu không đổi)");

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

            String passText = passwordField.getText();
            if (existingUser == null) { // Thêm mới
                if (passText.isEmpty() || passText.length() < 7) {
                    disable = true;
                }
            } else { // Sửa
                if (!passText.isEmpty() && passText.length() < 7) { // Nếu nhập pass mới thì phải đủ dài
                    disable = true;
                }
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
                String rawPassword = passwordField.getText();

                // Validate input (đã có phần validate disable nút, nhưng kiểm tra lại cho chắc)
                if (username.isEmpty() || email.isEmpty() || !email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi Nhập liệu", "Tên đăng nhập và Email (đúng định dạng) là bắt buộc.");
                    return null;
                }

                if (existingUser == null) { // Thêm mới
                    if (rawPassword.isEmpty() || rawPassword.length() < 7) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi Nhập liệu", "Mật khẩu là bắt buộc và phải có ít nhất 7 ký tự khi thêm người dùng mới.");
                        return null;
                    }
                    // Kiểm tra trùng lặp (UserService sẽ làm việc này với DB)
                    // Không cần gọi isUsernameTaken/isEmailTaken ở đây nữa nếu addUser xử lý và trả về false/exception
                    String hashedPassword = PasswordUtils.hashPassword(rawPassword);
                    // User constructor này sẽ tạo userId là UUID, nhưng UserService.addUser sẽ tạo ID YYYYMM-NNNN cho DB
                    return new User(username, email, hashedPassword, User.Role.USER);
                } else { // Sửa
                    // Kiểm tra trùng lặp (UserService.updateUser sẽ kiểm tra DB)
                    existingUser.setUsername(username);
                    existingUser.setEmail(email);
                    if (!rawPassword.isEmpty()) {
                        if (rawPassword.length() < 7) {
                            showAlert(Alert.AlertType.ERROR, "Lỗi Nhập liệu", "Mật khẩu mới phải có ít nhất 7 ký tự.");
                            return null;
                        }
                        existingUser.setPasswordHash(PasswordUtils.hashPassword(rawPassword));
                    }
                    return existingUser; // Trả về user đã cập nhật (với ID gốc)
                }
            }
            return null;
        });

        Optional<User> result = dialog.showAndWait();
        result.ifPresent(userFromForm -> { // Đổi tên biến để tránh nhầm lẫn
            boolean success = false;
            if (existingUser == null) { // Thêm mới
                success = userService.addUser(userFromForm); // addUser sẽ bỏ qua ID UUID của userFromForm và tạo ID mới
                if (success) showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã thêm người dùng mới: " + userFromForm.getUsername());
                else showAlert(Alert.AlertType.ERROR, "Lỗi Thêm", "Không thể thêm người dùng. Username hoặc Email có thể đã tồn tại, hoặc lỗi DB.");
            } else { // Sửa (userFromForm ở đây chính là existingUser đã được cập nhật)
                success = userService.updateUser(userFromForm);
                if (success) showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật thông tin người dùng: " + userFromForm.getUsername());
                else showAlert(Alert.AlertType.ERROR, "Lỗi Cập nhật", "Không thể cập nhật. Username/Email mới có thể trùng, hoặc lỗi DB.");
            }
            if (success) loadUsers(); // Tải lại danh sách user sau khi thêm/sửa
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
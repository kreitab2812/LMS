package com.lms.quanlythuvien.controllers.admin;

import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.transaction.BorrowingRecord;
import com.lms.quanlythuvien.models.transaction.LoanStatus;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.services.transaction.BorrowingRecordService;
import com.lms.quanlythuvien.services.user.UserService;
import com.lms.quanlythuvien.exceptions.DeletionRestrictedException; // Giả sử có custom exception

import javafx.beans.property.SimpleObjectProperty; // Cho cột ngày tháng
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
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class LoanManagementController implements Initializable {

    @FXML private TextField userSearchField;
    @FXML private TextField bookSearchField;
    @FXML private Label selectedUserInfoLabel;
    @FXML private Label selectedBookInfoLabel;
    @FXML private Button borrowBookButton;
    @FXML private TextField activeLoansFilterField;
    @FXML private TableView<BorrowingRecord> activeLoansTableView;

    // Các cột trong TableView - đảm bảo PropertyValueFactory khớp với getter trong BorrowingRecord.java đã sửa
    @FXML private TableColumn<BorrowingRecord, Integer> loanIdColumn; // << Đổi kiểu thành Integer
    @FXML private TableColumn<BorrowingRecord, String> loanBookTitleColumn;
    @FXML private TableColumn<BorrowingRecord, String> loanUserNameColumn;
    @FXML private TableColumn<BorrowingRecord, LocalDate> loanBorrowDateColumn;
    @FXML private TableColumn<BorrowingRecord, LocalDate> loanDueDateColumn;
    @FXML private TableColumn<BorrowingRecord, LoanStatus> loanStatusColumn;
    @FXML private TableColumn<BorrowingRecord, Void> loanActionColumn;
    // @FXML private Button returnBookButton; // Nút này không còn cần thiết nếu dùng nút trên dòng

    private BookManagementService bookManagementService;
    private UserService userService;
    private BorrowingRecordService borrowingRecordService;

    private ObservableList<BorrowingRecord> observableLoanList;
    private FilteredList<BorrowingRecord> filteredLoanList;

    private User selectedUserForLoan;
    private Book selectedBookForLoan;
    // private BorrowingRecord selectedLoanForReturn; // Không cần nữa nếu dùng nút trên dòng

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bookManagementService = BookManagementService.getInstance();
        userService = UserService.getInstance();
        borrowingRecordService = BorrowingRecordService.getInstance();

        // borrowingRecordService.initializeSampleData(); // <<--- LOẠI BỎ LỜI GỌI NÀY

        observableLoanList = FXCollections.observableArrayList();
        filteredLoanList = new FilteredList<>(observableLoanList, p -> true);
        activeLoansTableView.setItems(filteredLoanList);

        setupActiveLoansTableColumns();

        // Bỏ listener cho selectedItemProperty nếu chỉ dùng nút trên dòng để trả sách
        // activeLoansTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
        //     selectedLoanForReturn = newSelection;
        //     // if (returnBookButton != null) { // returnBookButton có thể đã bị loại bỏ
        //     //     returnBookButton.setDisable(newSelection == null || newSelection.getStatus() == LoanStatus.RETURNED);
        //     // }
        // });

        if (activeLoansFilterField != null) {
            activeLoansFilterField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterActiveLoansTable(newValue);
            });
        }

        if (borrowBookButton != null) borrowBookButton.setDisable(true);
        // if (returnBookButton != null) returnBookButton.setDisable(true); // Loại bỏ nếu không dùng nút chung

        loadActiveLoans();
    }

    private void setupActiveLoansTableColumns() {
        loanIdColumn.setCellValueFactory(new PropertyValueFactory<>("recordId")); // recordId giờ là int
        loanBorrowDateColumn.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        loanDueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        loanStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        loanBookTitleColumn.setCellValueFactory(cellData -> {
            BorrowingRecord record = cellData.getValue();
            if (record != null) {
                // Sử dụng bookInternalId và phương thức tìm sách bằng internalId
                Optional<Book> bookOpt = bookManagementService.findBookByInternalId(record.getBookInternalId());
                return new SimpleStringProperty(bookOpt.map(Book::getTitle).orElse("Sách ID: " + record.getBookInternalId()));
            }
            return new SimpleStringProperty("");
        });

        loanUserNameColumn.setCellValueFactory(cellData -> {
            BorrowingRecord record = cellData.getValue();
            if (record != null) {
                Optional<User> userOpt = userService.findUserById(record.getUserId());
                return new SimpleStringProperty(userOpt.map(User::getUsername).orElse("User ID: " + record.getUserId()));
            }
            return new SimpleStringProperty("");
        });

        Callback<TableColumn<BorrowingRecord, Void>, TableCell<BorrowingRecord, Void>> cellFactory = param -> new TableCell<>() {
            private final Button btnReturn = new Button("Trả"); // Đổi tên nút cho ngắn gọn
            {
                btnReturn.getStyleClass().add("secondary-button-small"); // Dùng class CSS cho nút nhỏ hơn
                btnReturn.setOnAction((ActionEvent event) -> {
                    BorrowingRecord record = getTableView().getItems().get(getIndex());
                    if (record != null && record.getStatus() != LoanStatus.RETURNED) {
                        handleReturnBookFromTableRowAction(record);
                    } else {
                        showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Lượt mượn này đã được trả hoặc không hợp lệ.");
                    }
                });
            }

            @Override
            public void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    BorrowingRecord record = getTableView().getItems().get(getIndex());
                    if (record != null && record.getStatus() != LoanStatus.RETURNED) {
                        setGraphic(btnReturn);
                    } else {
                        setGraphic(null); // Ẩn nút nếu đã trả
                    }
                }
            }
        };
        loanActionColumn.setCellFactory(cellFactory);
        loanActionColumn.setStyle("-fx-alignment: CENTER;");
    }

    private void loadActiveLoans() {
        borrowingRecordService.updateAllOverdueStatuses(LocalDate.now()); // Cập nhật trạng thái trước
        // Lấy tất cả các lượt mượn và lọc ở client hoặc sửa service để có hàm lấy active/overdue
        List<BorrowingRecord> allLoans = borrowingRecordService.getAllLoans();
        List<BorrowingRecord> activeAndOverdueLoans = allLoans.stream()
                .filter(loan -> loan.getStatus() == LoanStatus.ACTIVE || loan.getStatus() == LoanStatus.OVERDUE)
                .collect(Collectors.toList());

        observableLoanList.setAll(activeAndOverdueLoans);
        // activeLoansTableView.setItems(filteredLoanList); // filteredList đã được set items trong initialize
        activeLoansTableView.refresh(); // Quan trọng để bảng cập nhật sau khi thay đổi data source
    }

    private void filterActiveLoansTable(String searchText) {
        if (filteredLoanList == null) return;
        final String lowerCaseFilter = (searchText == null) ? "" : searchText.toLowerCase().trim();

        filteredLoanList.setPredicate(record -> {
            if (lowerCaseFilter.isEmpty()) {
                return true;
            }
            // Tìm theo ID lượt mượn (recordId giờ là int)
            if (String.valueOf(record.getRecordId()).contains(lowerCaseFilter)) return true;

            // Tìm theo tên sách (cần lấy tên sách từ internalId)
            Optional<Book> bookOpt = bookManagementService.findBookByInternalId(record.getBookInternalId());
            if (bookOpt.isPresent() && bookOpt.get().getTitle().toLowerCase().contains(lowerCaseFilter)) return true;

            // Tìm theo tên người dùng
            Optional<User> userOpt = userService.findUserById(record.getUserId());
            if (userOpt.isPresent() && userOpt.get().getUsername().toLowerCase().contains(lowerCaseFilter)) return true;

            return false;
        });
    }

    @FXML
    void handleSearchUserAction(ActionEvent event) {
        String searchTerm = userSearchField.getText().trim();
        if (searchTerm.isEmpty()) {
            selectedUserInfoLabel.setText("Người dùng: Vui lòng nhập ID hoặc Username");
            selectedUserForLoan = null;
            updateBorrowButtonState();
            return;
        }
        Optional<User> userOpt = userService.findUserById(searchTerm);
        if (userOpt.isEmpty()) {
            userOpt = userService.findUserByUsername(searchTerm);
        }

        if (userOpt.isPresent()) {
            selectedUserForLoan = userOpt.get();
            selectedUserInfoLabel.setText("User: " + selectedUserForLoan.getUsername() + " (ID: " + selectedUserForLoan.getUserId() + ")");
        } else {
            selectedUserForLoan = null;
            selectedUserInfoLabel.setText("User: Không tìm thấy '" + searchTerm + "'");
        }
        updateBorrowButtonState();
    }

    @FXML
    void handleSearchBookAction(ActionEvent event) {
        String searchTerm = bookSearchField.getText().trim();
        if (searchTerm.isEmpty()) {
            selectedBookInfoLabel.setText("Sách: Vui lòng nhập ISBN-13 hoặc Tiêu đề");
            selectedBookForLoan = null;
            updateBorrowButtonState();
            return;
        }
        // Ưu tiên tìm bằng ISBN-13 trước
        Optional<Book> bookOpt = bookManagementService.findBookByIsbn13InLibrary(searchTerm);

        if (bookOpt.isEmpty()) { // Nếu không thấy bằng ISBN, thử tìm bằng tiêu đề (lấy cuốn đầu tiên nếu có nhiều)
            List<Book> foundBooks = bookManagementService.searchBooksInLibrary(searchTerm, "TITLE");
            if (!foundBooks.isEmpty()) {
                bookOpt = Optional.of(foundBooks.get(0));
            }
        }

        if (bookOpt.isPresent()) {
            selectedBookForLoan = bookOpt.get();
            String availabilityText = (selectedBookForLoan.getAvailableQuantity() > 0) ?
                    " (Có sẵn: " + selectedBookForLoan.getAvailableQuantity() + ")" :
                    " - HẾT SÁCH";
            selectedBookInfoLabel.setText("Sách: " + selectedBookForLoan.getTitle() + availabilityText);
        } else {
            selectedBookForLoan = null;
            selectedBookInfoLabel.setText("Sách: Không tìm thấy '" + searchTerm + "'");
        }
        updateBorrowButtonState();
    }

    private void updateBorrowButtonState() {
        if (borrowBookButton == null) return;
        boolean disable = !(selectedUserForLoan != null &&
                selectedBookForLoan != null &&
                selectedBookForLoan.getIsbn13() != null && // Cần ISBN để tạo lượt mượn
                selectedBookForLoan.getAvailableQuantity() > 0);
        borrowBookButton.setDisable(disable);
    }

    @FXML
    void handleBorrowBookAction(ActionEvent event) {
        if (selectedUserForLoan == null || selectedBookForLoan == null || selectedBookForLoan.getIsbn13() == null) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng chọn người dùng và sách (có ISBN-13 hợp lệ) để cho mượn.");
            return;
        }
        // Số lượng đã được BookManagementService.handleBookBorrowed kiểm tra và giảm trừ
        // Và BorrowingRecordService.createLoan cũng đã kiểm tra mượn trùng

        LocalDate borrowDate = LocalDate.now();
        LocalDate dueDate = borrowDate.plus(7, ChronoUnit.DAYS); // Mặc định mượn 7 ngày

        // BorrowingRecordService.createLoan giờ nhận bookIsbn13
        Optional<BorrowingRecord> newLoanOpt = borrowingRecordService.createLoan(
                selectedBookForLoan.getIsbn13(), // <<--- Truyền ISBN-13
                selectedUserForLoan.getUserId(),
                borrowDate,
                dueDate
        );

        if (newLoanOpt.isPresent()) {
            BorrowingRecord newLoan = newLoanOpt.get();
            // userService.recordNewLoanForUser không còn quá quan trọng vì User.activeLoanIds không lưu vào DB
            // Nhưng có thể giữ lại nếu UI nào đó đang đọc từ User object được cache
            userService.recordNewLoanForUser(selectedUserForLoan.getUserId(), String.valueOf(newLoan.getRecordId())); // recordId giờ là int

            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    "Đã cho mượn sách: '" + selectedBookForLoan.getTitle() +
                            "'\nCho người dùng: '" + selectedUserForLoan.getUsername() + "'.\n" +
                            "Hạn trả: " + dueDate.toString());

            loadActiveLoans(); // Tải lại danh sách mượn
            // Refresh thông tin sách đã chọn để cập nhật số lượng có sẵn
            String currentBookSearchTerm = bookSearchField.getText();
            selectedBookForLoan = null; // Xóa lựa chọn cũ để bắt buộc tìm lại
            selectedBookInfoLabel.setText("Sách: Chọn lại sách hoặc tìm kiếm.");
            if(currentBookSearchTerm != null && !currentBookSearchTerm.isEmpty()) {
                bookSearchField.setText(currentBookSearchTerm);
                handleSearchBookAction(null); // Tìm lại sách
            }
            updateBorrowButtonState();
        } else {
            // Lỗi đã được BorrowingRecordService hoặc BookManagementService log, chỉ cần thông báo chung
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tạo bản ghi mượn sách. Lý do có thể:\n" +
                    "- Sách đã hết.\n" +
                    "- Người dùng đang mượn cuốn này và chưa trả.\n" +
                    "- Lỗi hệ thống. Vui lòng xem console log.");
        }
    }

    private void handleReturnBookFromTableRowAction(BorrowingRecord recordToReturn) {
        // recordToReturn.getRecordId() giờ là int
        confirmAndPerformReturn(recordToReturn);
    }

    // Bỏ @FXML nếu không có nút "Trả Sách" chung nữa
    // @FXML
    // void handleReturnBookAction(ActionEvent event) {
    //     // if (selectedLoanForReturn == null || selectedLoanForReturn.getStatus() == LoanStatus.RETURNED) {
    //     //     showAlert(Alert.AlertType.WARNING, "Chưa chọn lượt mượn", "Vui lòng chọn một lượt mượn đang hoạt động từ bảng để trả.");
    //     //     return;
    //     // }
    //     // confirmAndPerformReturn(selectedLoanForReturn);
    // }

    private void confirmAndPerformReturn(BorrowingRecord loanToReturn) {
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Xác nhận Trả sách");

        // Lấy thông tin sách và user cho thông báo
        String bookTitle = bookManagementService.findBookByInternalId(loanToReturn.getBookInternalId())
                .map(Book::getTitle).orElse("Sách ID: " + loanToReturn.getBookInternalId());
        String username = userService.findUserById(loanToReturn.getUserId())
                .map(User::getUsername).orElse("User ID: " + loanToReturn.getUserId());

        confirmationDialog.setHeaderText("Trả sách: '" + bookTitle + "'\nBởi người dùng: '" + username + "'?");
        confirmationDialog.setContentText("Đánh dấu lượt mượn (ID: " + loanToReturn.getRecordId() + ") là ĐÃ TRẢ?");

        Optional<ButtonType> result = confirmationDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            performReturnBookLogic(loanToReturn);
        }
    }

    private void performReturnBookLogic(BorrowingRecord loanToReturn) {
        // loanToReturn.getRecordId() giờ là int
        boolean recordUpdated = borrowingRecordService.recordBookReturn(loanToReturn.getRecordId(), LocalDate.now());

        if (recordUpdated) {
            // userService.recordLoanEndedForUser không còn quá quan trọng
            userService.recordLoanEndedForUser(loanToReturn.getUserId(), String.valueOf(loanToReturn.getRecordId()));

            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã ghi nhận trả sách thành công cho lượt mượn ID: " + loanToReturn.getRecordId());
            loadActiveLoans();

            // Refresh thông tin sách đã chọn nếu đó là sách vừa trả
            if(selectedBookForLoan != null && selectedBookForLoan.getInternalId() == loanToReturn.getBookInternalId()){
                String currentBookSearchTerm = bookSearchField.getText();
                selectedBookForLoan = null;
                selectedBookInfoLabel.setText("Sách: Chọn lại sách hoặc tìm kiếm.");
                if(currentBookSearchTerm != null && !currentBookSearchTerm.isEmpty()) {
                    bookSearchField.setText(currentBookSearchTerm);
                    handleSearchBookAction(null);
                }
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật bản ghi trả sách. Sách có thể đã được trả trước đó hoặc có lỗi xảy ra.");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null); // Gọn gàng hơn
        alert.setContentText(message);
        // Áp dụng CSS cho Dialog Pane nếu muốn (cần đảm bảo đường dẫn CSS đúng)
        URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
        if (cssUrl != null) {
            alert.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            // alert.getDialogPane().getStyleClass().add("my-dialog-pane"); // Thêm style class nếu cần
        }
        alert.showAndWait();
    }
}
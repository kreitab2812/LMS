package com.lms.quanlythuvien.controllers; // NHỚ ĐỔI CHO ĐÚNG PACKAGE CỦA CẬU

import com.lms.quanlythuvien.models.Book;
import com.lms.quanlythuvien.models.BorrowingRecord;
import com.lms.quanlythuvien.models.LoanStatus;
import com.lms.quanlythuvien.models.User;
import com.lms.quanlythuvien.services.BookManagementService;
import com.lms.quanlythuvien.services.BorrowingRecordService;
import com.lms.quanlythuvien.services.UserService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
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
import java.util.Optional;
import java.util.ResourceBundle;

public class LoanManagementController implements Initializable {

    //<editor-fold desc="FXML Injections from LoanManagementView.fxml">
    @FXML private TextField userSearchField;
    @FXML private Button searchUserButton;
    @FXML private TextField bookSearchField;
    @FXML private Button searchBookButton;
    @FXML private Label selectedUserInfoLabel;
    @FXML private Label selectedBookInfoLabel;
    @FXML private Button borrowBookButton;
    @FXML private TextField activeLoansFilterField;
    @FXML private TableView<BorrowingRecord> activeLoansTableView;
    @FXML private TableColumn<BorrowingRecord, String> loanIdColumn;
    @FXML private TableColumn<BorrowingRecord, String> loanBookTitleColumn;
    @FXML private TableColumn<BorrowingRecord, String> loanUserNameColumn;
    @FXML private TableColumn<BorrowingRecord, LocalDate> loanBorrowDateColumn;
    @FXML private TableColumn<BorrowingRecord, LocalDate> loanDueDateColumn;
    @FXML private TableColumn<BorrowingRecord, LoanStatus> loanStatusColumn;
    @FXML private TableColumn<BorrowingRecord, Void> loanActionColumn; // Cột cho nút "Trả sách"
    @FXML private Button returnBookButton; // Nút trả sách chung (nếu không dùng nút trên từng dòng)
    //</editor-fold>

    // Services
    private BookManagementService bookManagementService;
    private UserService userService;
    private BorrowingRecordService borrowingRecordService;

    // Data for TableView
    private ObservableList<BorrowingRecord> observableLoanList;
    private FilteredList<BorrowingRecord> filteredLoanList;

    // Selected items for borrowing
    private User selectedUserForLoan;
    private Book selectedBookForLoan;
    private BorrowingRecord selectedLoanForReturn;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Khởi tạo các services
        bookManagementService = new BookManagementService();
        userService = new UserService();
        borrowingRecordService = new BorrowingRecordService();

        // Khởi tạo danh sách cho TableView
        observableLoanList = FXCollections.observableArrayList();
        // TODO: Tải danh sách các lượt mượn đang hoạt động ban đầu
        // loadActiveLoans(); // Sẽ tạo phương thức này sau

        // Thiết lập các cột cho TableView
        setupActiveLoansTableColumns();

        // Thiết lập listener cho việc chọn dòng trong TableView (nếu dùng nút trả sách chung)
        activeLoansTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selectedLoanForReturn = newSelection;
            returnBookButton.setDisable(newSelection == null || newSelection.getStatus() == LoanStatus.RETURNED);
        });

        // (Tùy chọn) Listener cho ô lọc danh sách mượn
        activeLoansFilterField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterActiveLoansTable(newValue);
        });

        // Vô hiệu hóa nút "Cho Mượn Sách" ban đầu
        borrowBookButton.setDisable(true);
        // Vô hiệu hóa nút "Trả Sách" chung ban đầu
        returnBookButton.setDisable(true);

        // Tải dữ liệu ban đầu cho bảng
        loadActiveLoans();
    }

    private void setupActiveLoansTableColumns() {
        loanIdColumn.setCellValueFactory(new PropertyValueFactory<>("recordId"));
        loanBorrowDateColumn.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        loanDueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        loanStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Cột hiển thị Tiêu đề Sách (cần lấy từ bookId)
        loanBookTitleColumn.setCellValueFactory(cellData -> {
            String bookId = cellData.getValue().getBookId();
            Optional<Book> bookOpt = bookManagementService.findBookByIdInLibrary(bookId);
            return new SimpleStringProperty(bookOpt.map(Book::getTitle).orElse("Không rõ sách"));
        });

        // Cột hiển thị Tên Người Mượn (cần lấy từ userId)
        loanUserNameColumn.setCellValueFactory(cellData -> {
            String userId = cellData.getValue().getUserId();
            Optional<User> userOpt = userService.findUserById(userId);
            return new SimpleStringProperty(userOpt.map(User::getUsername).orElse("Không rõ người dùng"));
        });

        // Cột Hành Động (chứa nút "Trả Sách" cho mỗi dòng)
        Callback<TableColumn<BorrowingRecord, Void>, TableCell<BorrowingRecord, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<BorrowingRecord, Void> call(final TableColumn<BorrowingRecord, Void> param) {
                final TableCell<BorrowingRecord, Void> cell = new TableCell<>() {
                    private final Button btnReturn = new Button("Trả Sách");
                    {
                        btnReturn.getStyleClass().add("secondary-button"); // Hoặc "danger-button"
                        btnReturn.setOnAction((ActionEvent event) -> {
                            BorrowingRecord record = getTableView().getItems().get(getIndex());
                            handleReturnBookFromTableRowAction(record);
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            BorrowingRecord record = getTableView().getItems().get(getIndex());
                            // Chỉ hiển thị nút nếu sách chưa trả
                            if (record.getStatus() != LoanStatus.RETURNED) {
                                setGraphic(btnReturn);
                            } else {
                                setGraphic(null);
                            }
                        }
                    }
                };
                return cell;
            }
        };
        loanActionColumn.setCellFactory(cellFactory);
    }

    private void loadActiveLoans() {
        // Lấy danh sách các lượt mượn từ service (bao gồm cả ACTIVE và OVERDUE)
        // Trước khi lấy, cập nhật trạng thái quá hạn
        borrowingRecordService.updateAllOverdueStatuses(LocalDate.now());
        List<BorrowingRecord> activeLoans = borrowingRecordService.getAllLoans().stream()
                .filter(loan -> loan.getStatus() == LoanStatus.ACTIVE || loan.getStatus() == LoanStatus.OVERDUE)
                .collect(Collectors.toList());

        observableLoanList.setAll(activeLoans);
        filteredLoanList = new FilteredList<>(observableLoanList, p -> true); // Ban đầu hiển thị tất cả
        activeLoansTableView.setItems(filteredLoanList);
        activeLoansTableView.refresh(); // Đảm bảo table được cập nhật
        System.out.println("Active loans loaded: " + observableLoanList.size());
    }

    private void filterActiveLoansTable(String searchText) {
        if (filteredLoanList == null) return;
        if (searchText == null || searchText.isEmpty()) {
            filteredLoanList.setPredicate(p -> true); // Hiển thị tất cả nếu ô tìm kiếm trống
        } else {
            String lowerCaseFilter = searchText.toLowerCase();
            filteredLoanList.setPredicate(record -> {
                // Lấy thông tin sách và user để tìm kiếm
                Optional<Book> bookOpt = bookManagementService.findBookByIdInLibrary(record.getBookId());
                Optional<User> userOpt = userService.findUserById(record.getUserId());

                if (record.getRecordId().toLowerCase().contains(lowerCaseFilter)) return true;
                if (bookOpt.isPresent() && bookOpt.get().getTitle().toLowerCase().contains(lowerCaseFilter)) return true;
                if (userOpt.isPresent() && userOpt.get().getUsername().toLowerCase().contains(lowerCaseFilter)) return true;
                // Thêm các trường tìm kiếm khác nếu muốn (ví dụ: theo ngày, trạng thái)
                return false;
            });
        }
    }

    @FXML
    void handleSearchUserAction(ActionEvent event) {
        String searchTerm = userSearchField.getText().trim();
        if (searchTerm.isEmpty()) {
            selectedUserInfoLabel.setText("User: Vui lòng nhập ID hoặc Username");
            selectedUserForLoan = null;
            updateBorrowButtonState();
            return;
        }
        // Thử tìm theo ID trước, sau đó theo Username
        Optional<User> userOpt = userService.findUserById(searchTerm);
        if (!userOpt.isPresent()) {
            userOpt = userService.findUserByUsername(searchTerm);
        }

        if (userOpt.isPresent()) {
            selectedUserForLoan = userOpt.get();
            selectedUserInfoLabel.setText("User: " + selectedUserForLoan.getUsername() + " (ID: " + selectedUserForLoan.getUserId() + ")");
        } else {
            selectedUserForLoan = null;
            selectedUserInfoLabel.setText("User: Không tìm thấy người dùng '" + searchTerm + "'");
        }
        updateBorrowButtonState();
    }

    @FXML
    void handleSearchBookAction(ActionEvent event) {
        String searchTerm = bookSearchField.getText().trim();
        if (searchTerm.isEmpty()) {
            selectedBookInfoLabel.setText("Sách: Vui lòng nhập ID, ISBN, hoặc Tiêu đề");
            selectedBookForLoan = null;
            updateBorrowButtonState();
            return;
        }
        // Thử tìm theo nhiều tiêu chí (cần mở rộng logic tìm kiếm trong BookManagementService nếu muốn tìm theo tiêu đề/ISBN từ đây)
        // Tạm thời, chúng ta sẽ tìm theo ID trước, vì đây là thông tin chắc chắn nhất
        Optional<Book> bookOpt = bookManagementService.findBookByIdInLibrary(searchTerm);
        // Nếu muốn tìm theo ISBN hoặc Title, bạn cần thêm các phương thức tương ứng trong service hoặc sửa đổi findBookByIdInLibrary
        // Hoặc là, bạn có thể lấy tất cả sách và lọc ở đây, nhưng không hiệu quả lắm.

        if (bookOpt.isPresent()) {
            selectedBookForLoan = bookOpt.get();
            selectedBookInfoLabel.setText("Sách: " + selectedBookForLoan.getTitle() + " (Có sẵn: " + selectedBookForLoan.getAvailableQuantity() + ")");
            if (selectedBookForLoan.getAvailableQuantity() <= 0) {
                // Có thể đổi màu text hoặc thêm cảnh báo nếu sách đã hết
                selectedBookInfoLabel.setText(selectedBookInfoLabel.getText() + " - HẾT SÁCH");
            }
        } else {
            selectedBookForLoan = null;
            selectedBookInfoLabel.setText("Sách: Không tìm thấy sách '" + searchTerm + "'");
        }
        updateBorrowButtonState();
    }

    private void updateBorrowButtonState() {
        // Nút "Cho Mượn Sách" chỉ bật khi đã chọn user, chọn sách, và sách đó còn hàng
        boolean disable = !(selectedUserForLoan != null &&
                selectedBookForLoan != null &&
                selectedBookForLoan.getAvailableQuantity() > 0);
        borrowBookButton.setDisable(disable);
    }

    @FXML
    void handleBorrowBookAction(ActionEvent event) {
        if (selectedUserForLoan == null || selectedBookForLoan == null) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng chọn người dùng và sách để cho mượn.");
            return;
        }
        if (selectedBookForLoan.getAvailableQuantity() <= 0) {
            showAlert(Alert.AlertType.WARNING, "Hết sách", "Sách này hiện đã hết, không thể cho mượn.");
            return;
        }

        // Xác định ngày mượn và ngày hẹn trả (ví dụ: mượn trong 7 ngày)
        LocalDate borrowDate = LocalDate.now();
        LocalDate dueDate = borrowDate.plus(7, ChronoUnit.DAYS); // Mượn trong 7 ngày

        // 1. Gọi BookManagementService để cập nhật availableQuantity
        boolean bookAvailableAndUpdated = bookManagementService.handleBookBorrowed(selectedBookForLoan.getId());

        if (bookAvailableAndUpdated) {
            // 2. Gọi BorrowingRecordService để tạo bản ghi mượn
            BorrowingRecord newLoan = borrowingRecordService.createLoan(
                    selectedBookForLoan.getId(),
                    selectedUserForLoan.getUserId(),
                    borrowDate,
                    dueDate
            );

            if (newLoan != null) {
                // 3. Gọi UserService để cập nhật danh sách mượn của người dùng
                userService.recordNewLoanForUser(selectedUserForLoan.getUserId(), newLoan.getRecordId());

                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Đã cho mượn sách '" + selectedBookForLoan.getTitle() +
                                "' cho người dùng '" + selectedUserForLoan.getUsername() + "'.\n" +
                                "Hạn trả: " + dueDate.toString());

                // Làm mới giao diện
                loadActiveLoans(); // Tải lại bảng các lượt mượn
                // Cập nhật lại thông tin sách đã chọn (để thấy availableQuantity giảm)
                handleSearchBookAction(null); // Gọi lại để refresh thông tin sách
                // Reset lựa chọn
                // selectedUserForLoan = null; // Cân nhắc có nên reset hay không
                // selectedBookForLoan = null;
                // selectedUserInfoLabel.setText("User: (Chưa chọn)");
                // selectedBookInfoLabel.setText("Sách: (Chưa chọn)");
                updateBorrowButtonState();

            } else {
                // Hoàn tác việc cập nhật availableQuantity nếu tạo record lỗi
                bookManagementService.handleBookReturned(selectedBookForLoan.getId()); // Coi như trả lại sách vừa "mượn hụt"
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tạo bản ghi mượn sách.");
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật số lượng sách hoặc sách đã hết.");
        }
    }

    // Phương thức này được gọi từ nút "Trả Sách" trên từng dòng của TableView
    private void handleReturnBookFromTableRowAction(BorrowingRecord recordToReturn) {
        if (recordToReturn == null || recordToReturn.getStatus() == LoanStatus.RETURNED) {
            showAlert(Alert.AlertType.WARNING, "Thông tin không hợp lệ", "Lượt mượn này không hợp lệ hoặc đã được trả.");
            return;
        }

        // Hỏi xác nhận
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Xác nhận trả sách");
        Optional<Book> bookOpt = bookManagementService.findBookByIdInLibrary(recordToReturn.getBookId());
        String bookTitle = bookOpt.map(Book::getTitle).orElse(recordToReturn.getBookId());
        confirmationDialog.setHeaderText("Trả sách: " + bookTitle);
        confirmationDialog.setContentText("Bạn có chắc chắn muốn đánh dấu lượt mượn này là đã trả?");

        Optional<ButtonType> result = confirmationDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            performReturnBook(recordToReturn);
        }
    }

    @FXML
    void handleReturnBookAction(ActionEvent event) { // Được gọi từ nút "Trả Sách" chung
        if (selectedLoanForReturn == null || selectedLoanForReturn.getStatus() == LoanStatus.RETURNED) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn lượt mượn", "Vui lòng chọn một lượt mượn đang hoạt động để trả.");
            return;
        }
        // Hỏi xác nhận tương tự như trên
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Xác nhận trả sách");
        Optional<Book> bookOpt = bookManagementService.findBookByIdInLibrary(selectedLoanForReturn.getBookId());
        String bookTitle = bookOpt.map(Book::getTitle).orElse(selectedLoanForReturn.getBookId());
        confirmationDialog.setHeaderText("Trả sách: " + bookTitle);
        confirmationDialog.setContentText("Bạn có chắc chắn muốn đánh dấu lượt mượn này là đã trả?");

        Optional<ButtonType> result = confirmationDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            performReturnBook(selectedLoanForReturn);
        }
    }

    private void performReturnBook(BorrowingRecord loanToReturn) {
        // 1. Gọi BorrowingRecordService để cập nhật bản ghi trả
        boolean recordUpdated = borrowingRecordService.recordBookReturn(loanToReturn.getRecordId(), LocalDate.now());

        if (recordUpdated) {
            // 2. Gọi BookManagementService để cập nhật availableQuantity
            bookManagementService.handleBookReturned(loanToReturn.getBookId());

            // 3. Gọi UserService để cập nhật danh sách mượn của người dùng
            userService.recordLoanEndedForUser(loanToReturn.getUserId(), loanToReturn.getRecordId());

            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã ghi nhận trả sách thành công.");

            // Làm mới giao diện
            loadActiveLoans();
            // Cập nhật lại thông tin sách nếu nó đang được hiển thị ở phần chọn sách
            if(selectedBookForLoan != null && selectedBookForLoan.getId().equals(loanToReturn.getBookId())){
                handleSearchBookAction(null);
            }
            // Vô hiệu hóa nút trả sách chung nếu không còn dòng nào được chọn hoặc dòng được chọn đã trả
            if(activeLoansTableView.getSelectionModel().getSelectedItem() == null ||
                    activeLoansTableView.getSelectionModel().getSelectedItem().getStatus() == LoanStatus.RETURNED) {
                returnBookButton.setDisable(true);
            }

        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật bản ghi trả sách.");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        // Cậu có thể thêm CSS cho Alert nếu muốn, giống như đã làm ở BookManagementController
        // alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/lms/quanlythuvien/css/styles.css").toExternalForm());
        alert.showAndWait();
    }
}
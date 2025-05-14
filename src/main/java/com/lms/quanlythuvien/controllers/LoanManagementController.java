package com.lms.quanlythuvien.controllers; // NHỚ ĐỔI CHO ĐÚNG PACKAGE CỦA CẬU

import com.lms.quanlythuvien.models.Book;
import com.lms.quanlythuvien.models.BorrowingRecord;
import com.lms.quanlythuvien.models.LoanStatus;
import com.lms.quanlythuvien.models.User;
import com.lms.quanlythuvien.services.BookManagementService;
import com.lms.quanlythuvien.services.BorrowingRecordService;
import com.lms.quanlythuvien.services.UserService;
// import com.lms.quanlythuvien.utils.PasswordUtils; // Không cần trực tiếp trong controller này

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
// import javafx.scene.control.Dialog; // Không thấy dùng Dialog<User> ở đây
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
// import javafx.scene.Node; // Không thấy dùng Node trực tiếp
// import javafx.scene.control.TextInputDialog; // Không thấy dùng
// import javafx.scene.control.ComboBox; // Không thấy dùng
// import javafx.scene.layout.GridPane; // Không thấy dùng
// import javafx.scene.layout.VBox; // Không thấy dùng VBox khai báo ở đây
// import javafx.application.Platform; // Không thấy dùng Platform trực tiếp
// import javafx.geometry.Insets; // Không thấy dùng Insets trực tiếp

import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

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
    @FXML private TableColumn<BorrowingRecord, Void> loanActionColumn;
    @FXML private Button returnBookButton;
    //</editor-fold>

    // Services - Sử dụng Singleton
    private BookManagementService bookManagementService;
    private UserService userService;
    private BorrowingRecordService borrowingRecordService;

    // Data for TableView
    private ObservableList<BorrowingRecord> observableLoanList;
    private FilteredList<BorrowingRecord> filteredLoanList;

    // Selected items for borrowing/returning
    private User selectedUserForLoan;
    private Book selectedBookForLoan;
    private BorrowingRecord selectedLoanForReturn;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("DEBUG_LMC_INIT: LoanManagementController initialize() started.");
        // LẤY INSTANCE SINGLETON CỦA CÁC SERVICE
        bookManagementService = BookManagementService.getInstance();
        userService = UserService.getInstance();
        borrowingRecordService = BorrowingRecordService.getInstance();
        System.out.println("DEBUG_LMC_INIT: Services retrieved/instantiated.");
        System.out.println("DEBUG_LMC_INIT: BookManagementService instance: " + bookManagementService.hashCode());
        System.out.println("DEBUG_LMC_INIT: UserService instance: " + userService.hashCode());
        System.out.println("DEBUG_LMC_INIT: BorrowingRecordService instance: " + borrowingRecordService.hashCode());

        // (Quan trọng) Gọi initializeSampleData cho BorrowingRecordService nếu muốn có dữ liệu mẫu
        // và đảm bảo nó chỉ chạy một lần khi cần (ví dụ, khi danh sách rỗng)
        // Điều này nên được gọi sau khi tất cả các service liên quan (BMS, US) đã được getInstance()
        // để đảm bảo chúng sẵn sàng nếu initializeSampleData() của BRS cần đến chúng.
        // borrowingRecordService.initializeSampleData(); // Bỏ comment nếu muốn dữ liệu mẫu ở đây

        observableLoanList = FXCollections.observableArrayList();

        setupActiveLoansTableColumns();

        activeLoansTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selectedLoanForReturn = newSelection;
            if (returnBookButton != null) { // Kiểm tra null cho FXML component
                returnBookButton.setDisable(newSelection == null || newSelection.getStatus() == LoanStatus.RETURNED);
            }
        });

        if (activeLoansFilterField != null) { // Kiểm tra null
            activeLoansFilterField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterActiveLoansTable(newValue);
            });
        }

        if (borrowBookButton != null) borrowBookButton.setDisable(true);
        if (returnBookButton != null) returnBookButton.setDisable(true);

        loadActiveLoans(); // Tải dữ liệu ban đầu cho bảng
        System.out.println("DEBUG_LMC_INIT: LoanManagementController initialize() finished.");
    }

    private void setupActiveLoansTableColumns() {
        loanIdColumn.setCellValueFactory(new PropertyValueFactory<>("recordId"));
        loanBorrowDateColumn.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        loanDueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        loanStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        loanBookTitleColumn.setCellValueFactory(cellData -> {
            BorrowingRecord record = cellData.getValue();
            if (record != null && bookManagementService != null) {
                Optional<Book> bookOpt = bookManagementService.findBookByIdInLibrary(record.getBookId());
                return new SimpleStringProperty(bookOpt.map(Book::getTitle).orElse("Không rõ sách"));
            }
            return new SimpleStringProperty("");
        });

        loanUserNameColumn.setCellValueFactory(cellData -> {
            BorrowingRecord record = cellData.getValue();
            if (record != null && userService != null) {
                Optional<User> userOpt = userService.findUserById(record.getUserId());
                return new SimpleStringProperty(userOpt.map(User::getUsername).orElse("Không rõ người dùng"));
            }
            return new SimpleStringProperty("");
        });

        Callback<TableColumn<BorrowingRecord, Void>, TableCell<BorrowingRecord, Void>> cellFactory = param -> {
            final TableCell<BorrowingRecord, Void> cell = new TableCell<>() {
                private final Button btnReturn = new Button("Trả Sách");
                {
                    btnReturn.getStyleClass().add("secondary-button");
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
                        if (record.getStatus() != LoanStatus.RETURNED) {
                            setGraphic(btnReturn);
                        } else {
                            setGraphic(null);
                        }
                    }
                }
            };
            return cell;
        };
        loanActionColumn.setCellFactory(cellFactory);
    }

    private void loadActiveLoans() {
        if (borrowingRecordService == null) {
            System.err.println("ERROR_LMC_LOAD_LOANS: borrowingRecordService is null!");
            return;
        }
        // Trước khi lấy, cập nhật trạng thái quá hạn
        borrowingRecordService.updateAllOverdueStatuses(LocalDate.now());
        List<BorrowingRecord> activeLoans = borrowingRecordService.getAllLoans().stream()
                .filter(loan -> loan.getStatus() == LoanStatus.ACTIVE || loan.getStatus() == LoanStatus.OVERDUE)
                .collect(Collectors.toList());

        observableLoanList.setAll(activeLoans);
        if (filteredLoanList == null) { // Khởi tạo filteredLoanList nếu nó null
            filteredLoanList = new FilteredList<>(observableLoanList, p -> true);
            activeLoansTableView.setItems(filteredLoanList);
        } else {
            // Nếu đã có, setPredicate(p -> true) để reset filter hoặc để nó tự cập nhật
            // việc set lại items có thể hiệu quả hơn để đảm bảo refresh
            activeLoansTableView.setItems(null); // Thử xóa items cũ
            activeLoansTableView.layout();
            activeLoansTableView.setItems(filteredLoanList);
        }
        activeLoansTableView.refresh();
        System.out.println("DEBUG_LMC_LOAD_LOANS: Active loans loaded. Count: " + observableLoanList.size());
    }

    private void filterActiveLoansTable(String searchText) {
        if (filteredLoanList == null) return;
        if (searchText == null || searchText.isEmpty()) {
            filteredLoanList.setPredicate(p -> true);
        } else {
            String lowerCaseFilter = searchText.toLowerCase();
            filteredLoanList.setPredicate(record -> {
                if (bookManagementService == null || userService == null) return false; // An toàn
                Optional<Book> bookOpt = bookManagementService.findBookByIdInLibrary(record.getBookId());
                Optional<User> userOpt = userService.findUserById(record.getUserId());

                if (record.getRecordId().toLowerCase().contains(lowerCaseFilter)) return true;
                if (bookOpt.isPresent() && bookOpt.get().getTitle().toLowerCase().contains(lowerCaseFilter)) return true;
                if (userOpt.isPresent() && userOpt.get().getUsername().toLowerCase().contains(lowerCaseFilter)) return true;
                return false;
            });
        }
    }

    @FXML
    void handleSearchUserAction(ActionEvent event) {
        if (userSearchField == null || userService == null || selectedUserInfoLabel == null) return;
        String searchTerm = userSearchField.getText().trim();
        if (searchTerm.isEmpty()) {
            selectedUserInfoLabel.setText("Người dùng: Vui lòng nhập ID hoặc Username");
            selectedUserForLoan = null;
            updateBorrowButtonState();
            return;
        }
        Optional<User> userOpt = userService.findUserById(searchTerm);
        if (userOpt.isEmpty()) { // Sửa: dùng isEmpty() thay vì !isPresent() để rõ ràng hơn
            userOpt = userService.findUserByUsername(searchTerm);
        }

        if (userOpt.isPresent()) {
            selectedUserForLoan = userOpt.get();
            selectedUserInfoLabel.setText("Người dùng: " + selectedUserForLoan.getUsername() + " (ID: " + selectedUserForLoan.getUserId() + ")");
        } else {
            selectedUserForLoan = null;
            selectedUserInfoLabel.setText("Người dùng: Không tìm thấy '" + searchTerm + "'");
        }
        updateBorrowButtonState();
    }

    @FXML
    void handleSearchBookAction(ActionEvent event) {
        if (bookSearchField == null || bookManagementService == null || selectedBookInfoLabel == null) return;
        String searchTerm = bookSearchField.getText().trim();
        if (searchTerm.isEmpty()) {
            selectedBookInfoLabel.setText("Sách: Vui lòng nhập ID, ISBN, hoặc Tiêu đề");
            selectedBookForLoan = null;
            updateBorrowButtonState();
            return;
        }
        Optional<Book> bookOpt = bookManagementService.findBookByIdInLibrary(searchTerm);
        // TODO: Có thể mở rộng tìm kiếm theo ISBN/Title ở đây nếu cần
        // if (bookOpt.isEmpty()) bookOpt = bookManagementService.findBookByIsbn13InLibrary(searchTerm);
        // if (bookOpt.isEmpty()) bookOpt = bookManagementService.searchBooks(searchTerm, "TITLE").stream().findFirst();


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
            showAlert(Alert.AlertType.WARNING, "Hết sách", "Sách '" + selectedBookForLoan.getTitle() + "' hiện đã hết, không thể cho mượn.");
            return;
        }
        if (bookManagementService == null || borrowingRecordService == null || userService == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi Hệ thống", "Một hoặc nhiều service chưa được khởi tạo.");
            return;
        }

        // Kiểm tra xem user này có đang mượn cuốn sách này mà chưa trả không
        boolean alreadyBorrowed = borrowingRecordService.getActiveLoansByUserId(selectedUserForLoan.getUserId())
                .stream()
                .anyMatch(loan -> loan.getBookId().equals(selectedBookForLoan.getId()));
        if(alreadyBorrowed){
            showAlert(Alert.AlertType.WARNING, "Sách đã được mượn", "Người dùng '" + selectedUserForLoan.getUsername() + "' đang mượn cuốn sách này và chưa trả.");
            return;
        }


        LocalDate borrowDate = LocalDate.now();
        LocalDate dueDate = borrowDate.plus(7, ChronoUnit.DAYS); // Mượn trong 7 ngày

        boolean bookAvailabilityUpdated = bookManagementService.handleBookBorrowed(selectedBookForLoan.getId());

        if (bookAvailabilityUpdated) {
            BorrowingRecord newLoan = borrowingRecordService.createLoan(
                    selectedBookForLoan.getId(),
                    selectedUserForLoan.getUserId(),
                    borrowDate,
                    dueDate
            );

            if (newLoan != null) {
                userService.recordNewLoanForUser(selectedUserForLoan.getUserId(), newLoan.getRecordId());
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Đã cho mượn sách: '" + selectedBookForLoan.getTitle() +
                                "'\nCho người dùng: '" + selectedUserForLoan.getUsername() + "'.\n" +
                                "Hạn trả: " + dueDate.toString());

                loadActiveLoans();
                // Cập nhật lại thông tin sách đã chọn (để thấy availableQuantity giảm)
                // và có thể xóa lựa chọn để tránh mượn nhầm lần nữa
                String currentBookSearchTerm = bookSearchField.getText();
                handleSearchBookAction(null); // Gọi để refresh label
                bookSearchField.setText(currentBookSearchTerm); // Đặt lại text tìm kiếm nếu muốn
                if (currentBookSearchTerm != null && !currentBookSearchTerm.isEmpty()) handleSearchBookAction(null); // Trigger lại search nếu có text

                // Cân nhắc reset selectedUserForLoan và selectedBookForLoan
                // selectedUserForLoan = null;
                // selectedBookForLoan = null;
                // selectedUserInfoLabel.setText("Người dùng: (Chưa chọn)");
                // selectedBookInfoLabel.setText("Sách: (Chưa chọn)");
                updateBorrowButtonState();

            } else {
                bookManagementService.handleBookReturned(selectedBookForLoan.getId()); // Hoàn tác
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tạo bản ghi mượn sách. Sách có thể đã được người này mượn và chưa trả.");
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật số lượng sách hoặc sách đã hết (kiểm tra lại).");
        }
    }

    private void handleReturnBookFromTableRowAction(BorrowingRecord recordToReturn) {
        if (recordToReturn == null || recordToReturn.getStatus() == LoanStatus.RETURNED) {
            showAlert(Alert.AlertType.WARNING, "Không hợp lệ", "Lượt mượn này không hợp lệ hoặc đã được trả.");
            return;
        }
        confirmAndPerformReturn(recordToReturn);
    }

    @FXML
    void handleReturnBookAction(ActionEvent event) {
        if (selectedLoanForReturn == null || selectedLoanForReturn.getStatus() == LoanStatus.RETURNED) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn lượt mượn", "Vui lòng chọn một lượt mượn đang hoạt động từ bảng để trả.");
            return;
        }
        confirmAndPerformReturn(selectedLoanForReturn);
    }

    private void confirmAndPerformReturn(BorrowingRecord loanToReturn) {
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Xác nhận Trả sách");
        String bookTitle = bookManagementService.findBookByIdInLibrary(loanToReturn.getBookId())
                .map(Book::getTitle).orElse("ID: " + loanToReturn.getBookId());
        String username = userService.findUserById(loanToReturn.getUserId())
                .map(User::getUsername).orElse("ID: " + loanToReturn.getUserId());

        confirmationDialog.setHeaderText("Trả sách: '" + bookTitle + "' bởi người dùng: '" + username + "'?");
        confirmationDialog.setContentText("Bạn có chắc chắn muốn đánh dấu lượt mượn này là đã trả không?");

        Optional<ButtonType> result = confirmationDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            performReturnBookLogic(loanToReturn);
        }
    }

    private void performReturnBookLogic(BorrowingRecord loanToReturn) {
        if (borrowingRecordService == null || bookManagementService == null || userService == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi Hệ thống", "Một hoặc nhiều service chưa được khởi tạo để thực hiện trả sách.");
            return;
        }
        boolean recordUpdated = borrowingRecordService.recordBookReturn(loanToReturn.getRecordId(), LocalDate.now());

        if (recordUpdated) {
            bookManagementService.handleBookReturned(loanToReturn.getBookId());
            userService.recordLoanEndedForUser(loanToReturn.getUserId(), loanToReturn.getRecordId());
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã ghi nhận trả sách thành công.");

            loadActiveLoans();
            // Cập nhật lại thông tin sách nếu nó đang được hiển thị ở phần chọn sách
            if(selectedBookForLoan != null && selectedBookForLoan.getId().equals(loanToReturn.getBookId())){
                handleSearchBookAction(null); // Refresh book info label
            }
            // Cập nhật trạng thái nút trả sách chung
            if (activeLoansTableView.getSelectionModel().getSelectedItem() == null ||
                    (selectedLoanForReturn != null && selectedLoanForReturn.getRecordId().equals(loanToReturn.getRecordId()) &&
                            activeLoansTableView.getSelectionModel().getSelectedItem().getStatus() == LoanStatus.RETURNED)) {
                if(returnBookButton != null) returnBookButton.setDisable(true);
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật bản ghi trả sách. Sách có thể đã được trả trước đó.");
        }
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
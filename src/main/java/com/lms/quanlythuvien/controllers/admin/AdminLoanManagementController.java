package com.lms.quanlythuvien.controllers.admin;

import com.lms.quanlythuvien.MainApp; // Có thể cần cho việc lấy resource
import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.transaction.BorrowingRecord;
import com.lms.quanlythuvien.models.transaction.BorrowingRequest;
import com.lms.quanlythuvien.models.transaction.LoanStatus;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.services.transaction.BorrowingRecordService;
import com.lms.quanlythuvien.services.transaction.BorrowingRequestService;
import com.lms.quanlythuvien.services.user.UserService;
import com.lms.quanlythuvien.controllers.admin.dialogs.QRScannerDialogController; // Giả sử bạn đã tạo controller này

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox; // fx:id="loanManagementRoot"
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class AdminLoanManagementController implements Initializable {

    // Root VBox của toàn bộ view
    @FXML private VBox loanManagementRoot;

    // TabPane và các Tab (fx:id phải khớp với FXML)
    @FXML private TabPane loanManagementTabPane;
    @FXML private Tab manualLoanTab;
    @FXML private Tab pendingRequestsTab;
    @FXML private Tab activeLoansTab;

    // --- Thành phần UI cho Tab 1: Cho Mượn Thủ Công ---
    @FXML private TextField userSearchField;
    @FXML private TextField bookSearchField;
    @FXML private Label selectedUserInfoLabel;
    @FXML private Label selectedBookInfoLabel;
    @FXML private Button borrowBookButton;
    @FXML private Button searchUserButton; // Đảm bảo FXML có fx:id này
    @FXML private Button searchBookButton; // Đảm bảo FXML có fx:id này

    // --- Thành phần UI cho Tab 2: Duyệt Yêu Cầu ---
    @FXML private TextField pendingRequestsFilterField;
    @FXML private TableView<BorrowingRequest> pendingRequestsTableView;
    @FXML private TableColumn<BorrowingRequest, String> reqIdColumn;
    @FXML private TableColumn<BorrowingRequest, String> reqBookTitleColumn;
    @FXML private TableColumn<BorrowingRequest, String> reqUserNameColumn;
    @FXML private TableColumn<BorrowingRequest, String> reqDateColumn;
    @FXML private TableColumn<BorrowingRequest, String> reqStatusColumn;
    @FXML private TableColumn<BorrowingRequest, Void> reqActionColumn;

    // --- Thành phần UI cho Tab 3: Quản Lý Mượn/Trả Hiện Tại ---
    @FXML private TextField activeLoansFilterField;
    @FXML private TableView<BorrowingRecord> activeLoansTableView;
    @FXML private TableColumn<BorrowingRecord, Integer> loanIdColumn;
    @FXML private TableColumn<BorrowingRecord, String> loanBookTitleColumn;
    @FXML private TableColumn<BorrowingRecord, String> loanUserNameColumn;
    @FXML private TableColumn<BorrowingRecord, String> loanBorrowDateColumn;
    @FXML private TableColumn<BorrowingRecord, String> loanDueDateColumn;
    @FXML private TableColumn<BorrowingRecord, String> loanStatusColumn;
    @FXML private TableColumn<BorrowingRecord, Void> loanActionColumn;


    private BookManagementService bookManagementService;
    private UserService userService;
    private BorrowingRecordService borrowingRecordService;
    private BorrowingRequestService borrowingRequestService;

    private ObservableList<BorrowingRecord> observableLoanList;
    private FilteredList<BorrowingRecord> filteredLoanList;
    private ObservableList<BorrowingRequest> observableRequestList;
    private FilteredList<BorrowingRequest> filteredRequestList;

    private User selectedUserForLoan;
    private Book selectedBookForLoan;

    // Cache để giảm thiểu truy vấn DB lặp lại
    private Map<Integer, String> bookInternalIdToTitleCache = new HashMap<>();
    private Map<String, String> bookIsbnToTitleCache = new HashMap<>();
    private Map<String, String> userIdToNameCache = new HashMap<>(); // Lưu trữ tên đầy đủ hoặc username

    private AdminDashboardController dashboardController;
    private final DateTimeFormatter displayDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bookManagementService = BookManagementService.getInstance();
        userService = UserService.getInstance();
        borrowingRecordService = BorrowingRecordService.getInstance();
        borrowingRequestService = BorrowingRequestService.getInstance();

        // Tab 1: Cho Mượn Thủ Công
        updateBorrowButtonState();

        // Tab 2: Duyệt Yêu Cầu
        observableRequestList = FXCollections.observableArrayList();
        setupPendingRequestsTableColumns();
        filteredRequestList = new FilteredList<>(observableRequestList, p -> true);
        pendingRequestsTableView.setItems(filteredRequestList);
        pendingRequestsTableView.setPlaceholder(new Label("Không có yêu cầu mượn sách nào."));
        if (pendingRequestsFilterField != null) {
            pendingRequestsFilterField.textProperty().addListener((obs, ov, nv) -> filterPendingRequestsTable(nv));
        }

        // Tab 3: Quản Lý Mượn/Trả Hiện Tại
        observableLoanList = FXCollections.observableArrayList();
        setupActiveLoansTableColumns();
        filteredLoanList = new FilteredList<>(observableLoanList, p -> true);
        activeLoansTableView.setItems(filteredLoanList);
        activeLoansTableView.setPlaceholder(new Label("Không có lượt mượn nào đang hoạt động hoặc quá hạn."));
        if(activeLoansFilterField != null) {
            activeLoansFilterField.textProperty().addListener((obs, ov, nv) -> filterActiveLoansTable(nv));
        }

        // Listener cho TabPane
        if (loanManagementTabPane != null) {
            loanManagementTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab != null) {
                    if (newTab == pendingRequestsTab) {
                        loadAndCachePendingRequests();
                    } else if (newTab == activeLoansTab) {
                        loadAndCacheActiveLoans();
                    }
                }
            });
            // Load dữ liệu cho tab được chọn mặc định nếu cần
            Tab initialTab = loanManagementTabPane.getSelectionModel().getSelectedItem();
            if (initialTab == pendingRequestsTab) {
                Platform.runLater(this::loadAndCachePendingRequests);
            } else if (initialTab == activeLoansTab) {
                Platform.runLater(this::loadAndCacheActiveLoans);
            }
        }
    }

    // --- TAB 1: CHO MƯỢN THỦ CÔNG ---
    @FXML
    void handleSearchUserAction(ActionEvent event) {
        String searchTerm = userSearchField.getText().trim();
        if (searchTerm.isEmpty()) {
            selectedUserInfoLabel.setText("Người dùng: (Nhập ID/Username)");
            selectedUserForLoan = null;
            updateBorrowButtonState();
            return;
        }
        Optional<User> userOpt = userService.findUserById(searchTerm);
        if (userOpt.isEmpty()) userOpt = userService.findUserByUsername(searchTerm);

        if (userOpt.isPresent()) {
            selectedUserForLoan = userOpt.get();
            selectedUserInfoLabel.setText("User: " + selectedUserForLoan.getFullNameOrDefault(selectedUserForLoan.getUsername()) + " (ID: " + selectedUserForLoan.getUserId() + ")");
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
            selectedBookInfoLabel.setText("Sách: (Nhập ISBN/Tiêu đề)");
            selectedBookForLoan = null;
            updateBorrowButtonState();
            return;
        }
        Optional<Book> bookOpt = bookManagementService.findBookByIsbn13InLibrary(searchTerm);
        if (bookOpt.isEmpty()) { // Thử tìm theo tiêu đề nếu không thấy bằng ISBN
            List<Book> foundBooks = bookManagementService.searchBooksInLibrary(searchTerm, "TITLE");
            if (!foundBooks.isEmpty()) bookOpt = Optional.of(foundBooks.get(0)); // Lấy cuốn đầu tiên
        }

        if (bookOpt.isPresent()) {
            selectedBookForLoan = bookOpt.get();
            String availabilityText = (selectedBookForLoan.getAvailableQuantity() > 0) ?
                    " (Có sẵn: " + selectedBookForLoan.getAvailableQuantity() + ")" : " - Đã hết sách";
            selectedBookInfoLabel.setText("Sách: " + selectedBookForLoan.getTitleOrDefault("Không rõ") + availabilityText);
        } else {
            selectedBookForLoan = null;
            selectedBookInfoLabel.setText("Sách: Không tìm thấy '" + searchTerm + "'");
        }
        updateBorrowButtonState();
    }

    private void updateBorrowButtonState() {
        boolean disable = !(selectedUserForLoan != null && selectedBookForLoan != null &&
                selectedBookForLoan.getIsbn13() != null && !selectedBookForLoan.getIsbn13().isEmpty() &&
                selectedBookForLoan.getAvailableQuantity() > 0);
        if (borrowBookButton != null) borrowBookButton.setDisable(disable);
    }

    @FXML
    void handleBorrowBookAction(ActionEvent event) {
        if (selectedUserForLoan == null || selectedBookForLoan == null || selectedBookForLoan.getIsbn13() == null || selectedBookForLoan.getIsbn13().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng chọn người dùng và sách hợp lệ (có ISBN).");
            return;
        }
        if (selectedBookForLoan.getAvailableQuantity() <= 0) {
            showAlert(Alert.AlertType.WARNING, "Hết sách", "Sách '" + selectedBookForLoan.getTitleOrDefault("Không rõ") + "' đã hết.");
            return;
        }

        LocalDate borrowDate = LocalDate.now();
        LocalDate dueDate = borrowDate.plusDays(7); // Mặc định 7 ngày

        // Chuẩn bị dữ liệu QR cho Bill
        String qrCodeDataForBill = selectedBookForLoan.getQrCodeData();
        if (qrCodeDataForBill == null || qrCodeDataForBill.trim().isEmpty()) {
            qrCodeDataForBill = selectedBookForLoan.getId(); // ID chính (ISBN13)
            if (qrCodeDataForBill == null || qrCodeDataForBill.trim().isEmpty()){
                qrCodeDataForBill = "BOOK_" +selectedBookForLoan.getInternalId(); // Fallback: BOOK_InternalID
            }
        }
        Book bookForBillDisplay = new Book(
                selectedBookForLoan.getId(), selectedBookForLoan.getTitle(), selectedBookForLoan.getAuthors(),
                selectedBookForLoan.getPublisher(), selectedBookForLoan.getPublishedDate(), selectedBookForLoan.getDescription(),
                selectedBookForLoan.getCategories(), selectedBookForLoan.getThumbnailUrl(), selectedBookForLoan.getInfoLink(),
                selectedBookForLoan.getIsbn10(), selectedBookForLoan.getPageCount(), selectedBookForLoan.getAverageRating(),
                selectedBookForLoan.getRatingsCount(), selectedBookForLoan.getTotalQuantity()
        );
        bookForBillDisplay.setInternalId(selectedBookForLoan.getInternalId());
        bookForBillDisplay.setQrCodeData(qrCodeDataForBill); // Gán QR đã chuẩn bị

        showLoanBillDialog(bookForBillDisplay, selectedUserForLoan, borrowDate, dueDate, event);

        // Thực hiện tạo lượt mượn (sau khi bill đã được hiển thị và có thể đã đóng)
        Optional<BorrowingRecord> newLoanOpt = borrowingRecordService.createLoan(
                selectedBookForLoan.getIsbn13(), selectedUserForLoan.getUserId(), borrowDate, dueDate);

        if (newLoanOpt.isPresent()) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    "Đã cho mượn sách: '" + selectedBookForLoan.getTitleOrDefault("Không rõ") + "'\nCho: '" + selectedUserForLoan.getUsername() + "'.\nHạn trả: " + dueDate.format(displayDateFormatter));

            // Tải lại danh sách ở Tab 3 nếu nó đang được hiển thị hoặc là tab active
            if (loanManagementTabPane.getSelectionModel().getSelectedItem() == activeLoansTab || (activeLoansTab != null && activeLoansTab.isSelected()) ) {
                loadAndCacheActiveLoans();
            }

            // Cập nhật lại thông tin sách vừa cho mượn (nếu nó vẫn đang được chọn)
            if (selectedBookForLoan != null) {
                Optional<Book> refreshedBookOpt = bookManagementService.findBookByIsbn13InLibrary(selectedBookForLoan.getIsbn13());
                if(refreshedBookOpt.isPresent()){
                    selectedBookForLoan = refreshedBookOpt.get(); // Cập nhật selectedBookForLoan
                    String availabilityText = (selectedBookForLoan.getAvailableQuantity() > 0) ?
                            " (Có sẵn: " + selectedBookForLoan.getAvailableQuantity() + ")" : " - Đã hết sách";
                    selectedBookInfoLabel.setText("Sách: " + selectedBookForLoan.getTitleOrDefault("Không rõ") + availabilityText);
                } else {
                    selectedBookInfoLabel.setText("Sách: " + selectedBookForLoan.getTitleOrDefault("Không rõ") + " (lỗi cập nhật SL)");
                }
            }
            clearManualLoanFields(); // Xóa các trường sau khi thành công
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi Tạo Lượt Mượn", "Không thể tạo bản ghi mượn sách. Sách có thể đã hết, người dùng đang mượn, hoặc lỗi DB (kiểm tra console).");
        }
    }

    private void clearManualLoanFields() {
        userSearchField.clear();
        bookSearchField.clear();
        selectedUserForLoan = null;
        selectedBookForLoan = null;
        selectedUserInfoLabel.setText("User: (Chưa chọn)");
        selectedBookInfoLabel.setText("Sách: (Chưa chọn)");
        updateBorrowButtonState();
    }


    // --- TAB 2: DUYỆT YÊU CẦU ---
    private void setupPendingRequestsTableColumns() {
        reqIdColumn.setCellValueFactory(new PropertyValueFactory<>("requestId"));
        reqDateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getRequestDate() != null ? cellData.getValue().getRequestDate().format(displayDateFormatter) : "N/A"
        ));
        reqStatusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getStatus() != null ? cellData.getValue().getStatus().getDisplayName() : "N/A"
        ));

        reqBookTitleColumn.setCellValueFactory(cellData -> {
            BorrowingRequest request = cellData.getValue();
            return new SimpleStringProperty(bookIsbnToTitleCache.getOrDefault(request.getBookIsbn13(), "ISBN: " + request.getBookIsbn13()));
        });

        reqUserNameColumn.setCellValueFactory(cellData -> {
            BorrowingRequest request = cellData.getValue();
            return new SimpleStringProperty(userIdToNameCache.getOrDefault(request.getUserId(), "User ID: " + request.getUserId()));
        });

        Callback<TableColumn<BorrowingRequest, Void>, TableCell<BorrowingRequest, Void>> actionCellFactoryRequests = param -> new TableCell<>() {
            private final Button btnApprove = new Button("Duyệt");
            private final Button btnReject = new Button("Từ chối");
            private final HBox pane = new HBox(5, btnApprove, btnReject);
            {
                btnApprove.getStyleClass().add("success-button-small");
                btnReject.getStyleClass().add("danger-button-small");
                pane.setAlignment(Pos.CENTER);

                btnApprove.setOnAction(event -> {
                    BorrowingRequest request = getTableView().getItems().get(getIndex());
                    if (request != null && request.getStatus() == BorrowingRequest.RequestStatus.PENDING) {
                        handleApproveRequest(request);
                    }
                });
                btnReject.setOnAction(event -> {
                    BorrowingRequest request = getTableView().getItems().get(getIndex());
                    if (request != null && request.getStatus() == BorrowingRequest.RequestStatus.PENDING) {
                        handleRejectRequest(request);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    BorrowingRequest request = getTableView().getItems().get(getIndex());
                    if (request != null && request.getStatus() == BorrowingRequest.RequestStatus.PENDING) {
                        setGraphic(pane);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        };
        reqActionColumn.setCellFactory(actionCellFactoryRequests);
    }

    private void loadAndCachePendingRequests() {
        List<BorrowingRequest> pending = borrowingRequestService.getRequestsByStatus(BorrowingRequest.RequestStatus.PENDING);

        // Làm mới cache liên quan
        // bookIsbnToTitleCache.clear(); // Cân nhắc nếu cache này lớn và dùng chung
        // userIdToNameCache.clear();

        if (!pending.isEmpty()) {
            Set<String> bookIsbns = pending.stream().map(BorrowingRequest::getBookIsbn13).filter(Objects::nonNull).collect(Collectors.toSet());
            if (!bookIsbns.isEmpty()) {
                Map<String, Book> booksData = bookManagementService.getBooksByIsbns(bookIsbns);
                booksData.forEach((isbn, book) -> bookIsbnToTitleCache.put(isbn, book.getTitleOrDefault("ISBN: " + isbn)));
            }

            Set<String> userIds = pending.stream().map(BorrowingRequest::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
            if (!userIds.isEmpty()) {
                Map<String, User> users = userService.getUsersMapByIds(userIds);
                users.forEach((id, user) -> userIdToNameCache.put(id, user.getFullNameOrDefault(user.getUsername())));
            }
        }
        observableRequestList.setAll(pending);
        if(pending.isEmpty()){
            pendingRequestsTableView.setPlaceholder(new Label("Không có yêu cầu mượn sách nào đang chờ duyệt."));
        } else {
            pendingRequestsTableView.setPlaceholder(null);
        }
        pendingRequestsTableView.refresh(); // Giúp cập nhật hiển thị cell factory nếu cần
    }

    private void filterPendingRequestsTable(String searchText) {
        if (filteredRequestList == null) return;
        final String lowerCaseFilter = (searchText == null) ? "" : searchText.toLowerCase().trim();
        filteredRequestList.setPredicate(request -> {
            if (lowerCaseFilter.isEmpty()) return true;
            if (request.getRequestId() != null && request.getRequestId().toLowerCase().contains(lowerCaseFilter)) return true;

            String bookTitle = bookIsbnToTitleCache.get(request.getBookIsbn13());
            if (bookTitle != null && bookTitle.toLowerCase().contains(lowerCaseFilter)) return true;
            if (request.getBookIsbn13() != null && request.getBookIsbn13().toLowerCase().contains(lowerCaseFilter)) return true;

            String userName = userIdToNameCache.get(request.getUserId());
            if (userName != null && userName.toLowerCase().contains(lowerCaseFilter)) return true;
            if (request.getUserId() != null && request.getUserId().toLowerCase().contains(lowerCaseFilter)) return true;

            if(request.getStatus() != null && request.getStatus().getDisplayName().toLowerCase().contains(lowerCaseFilter)) return true;

            return false;
        });
    }

    private void handleApproveRequest(BorrowingRequest request) {
        TextInputDialog dueDateDialog = new TextInputDialog(LocalDate.now().plusDays(7).format(displayDateFormatter));
        dueDateDialog.setTitle("Duyệt Yêu Cầu Mượn Sách");
        String bookTitle = bookIsbnToTitleCache.getOrDefault(request.getBookIsbn13(), request.getBookIsbn13());
        String userName = userIdToNameCache.getOrDefault(request.getUserId(), request.getUserId());
        dueDateDialog.setHeaderText("Duyệt yêu cầu cho sách: " + bookTitle + "\nBởi người dùng: " + userName);
        dueDateDialog.setContentText("Nhập ngày hẹn trả (dd/MM/yyyy):");
        applyDialogStyles(dueDateDialog.getDialogPane());

        Optional<String> result = dueDateDialog.showAndWait();
        result.ifPresent(dateStr -> {
            try {
                LocalDate dueDateForLoan = LocalDate.parse(dateStr, displayDateFormatter);
                if (dueDateForLoan.isBefore(LocalDate.now())) {
                    showAlert(Alert.AlertType.ERROR, "Ngày không hợp lệ", "Ngày hẹn trả không được là ngày trong quá khứ.");
                    return;
                }
                boolean success = borrowingRequestService.approveRequestAndCreateLoan(request.getRequestId(), "Admin duyệt", dueDateForLoan);
                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã duyệt yêu cầu và tạo lượt mượn.");
                    loadAndCachePendingRequests();
                    if (loanManagementTabPane.getSelectionModel().getSelectedItem() == activeLoansTab || (activeLoansTab != null && activeLoansTab.isSelected())) {
                        loadAndCacheActiveLoans();
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể duyệt yêu cầu. Sách có thể đã hết hoặc có lỗi xảy ra.");
                }
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi Định Dạng Ngày", "Ngày nhập không đúng định dạng dd/MM/yyyy.");
            }
        });
    }

    private void handleRejectRequest(BorrowingRequest request) {
        TextInputDialog reasonDialog = new TextInputDialog();
        reasonDialog.setTitle("Từ Chối Yêu Cầu Mượn Sách");
        reasonDialog.setHeaderText("Nhập lý do từ chối yêu cầu ID: " + request.getRequestId());
        reasonDialog.setContentText("Lý do từ chối:");
        applyDialogStyles(reasonDialog.getDialogPane());

        Optional<String> result = reasonDialog.showAndWait();
        result.ifPresent(reason -> {
            boolean success = borrowingRequestService.rejectRequest(request.getRequestId(), reason.trim().isEmpty() ? "Không có lý do cụ thể." : reason.trim());
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã từ chối yêu cầu.");
                loadAndCachePendingRequests();
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể từ chối yêu cầu.");
            }
        });
    }

    // --- TAB 3: QUẢN LÝ LƯỢT MƯỢN HIỆN TẠI ---
    private void setupActiveLoansTableColumns() {
        loanIdColumn.setCellValueFactory(new PropertyValueFactory<>("recordId"));
        loanBorrowDateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getBorrowDate() != null ? cellData.getValue().getBorrowDate().format(displayDateFormatter) : "N/A"
        ));
        loanDueDateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getDueDate() != null ? cellData.getValue().getDueDate().format(displayDateFormatter) : "N/A"
        ));
        loanStatusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getStatus() != null ? cellData.getValue().getStatus().getDisplayName() : "N/A"
        ));

        loanBookTitleColumn.setCellValueFactory(cellData -> {
            BorrowingRecord record = cellData.getValue();
            return new SimpleStringProperty(bookInternalIdToTitleCache.getOrDefault(record.getBookInternalId(), "Sách ID: " + record.getBookInternalId()));
        });

        loanUserNameColumn.setCellValueFactory(cellData -> {
            BorrowingRecord record = cellData.getValue();
            return new SimpleStringProperty(userIdToNameCache.getOrDefault(record.getUserId(), "User ID: " + record.getUserId()));
        });

        Callback<TableColumn<BorrowingRecord, Void>, TableCell<BorrowingRecord, Void>> cellFactoryForActions = param -> new TableCell<>() {
            private final Button btnReturnWithQR = new Button("Trả (QR)");
            private final Button btnViewBill = new Button("Xem Bill");
            private final HBox actionPane = new HBox(5, btnReturnWithQR, btnViewBill);
            {
                btnReturnWithQR.getStyleClass().add("secondary-button-small");
                btnViewBill.getStyleClass().add("info-button-small");
                actionPane.setAlignment(Pos.CENTER);

                btnReturnWithQR.setOnAction(event -> {
                    BorrowingRecord record = getTableView().getItems().get(getIndex());
                    if (record != null && record.getStatus() != LoanStatus.RETURNED) {
                        handleReturnBookWithQRScannerAction(record);
                    } else {
                        showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Lượt mượn này đã được xử lý trả.");
                    }
                });
                btnViewBill.setOnAction(event -> {
                    BorrowingRecord record = getTableView().getItems().get(getIndex());
                    if (record != null) {
                        handleViewExistingLoanBillAction(record, event);
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    BorrowingRecord record = getTableView().getItems().get(getIndex());
                    if (record != null) {
                        btnReturnWithQR.setDisable(record.getStatus() == LoanStatus.RETURNED);
                        setGraphic(actionPane);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        };
        loanActionColumn.setCellFactory(cellFactoryForActions);
    }

    private void loadAndCacheActiveLoans() {
        List<BorrowingRecord> activeAndOverdueLoans = borrowingRecordService.getAllActiveLoans();
        // Clear cache cục bộ cho tab này (hoặc cache thông minh hơn)
        // bookInternalIdToTitleCache.clear();
        // userIdToNameCache.clear();

        if (!activeAndOverdueLoans.isEmpty()) {
            Set<Integer> bookIds = activeAndOverdueLoans.stream().map(BorrowingRecord::getBookInternalId).collect(Collectors.toSet());
            if (!bookIds.isEmpty()) {
                List<Book> books = bookManagementService.getBooksByInternalIds(bookIds);
                books.forEach(book -> bookInternalIdToTitleCache.put(book.getInternalId(), book.getTitleOrDefault("Không rõ")));
            }

            Set<String> userIds = activeAndOverdueLoans.stream().map(BorrowingRecord::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
            if (!userIds.isEmpty()) {
                Map<String, User> users = userService.getUsersMapByIds(userIds);
                users.forEach((id, user) -> userIdToNameCache.put(id, user.getFullNameOrDefault(user.getUsername())));
            }
        }
        observableLoanList.setAll(activeAndOverdueLoans);
        if(activeAndOverdueLoans.isEmpty()){
            activeLoansTableView.setPlaceholder(new Label("Không có lượt mượn nào đang hoạt động hoặc quá hạn."));
        } else {
            activeLoansTableView.setPlaceholder(null);
        }
        activeLoansTableView.refresh();
    }

    private void filterActiveLoansTable(String searchText) {
        if (filteredLoanList == null) return;
        final String lowerCaseFilter = (searchText == null) ? "" : searchText.toLowerCase().trim();

        filteredLoanList.setPredicate(record -> {
            if (lowerCaseFilter.isEmpty()) return true;
            if (String.valueOf(record.getRecordId()).contains(lowerCaseFilter)) return true;

            String bookTitle = bookInternalIdToTitleCache.get(record.getBookInternalId());
            if (bookTitle != null && bookTitle.toLowerCase().contains(lowerCaseFilter)) return true;

            String userName = userIdToNameCache.get(record.getUserId());
            if (userName != null && userName.toLowerCase().contains(lowerCaseFilter)) return true;

            if (record.getUserId() != null && record.getUserId().toLowerCase().contains(lowerCaseFilter)) return true;
            if (record.getStatus() != null && record.getStatus().getDisplayName().toLowerCase().contains(lowerCaseFilter)) return true;

            return false;
        });
    }

    private void handleViewExistingLoanBillAction(BorrowingRecord record, ActionEvent event) {
        if (record == null) return;
        Optional<Book> bookOpt = bookManagementService.findBookByInternalId(record.getBookInternalId());
        Optional<User> userOpt = userService.findUserById(record.getUserId());

        if (bookOpt.isPresent() && userOpt.isPresent()) {
            Book book = bookOpt.get();
            User user = userOpt.get();

            String qrCodeDataForBill = book.getQrCodeData();
            if (qrCodeDataForBill == null || qrCodeDataForBill.trim().isEmpty()) {
                qrCodeDataForBill = book.getId();
                if (qrCodeDataForBill == null || qrCodeDataForBill.trim().isEmpty()){
                    qrCodeDataForBill = "BOOK_" + book.getInternalId();
                }
            }
            Book bookForBillDisplay = new Book( // Tạo bản sao để set QR code riêng cho bill
                    book.getId(), book.getTitle(), book.getAuthors(), book.getPublisher(),
                    book.getPublishedDate(), book.getDescription(), book.getCategories(),
                    book.getThumbnailUrl(), book.getInfoLink(), book.getIsbn10(),
                    book.getPageCount(), book.getAverageRating(), book.getRatingsCount(),
                    book.getTotalQuantity()
            );
            bookForBillDisplay.setInternalId(book.getInternalId());
            bookForBillDisplay.setQrCodeData(qrCodeDataForBill);

            showLoanBillDialog(bookForBillDisplay, user, record.getBorrowDate(), record.getDueDate(), event);
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi Dữ Liệu", "Không tìm thấy thông tin sách hoặc người dùng cho lượt mượn này.");
        }
    }

    private void showLoanBillDialog(Book book, User user, LocalDate borrowDate, LocalDate dueDate, ActionEvent eventForOwner) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/quanlythuvien/fxml/admin/LoanBillView.fxml"));
            Parent billRoot = loader.load();
            LoanBillController billController = loader.getController();
            billController.setBillData(book, user, borrowDate, dueDate);

            Stage billStage = new Stage();
            billStage.setTitle("Chi Tiết Phiếu Mượn");
            billStage.initModality(Modality.WINDOW_MODAL);
            if (eventForOwner != null && eventForOwner.getSource() instanceof Node) {
                billStage.initOwner(((Node) eventForOwner.getSource()).getScene().getWindow());
            } else if (loanManagementRoot != null && loanManagementRoot.getScene() != null) {
                billStage.initOwner(loanManagementRoot.getScene().getWindow());
            }

            Scene billScene = new Scene(billRoot);
            applyDialogStyles(billStage); // Áp dụng CSS cho Stage
            billStage.setScene(billScene);
            billStage.show(); // Dùng show() vì đây là xem lại, không cần block
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi Hiển Thị Bill", "Không thể hiển thị phiếu mượn: " + e.getMessage());
        }
    }

    // --- LOGIC CAMERA QR SCANNER ---
    private void handleReturnBookWithQRScannerAction(BorrowingRecord recordToReturn) {
        if (recordToReturn == null) return;

        Optional<Book> bookOpt = bookManagementService.findBookByInternalId(recordToReturn.getBookInternalId());
        if (bookOpt.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi Sách", "Không tìm thấy thông tin sách cho lượt mượn này.");
            return;
        }
        Book loanedBook = bookOpt.get();
        String expectedQrData = loanedBook.getQrCodeData();
        if (expectedQrData == null || expectedQrData.trim().isEmpty()) {
            expectedQrData = loanedBook.getId(); // ISBN13 hoặc CustomID
            if (expectedQrData == null || expectedQrData.trim().isEmpty()) {
                expectedQrData = "BOOK_" + loanedBook.getInternalId(); // Fallback
            }
            System.out.println("WARN_LMC_QR: Book " + loanedBook.getTitleOrDefault("N/A") + " has no specific QR data, expecting generated: " + expectedQrData);
        }

        // Gọi dialog quét QR
        Optional<String> scannedResult = showActualQRScannerDialog(loanedBook.getTitleOrDefault("Sách đang chọn"));

        final String finalExpectedQrData = expectedQrData; // Cần final để dùng trong lambda
        scannedResult.ifPresent(scannedData -> {
            if (scannedData.equals(finalExpectedQrData)) {
                showAlert(Alert.AlertType.INFORMATION, "Xác nhận thành công", "Mã QR khớp. Đang xử lý trả sách...");
                performReturnBookLogic(recordToReturn);
            } else {
                showAlert(Alert.AlertType.ERROR, "Xác nhận thất bại", "Mã QR không khớp.\nQuét được: " + scannedData + "\nKỳ vọng: " + finalExpectedQrData);
            }
        });
        if (scannedResult.isEmpty()) { // Người dùng có thể đã hủy dialog quét
            System.out.println("INFO_LMC_QR: QR Scanning was cancelled or failed to return data.");
        }
    }

    // Đây là nơi bạn sẽ gọi dialog camera thật của mình
    private Optional<String> showActualQRScannerDialog(String bookTitleForDisplay) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/quanlythuvien/fxml/admin/dialogs/QRScannerDialogView.fxml"));
            Parent root = loader.load();
            QRScannerDialogController scannerController = loader.getController();
            // Bạn có thể cần truyền thêm thông tin vào scannerController nếu cần, ví dụ:
            // scannerController.setPurpose("BookReturnScan");

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Quét Mã QR Sách Trả - " + bookTitleForDisplay);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            if (loanManagementRoot != null && loanManagementRoot.getScene() != null) {
                dialogStage.initOwner(loanManagementRoot.getScene().getWindow());
            }

            Scene scene = new Scene(root);
            applyDialogStyles(dialogStage);
            dialogStage.setScene(scene);

            dialogStage.setOnCloseRequest(event -> {
                System.out.println("QRScannerDialog (AdminLoan): Close request on stage, stopping camera.");
                scannerController.stopCameraAndTask(); // Đảm bảo camera dừng
            });

            dialogStage.showAndWait(); // Chờ dialog này đóng

            return scannerController.getScannedQrCode(); // Lấy kết quả từ controller của dialog quét

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi Giao Diện", "Không thể mở cửa sổ quét mã QR.");
            return Optional.empty();
        }
    }

    private void performReturnBookLogic(BorrowingRecord loanToReturn) {
        boolean success = borrowingRecordService.recordBookReturn(loanToReturn.getRecordId(), LocalDate.now());
        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã ghi nhận trả sách cho ID: " + loanToReturn.getRecordId());
            loadAndCacheActiveLoans();
            // Cập nhật lại thông tin sách nếu nó đang được chọn ở tab 1
            if (selectedBookForLoan != null && selectedBookForLoan.getInternalId() == loanToReturn.getBookInternalId()) {
                Optional<Book> refreshedBookOpt = bookManagementService.findBookByInternalId(loanToReturn.getBookInternalId());
                refreshedBookOpt.ifPresent(book -> {
                    selectedBookForLoan = book;
                    String availabilityText = (selectedBookForLoan.getAvailableQuantity() > 0) ?
                            " (Có sẵn: " + selectedBookForLoan.getAvailableQuantity() + ")" : " - Đã hết sách";
                    selectedBookInfoLabel.setText("Sách: " + selectedBookForLoan.getTitleOrDefault("Không rõ") + availabilityText);
                    updateBorrowButtonState();
                });
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật bản ghi trả sách.");
        }
    }

    // --- Các phương thức tiện ích ---
    private void applyDialogStyles(DialogPane dialogPane) {
        try {
            URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
            if (cssUrl != null) dialogPane.getStylesheets().add(cssUrl.toExternalForm());
        } catch (Exception e) { System.err.println("WARN_DIALOG_CSS: Failed to load CSS for DialogPane: " + e.getMessage()); }
    }

    private void applyDialogStyles(Stage stage) {
        try {
            URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
            if (cssUrl != null && stage.getScene() != null) {
                stage.getScene().getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) { System.err.println("WARN_STAGE_CSS: Failed to load CSS for Stage: " + e.getMessage()); }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (alert.getDialogPane() != null) {
            applyDialogStyles(alert.getDialogPane());
        }
        alert.showAndWait();
    }

    public void setDashboardController(AdminDashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }
}
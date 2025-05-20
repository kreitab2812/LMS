package com.lms.quanlythuvien.controllers.admin;

import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.transaction.BorrowingRecord;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.transaction.BorrowingRecordService;
import com.lms.quanlythuvien.services.user.UserService;
import com.lms.quanlythuvien.utils.session.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
// import javafx.scene.control.cell.PropertyValueFactory; // Không dùng trực tiếp nếu dùng Wrapper

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class BookLoanHistoryController implements Initializable {

    @FXML private Label bookTitleLabel;
    @FXML private TableView<BorrowingRecordDisplayWrapper> loanHistoryTableView;
    @FXML private TableColumn<BorrowingRecordDisplayWrapper, String> userIdColumn;
    @FXML private TableColumn<BorrowingRecordDisplayWrapper, String> userNameColumn;
    @FXML private TableColumn<BorrowingRecordDisplayWrapper, String> borrowDateColumn;
    @FXML private TableColumn<BorrowingRecordDisplayWrapper, String> dueDateColumn;
    @FXML private TableColumn<BorrowingRecordDisplayWrapper, String> returnDateColumn;
    @FXML private TableColumn<BorrowingRecordDisplayWrapper, String> statusColumn;

    private BorrowingRecordService borrowingRecordService;
    private UserService userService;
    private Book selectedBook;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        borrowingRecordService = BorrowingRecordService.getInstance();
        userService = UserService.getInstance();

        // SỬ DỤNG PHƯƠNG THỨC ĐÚNG CỦA SESSIONMANAGER
        this.selectedBook = SessionManager.getInstance().getSelectedBookForHistory();
        // SessionManager.getSelectedBookForHistory() đã tự clear book sau khi lấy.

        setupTableColumns();

        if (this.selectedBook != null) {
            bookTitleLabel.setText("Tên sách: " + selectedBook.getTitleOrDefault("Không rõ"));
            loadLoanHistory();
        } else {
            bookTitleLabel.setText("Tên sách: Không thể xác định sách");
            loanHistoryTableView.setPlaceholder(new Label("Không có sách nào được chọn để xem lịch sử."));
            System.err.println("BookLoanHistoryController: selectedBook from session is null.");
        }
    }

    private void setupTableColumns() {
        userIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUserId()));
        userNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUserName()));
        borrowDateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getBorrowDate()));
        dueDateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDueDate()));
        returnDateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getReturnDate()));
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));
    }

    private void loadLoanHistory() {
        if (selectedBook == null || selectedBook.getInternalId() <= 0) { // Kiểm tra internalId hợp lệ
            System.err.println("BookLoanHistoryController: Cannot load history, selected book is null or has invalid internalId.");
            loanHistoryTableView.setPlaceholder(new Label("Không thể tải lịch sử do thiếu thông tin sách."));
            return;
        }

        List<BorrowingRecord> records = borrowingRecordService.getLoansByBookInternalId(selectedBook.getInternalId(), false);

        if (records.isEmpty()) {
            loanHistoryTableView.setPlaceholder(new Label("Sách này chưa có ai mượn."));
            return;
        }

        Set<String> userIds = records.stream()
                .map(BorrowingRecord::getUserId)
                .filter(id -> id != null && !id.trim().isEmpty()) // Bỏ qua userId null/empty
                .collect(Collectors.toSet());
        Map<String, User> userMap = userService.getUsersMapByIds(userIds);

        ObservableList<BorrowingRecordDisplayWrapper> displayList = FXCollections.observableArrayList();
        for (BorrowingRecord record : records) {
            User user = userMap.get(record.getUserId());
            String userName = "N/A";
            if (user != null) {
                userName = user.getFullNameOrDefault(user.getUsernameOrDefault("ID: " + record.getUserId()));
            } else if (record.getUserId() != null) {
                userName = "User ID: " + record.getUserId() + " (không tìm thấy)";
            }
            displayList.add(new BorrowingRecordDisplayWrapper(record, userName, dateFormatter));
        }
        loanHistoryTableView.setItems(displayList);
    }

    public static class BorrowingRecordDisplayWrapper {
        private final String userId;
        private final String userName;
        private final String borrowDate;
        private final String dueDate;
        private final String returnDate;
        private final String status;

        public BorrowingRecordDisplayWrapper(BorrowingRecord record, String userName, DateTimeFormatter formatter) {
            this.userId = record.getUserId() != null ? record.getUserId() : "N/A";
            this.userName = userName;
            this.borrowDate = record.getBorrowDate() != null ? record.getBorrowDate().format(formatter) : "N/A";
            this.dueDate = record.getDueDate() != null ? record.getDueDate().format(formatter) : "N/A";
            this.returnDate = record.getReturnDate() != null ? record.getReturnDate().format(formatter) : "N/A";
            this.status = record.getStatus() != null ? record.getStatus().toString() : "N/A";
        }

        public String getUserId() { return userId; }
        public String getUserName() { return userName; }
        public String getBorrowDate() { return borrowDate; }
        public String getDueDate() { return dueDate; }
        public String getReturnDate() { return returnDate; }
        public String getStatus() { return status; }
    }
}
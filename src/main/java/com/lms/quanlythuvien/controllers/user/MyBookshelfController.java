package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.transaction.BorrowingRecord;
import com.lms.quanlythuvien.models.transaction.LoanStatus;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.services.transaction.BorrowingRecordService;
import com.lms.quanlythuvien.utils.session.SessionManager;
import com.lms.quanlythuvien.models.transaction.BorrowingRequest; // Đã có import này
import com.lms.quanlythuvien.services.transaction.BorrowingRequestService; // << THÊM IMPORT NÀY

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.TilePane;

import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MyBookshelfController implements Initializable {

    @FXML private TabPane myBookshelfTabPane;
    @FXML private Tab borrowedBooksTab;
    @FXML private TilePane borrowedBooksTilePane;

    @FXML private Tab readingHistoryTab;
    @FXML private TilePane readingHistoryTilePane;

    @FXML private Tab loanRequestsTab; // << KHAI BÁO FXML CHO TAB YÊU CẦU
    @FXML private TilePane loanRequestsTilePane; // << KHAI BÁO FXML CHO TILEPANE CỦA TAB YÊU CẦU

    private User currentUser;
    private BorrowingRecordService borrowingRecordService;
    private BookManagementService bookManagementService;
    private BorrowingRequestService borrowingRequestService; // << THÊM SERVICE NÀY

    public MyBookshelfController() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        borrowingRecordService = BorrowingRecordService.getInstance();
        bookManagementService = BookManagementService.getInstance();
        borrowingRequestService = BorrowingRequestService.getInstance(); // << KHỞI TẠO SERVICE
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (currentUser == null) {
            System.err.println("MyBookshelfController: currentUser is null. Cannot load data.");
            if (borrowedBooksTilePane != null) borrowedBooksTilePane.getChildren().setAll(new Label("Vui lòng đăng nhập."));
            if (readingHistoryTilePane != null) readingHistoryTilePane.getChildren().setAll(new Label("Vui lòng đăng nhập."));
            if (loanRequestsTilePane != null) loanRequestsTilePane.getChildren().setAll(new Label("Vui lòng đăng nhập.")); // Thêm cho tab mới
            return;
        }

        // Load dữ liệu cho tab được chọn ban đầu hoặc tab đầu tiên
        // Giả sử tab đầu tiên là "Sách Đang Mượn"
        if (myBookshelfTabPane.getSelectionModel().getSelectedItem() == borrowedBooksTab ||
                myBookshelfTabPane.getSelectionModel().getSelectedItem() == null) { // Nếu chưa có tab nào được chọn (lần đầu)
            loadBorrowedBooks();
        } else if (myBookshelfTabPane.getSelectionModel().getSelectedItem() == readingHistoryTab) {
            loadReadingHistory();
        } else if (myBookshelfTabPane.getSelectionModel().getSelectedItem() == loanRequestsTab) {
            loadLoanRequests();
        }


        myBookshelfTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == borrowedBooksTab) {
                loadBorrowedBooks();
            } else if (newTab == readingHistoryTab) {
                loadReadingHistory();
            } else if (newTab == loanRequestsTab) { // << THÊM XỬ LÝ CHO TAB YÊU CẦU
                loadLoanRequests();
            }
            // else if (newTab == favouriteBooksTab) { loadFavouriteBooks(); } // Cho tương lai
        });
    }

    private void loadBorrowedBooks() {
        if (currentUser == null || borrowedBooksTilePane == null) return;
        System.out.println("DEBUG_MyBookshelfCtrl: Loading borrowed books...");
        borrowedBooksTilePane.getChildren().clear();

        List<BorrowingRecord> activeLoans = borrowingRecordService.getLoansByUserId(currentUser.getUserId(), true);
        activeLoans.sort(Comparator.comparing(BorrowingRecord::getDueDate));

        if (activeLoans.isEmpty()) {
            borrowedBooksTilePane.getChildren().add(new Label("Bạn không có sách nào đang mượn."));
        } else {
            for (BorrowingRecord record : activeLoans) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/quanlythuvien/fxml/user/BookCardBorrowedView.fxml"));
                    Node bookCardNode = loader.load();
                    BookCardBorrowedController cardController = loader.getController();

                    Optional<Book> bookOpt = bookManagementService.findBookByInternalId(record.getBookInternalId());
                    if (bookOpt.isPresent()) {
                        cardController.setData(record, bookOpt.get());
                        cardController.setParentController(this);
                        borrowedBooksTilePane.getChildren().add(bookCardNode);
                    } else {
                        System.err.println("Book not found for internalId: " + record.getBookInternalId() + " in borrowed list.");
                    }
                } catch (IOException e) {
                    System.err.println("Error loading BookCardBorrowedView.fxml for borrowed books: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        System.out.println("DEBUG_MyBookshelfCtrl: Borrowed books loaded. Count: " + activeLoans.size());
    }

    private void loadReadingHistory() {
        if (currentUser == null || readingHistoryTilePane == null) return;
        System.out.println("DEBUG_MyBookshelfCtrl: Loading reading history...");
        readingHistoryTilePane.getChildren().clear();

        List<BorrowingRecord> allUserLoans = borrowingRecordService.getLoansByUserId(currentUser.getUserId(), false);
        List<BorrowingRecord> returnedLoans = allUserLoans.stream()
                .filter(r -> r.getStatus() == LoanStatus.RETURNED)
                .sorted(Comparator.comparing(BorrowingRecord::getReturnDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        if (returnedLoans.isEmpty()) {
            readingHistoryTilePane.getChildren().add(new Label("Bạn chưa có lịch sử đọc sách nào."));
        } else {
            for (BorrowingRecord record : returnedLoans) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/quanlythuvien/fxml/user/BookCardBorrowedView.fxml"));
                    Node bookCardNode = loader.load();
                    BookCardBorrowedController cardController = loader.getController();

                    Optional<Book> bookOpt = bookManagementService.findBookByInternalId(record.getBookInternalId());
                    if (bookOpt.isPresent()) {
                        cardController.setDataForHistory(record, bookOpt.get());
                        readingHistoryTilePane.getChildren().add(bookCardNode);
                    } else {
                        System.err.println("Book not found for internalId: " + record.getBookInternalId() + " in history list.");
                    }
                } catch (IOException e) {
                    System.err.println("Error loading BookCardBorrowedView.fxml for history: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        System.out.println("DEBUG_MyBookshelfCtrl: Reading history loaded. Count: " + returnedLoans.size());
    }

    public void refreshBorrowedBooksTab() {
        System.out.println("DEBUG_MyBookshelfCtrl: Refreshing borrowed books tab (called by child card).");
        if (currentUser != null && borrowedBooksTilePane != null) {
            loadBorrowedBooks();
        }
    }

    // << --- THÊM CÁC PHƯƠNG THỨC CHO TAB YÊU CẦU MƯỢN SÁCH --- >>
    private void loadLoanRequests() {
        if (currentUser == null || loanRequestsTilePane == null) {
            if (loanRequestsTilePane != null) loanRequestsTilePane.getChildren().setAll(new Label("Vui lòng đăng nhập để xem yêu cầu."));
            return;
        }
        System.out.println("DEBUG_MyBookshelfCtrl: Loading loan requests for user " + currentUser.getUserId());
        loanRequestsTilePane.getChildren().clear();

        List<BorrowingRequest> requests = borrowingRequestService.getRequestsByUserId(currentUser.getUserId());
        // Sắp xếp theo ngày yêu cầu mới nhất lên đầu
        requests.sort(Comparator.comparing(BorrowingRequest::getRequestDate).reversed());

        if (requests.isEmpty()) {
            loanRequestsTilePane.getChildren().add(new Label("Bạn không có yêu cầu mượn sách nào."));
        } else {
            for (BorrowingRequest request : requests) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/quanlythuvien/fxml/user/LoanRequestCardView.fxml"));
                    Node requestCardNode = loader.load();
                    LoanRequestCardController cardController = loader.getController();

                    // Lấy thông tin sách từ BookManagementService dựa trên ISBN-13 trong request
                    Optional<Book> bookOpt = bookManagementService.findBookByIsbn13InLibrary(request.getBookIsbn13());

                    cardController.setData(request, bookOpt.orElse(null), this); // Truyền this (MyBookshelfController)

                    loanRequestsTilePane.getChildren().add(requestCardNode);
                } catch (IOException e) {
                    System.err.println("Error loading LoanRequestCardView.fxml: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        System.out.println("DEBUG_MyBookshelfCtrl: Loan requests loaded. Count: " + requests.size());
    }

    public void refreshLoanRequestsTab() {
        System.out.println("DEBUG_MyBookshelfCtrl: Refreshing loan requests tab...");
        if (currentUser != null && loanRequestsTilePane != null) {
            loadLoanRequests();
        }
    }
    // << ----------------------------------------------------------- >>
}
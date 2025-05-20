package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.item.Author;
import com.lms.quanlythuvien.models.transaction.BorrowingRecord;
import com.lms.quanlythuvien.models.transaction.LoanStatus;
import com.lms.quanlythuvien.models.transaction.BorrowingRequest;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.services.transaction.BorrowingRecordService;
import com.lms.quanlythuvien.services.transaction.BorrowingRequestService;
import com.lms.quanlythuvien.services.user.FavoriteBookService;
import com.lms.quanlythuvien.utils.session.SessionManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.TilePane;
import javafx.scene.control.ScrollPane;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MyBookshelfController implements Initializable {

    @FXML private TabPane myBookshelfTabPane;
    @FXML private Tab borrowedBooksTab;
    @FXML private TilePane borrowedBooksTilePane;
    @FXML private Tab readingHistoryTab;
    @FXML private TilePane readingHistoryTilePane;
    @FXML private Tab loanRequestsTab;
    @FXML private TilePane loanRequestsTilePane;
    @FXML private Tab favoriteBooksTab;
    @FXML private TilePane favoriteBooksTilePane;

    private User currentUser;
    private BorrowingRecordService borrowingRecordService;
    private BookManagementService bookManagementService;
    private BorrowingRequestService borrowingRequestService;
    private FavoriteBookService favoriteBookService;

    private UserDashboardController dashboardController;

    public void setDashboardController(UserDashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    public MyBookshelfController() {
        // Constructor
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        borrowingRecordService = BorrowingRecordService.getInstance();
        bookManagementService = BookManagementService.getInstance();
        borrowingRequestService = BorrowingRequestService.getInstance();
        favoriteBookService = FavoriteBookService.getInstance();

        if (currentUser == null) {
            showLoginRequiredMessage(borrowedBooksTilePane, "Vui lòng đăng nhập để xem mục này.");
            showLoginRequiredMessage(readingHistoryTilePane, "Vui lòng đăng nhập để xem mục này.");
            showLoginRequiredMessage(loanRequestsTilePane, "Vui lòng đăng nhập để xem mục này.");
            showLoginRequiredMessage(favoriteBooksTilePane, "Vui lòng đăng nhập để xem sách yêu thích.");
            if (myBookshelfTabPane != null) myBookshelfTabPane.setDisable(true);
            return;
        }

        configureTilePane(borrowedBooksTilePane);
        configureTilePane(readingHistoryTilePane);
        configureTilePane(loanRequestsTilePane);
        configureTilePane(favoriteBooksTilePane);

        String targetTabName = SessionManager.getInstance().getTargetMyBookshelfTab();
        Platform.runLater(() -> {
            Tab tabToSelect = borrowedBooksTab; // Mặc định
            if (targetTabName != null) {
                if ("requests".equalsIgnoreCase(targetTabName) && loanRequestsTab != null) {
                    tabToSelect = loanRequestsTab;
                } else if ("history".equalsIgnoreCase(targetTabName) && readingHistoryTab != null) {
                    tabToSelect = readingHistoryTab;
                } else if ("favorites".equalsIgnoreCase(targetTabName) && favoriteBooksTab != null) {
                    tabToSelect = favoriteBooksTab;
                }
            }

            if (myBookshelfTabPane != null && tabToSelect != null) { // Kiểm tra null trước khi select
                myBookshelfTabPane.getSelectionModel().select(tabToSelect);
                loadDataForSelectedTab(tabToSelect); // Load cho tab được chọn
            } else if (myBookshelfTabPane != null && !myBookshelfTabPane.getTabs().isEmpty()){ // Nếu không có target, chọn tab đầu tiên
                myBookshelfTabPane.getSelectionModel().selectFirst();
                loadDataForSelectedTab(myBookshelfTabPane.getSelectionModel().getSelectedItem());
            }
        });

        myBookshelfTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null && currentUser != null) {
                loadDataForSelectedTab(newTab);
            }
        });
    }

    private void loadDataForSelectedTab(Tab selectedTab) {
        if (selectedTab == null || currentUser == null) return; // Thêm kiểm tra currentUser

        if (selectedTab == borrowedBooksTab) {
            loadBorrowedBooks();
        } else if (selectedTab == readingHistoryTab) {
            loadReadingHistory();
        } else if (selectedTab == loanRequestsTab) {
            loadLoanRequests();
        } else if (selectedTab == favoriteBooksTab) {
            loadFavoriteBooks();
        }
    }

    private void configureTilePane(TilePane tilePane) {
        if (tilePane != null) {
            tilePane.setPadding(new Insets(15));
            tilePane.setHgap(15);
            tilePane.setVgap(15);
        }
    }

    private void showLoginRequiredMessage(TilePane tilePane, String message) {
        if (tilePane != null) {
            tilePane.getChildren().clear();
            Label msgLabel = new Label(message);
            msgLabel.getStyleClass().add("placeholder-text-large");
            tilePane.getChildren().add(msgLabel);
        }
    }

    private void showEmptyMessage(TilePane tilePane, String message) {
        if (tilePane != null) {
            tilePane.getChildren().clear();
            Label msgLabel = new Label(message);
            msgLabel.getStyleClass().add("placeholder-text");
            tilePane.getChildren().add(msgLabel);
        }
    }

    private void loadBorrowedBooks() {
        if (currentUser == null || borrowedBooksTilePane == null) return;
        borrowedBooksTilePane.getChildren().clear();

        List<BorrowingRecord> activeLoans = borrowingRecordService.getLoansByUserId(currentUser.getUserId(), true);
        activeLoans.sort(Comparator.comparing(BorrowingRecord::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())));

        if (activeLoans.isEmpty()) {
            showEmptyMessage(borrowedBooksTilePane, "Bạn không có sách nào đang mượn.");
        } else {
            Set<Integer> bookInternalIds = activeLoans.stream()
                    .map(BorrowingRecord::getBookInternalId)
                    .collect(Collectors.toSet());
            Map<Integer, Book> booksMap = bookManagementService.getBooksByInternalIds(bookInternalIds)
                    .stream()
                    .collect(Collectors.toMap(Book::getInternalId, Function.identity()));
            for (BorrowingRecord record : activeLoans) {
                Book book = booksMap.get(record.getBookInternalId());
                if (book != null) {
                    addBookCardToTilePane(record, book, borrowedBooksTilePane, true);
                } else {
                    System.err.println("WARN_MBSC_BORROWED: Book not found for internalId: " + record.getBookInternalId() + " in loan record ID: " + record.getRecordId());
                }
            }
        }
    }

    private void loadReadingHistory() {
        if (currentUser == null || readingHistoryTilePane == null) return;
        readingHistoryTilePane.getChildren().clear();

        List<BorrowingRecord> allUserLoans = borrowingRecordService.getLoansByUserId(currentUser.getUserId(), false);
        List<BorrowingRecord> returnedLoans = allUserLoans.stream()
                .filter(r -> r.getStatus() == LoanStatus.RETURNED && r.getReturnDate() != null)
                .sorted(Comparator.comparing(BorrowingRecord::getReturnDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        if (returnedLoans.isEmpty()) {
            showEmptyMessage(readingHistoryTilePane, "Lịch sử đọc sách của bạn còn trống.");
        } else {
            Set<Integer> bookInternalIds = returnedLoans.stream()
                    .map(BorrowingRecord::getBookInternalId)
                    .collect(Collectors.toSet());
            Map<Integer, Book> booksMap = bookManagementService.getBooksByInternalIds(bookInternalIds)
                    .stream()
                    .collect(Collectors.toMap(Book::getInternalId, Function.identity()));
            for (BorrowingRecord record : returnedLoans) {
                Book book = booksMap.get(record.getBookInternalId());
                if (book != null) {
                    addBookCardToTilePane(record, book, readingHistoryTilePane, false);
                } else {
                    System.err.println("WARN_MBSC_HISTORY: Book not found for internalId: " + record.getBookInternalId() + " in loan record ID: " + record.getRecordId());
                }
            }
        }
    }

    private void addBookCardToTilePane(BorrowingRecord record, Book book, TilePane targetPane, boolean isCurrentLoanView) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/quanlythuvien/fxml/user/BookCardBorrowedView.fxml"));
            Node bookCardNode = loader.load();
            BookCardBorrowedController cardController = loader.getController();
            // Giả sử BookCardBorrowedController có setParentShelfController hoặc setData nhận MyBookshelfController
            // cardController.setParentShelfController(this); // Dòng này đã được bạn yêu cầu bỏ ở lần sửa trước
            if (isCurrentLoanView) {
                cardController.setDataForCurrentLoan(record, book, this);
            } else {
                cardController.setDataForLoanHistory(record, book, this);
            }
            targetPane.getChildren().add(bookCardNode);
        } catch (IOException e) {
            System.err.println("ERROR_MBSC_LOAD_BOOK_CARD: Failed to load BookCardBorrowedView for book '" + book.getTitleOrDefault("N/A") + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadLoanRequests() {
        if (currentUser == null || loanRequestsTilePane == null) {
            System.err.println("DEBUG_MBSC_LOAD_REQ: currentUser or loanRequestsTilePane is null. Aborting.");
            return;
        }
        System.out.println("DEBUG_MBSC_LOAD_REQ: Loading loan requests for user: " + currentUser.getUserId());
        loanRequestsTilePane.getChildren().clear();

        List<BorrowingRequest> requests = borrowingRequestService.getRequestsByUserId(currentUser.getUserId());
        System.out.println("DEBUG_MBSC_LOAD_REQ: Found " + requests.size() + " requests for user.");

        if (requests.isEmpty()) {
            showEmptyMessage(loanRequestsTilePane, "Bạn không có yêu cầu mượn sách nào.");
        } else {
            requests.sort(Comparator.comparing(BorrowingRequest::getRequestDate).reversed());

            Set<String> bookIsbns = requests.stream()
                    .map(BorrowingRequest::getBookIsbn13)
                    .filter(isbn -> isbn != null && !isbn.trim().isEmpty())
                    .collect(Collectors.toSet());
            System.out.println("DEBUG_MBSC_LOAD_REQ: Collected " + bookIsbns.size() + " unique ISBNs for lookup: " + bookIsbns);

            Map<String, Book> booksMap = new HashMap<>();
            if (!bookIsbns.isEmpty()) {
                booksMap = bookManagementService.getBooksByIsbns(bookIsbns);
                System.out.println("DEBUG_MBSC_LOAD_REQ: Fetched " + booksMap.size() + " books from collected ISBNs.");
            } else {
                System.out.println("DEBUG_MBSC_LOAD_REQ: No valid ISBNs to fetch book details.");
            }

            int cardsAdded = 0;
            for (BorrowingRequest request : requests) {
                Book book = null;
                if (request.getBookIsbn13() != null && !request.getBookIsbn13().trim().isEmpty()) {
                    book = booksMap.get(request.getBookIsbn13().trim());
                }
                if (book == null) {
                    System.out.println("DEBUG_MBSC_LOAD_REQ: Book details not found for ISBN: " + request.getBookIsbn13() + " (Request ID: " + request.getRequestId() + "). Card will show limited info.");
                }
                addLoanRequestCardToTilePane(request, book, loanRequestsTilePane);
                cardsAdded++;
            }
            System.out.println("DEBUG_MBSC_LOAD_REQ: Added " + cardsAdded + " request cards to TilePane.");
            if (cardsAdded == 0 && !requests.isEmpty()) {
                showEmptyMessage(loanRequestsTilePane, "Không thể hiển thị chi tiết các yêu cầu mượn sách.");
            }
        }
    }

    private void addLoanRequestCardToTilePane(BorrowingRequest request, Book book, TilePane targetPane) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/quanlythuvien/fxml/user/LoanRequestCardView.fxml"));
            Node requestCardNode = loader.load();
            LoanRequestCardController cardController = loader.getController();
            cardController.setData(request, book, this);
            targetPane.getChildren().add(requestCardNode);
        } catch (IOException e) {
            System.err.println("ERROR_MBSC_LOAD_REQ_CARD: Failed to load LoanRequestCardView for request ID " + request.getRequestId() + ": " + e.getMessage());
            e.printStackTrace();
            Label errorLabel = new Label("Lỗi tải card cho YC ID: " + request.getRequestId());
            targetPane.getChildren().add(errorLabel);
        }
    }

    private void loadFavoriteBooks() {
        if (currentUser == null || favoriteBooksTilePane == null) return;
        System.out.println("DEBUG_MBSC_FAV: Loading favorite books for user: " + currentUser.getUserId());
        favoriteBooksTilePane.getChildren().clear();

        List<Book> favoriteBooks = favoriteBookService.getFavoriteBooksByUserId(currentUser.getUserId());
        System.out.println("DEBUG_MBSC_FAV: Found " + favoriteBooks.size() + " favorite books.");

        if (favoriteBooks.isEmpty()) {
            showEmptyMessage(favoriteBooksTilePane, "Bạn chưa có sách yêu thích nào.");
        } else {
            // --- SỬA LỖI Ở ĐÂY ---
            favoriteBooks.sort(Comparator.comparing(
                    book -> book.getTitleOrDefault(""), // Gọi getTitleOrDefault với giá trị mặc định
                    String.CASE_INSENSITIVE_ORDER
            ));
            // --- KẾT THÚC SỬA LỖI ---
            for (Book book : favoriteBooks) {
                addBookToFavoriteTilePane(book, favoriteBooksTilePane);
            }
        }
    }

    private void addBookToFavoriteTilePane(Book book, TilePane targetPane) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/quanlythuvien/fxml/user/BookCardLibraryView.fxml"));
            Node bookCardNode = loader.load();
            BookCardLibraryController cardController = loader.getController();

            // Giả định BookCardLibraryController.setData(Book book, Object parentContext) đã được sửa
            cardController.setData(book, this);

            System.out.println("DEBUG_MBSC_FAV: Adding card for favorite book: " + book.getTitleOrDefault("N/A"));
            targetPane.getChildren().add(bookCardNode);
        } catch (IOException e) {
            System.err.println("ERROR_MBSC_FAV_CARD: Failed to load BookCardLibraryView for favorite book '" + book.getTitleOrDefault("N/A") + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void refreshCurrentTabData() {
        if (myBookshelfTabPane == null || currentUser == null) return;
        Tab selectedTab = myBookshelfTabPane.getSelectionModel().getSelectedItem();
        System.out.println("DEBUG_MBSC: Refreshing current tab data: " + (selectedTab != null ? selectedTab.getText() : "None"));
        if (selectedTab == null) { // Nếu không có tab nào được chọn, có thể chọn tab mặc định
            if (!myBookshelfTabPane.getTabs().isEmpty()) {
                myBookshelfTabPane.getSelectionModel().selectFirst();
                selectedTab = myBookshelfTabPane.getSelectionModel().getSelectedItem();
            } else {
                return; // Không có tab nào để làm mới
            }
        }
        loadDataForSelectedTab(selectedTab);
    }

    public void navigateToBookDetail(Book book) {
        if (dashboardController != null && book != null) {
            SessionManager.getInstance().setSelectedBook(book);
            dashboardController.loadViewIntoCenter("user/BookDetailView.fxml");
        } else {
            System.err.println("ERROR_MBSC_NAV_BOOK_DETAIL: DashboardController or Book is null. Cannot navigate.");
            if (dashboardController == null) System.err.println(">> DashboardController is null in MyBookshelfController");
            if (book == null) System.err.println(">> Book parameter is null in navigateToBookDetail");
        }
    }

    public void navigateToAuthorDetail(Author author) {
        if (dashboardController != null && author != null) {
            SessionManager.getInstance().setSelectedAuthor(author);
            dashboardController.loadViewIntoCenter("user/AuthorDetailView.fxml");
        } else {
            System.err.println("ERROR_MBSC_NAV_AUTHOR_DETAIL: DashboardController or Author is null.");
        }
    }
}
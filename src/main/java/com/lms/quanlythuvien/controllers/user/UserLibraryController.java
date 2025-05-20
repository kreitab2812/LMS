package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.item.Author;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.services.library.AuthorManagementService;
import com.lms.quanlythuvien.utils.session.SessionManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;
// import javafx.scene.control.ListView; // Bỏ nếu không dùng genreFilterSidebar cũ
// import javafx.scene.layout.VBox; // Bỏ nếu không dùng genreFilterSidebar cũ


import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class UserLibraryController implements Initializable {

    @FXML private TabPane libraryTabPane;
    @FXML private Tab booksLibraryTab;
    @FXML private TilePane booksTilePane;
    @FXML private Tab authorsLibraryTab;
    @FXML private TilePane authorsTilePane;

    @FXML private ComboBox<String> genreFilterComboBox;
    @FXML private TextField bookTabSearchField;
    @FXML private Button applyBookTabFilterButton;
    @FXML private Button clearBookTabFilterButton;

    @FXML private TextField authorTabSearchField;
    @FXML private Button applyAuthorTabSearchButton;
    @FXML private Button clearAuthorTabSearchButton;

    private User currentUser;
    private BookManagementService bookManagementService;
    private AuthorManagementService authorManagementService;

    private ObservableList<Book> allLibraryBooksObservable;
    private ObservableList<Author> allLibraryAuthorsObservable;
    private ObservableList<String> allGenresObservable;

    public UserLibraryController() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        bookManagementService = BookManagementService.getInstance();
        authorManagementService = AuthorManagementService.getInstance();
        allLibraryBooksObservable = FXCollections.observableArrayList();
        allLibraryAuthorsObservable = FXCollections.observableArrayList();
        allGenresObservable = FXCollections.observableArrayList();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (currentUser == null) {
            handleUserNotLoggedIn();
            return;
        }

        loadAllBooksFromService();
        loadAllAuthorsFromService();
        populateGenreFilterComboBox();

        genreFilterComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldGenre, newGenre) -> {
            if (newGenre != null) {
                // Tự động lọc khi chọn ComboBox, không cần nút "Lọc" riêng cho thể loại nữa
                // Hoặc nếu muốn giữ nút "Lọc" thì không gọi applyCombinedBookFilter() ở đây
                applyCombinedBookFilter();
            }
        });

        libraryTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            clearLocalFiltersOnTabChange();
            if (newTab == booksLibraryTab) {
                displayBooksInTilePane(allLibraryBooksObservable);
            } else if (newTab == authorsLibraryTab) {
                displayAuthorsInTilePane(allLibraryAuthorsObservable);
            }
        });

        Platform.runLater(() -> {
            Tab selectedTab = libraryTabPane.getSelectionModel().getSelectedItem();
            if (selectedTab == null) { // Nếu chưa có tab nào được chọn, chọn tab sách
                libraryTabPane.getSelectionModel().select(booksLibraryTab);
            }
            // onViewActivated sẽ được UserDashboardController gọi sau khi view này được load
            // để xử lý global search nếu có.
            // Nếu không có global search, onViewActivated sẽ tự load tab hiện tại.
        });
    }

    private void handleUserNotLoggedIn() {
        // ... (giữ nguyên)
        Label loginMsgBooks = new Label("Vui lòng đăng nhập để xem thư viện.");
        loginMsgBooks.getStyleClass().add("placeholder-text-tilepane");
        if (booksTilePane != null) booksTilePane.getChildren().setAll(loginMsgBooks);

        Label loginMsgAuthors = new Label("Vui lòng đăng nhập để xem tác giả.");
        loginMsgAuthors.getStyleClass().add("placeholder-text-tilepane");
        if (authorsTilePane != null) authorsTilePane.getChildren().setAll(loginMsgAuthors);
    }

    public void onViewActivated() {
        // ... (giữ nguyên logic onViewActivated từ lượt #82, nó sẽ gọi handleGlobalSearch) ...
        System.out.println("UserLibraryController: onViewActivated called.");
        if (currentUser == null) {
            handleUserNotLoggedIn();
            return;
        }

        if (allLibraryBooksObservable.isEmpty()) loadAllBooksFromService();
        if (allLibraryAuthorsObservable.isEmpty()) loadAllAuthorsFromService();
        // Đảm bảo ComboBox thể loại được điền nếu nó rỗng (và allBooks đã được load)
        if (genreFilterComboBox != null && genreFilterComboBox.getItems().size() <= 1 && !allLibraryBooksObservable.isEmpty()) {
            populateGenreFilterComboBox();
        }


        String globalQuery = SessionManager.getInstance().getGlobalSearchQuery();
        String globalType = SessionManager.getInstance().getGlobalSearchType();

        if (globalQuery != null && globalType != null) {
            System.out.println("UserLibraryController: Processing global search on activation: Query=[" + globalQuery + "], Type=[" + globalType + "]");
            handleGlobalSearch(globalQuery, globalType);
            SessionManager.getInstance().setGlobalSearchQuery(null);
            SessionManager.getInstance().setGlobalSearchType(null);
        } else {
            Tab selectedTab = libraryTabPane.getSelectionModel().getSelectedItem();
            if (selectedTab == booksLibraryTab || selectedTab == null) {
                libraryTabPane.getSelectionModel().select(booksLibraryTab);
                applyCombinedBookFilter(); // Áp dụng filter hiện tại (có thể là "Tất cả thể loại" và keyword rỗng)
            } else if (selectedTab == authorsLibraryTab) {
                applyAuthorTabSearch(); // Áp dụng filter hiện tại cho tác giả
            }
        }
    }

    public void handleGlobalSearch(String query, String searchType) {
        // ... (giữ nguyên logic từ lượt #82) ...
        if (currentUser == null) return;
        clearLocalFiltersOnTabChange();

        if ("Tên tác giả".equalsIgnoreCase(searchType)) {
            libraryTabPane.getSelectionModel().select(authorsLibraryTab);
            if(authorTabSearchField != null) authorTabSearchField.setText(query); // Điền query vào ô search của tab
            applyAuthorTabSearch(); // Gọi hàm search cục bộ
        } else {
            libraryTabPane.getSelectionModel().select(booksLibraryTab);
            if ("Thể loại sách".equalsIgnoreCase(searchType) && genreFilterComboBox != null) {
                // Tìm xem query có khớp với giá trị nào trong ComboBox không
                String matchedGenre = genreFilterComboBox.getItems().stream()
                        .filter(g -> query.equalsIgnoreCase(g))
                        .findFirst().orElse("Tất cả thể loại");
                genreFilterComboBox.setValue(matchedGenre);
                if (bookTabSearchField != null) bookTabSearchField.clear(); // Xóa keyword nếu tìm theo thể loại từ global
            } else { // Tìm kiếm chung cho sách (Tiêu đề, ISBN, Tất cả)
                if (genreFilterComboBox != null) genreFilterComboBox.setValue("Tất cả thể loại");
                if (bookTabSearchField != null) bookTabSearchField.setText(query);
            }
            applyCombinedBookFilter(); // Áp dụng filter kết hợp
        }
    }

    private void loadAllBooksFromService() { /* ... (giữ nguyên) ... */ }
    private void populateGenreFilterComboBox() { /* ... (giữ nguyên) ... */ }

    @FXML
    private void handleClearBookTabFilter(ActionEvent event) { // Đổi tên cho nút "Xóa lọc" của tab sách
        if (bookTabSearchField != null) bookTabSearchField.clear();
        if (genreFilterComboBox != null) genreFilterComboBox.setValue("Tất cả thể loại");
        applyCombinedBookFilter();
    }

    @FXML
    private void handleApplyBookTabFilter(ActionEvent event) { // Nút "Tìm" trong tab Sách
        applyCombinedBookFilter();
    }

    /**
     * Lọc và hiển thị sách dựa trên thể loại từ ComboBox VÀ từ khóa từ TextField.
     */
    private void applyCombinedBookFilter() { // <<--- ĐÂY LÀ PHƯƠNG THỨC QUAN TRỌNG
        if (currentUser == null || booksTilePane == null) return;

        String selectedGenre = (genreFilterComboBox != null && genreFilterComboBox.getValue() != null) ?
                genreFilterComboBox.getValue() : "Tất cả thể loại";
        String keyword = (bookTabSearchField != null) ?
                bookTabSearchField.getText().toLowerCase().trim() : "";

        List<Book> booksToFilter = new ArrayList<>(allLibraryBooksObservable);
        ObservableList<Book> filteredList;

        // 1. Lọc theo thể loại
        if (!"Tất cả thể loại".equalsIgnoreCase(selectedGenre)) {
            booksToFilter = booksToFilter.stream()
                    .filter(book -> book.getCategories() != null &&
                            book.getCategories().stream()
                                    .anyMatch(genre -> selectedGenre.equalsIgnoreCase(genre.trim())))
                    .collect(Collectors.toList());
        }

        // 2. Lọc theo từ khóa trên kết quả đã lọc theo thể loại (hoặc trên toàn bộ nếu "Tất cả thể loại")
        if (!keyword.isEmpty()) {
            filteredList = FXCollections.observableArrayList(
                    booksToFilter.stream()
                            .filter(book -> (book.getTitle() != null && book.getTitle().toLowerCase().contains(keyword)) ||
                                    (book.getAuthors() != null && book.getAuthors().stream().anyMatch(author -> author.toLowerCase().contains(keyword))) ||
                                    (book.getIsbn13() != null && book.getIsbn13().contains(keyword)) ||
                                    (book.getIsbn10() != null && book.getIsbn10().contains(keyword)))
                            .collect(Collectors.toList())
            );
        } else {
            filteredList = FXCollections.observableArrayList(booksToFilter);
        }

        filteredList.sort(Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER));
        displayBooksInTilePane(filteredList);
    }

    // Đảm bảo phương thức này tồn tại và đúng tên
    private void filterBooksByGenre(String selectedGenre) {
        // Hàm này giờ sẽ là một phần của applyCombinedBookFilter,
        // hoặc applyCombinedBookFilter sẽ là hàm chính được gọi.
        // Để đơn giản, khi ComboBox thay đổi, ta gọi applyCombinedBookFilter.
        // Nút "Lọc" cũng gọi applyCombinedBookFilter.
        // Nút "Bỏ lọc" trong FXML gọi handleClearBookTabFilter (đã đổi tên)
        applyCombinedBookFilter();
    }


    private void displayBooksInTilePane(ObservableList<Book> booksToDisplay) { /* ... (giữ nguyên) ... */ }
    private void loadAllAuthorsFromService() { /* ... (giữ nguyên) ... */ }
    private void displayAuthorsInTilePane(ObservableList<Author> authorsToDisplay) { /* ... (giữ nguyên) ... */ }

    @FXML
    private void handleApplyAuthorTabSearch(ActionEvent event) { // Nút "Tìm" trong tab Tác giả
        applyAuthorTabSearch();
    }

    private void applyAuthorTabSearch() { // Hàm mới để dùng chung
        String keyword = (authorTabSearchField != null) ? authorTabSearchField.getText().toLowerCase().trim() : "";
        // searchAuthors nên trả về tất cả nếu keyword rỗng
        List<Author> searchResult = authorManagementService.searchAuthors(keyword);
        displayAuthorsInTilePane(FXCollections.observableArrayList(searchResult));
    }


    @FXML
    private void handleClearAuthorTabSearch(ActionEvent event) { // Nút "Xóa tìm" trong tab Tác giả
        if (authorTabSearchField != null) authorTabSearchField.clear();
        displayAuthorsInTilePane(allLibraryAuthorsObservable);
    }

    private void clearLocalFiltersOnTabChange() {
        if (genreFilterComboBox != null) genreFilterComboBox.setValue("Tất cả thể loại");
        if (bookTabSearchField != null) bookTabSearchField.clear();
        if (authorTabSearchField != null) authorTabSearchField.clear();
    }
}
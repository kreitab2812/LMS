package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.models.item.Author;
import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.library.AuthorManagementService;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.utils.session.SessionManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList; // Có thể dùng nếu lọc phức tạp hơn
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*; // Import chung cho Control
import javafx.scene.layout.TilePane;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class UserLibraryController implements Initializable {

    @FXML private TabPane libraryTabPane;
    @FXML private Tab booksLibraryTab;
    @FXML private TilePane booksTilePane;
    @FXML private Tab authorsLibraryTab;
    @FXML private TilePane authorsTilePane;

    @FXML private ComboBox<String> genreFilterComboBox;
    @FXML private TextField bookTabSearchField;
    @FXML private Button applyBookTabFilterButton; // Nút "Tìm" của tab Sách
    @FXML private Button clearBookTabFilterButton; // Nút "Xóa lọc" của tab Sách

    @FXML private TextField authorTabSearchField;
    @FXML private Button applyAuthorTabSearchButton; // Nút "Tìm" của tab Tác giả
    @FXML private Button clearAuthorTabSearchButton; // Nút "Xóa tìm" của tab Tác giả

    private User currentUser;
    private BookManagementService bookManagementService;
    private AuthorManagementService authorManagementService;

    // Dùng để lưu trữ toàn bộ sách/tác giả đã tải từ service
    private ObservableList<Book> allLibraryBooksObservable = FXCollections.observableArrayList();
    private ObservableList<Author> allLibraryAuthorsObservable = FXCollections.observableArrayList();
    // private ObservableList<String> allGenresObservable = FXCollections.observableArrayList(); // Không cần lưu trữ riêng nếu ComboBox tự quản lý

    private UserDashboardController dashboardController; // Để điều hướng

    public void setDashboardController(UserDashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        bookManagementService = BookManagementService.getInstance();
        authorManagementService = AuthorManagementService.getInstance();

        if (currentUser == null) {
            showLoginRequiredMessage(booksTilePane, "Vui lòng đăng nhập để xem thư viện sách.");
            showLoginRequiredMessage(authorsTilePane, "Vui lòng đăng nhập để xem danh sách tác giả.");
            if (libraryTabPane != null) libraryTabPane.setDisable(true);
            return;
        }

        configureTilePane(booksTilePane);
        configureTilePane(authorsTilePane);

        // Không load data ngay, để onViewActivated xử lý
        // populateGenreFilterComboBox(); // Sẽ được gọi khi load sách

        // Listener cho ComboBox thể loại (tự động lọc khi chọn)
        if (genreFilterComboBox != null) {
            genreFilterComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldGenre, newGenre) -> {
                if (newGenre != null) { // && libraryTabPane.getSelectionModel().getSelectedItem() == booksLibraryTab
                    applyCombinedBookFilter();
                }
            });
        }

        // Listener cho việc thay đổi tab
        if (libraryTabPane != null) {
            libraryTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                clearLocalFiltersOnTabChange(); // Xóa filter của tab cũ
                if (newTab == booksLibraryTab) {
                    if (allLibraryBooksObservable.isEmpty()) loadAllBooksFromService(); // Load nếu chưa có
                    else displayBooksInTilePane(allLibraryBooksObservable); // Hiển thị lại toàn bộ
                    if (genreFilterComboBox.getItems().size() <=1 && !allLibraryBooksObservable.isEmpty()) populateGenreFilterComboBox();
                } else if (newTab == authorsLibraryTab) {
                    if (allLibraryAuthorsObservable.isEmpty()) loadAllAuthorsFromService();
                    else displayAuthorsInTilePane(allLibraryAuthorsObservable);
                }
            });
        }

        // onViewActivated sẽ được gọi bởi UserDashboardController sau khi load
        // Không cần Platform.runLater ở đây nữa
    }

    private void configureTilePane(TilePane tilePane) {
        if (tilePane != null) {
            tilePane.setPadding(new Insets(15));
            tilePane.setHgap(15);
            tilePane.setVgap(15);
            // tilePane.setPrefColumns(4); // Ví dụ, có thể set trong FXML
        }
    }

    private void showLoginRequiredMessage(TilePane tilePane, String message) {
        if (tilePane != null) {
            Label msgLabel = new Label(message);
            msgLabel.getStyleClass().add("placeholder-text");
            tilePane.getChildren().setAll(msgLabel);
        }
    }

    public void onViewActivated() {
        System.out.println("DEBUG_ULC: onViewActivated called.");
        if (currentUser == null) return;

        // Load data nếu chưa có
        if (allLibraryBooksObservable.isEmpty()) loadAllBooksFromService();
        if (allLibraryAuthorsObservable.isEmpty()) loadAllAuthorsFromService();
        if (genreFilterComboBox != null && genreFilterComboBox.getItems().size() <=1 && !allLibraryBooksObservable.isEmpty()) {
            populateGenreFilterComboBox();
        }

        String globalQuery = SessionManager.getInstance().getGlobalSearchQuery();
        String globalType = SessionManager.getInstance().getGlobalSearchType();

        if (globalQuery != null && globalType != null) {
            System.out.println("DEBUG_ULC: Processing global search: Query=[" + globalQuery + "], Type=[" + globalType + "]");
            handleGlobalSearch(globalQuery, globalType);
            // Xóa query khỏi session sau khi đã xử lý
            SessionManager.getInstance().setGlobalSearchQuery(null);
            SessionManager.getInstance().setGlobalSearchType(null);
        } else { // Nếu không có global search, hiển thị tab hiện tại với filter mặc định
            Tab selectedTab = libraryTabPane.getSelectionModel().getSelectedItem();
            if (selectedTab == null || selectedTab == booksLibraryTab) {
                libraryTabPane.getSelectionModel().select(booksLibraryTab); // Đảm bảo tab sách được chọn
                applyCombinedBookFilter(); // Áp dụng filter (có thể là rỗng)
            } else if (selectedTab == authorsLibraryTab) {
                applyAuthorTabSearch(); // Áp dụng filter tác giả (có thể rỗng)
            }
        }
    }

    public void handleGlobalSearch(String query, String searchType) {
        if (currentUser == null) return;
        clearLocalFiltersOnTabChange(); // Xóa filter cục bộ trước khi áp dụng global search

        if ("Tên tác giả".equalsIgnoreCase(searchType)) {
            libraryTabPane.getSelectionModel().select(authorsLibraryTab);
            if(authorTabSearchField != null) authorTabSearchField.setText(query);
            applyAuthorTabSearch();
        } else { // "Tất cả", "Tiêu đề sách", "ISBN", "Thể loại sách"
            libraryTabPane.getSelectionModel().select(booksLibraryTab);
            if ("Thể loại sách".equalsIgnoreCase(searchType) && genreFilterComboBox != null) {
                String matchedGenre = genreFilterComboBox.getItems().stream()
                        .filter(g -> query.equalsIgnoreCase(g.trim()))
                        .findFirst().orElse("Tất cả thể loại");
                genreFilterComboBox.setValue(matchedGenre);
                if (bookTabSearchField != null) bookTabSearchField.clear(); // Xóa keyword nếu chỉ tìm theo thể loại
            } else { // Các trường hợp tìm sách còn lại
                if (genreFilterComboBox != null) genreFilterComboBox.setValue("Tất cả thể loại");
                if (bookTabSearchField != null) bookTabSearchField.setText(query);
            }
            applyCombinedBookFilter();
        }
    }

    private void loadAllBooksFromService() {
        if (bookManagementService == null) return;
        System.out.println("DEBUG_ULC: Loading all books from service...");
        // Giả sử getAllBooksInLibrary trả về danh sách sách đã được populate đầy đủ
        List<Book> books = bookManagementService.getAllBooksInLibrary();
        allLibraryBooksObservable.setAll(books);
        System.out.println("DEBUG_ULC: Loaded " + books.size() + " books.");
        // Populate genre filter sau khi đã có sách
        if (!books.isEmpty() && genreFilterComboBox !=null && genreFilterComboBox.getItems().size() <=1) {
            populateGenreFilterComboBox();
        }
        // Hiển thị sách nếu đang ở tab sách
        if (libraryTabPane.getSelectionModel().getSelectedItem() == booksLibraryTab) {
            applyCombinedBookFilter(); // Áp dụng filter mặc định (có thể là rỗng)
        }
    }

    private void populateGenreFilterComboBox() {
        if (genreFilterComboBox == null || allLibraryBooksObservable.isEmpty()) return;

        Set<String> genres = new HashSet<>();
        for (Book book : allLibraryBooksObservable) {
            if (book.getCategories() != null) {
                book.getCategories().forEach(genre -> genres.add(genre.trim()));
            }
        }
        List<String> sortedGenres = new ArrayList<>(genres);
        Collections.sort(sortedGenres, String.CASE_INSENSITIVE_ORDER);

        ObservableList<String> comboBoxItems = FXCollections.observableArrayList();
        comboBoxItems.add("Tất cả thể loại"); // Mục chọn mặc định
        comboBoxItems.addAll(sortedGenres);

        genreFilterComboBox.setItems(comboBoxItems);
        genreFilterComboBox.setValue("Tất cả thể loại"); // Set giá trị mặc định
        System.out.println("DEBUG_ULC: Genre filter ComboBox populated with " + sortedGenres.size() + " genres.");
    }

    @FXML
    private void handleApplyBookTabFilter(ActionEvent event) { // Nút "Tìm" của tab Sách
        applyCombinedBookFilter();
    }

    @FXML
    private void handleClearBookTabFilter(ActionEvent event) {
        if (bookTabSearchField != null) bookTabSearchField.clear();
        if (genreFilterComboBox != null) genreFilterComboBox.setValue("Tất cả thể loại");
        applyCombinedBookFilter(); // Hiển thị lại toàn bộ sách
    }

    private void applyCombinedBookFilter() {
        if (currentUser == null || booksTilePane == null) return;
        if (allLibraryBooksObservable.isEmpty() && bookManagementService != null) { // Nếu chưa load sách thì load
            loadAllBooksFromService();
            // loadAllBooksFromService đã gọi applyCombinedBookFilter() nếu ở tab sách, nên có thể return ở đây
            // hoặc để nó chạy tiếp với allLibraryBooksObservable vừa được điền
        }

        String selectedGenre = (genreFilterComboBox != null && genreFilterComboBox.getValue() != null) ?
                genreFilterComboBox.getValue() : "Tất cả thể loại";
        String keyword = (bookTabSearchField != null) ?
                bookTabSearchField.getText().toLowerCase().trim() : "";

        List<Book> filteredBooks = allLibraryBooksObservable.stream()
                .filter(book -> {
                    boolean genreMatch = "Tất cả thể loại".equalsIgnoreCase(selectedGenre) ||
                            (book.getCategories() != null &&
                                    book.getCategories().stream().anyMatch(g -> selectedGenre.equalsIgnoreCase(g.trim())));
                    if (!genreMatch) return false;

                    if (keyword.isEmpty()) return true; // Nếu không có keyword, chỉ cần match genre

                    return (book.getTitle() != null && book.getTitle().toLowerCase().contains(keyword)) ||
                            (book.getAuthors() != null && book.getAuthors().stream().anyMatch(author -> author.toLowerCase().contains(keyword))) ||
                            (book.getIsbn13() != null && book.getIsbn13().contains(keyword)) ||
                            (book.getIsbn10() != null && book.getIsbn10().contains(keyword));
                })
                .sorted(Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        displayBooksInTilePane(FXCollections.observableArrayList(filteredBooks));
    }

    private void displayBooksInTilePane(ObservableList<Book> booksToDisplay) {
        if (booksTilePane == null) return;
        booksTilePane.getChildren().clear();
        if (booksToDisplay.isEmpty()) {
            Label noBooksLabel = new Label("Không tìm thấy sách nào phù hợp.");
            noBooksLabel.getStyleClass().add("placeholder-text");
            booksTilePane.getChildren().add(noBooksLabel);
        } else {
            for (Book book : booksToDisplay) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/quanlythuvien/fxml/user/BookCardLibraryView.fxml"));
                    Node bookCardNode = loader.load();
                    BookCardLibraryController cardController = loader.getController();
                    cardController.setData(book, this); // Truyền 'this' (UserLibraryController)
                    booksTilePane.getChildren().add(bookCardNode);
                } catch (IOException e) {
                    System.err.println("ERROR_ULC_LOAD_BOOK_CARD: Failed to load BookCardLibraryView: " + e.getMessage());
                }
            }
        }
    }

    private void loadAllAuthorsFromService() {
        if (authorManagementService == null) return;
        System.out.println("DEBUG_ULC: Loading all authors from service...");
        List<Author> authors = authorManagementService.getAllAuthors(); // Lấy tất cả tác giả
        allLibraryAuthorsObservable.setAll(authors);
        System.out.println("DEBUG_ULC: Loaded " + authors.size() + " authors.");
        // Hiển thị tác giả nếu đang ở tab tác giả
        if (libraryTabPane.getSelectionModel().getSelectedItem() == authorsLibraryTab) {
            applyAuthorTabSearch(); // Áp dụng filter mặc định (có thể rỗng)
        }
    }

    private void displayAuthorsInTilePane(ObservableList<Author> authorsToDisplay) {
        if (authorsTilePane == null) return;
        authorsTilePane.getChildren().clear();
        if (authorsToDisplay.isEmpty()) {
            Label noAuthorsLabel = new Label("Không tìm thấy tác giả nào.");
            noAuthorsLabel.getStyleClass().add("placeholder-text");
            authorsTilePane.getChildren().add(noAuthorsLabel);
        } else {
            for (Author author : authorsToDisplay) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/quanlythuvien/fxml/user/AuthorCardView.fxml"));
                    Node authorCardNode = loader.load();
                    AuthorCardController cardController = loader.getController();
                    cardController.setData(author, this); // Truyền 'this' (UserLibraryController)
                    authorsTilePane.getChildren().add(authorCardNode);
                } catch (IOException e) {
                    System.err.println("ERROR_ULC_LOAD_AUTHOR_CARD: Failed to load AuthorCardView: " + e.getMessage());
                }
            }
        }
    }

    @FXML
    private void handleApplyAuthorTabSearch(ActionEvent event) {
        applyAuthorTabSearch();
    }

    private void applyAuthorTabSearch() {
        if (currentUser == null || authorsTilePane == null) return;
        if (allLibraryAuthorsObservable.isEmpty() && authorManagementService != null) {
            loadAllAuthorsFromService();
        }
        String keyword = (authorTabSearchField != null) ? authorTabSearchField.getText().toLowerCase().trim() : "";

        List<Author> filteredAuthors;
        if (keyword.isEmpty()) {
            filteredAuthors = new ArrayList<>(allLibraryAuthorsObservable);
        } else {
            filteredAuthors = allLibraryAuthorsObservable.stream()
                    .filter(author -> author.getName() != null && author.getName().toLowerCase().contains(keyword))
                    .collect(Collectors.toList());
        }
        filteredAuthors.sort(Comparator.comparing(Author::getName, String.CASE_INSENSITIVE_ORDER));
        displayAuthorsInTilePane(FXCollections.observableArrayList(filteredAuthors));
    }

    @FXML
    private void handleClearAuthorTabSearch(ActionEvent event) {
        if (authorTabSearchField != null) authorTabSearchField.clear();
        displayAuthorsInTilePane(allLibraryAuthorsObservable); // Hiển thị lại toàn bộ
    }

    private void clearLocalFiltersOnTabChange() {
        if (genreFilterComboBox != null) genreFilterComboBox.setValue("Tất cả thể loại");
        if (bookTabSearchField != null) bookTabSearchField.clear();
        if (authorTabSearchField != null) authorTabSearchField.clear();
    }

    // Phương thức điều hướng được gọi từ các card con
    public void navigateToBookDetail() {
        if (dashboardController != null && SessionManager.getInstance().getSelectedBook() != null) {
            dashboardController.loadViewIntoCenter("user/BookDetailView.fxml");
        } else {
            System.err.println("ERROR_ULC_NAV_BOOK_DETAIL: DashboardController or selected book is null.");
        }
    }

    public void navigateToAuthorDetail() {
        if (dashboardController != null && SessionManager.getInstance().getSelectedAuthor() != null) {
            dashboardController.loadViewIntoCenter("user/AuthorDetailView.fxml");
        } else {
            System.err.println("ERROR_ULC_NAV_AUTHOR_DETAIL: DashboardController or selected author is null.");
        }
    }
}
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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
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
    @FXML private Button applyBookTabFilterButton;
    @FXML private Button clearBookTabFilterButton;

    @FXML private TextField authorTabSearchField;
    @FXML private Button applyAuthorTabSearchButton;
    @FXML private Button clearAuthorTabSearchButton;

    private User currentUser;
    private BookManagementService bookManagementService;
    private AuthorManagementService authorManagementService;

    private ObservableList<Book> allLibraryBooksObservable = FXCollections.observableArrayList();
    private ObservableList<Author> allLibraryAuthorsObservable = FXCollections.observableArrayList();

    private UserDashboardController dashboardController;

    public void setDashboardController(UserDashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("DEBUG_ULC: Initializing UserLibraryController...");
        currentUser = SessionManager.getInstance().getCurrentUser();
        bookManagementService = BookManagementService.getInstance();
        authorManagementService = AuthorManagementService.getInstance();

        if (currentUser == null) {
            showLoginRequiredMessage(booksTilePane, "Vui lòng đăng nhập để xem thư viện sách.");
            showLoginRequiredMessage(authorsTilePane, "Vui lòng đăng nhập để xem danh sách tác giả.");
            if (libraryTabPane != null) libraryTabPane.setDisable(true);
            System.out.println("DEBUG_ULC: User not logged in. Disabling library view.");
            return;
        }
        System.out.println("DEBUG_ULC: User logged in: " + currentUser.getUsername());

        configureTilePane(booksTilePane);
        configureTilePane(authorsTilePane);

        if (genreFilterComboBox != null) {
            genreFilterComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldGenre, newGenre) -> {
                if (newGenre != null && libraryTabPane.getSelectionModel().getSelectedItem() == booksLibraryTab) {
                    System.out.println("DEBUG_ULC: Genre filter changed to: " + newGenre);
                    applyCombinedBookFilter();
                }
            });
        }

        if (libraryTabPane != null) {
            libraryTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab != null) {
                    System.out.println("DEBUG_ULC: Tab changed. Old: " + (oldTab != null ? oldTab.getText() : "null") + ", New: " + newTab.getText());
                    clearLocalFiltersOnTabChange(oldTab);
                    loadDataForSelectedTab(newTab);
                }
            });
        }
        System.out.println("DEBUG_ULC: Initialization complete.");
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

    public void onViewActivated() {
        System.out.println("DEBUG_ULC: onViewActivated called.");
        if (currentUser == null) {
            System.out.println("DEBUG_ULC: onViewActivated - User not logged in.");
            if (libraryTabPane != null) libraryTabPane.setDisable(true);
            return;
        }
        if (libraryTabPane != null) libraryTabPane.setDisable(false);

        String globalQuery = SessionManager.getInstance().getGlobalSearchQuery();
        String globalType = SessionManager.getInstance().getGlobalSearchType();

        if (globalQuery != null && globalType != null) {
            System.out.println("DEBUG_ULC: onViewActivated - Processing global search: Query=[" + globalQuery + "], Type=[" + globalType + "]");
            if (allLibraryBooksObservable.isEmpty() && !("Tên tác giả".equalsIgnoreCase(globalType))) {
                loadAllBooksFromService(false);
            }
            if (allLibraryAuthorsObservable.isEmpty() && "Tên tác giả".equalsIgnoreCase(globalType)) {
                loadAllAuthorsFromService(false);
            }
            if (genreFilterComboBox != null && genreFilterComboBox.getItems().size() <= 1 && !allLibraryBooksObservable.isEmpty()) {
                populateGenreFilterComboBox();
            }
            handleGlobalSearch(globalQuery, globalType);
            SessionManager.getInstance().setGlobalSearchQuery(null);
            SessionManager.getInstance().setGlobalSearchType(null);
        } else {
            System.out.println("DEBUG_ULC: onViewActivated - No global search. Loading data for current/default tab.");
            Tab selectedTab = libraryTabPane.getSelectionModel().getSelectedItem();
            if (selectedTab == null && !libraryTabPane.getTabs().isEmpty()) {
                System.out.println("DEBUG_ULC: onViewActivated - No tab selected, defaulting to first tab.");
                libraryTabPane.getSelectionModel().selectFirst();
                // Listener của TabPane sẽ tự động gọi loadDataForSelectedTab
            } else if (selectedTab != null) {
                System.out.println("DEBUG_ULC: onViewActivated - Tab already selected: " + selectedTab.getText() + ". Ensuring data is loaded.");
                loadDataForSelectedTab(selectedTab);
            } else if (libraryTabPane.getTabs().isEmpty()){
                System.out.println("DEBUG_ULC: onViewActivated - No tabs in TabPane.");
            }
        }
    }

    private void loadDataForSelectedTab(Tab tab) {
        if (tab == null || currentUser == null) {
            System.out.println("DEBUG_ULC: loadDataForSelectedTab - Tab or currentUser is null. Skipping load.");
            return;
        }
        System.out.println("DEBUG_ULC: loadDataForSelectedTab - Loading data for tab: " + tab.getText());

        if (tab == booksLibraryTab) {
            if (allLibraryBooksObservable.isEmpty()) {
                loadAllBooksFromService(true);
            } else {
                applyCombinedBookFilter();
            }
            if (genreFilterComboBox != null && genreFilterComboBox.getItems().size() <= 1 && !allLibraryBooksObservable.isEmpty()) {
                populateGenreFilterComboBox();
            }
        } else if (tab == authorsLibraryTab) {
            if (allLibraryAuthorsObservable.isEmpty()) {
                loadAllAuthorsFromService(true);
            } else {
                applyAuthorTabSearch();
            }
        }
    }

    public void handleGlobalSearch(String query, String searchType) {
        System.out.println("DEBUG_ULC: handleGlobalSearch - Query: " + query + ", Type: " + searchType);
        if (currentUser == null) return;
        clearLocalFiltersOnTabChange(null);

        if ("Tên tác giả".equalsIgnoreCase(searchType)) {
            libraryTabPane.getSelectionModel().select(authorsLibraryTab);
            Platform.runLater(() -> {
                if (authorTabSearchField != null) authorTabSearchField.setText(query);
                applyAuthorTabSearch();
            });
        } else {
            libraryTabPane.getSelectionModel().select(booksLibraryTab);
            Platform.runLater(() -> {
                if ("Thể loại sách".equalsIgnoreCase(searchType) && genreFilterComboBox != null) {
                    if (genreFilterComboBox.getItems().size() <= 1 && !allLibraryBooksObservable.isEmpty()) {
                        populateGenreFilterComboBox();
                    }
                    String matchedGenre = genreFilterComboBox.getItems().stream()
                            .filter(g -> query.equalsIgnoreCase(g.trim()))
                            .findFirst().orElse("Tất cả thể loại");
                    genreFilterComboBox.setValue(matchedGenre);
                    if (bookTabSearchField != null) bookTabSearchField.clear();
                } else {
                    if (genreFilterComboBox != null) genreFilterComboBox.setValue("Tất cả thể loại");
                    if (bookTabSearchField != null) bookTabSearchField.setText(query);
                    applyCombinedBookFilter();
                }
            });
        }
    }

    private void loadAllBooksFromService(boolean applyFilterAfterLoad) {
        if (bookManagementService == null) return;
        System.out.println("DEBUG_ULC: loadAllBooksFromService called. applyFilterAfterLoad: " + applyFilterAfterLoad);
        List<Book> books = bookManagementService.getAllBooksInLibrary();
        allLibraryBooksObservable.setAll(books);
        System.out.println("DEBUG_ULC: Loaded " + books.size() + " books into allLibraryBooksObservable.");

        if (!books.isEmpty() && genreFilterComboBox != null && genreFilterComboBox.getItems().size() <= 1) {
            populateGenreFilterComboBox();
        }
        if (applyFilterAfterLoad) {
            applyCombinedBookFilter();
        }
    }

    private void loadAllAuthorsFromService(boolean applyFilterAfterLoad) {
        if (authorManagementService == null) return;
        System.out.println("DEBUG_ULC: loadAllAuthorsFromService called. applyFilterAfterLoad: " + applyFilterAfterLoad);
        List<Author> authors = authorManagementService.getAllAuthors();
        allLibraryAuthorsObservable.setAll(authors);
        System.out.println("DEBUG_ULC: Loaded " + authors.size() + " authors into allLibraryAuthorsObservable.");
        if (applyFilterAfterLoad) {
            applyAuthorTabSearch();
        }
    }

    private void populateGenreFilterComboBox() {
        if (genreFilterComboBox == null || allLibraryBooksObservable.isEmpty()) return;
        System.out.println("DEBUG_ULC: Populating genre filter ComboBox...");
        Set<String> genres = new HashSet<>();
        for (Book book : allLibraryBooksObservable) {
            if (book.getCategories() != null) {
                book.getCategories().forEach(genre -> {
                    if (genre != null && !genre.trim().isEmpty()) genres.add(genre.trim());
                });
            }
        }
        List<String> sortedGenres = new ArrayList<>(genres);
        Collections.sort(sortedGenres, String.CASE_INSENSITIVE_ORDER);

        ObservableList<String> comboBoxItems = FXCollections.observableArrayList();
        comboBoxItems.add("Tất cả thể loại");
        comboBoxItems.addAll(sortedGenres);

        String currentValue = genreFilterComboBox.getValue();
        // Chỉ cập nhật items nếu chúng thực sự khác để tránh trigger listener không cần thiết
        if (!new HashSet<>(genreFilterComboBox.getItems()).equals(new HashSet<>(comboBoxItems))) {
            genreFilterComboBox.setItems(comboBoxItems);
        }

        if (currentValue != null && comboBoxItems.contains(currentValue)) {
            genreFilterComboBox.setValue(currentValue);
        } else {
            genreFilterComboBox.setValue("Tất cả thể loại");
        }
        System.out.println("DEBUG_ULC: Genre filter ComboBox populated. Items: " + comboBoxItems.size());
    }

    @FXML
    private void handleApplyBookTabFilter(ActionEvent event) {
        System.out.println("DEBUG_ULC: Apply Book Tab Filter button clicked.");
        applyCombinedBookFilter();
    }

    @FXML
    private void handleClearBookTabFilter(ActionEvent event) {
        System.out.println("DEBUG_ULC: Clear Book Tab Filter button clicked.");
        if (bookTabSearchField != null) bookTabSearchField.clear();
        if (genreFilterComboBox != null) genreFilterComboBox.setValue("Tất cả thể loại");
    }

    private void applyCombinedBookFilter() {
        if (currentUser == null || booksTilePane == null) {
            System.out.println("DEBUG_ULC: applyCombinedBookFilter - currentUser or booksTilePane is null.");
            return;
        }
        if (allLibraryBooksObservable.isEmpty() && bookManagementService != null) {
            System.out.println("DEBUG_ULC: applyCombinedBookFilter - Book list is empty, attempting to load...");
            loadAllBooksFromService(false); // Load, nhưng hàm này sẽ không tự apply filter nữa
            // mà việc apply filter sẽ diễn ra ngay sau đây.
        }

        String selectedGenre = (genreFilterComboBox != null && genreFilterComboBox.getValue() != null) ?
                genreFilterComboBox.getValue() : "Tất cả thể loại";
        String keyword = (bookTabSearchField != null) ?
                bookTabSearchField.getText().toLowerCase().trim() : "";

        System.out.println("DEBUG_ULC: Applying book filter. Genre: [" + selectedGenre + "], Keyword: [" + keyword + "], Total books in observable list: " + allLibraryBooksObservable.size());

        List<Book> filteredBooks = allLibraryBooksObservable.stream()
                .filter(book -> {
                    boolean genreMatch = "Tất cả thể loại".equalsIgnoreCase(selectedGenre) ||
                            (book.getCategories() != null &&
                                    book.getCategories().stream().anyMatch(g -> selectedGenre.equalsIgnoreCase(g.trim())));
                    if (!genreMatch) return false;

                    if (keyword.isEmpty()) return true;

                    return (book.getTitle() != null && book.getTitle().toLowerCase().contains(keyword)) ||
                            (book.getAuthorsFormatted("").toLowerCase().contains(keyword)) ||
                            (book.getIsbn13() != null && book.getIsbn13().contains(keyword)) ||
                            (book.getIsbn10() != null && book.getIsbn10().contains(keyword));
                })
                // --- SỬA LỖI Ở ĐÂY (DÒNG 337 HOẶC XUNG QUANH ĐÓ) ---
                .sorted(Comparator.comparing(
                        book -> book.getTitleOrDefault(""), // Gọi getTitleOrDefault với giá trị mặc định
                        String.CASE_INSENSITIVE_ORDER
                ))
                // --- KẾT THÚC SỬA LỖI ---
                .collect(Collectors.toList());

        System.out.println("DEBUG_ULC: Filtered books count: " + filteredBooks.size());
        displayBooksInTilePane(FXCollections.observableArrayList(filteredBooks));
    }

    private void displayBooksInTilePane(ObservableList<Book> booksToDisplay) {
        if (booksTilePane == null) {
            System.out.println("DEBUG_ULC: displayBooksInTilePane - booksTilePane is null.");
            return;
        }
        booksTilePane.getChildren().clear();
        if (booksToDisplay.isEmpty()) {
            System.out.println("DEBUG_ULC: displayBooksInTilePane - No books to display.");
            showEmptyMessage(booksTilePane, "Không tìm thấy sách nào phù hợp với tiêu chí.");
        } else {
            System.out.println("DEBUG_ULC: displayBooksInTilePane - Displaying " + booksToDisplay.size() + " books.");
            for (Book book : booksToDisplay) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/quanlythuvien/fxml/user/BookCardLibraryView.fxml"));
                    Node bookCardNode = loader.load();
                    BookCardLibraryController cardController = loader.getController();
                    cardController.setData(book, this);
                    booksTilePane.getChildren().add(bookCardNode);
                } catch (IOException e) {
                    System.err.println("ERROR_ULC_LOAD_BOOK_CARD: Failed to load BookCardLibraryView for book '" + book.getTitleOrDefault("N/A") + "': " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private void displayAuthorsInTilePane(ObservableList<Author> authorsToDisplay) {
        if (authorsTilePane == null) return;
        authorsTilePane.getChildren().clear();
        if (authorsToDisplay.isEmpty()) {
            showEmptyMessage(authorsTilePane, "Không tìm thấy tác giả nào phù hợp.");
        } else {
            for (Author author : authorsToDisplay) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/quanlythuvien/fxml/user/AuthorCardView.fxml"));
                    Node authorCardNode = loader.load();
                    AuthorCardController cardController = loader.getController();
                    cardController.setData(author, this);
                    authorsTilePane.getChildren().add(authorCardNode);
                } catch (IOException e) {
                    System.err.println("ERROR_ULC_LOAD_AUTHOR_CARD: Failed to load AuthorCardView for author '" + author.getName() + "': " + e.getMessage());
                    e.printStackTrace();
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
            loadAllAuthorsFromService(false);
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
        // Sắp xếp danh sách tác giả theo tên
        filteredAuthors.sort(Comparator.comparing(
                author -> author.getName() != null ? author.getName() : "", // Xử lý null cho tên tác giả
                String.CASE_INSENSITIVE_ORDER
        ));
        displayAuthorsInTilePane(FXCollections.observableArrayList(filteredAuthors));
    }

    @FXML
    private void handleClearAuthorTabSearch(ActionEvent event) {
        if (authorTabSearchField != null) authorTabSearchField.clear();
        applyAuthorTabSearch();
    }

    private void clearLocalFiltersOnTabChange(Tab oldTab) {
        System.out.println("DEBUG_ULC: Clearing local filters for tab change.");
        if (genreFilterComboBox != null) {
            // Chỉ setValue nếu nó thực sự khác để tránh trigger listener không cần thiết
            if (!"Tất cả thể loại".equals(genreFilterComboBox.getValue())) {
                genreFilterComboBox.setValue("Tất cả thể loại");
            }
        }
        if (bookTabSearchField != null && !bookTabSearchField.getText().isEmpty()) {
            bookTabSearchField.clear();
        }
        if (authorTabSearchField != null && !authorTabSearchField.getText().isEmpty()) {
            authorTabSearchField.clear();
        }
        // Việc setValue/clear ở trên CÓ THỂ trigger listener và gọi lại hàm filter.
        // Nếu không, bạn cần gọi hàm filter tương ứng ở đây hoặc trong listener của TabPane.
    }

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
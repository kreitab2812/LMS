package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.utils.session.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*; // Import Control chung
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class UserBookSearchController implements Initializable {

    @FXML private TextField userSearchKeywordField;
    @FXML private ComboBox<String> userSearchTypeComboBox;
    @FXML private Button userSearchButton;
    @FXML private TableView<Book> userBooksTableView;
    @FXML private TableColumn<Book, String> userBookTitleColumn;
    @FXML private TableColumn<Book, String> userBookAuthorsColumn;
    @FXML private TableColumn<Book, String> userBookPublisherColumn;
    @FXML private TableColumn<Book, String> userBookYearColumn;
    @FXML private TableColumn<Book, Integer> userBookAvailableColumn;
    @FXML private TableColumn<Book, Void> userBookDetailsColumn;

    private BookManagementService bookManagementService;
    private ObservableList<Book> observableBookList;

    private UserDashboardController dashboardController;

    public void setDashboardController(UserDashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("DEBUG_UBSC_INIT: UserBookSearchController initialize() started.");
        bookManagementService = BookManagementService.getInstance();

        observableBookList = FXCollections.observableArrayList();
        userBooksTableView.setItems(observableBookList);

        userSearchTypeComboBox.setItems(FXCollections.observableArrayList("Tất cả", "Tiêu đề", "Tác giả", "ISBN", "Thể loại"));
        userSearchTypeComboBox.setValue("Tất cả");

        setupTableColumns();
        loadInitialOrGlobalSearchBooks();

        userBooksTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Book selectedBook = userBooksTableView.getSelectionModel().getSelectedItem();
                if (selectedBook != null) {
                    showBookDetailsPopup(selectedBook, (Node)event.getSource());
                }
            }
        });
        System.out.println("DEBUG_UBSC_INIT: UserBookSearchController initialize() finished.");
    }

    private void setupTableColumns() {
        userBookTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        userBookAuthorsColumn.setCellValueFactory(cellData -> {
            Book book = cellData.getValue();
            return new SimpleStringProperty(book != null ? book.getAuthorsFormatted("N/A") : "");
        });
        userBookPublisherColumn.setCellValueFactory(new PropertyValueFactory<>("publisher"));
        userBookYearColumn.setCellValueFactory(new PropertyValueFactory<>("publishedDate"));
        userBookAvailableColumn.setCellValueFactory(new PropertyValueFactory<>("availableQuantity"));

        Callback<TableColumn<Book, Void>, TableCell<Book, Void>> cellFactory = param -> new TableCell<>() {
            private final Button btnViewDetails = new Button("Chi tiết");
            {
                btnViewDetails.getStyleClass().add("secondary-button-small");
                btnViewDetails.setOnAction((ActionEvent event) -> {
                    // Kiểm tra getIndex() hợp lệ trước khi lấy item
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        Book book = getTableView().getItems().get(getIndex());
                        if (book != null) { // Kiểm tra book không null
                            showBookDetailsPopup(book, btnViewDetails);
                        }
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(btnViewDetails);
                }
            }
        };
        userBookDetailsColumn.setCellFactory(cellFactory);
        userBookDetailsColumn.setStyle("-fx-alignment: CENTER;");
    }

    private void loadInitialOrGlobalSearchBooks() {
        String globalQuery = SessionManager.getInstance().getGlobalSearchQuery();
        String globalType = SessionManager.getInstance().getGlobalSearchType();

        if (globalQuery != null && globalType != null) {
            System.out.println("DEBUG_UBSC_INIT_SEARCH: Applying global search: Query=["+globalQuery+"], Type=["+globalType+"]");
            if(userSearchKeywordField != null) userSearchKeywordField.setText(globalQuery);
            if (userSearchTypeComboBox != null && userSearchTypeComboBox.getItems().contains(globalType)) {
                userSearchTypeComboBox.setValue(globalType);
            } else if (userSearchTypeComboBox != null) {
                userSearchTypeComboBox.setValue("Tất cả");
            }
            performSearch(globalQuery, globalType);
            SessionManager.getInstance().setGlobalSearchQuery(null);
            SessionManager.getInstance().setGlobalSearchType(null);
        } else {
            List<Book> allBooks = bookManagementService.getAllBooksInLibrary();
            observableBookList.setAll(allBooks);
            if(userBooksTableView != null) {
                userBooksTableView.setPlaceholder(new Label(allBooks.isEmpty() ? "Thư viện chưa có sách." : "Nhập từ khóa để tìm kiếm."));
            }
        }
    }

    @FXML
    void handleUserSearchBooksAction(ActionEvent event) {
        String keyword = userSearchKeywordField != null ? userSearchKeywordField.getText() : "";
        String searchType = userSearchTypeComboBox != null ? userSearchTypeComboBox.getValue() : "Tất cả";
        performSearch(keyword, searchType);
    }

    private void performSearch(String keyword, String searchType) {
        if (bookManagementService == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Dịch vụ sách chưa sẵn sàng.");
            return;
        }
        List<Book> searchResult = bookManagementService.searchBooksInLibrary(keyword, searchType);
        observableBookList.setAll(searchResult);
        if (userBooksTableView != null) {
            userBooksTableView.setPlaceholder(new Label(searchResult.isEmpty() ? "Không tìm thấy sách nào phù hợp." : ""));
        }
        System.out.println("DEBUG_UBSC_SEARCH: Performed search. Keyword: [" + keyword + "], Type: [" + searchType + "]. Found: " + searchResult.size());
    }

    private void showBookDetailsPopup(Book book, Node ownerNode) {
        if (book == null) {
            showAlert(Alert.AlertType.WARNING, "Lỗi Sách", "Không có thông tin sách để hiển thị.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/quanlythuvien/fxml/user/BookDetailView.fxml"));
            Parent root = loader.load();

            BookDetailController detailController = loader.getController();
            if (this.dashboardController != null) {
                detailController.setDashboardController(this.dashboardController);
            }
            // SỬA LỖI: Gọi đúng tên phương thức trong BookDetailController
            detailController.setBookDataUI(book);

            Stage detailStage = new Stage();
            detailStage.setTitle("Chi Tiết Sách: " + book.getTitleOrDefault("Sách không tên"));
            detailStage.initModality(Modality.APPLICATION_MODAL);

            Window parentWindow = (ownerNode != null && ownerNode.getScene() != null) ? ownerNode.getScene().getWindow() : null;
            if (parentWindow == null && userBooksTableView != null && userBooksTableView.getScene() != null) {
                parentWindow = userBooksTableView.getScene().getWindow();
            }
            if (parentWindow != null) detailStage.initOwner(parentWindow);

            Scene scene = new Scene(root);
            applyDialogStyles(scene); // Áp dụng CSS cho Scene
            detailStage.setScene(scene);
            detailStage.setWidth(850); // Tăng chiều rộng một chút
            detailStage.setHeight(700); // Tăng chiều cao một chút
            detailStage.showAndWait();

            // Cập nhật sách trong bảng sau khi dialog chi tiết đóng
            Optional<Book> updatedBookOpt = bookManagementService.findBookByIsbn13InLibrary(book.getIsbn13());
            if (updatedBookOpt.isPresent()) {
                Book updatedBook = updatedBookOpt.get();
                int index = -1;
                for (int i = 0; i < observableBookList.size(); i++) {
                    Book bInList = observableBookList.get(i);
                    // So sánh bằng ID chính (ISBN13) hoặc internalId
                    if (bInList.getId() != null && bInList.getId().equals(updatedBook.getId())) {
                        index = i;
                        break;
                    } else if (bInList.getInternalId() > 0 && bInList.getInternalId() == updatedBook.getInternalId()) {
                        index = i;
                        break;
                    }
                }
                if (index != -1) {
                    observableBookList.set(index, updatedBook);
                } else { // Nếu không tìm thấy, có thể là do list đã bị filter, thử refresh toàn bộ
                    performSearch(userSearchKeywordField.getText(), userSearchTypeComboBox.getValue());
                }
            } else { // Sách có thể đã bị xóa
                performSearch(userSearchKeywordField.getText(), userSearchTypeComboBox.getValue());
            }
            if (userBooksTableView != null) userBooksTableView.refresh();

        } catch (IOException e) {
            System.err.println("ERROR_UBSC_DETAILS_IO: Failed to load BookDetailView.fxml: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Lỗi Hiển Thị", "Không thể mở chi tiết sách.");
        } catch (Exception e) {
            System.err.println("CRITICAL_UBSC_DETAILS_UNEXPECTED: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi Không Mong Đợi", "Lỗi khi mở chi tiết sách.");
        }
    }

    private void applyDialogStyles(Scene scene) { // Nhận Scene
        try {
            URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
        } catch (Exception e) { System.err.println("WARN_SCENE_CSS: Failed to load CSS for scene: " + e.getMessage()); }
    }

    // Giữ lại applyDialogStyles(DialogPane) cho Alert
    private void applyDialogStyles(DialogPane dialogPane) { // Nhận DialogPane
        try {
            URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
            if (cssUrl != null) {
                dialogPane.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) { System.err.println("WARN_DIALOGPANE_CSS: Failed to load CSS for dialog pane: " + e.getMessage()); }
    }


    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyDialogStyles(alert.getDialogPane());
        alert.showAndWait();
    }
}
package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.services.library.BookManagementService;
// Các import cần thiết khác
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class UserBookSearchController implements Initializable {

    @FXML private TextField userSearchKeywordField;
    @FXML private ComboBox<String> userSearchTypeComboBox;
    // @FXML private Button userSearchButton; // Không cần khai báo nếu chỉ dùng onAction trong FXML
    @FXML private TableView<Book> userBooksTableView;
    @FXML private TableColumn<Book, String> userBookTitleColumn;
    @FXML private TableColumn<Book, String> userBookAuthorsColumn;
    @FXML private TableColumn<Book, String> userBookPublisherColumn;
    @FXML private TableColumn<Book, String> userBookYearColumn;
    @FXML private TableColumn<Book, Integer> userBookAvailableColumn;
    @FXML private TableColumn<Book, Void> userBookDetailsColumn;

    private BookManagementService bookManagementService;
    private ObservableList<Book> observableBookList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("DEBUG_UBSC_INIT: UserBookSearchController initialize() started.");
        bookManagementService = BookManagementService.getInstance(); // Sử dụng Singleton
        System.out.println("DEBUG_UBSC_INIT: BookManagementService instance: " +
                (bookManagementService != null ? bookManagementService.hashCode() : "null"));

        observableBookList = FXCollections.observableArrayList();
        userBooksTableView.setItems(observableBookList);

        userSearchTypeComboBox.setItems(FXCollections.observableArrayList("Tất cả", "Tiêu đề", "Tác giả", "ISBN"));
        userSearchTypeComboBox.setValue("Tất cả");

        setupTableColumns();
        loadInitialBooks();

        userBooksTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Book selectedBook = userBooksTableView.getSelectionModel().getSelectedItem();
                if (selectedBook != null) {
                    showBookDetailsPopup(selectedBook);
                }
            }
        });
        System.out.println("DEBUG_UBSC_INIT: UserBookSearchController initialize() finished.");
    }

    private void setupTableColumns() {
        userBookTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        userBookAuthorsColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue() != null) {
                List<String> authors = cellData.getValue().getAuthors();
                return new SimpleStringProperty(authors != null && !authors.isEmpty() ? String.join(", ", authors) : "N/A");
            }
            return new SimpleStringProperty("");
        });
        userBookPublisherColumn.setCellValueFactory(new PropertyValueFactory<>("publisher"));
        userBookYearColumn.setCellValueFactory(new PropertyValueFactory<>("publishedDate"));
        userBookAvailableColumn.setCellValueFactory(new PropertyValueFactory<>("availableQuantity"));

        Callback<TableColumn<Book, Void>, TableCell<Book, Void>> cellFactory = param -> new TableCell<>() {
            private final Button btnViewDetails = new Button("Xem");
            {
                btnViewDetails.getStyleClass().add("secondary-button");
                btnViewDetails.setOnAction((ActionEvent event) -> {
                    Book book = getTableView().getItems().get(getIndex());
                    if (book != null) { // Kiểm tra book không null
                        showBookDetailsPopup(book);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    // Kiểm tra xem dòng có dữ liệu không trước khi lấy getIndex()
                    if (getTableView().getItems().size() > getIndex() && getIndex() >= 0) {
                        setGraphic(btnViewDetails);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        };
        userBookDetailsColumn.setCellFactory(cellFactory);
    }

    private void loadInitialBooks() {
        if (bookManagementService != null) {
            List<Book> allBooks = bookManagementService.getAllBooksInLibrary();
            observableBookList.setAll(allBooks);
            if (allBooks.isEmpty()) {
                userBooksTableView.setPlaceholder(new Label("Thư viện hiện chưa có sách nào."));
            }
            System.out.println("DEBUG_UBSC_LOAD_INIT: Loaded " + allBooks.size() + " books initially.");
        } else {
            System.err.println("ERROR_UBSC_LOAD_INIT: bookManagementService is null!");
            userBooksTableView.setPlaceholder(new Label("Lỗi tải danh sách sách."));
        }
    }

    @FXML
    void handleUserSearchBooksAction(ActionEvent event) { // event có thể không dùng, nhưng giữ lại cho chuẩn onAction
        if (userSearchKeywordField == null || userSearchTypeComboBox == null || bookManagementService == null) {
            System.err.println("ERROR_UBSC_SEARCH: Search fields or service is null.");
            showAlert(Alert.AlertType.ERROR, "Lỗi Hệ thống", "Chức năng tìm kiếm chưa sẵn sàng.");
            return;
        }
        String keyword = userSearchKeywordField.getText().trim();
        String searchType = userSearchTypeComboBox.getValue();

        if (keyword.isEmpty() && (searchType == null || searchType.equalsIgnoreCase("Tất cả"))) {
            List<Book> allBooks = bookManagementService.getAllBooksInLibrary();
            observableBookList.setAll(allBooks);
            userBooksTableView.setPlaceholder(new Label(allBooks.isEmpty() ? "Thư viện hiện chưa có sách nào." : "Nhập từ khóa để tìm kiếm."));
            System.out.println("DEBUG_UBSC_SEARCH: Keyword empty, showing all books. Count: " + observableBookList.size());
            return;
        }

        List<Book> searchResult = bookManagementService.searchBooksInLibrary(keyword, searchType);
        observableBookList.setAll(searchResult);

        if (searchResult.isEmpty()) {
            userBooksTableView.setPlaceholder(new Label("Không tìm thấy sách nào phù hợp với từ khóa: '" + keyword + "'"));
        }
        System.out.println("DEBUG_UBSC_SEARCH: Search performed. Keyword: [" + keyword + "], Type: [" + searchType + "]. Found " + searchResult.size() + " books.");
    }

    private void showBookDetailsPopup(Book book) {
        if (book == null) {
            System.err.println("ERROR_UBSC_DETAILS: Attempted to show details for a null book.");
            return;
        }
        System.out.println("DEBUG_UBSC_DETAILS: Showing details for book: " + book.getTitle());
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/quanlythuvien/fxml/user/BookDetailView.fxml"));
            Parent root = loader.load();

            BookDetailController detailController = loader.getController();
            if (detailController != null) {
                detailController.setBookData(book); // Gọi setBookData đã bỏ tham số isAdminView
            } else {
                System.err.println("ERROR_UBSC_DETAILS: BookDetailController is null after loading FXML.");
                showAlert(Alert.AlertType.ERROR, "Lỗi Giao Diện", "Không thể khởi tạo cửa sổ chi tiết sách.");
                return;
            }

            Stage detailStage = new Stage();
            detailStage.setTitle("Chi Tiết Sách: " + book.getTitle());
            detailStage.initModality(Modality.APPLICATION_MODAL);
            if (userBooksTableView.getScene() != null && userBooksTableView.getScene().getWindow() != null) {
                detailStage.initOwner(userBooksTableView.getScene().getWindow()); // Đặt owner nếu có thể
            }

            Scene scene = new Scene(root);
            URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            detailStage.setScene(scene);
            detailStage.showAndWait();

        } catch (IOException e) {
            System.err.println("ERROR_UBSC_DETAILS: Failed to load BookDetailView.fxml.");
            // e.printStackTrace(); // Thay thế bằng showAlert hoặc logging framework
            showAlert(Alert.AlertType.ERROR, "Lỗi Hiển Thị", "Không thể mở cửa sổ chi tiết sách: " + e.getMessage());
        } catch (Exception e) { // Bắt các lỗi khác có thể xảy ra
            System.err.println("CRITICAL_UBSC_DETAILS: Unexpected error showing book details popup.");
            // e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi Không Mong Đợi", "Đã xảy ra lỗi không mong đợi khi mở chi tiết sách.");
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
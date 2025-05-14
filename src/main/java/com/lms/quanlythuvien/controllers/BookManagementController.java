package com.lms.quanlythuvien.controllers;

import com.lms.quanlythuvien.models.Book;
import com.lms.quanlythuvien.services.BookManagementService; // Service Singleton
import com.lms.quanlythuvien.services.GoogleBooksService;   // Giả sử không phải Singleton, hoặc là Singleton nếu cần

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.geometry.Insets;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.stream.Collectors;

public class BookManagementController implements Initializable {

    //<editor-fold desc="FXML Injections">
    @FXML private TableView<Book> booksTableView;
    @FXML private TableColumn<Book, String> idColumn;
    @FXML private TableColumn<Book, String> titleColumn;
    @FXML private TableColumn<Book, String> authorsColumn;
    @FXML private TableColumn<Book, String> publisherColumn;
    @FXML private TableColumn<Book, String> publishedDateColumn; // Sẽ là "Năm XB"
    @FXML private TableColumn<Book, Integer> quantityColumn;      // Sẽ là "SL Có Sẵn" (availableQuantity)
    // (Tùy chọn) Nếu muốn thêm cột tổng số lượng:
    // @FXML private TableColumn<Book, Integer> totalQuantityColumn; // Sẽ là "Tổng SL" (totalQuantity)
    @FXML private TableColumn<Book, String> locationColumn;

    @FXML private Button addBookButton;
    @FXML private Button editBookButton;
    @FXML private Button deleteBookButton;
    @FXML private TextField searchInLibraryField;
    @FXML private Button searchInLibraryButton; // Đã có fx:id trong FXML cậu gửi
    //</editor-fold>

    private BookManagementService bookManagementService;
    private GoogleBooksService googleBooksService; // Giữ nguyên cách khởi tạo nếu nó không có trạng thái cần duy trì

    private ObservableList<Book> observableBookList;
    private FilteredList<Book> filteredBookList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("DEBUG_BMC_INIT: BookManagementController initialize() started.");
        // LẤY INSTANCE SINGLETON
        bookManagementService = BookManagementService.getInstance();
        System.out.println("DEBUG_BMC_INIT: BookManagementService instance: " + bookManagementService.hashCode());

        googleBooksService = new GoogleBooksService(); // Giữ nguyên nếu đây là service không trạng thái
        System.out.println("DEBUG_BMC_INIT: GoogleBooksService instantiated.");

        observableBookList = FXCollections.observableArrayList();

        setupTableColumns();
        loadBooksToTable(); // Tải dữ liệu ban đầu

        booksTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean bookSelected = (newSelection != null);
            editBookButton.setDisable(!bookSelected);
            deleteBookButton.setDisable(!bookSelected);
        });

        searchInLibraryField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterBookTable(newValue);
        });
        System.out.println("DEBUG_BMC_INIT: BookManagementController initialize() finished.");
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id")); // Hoặc "bookId" tùy theo tên thuộc tính trong Book.java
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorsColumn.setCellValueFactory(cellData -> {
            List<String> authors = cellData.getValue().getAuthors();
            return new SimpleStringProperty(
                    (authors != null && !authors.isEmpty()) ? String.join(", ", authors) : "N/A"
            );
        });
        publisherColumn.setCellValueFactory(new PropertyValueFactory<>("publisher"));
        publishedDateColumn.setCellValueFactory(new PropertyValueFactory<>("publishedDate")); // Model Book nên có publishedDate là String

        // Hiển thị availableQuantity
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("availableQuantity"));
        // Nếu có cột totalQuantity:
        // totalQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("totalQuantity"));

        locationColumn.setCellValueFactory(new PropertyValueFactory<>("shelfLocation"));
    }

    private void loadBooksToTable() {
        // Dữ liệu sách giờ sẽ được duy trì trong BookManagementService Singleton
        observableBookList.setAll(bookManagementService.getAllBooksInLibrary());
        filteredBookList = new FilteredList<>(observableBookList, p -> true);
        booksTableView.setItems(filteredBookList);
        System.out.println("DEBUG_BMC_LOAD: Books loaded into table. Count: " + observableBookList.size());
    }

    private void filterBookTable(String searchText) {
        if (filteredBookList == null) return;
        if (searchText == null || searchText.isEmpty()) {
            filteredBookList.setPredicate(p -> true);
        } else {
            String lowerCaseFilter = searchText.toLowerCase();
            filteredBookList.setPredicate(book -> {
                if (book.getTitle() != null && book.getTitle().toLowerCase().contains(lowerCaseFilter)) return true;
                if (book.getAuthors() != null && book.getAuthors().stream().anyMatch(author -> author.toLowerCase().contains(lowerCaseFilter))) return true;
                if (book.getIsbn13() != null && book.getIsbn13().contains(lowerCaseFilter)) return true;
                if (book.getIsbn10() != null && book.getIsbn10().contains(lowerCaseFilter)) return true;
                // Thêm tìm kiếm theo nhà xuất bản, vị trí nếu muốn
                if (book.getPublisher() != null && book.getPublisher().toLowerCase().contains(lowerCaseFilter)) return true;
                if (book.getShelfLocation() != null && book.getShelfLocation().toLowerCase().contains(lowerCaseFilter)) return true;
                return false;
            });
        }
    }

    @FXML
    private void handleAddBookAction(ActionEvent event) {
        System.out.println("DEBUG_BMC_ADD: Add New Book action triggered.");
        Optional<Book> newBookOptional = showBookDialog(null); // Truyền null cho sách mới
        newBookOptional.ifPresent(bookFromDialog -> {
            // Xác định ID cho sách mới (giữ nguyên logic cũ của cậu)
            if (bookFromDialog.getId() == null || bookFromDialog.getId().trim().isEmpty()) {
                if (bookFromDialog.getIsbn13() != null && !bookFromDialog.getIsbn13().trim().isEmpty()) {
                    bookFromDialog.setId(bookFromDialog.getIsbn13());
                } else if (bookFromDialog.getIsbn10() != null && !bookFromDialog.getIsbn10().trim().isEmpty()) {
                    bookFromDialog.setId(bookFromDialog.getIsbn10());
                } else {
                    bookFromDialog.setId(UUID.randomUUID().toString());
                }
            }

            if (bookManagementService.addBookToLibrary(bookFromDialog)) {
                // observableBookList.add(bookFromDialog); // Không cần thêm trực tiếp, loadBooksToTable sẽ lấy từ service
                loadBooksToTable(); // Tải lại toàn bộ danh sách từ service để đảm bảo đồng bộ
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã thêm sách mới: " + bookFromDialog.getTitle());
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể thêm sách. Mã sách/ISBN '" + bookFromDialog.getId() + "' có thể đã tồn tại.");
            }
        });
    }

    @FXML
    private void handleEditBookAction(ActionEvent event) {
        Book selectedBook = booksTableView.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn sách", "Vui lòng chọn một cuốn sách để sửa.");
            return;
        }
        System.out.println("DEBUG_BMC_EDIT: Edit Book action triggered for: " + selectedBook.getTitle());
        Optional<Book> updatedBookOptional = showBookDialog(selectedBook); // Truyền sách hiện tại để edit

        updatedBookOptional.ifPresent(bookFromDialog -> {
            // Logic cập nhật trong BookManagementService đã xử lý totalQuantity và availableQuantity
            if (bookManagementService.updateBookInLibrary(bookFromDialog)) {
                loadBooksToTable(); // Tải lại để cập nhật thay đổi
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật sách: " + bookFromDialog.getTitle());
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật sách. Có thể sách không còn tồn tại.");
            }
        });
    }

    @FXML
    private void handleDeleteBookAction(ActionEvent event) {
        Book selectedBook = booksTableView.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn sách", "Vui lòng chọn một cuốn sách để xóa.");
            return;
        }
        System.out.println("DEBUG_BMC_DELETE: Delete Book action triggered for: " + selectedBook.getTitle());

        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Xác nhận Xóa");
        confirmationDialog.setHeaderText("Xóa Sách: " + selectedBook.getTitle());
        confirmationDialog.setContentText("Bạn có chắc chắn muốn xóa cuốn sách này không? \n(Lưu ý: Nếu sách đang được mượn, việc xóa có thể không được phép hoặc cần xử lý thêm).");
        Optional<ButtonType> result = confirmationDialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            // BookManagementService.deleteBookFromLibrary nên có kiểm tra nếu sách đang được mượn
            if (bookManagementService.deleteBookFromLibrary(selectedBook.getId())) {
                loadBooksToTable(); // Tải lại danh sách
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã xóa sách thành công!");
            } else {
                // Thông báo lỗi có thể cụ thể hơn từ service (ví dụ: sách đang được mượn)
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xóa sách. Sách có thể đang được cho mượn hoặc không tìm thấy.");
            }
        }
    }

    @FXML
    private void handleSearchInLibraryAction(ActionEvent event) {
        // Logic lọc đã được xử lý bởi listener của searchInLibraryField.textProperty()
        // Nút này có thể không cần thiết nếu đã có auto-filter.
        // Hoặc nếu muốn nó là nút kích hoạt tìm kiếm chính thức:
        System.out.println("DEBUG_BMC_SEARCH_BTN: Search In Library button clicked.");
        filterBookTable(searchInLibraryField.getText()); // Gọi lại filter
        if (filteredBookList.isEmpty() && !searchInLibraryField.getText().trim().isEmpty()) {
            // showAlert(Alert.AlertType.INFORMATION,"Kết quả Tìm kiếm", "Không tìm thấy sách nào trong thư viện khớp với từ khóa.");
            // Placeholder của TableView sẽ tự hiển thị nếu danh sách rỗng
        }
    }

    // Phương thức showBookDialog giữ nguyên logic hiển thị và lấy dữ liệu từ dialog
    // Chỉ cần đảm bảo nó tạo đối tượng Book với constructor mới (nhận initialQuantity)
    // và khi edit, nó truyền đúng totalQuantity vào ô nhập liệu.
    private Optional<Book> showBookDialog(Book existingBook) {
        Dialog<Book> dialog = new Dialog<>();
        dialog.setTitle(existingBook == null ? "Thêm Sách Mới" : "Chỉnh sửa Thông tin Sách");
        dialog.setHeaderText(existingBook == null ? "Nhập chi tiết sách:" : "Chỉnh sửa chi tiết cho: " + (existingBook.getTitle() != null ? existingBook.getTitle() : "Sách được chọn"));

        // Áp dụng CSS cho Dialog
        URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
        if (cssUrl != null) {
            dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("WARN_BMC_DIALOG: Cannot find styles.css for dialog.");
        }


        ButtonType saveButtonType = ButtonType.OK;
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        Node saveButtonNode = dialog.getDialogPane().lookupButton(saveButtonType); // Lấy Node của nút Save
        saveButtonNode.setDisable(true); // Vô hiệu hóa ban đầu

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10)); // Giảm padding phải

        // --- Form Fields ---
        TextField isbnField = new TextField();
        isbnField.setPromptText("ISBN-13 (dùng làm Mã Sách nếu mới)");
        Button fetchButton = new Button("Lấy từ Google");
        fetchButton.getStyleClass().add("primary-button-small");

        TextField titleField = new TextField(); titleField.setPromptText("Tiêu đề*");
        TextField authorsField = new TextField(); authorsField.setPromptText("Tác giả (cách nhau bởi dấu phẩy)");
        TextField publisherField = new TextField(); publisherField.setPromptText("Nhà xuất bản");
        TextField publishedDateField = new TextField(); publishedDateField.setPromptText("Năm XB (YYYY hoặc YYYY-MM-DD)");
        TextField categoriesField = new TextField(); categoriesField.setPromptText("Thể loại (cách nhau bởi dấu phẩy)");
        TextArea descriptionArea = new TextArea(); descriptionArea.setPromptText("Mô tả");
        descriptionArea.setPrefRowCount(3); descriptionArea.setWrapText(true); // Giảm số dòng
        TextField thumbnailUrlField = new TextField(); thumbnailUrlField.setPromptText("URL Ảnh bìa");
        TextField pageCountField = new TextField(); pageCountField.setPromptText("Số trang");
        TextField quantityField = new TextField(); quantityField.setPromptText("Tổng số lượng trong kho*"); // Label cho totalQuantity
        TextField locationField = new TextField(); locationField.setPromptText("Vị trí trên kệ");

        // --- Layout ---
        grid.add(new Label("ISBN:"), 0, 0);
        HBox isbnBox = new HBox(5, isbnField, fetchButton); HBox.setHgrow(isbnField, Priority.ALWAYS);
        grid.add(isbnBox, 1, 0, 2, 1);

        grid.add(new Label("Tiêu đề*:"), 0, 1); grid.add(titleField, 1, 1, 2, 1);
        grid.add(new Label("Tác giả:"), 0, 2); grid.add(authorsField, 1, 2, 2, 1);
        grid.add(new Label("Nhà XB:"), 0, 3); grid.add(publisherField, 1, 3, 2, 1);
        grid.add(new Label("Năm XB:"), 0, 4); grid.add(publishedDateField, 1, 4, 2, 1);
        grid.add(new Label("Thể loại:"), 0, 5); grid.add(categoriesField, 1, 5, 2, 1);
        grid.add(new Label("Mô tả:"), 0, 6); grid.add(descriptionArea, 1, 6, 2, 1);
        grid.add(new Label("URL Ảnh bìa:"), 0, 7); grid.add(thumbnailUrlField, 1, 7, 2, 1);
        grid.add(new Label("Số trang:"), 0, 8); grid.add(pageCountField, 1, 8, 2, 1);
        grid.add(new Label("Tổng SL*:"), 0, 9); grid.add(quantityField, 1, 9, 2, 1); // Label cho totalQuantity
        grid.add(new Label("Vị trí:"), 0, 10); grid.add(locationField, 1, 10, 2, 1);

        dialog.getDialogPane().setContent(grid);
        // dialog.getDialogPane().setPrefWidth(500); // Có thể không cần nếu grid tự co giãn tốt

        // Validate để enable/disable nút Save
        Runnable validateSaveButton = () -> {
            boolean disable = titleField.getText().trim().isEmpty() ||
                    quantityField.getText().trim().isEmpty();
            try {
                if (!quantityField.getText().trim().isEmpty()) {
                    int qty = Integer.parseInt(quantityField.getText().trim());
                    if (qty < 0) disable = true; // Số lượng không được âm
                }
            } catch (NumberFormatException e) {
                disable = true; // Nếu không phải số thì disable
            }
            saveButtonNode.setDisable(disable);
        };
        titleField.textProperty().addListener((obs, oldVal, newVal) -> validateSaveButton.run());
        quantityField.textProperty().addListener((obs, oldVal, newVal) -> validateSaveButton.run());

        fetchButton.setOnAction(e -> {
            // Giữ nguyên logic fetch từ Google Books của cậu
            // ... (đảm bảo nó có Task và Platform.runLater) ...
            String isbn = isbnField.getText().trim();
            if (isbn.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập ISBN để lấy chi tiết.");
                return;
            }
            fetchButton.setDisable(true);
            Label loadingLabel = new Label("Đang lấy thông tin...");
            // grid.add(loadingLabel, 1, 11, 2, 1); // Tạm thời không thêm vào grid để tránh làm thay đổi layout
            dialog.getDialogPane().setExpandableContent(new HBox(loadingLabel)); // Hiển thị ở đâu đó
            dialog.getDialogPane().setExpanded(true);


            Task<Optional<Book>> fetchTask = new Task<>() {
                @Override protected Optional<Book> call() { return googleBooksService.getBookDetailsByISBN(isbn); }
            };
            fetchTask.setOnSucceeded(workerStateEvent -> {
                Platform.runLater(() -> {
                    dialog.getDialogPane().setExpanded(false); // Ẩn loading
                    fetchButton.setDisable(false);

                    Optional<Book> fetchedBookOpt = fetchTask.getValue();
                    if (fetchedBookOpt.isPresent()) {
                        Book fetchedBook = fetchedBookOpt.get();
                        titleField.setText(fetchedBook.getTitle() != null ? fetchedBook.getTitle() : "");
                        authorsField.setText(fetchedBook.getAuthors() != null ? String.join(", ", fetchedBook.getAuthors()) : "");
                        publisherField.setText(fetchedBook.getPublisher() != null ? fetchedBook.getPublisher() : "");
                        publishedDateField.setText(fetchedBook.getPublishedDate() != null ? fetchedBook.getPublishedDate() : "");
                        categoriesField.setText(fetchedBook.getCategories() != null ? String.join(", ", fetchedBook.getCategories()) : "");
                        descriptionArea.setText(fetchedBook.getDescription() != null ? fetchedBook.getDescription() : "");
                        thumbnailUrlField.setText(fetchedBook.getThumbnailUrl() != null ? fetchedBook.getThumbnailUrl() : "");
                        pageCountField.setText(fetchedBook.getPageCount() != null ? String.valueOf(fetchedBook.getPageCount()) : "");
                        isbnField.setText(isbn); // Giữ lại ISBN đã nhập hoặc dùng ISBN từ kết quả
                        // Không set quantity từ Google Books, admin sẽ tự nhập
                        validateSaveButton.run();
                    } else {
                        showAlert(Alert.AlertType.INFORMATION, "Không tìm thấy", "Không tìm thấy chi tiết sách trên Google Books cho ISBN: " + isbn);
                    }
                });
            });
            fetchTask.setOnFailed(workerStateEvent -> {
                Platform.runLater(() -> {
                    dialog.getDialogPane().setExpanded(false);
                    fetchButton.setDisable(false);
                    showAlert(Alert.AlertType.ERROR, "Lỗi API", "Lỗi khi lấy chi tiết sách từ Google Books.");
                    if(fetchTask.getException() != null) fetchTask.getException().printStackTrace();
                });
            });
            new Thread(fetchTask).start();
        });

        if (existingBook != null) { // Điền thông tin nếu là chỉnh sửa
            isbnField.setText(existingBook.getIsbn13() != null ? existingBook.getIsbn13() : existingBook.getIsbn10());
            isbnField.setEditable(false); // Không cho sửa ID/ISBN khi edit
            fetchButton.setDisable(true); // Vô hiệu hóa fetch khi edit

            titleField.setText(existingBook.getTitle());
            authorsField.setText(existingBook.getAuthors() != null ? String.join(", ", existingBook.getAuthors()) : "");
            publisherField.setText(existingBook.getPublisher());
            publishedDateField.setText(existingBook.getPublishedDate());
            categoriesField.setText(existingBook.getCategories() != null ? String.join(", ", existingBook.getCategories()) : "");
            descriptionArea.setText(existingBook.getDescription());
            thumbnailUrlField.setText(existingBook.getThumbnailUrl());
            pageCountField.setText(existingBook.getPageCount() != null ? String.valueOf(existingBook.getPageCount()) : "");
            quantityField.setText(String.valueOf(existingBook.getTotalQuantity())); // Hiển thị totalQuantity
            locationField.setText(existingBook.getShelfLocation());
        }
        validateSaveButton.run(); // Kiểm tra nút save ban đầu


        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // Validate lần cuối
                if (titleField.getText().trim().isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Tiêu đề không được để trống.");
                    return null;
                }
                int parsedTotalQuantity;
                try {
                    parsedTotalQuantity = quantityField.getText().trim().isEmpty() ? 0 : Integer.parseInt(quantityField.getText().trim());
                    if (parsedTotalQuantity < 0) throw new NumberFormatException("Số lượng không âm.");
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Tổng số lượng phải là một số nguyên không âm.");
                    return null;
                }
                Integer parsedPageCount = null;
                if (!pageCountField.getText().trim().isEmpty()) {
                    try {
                        parsedPageCount = Integer.parseInt(pageCountField.getText().trim());
                        if (parsedPageCount < 0) throw new NumberFormatException("Số trang không âm.");
                    } catch (NumberFormatException e) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Số trang phải là một số nguyên không âm.");
                        return null;
                    }
                }

                String id;
                if (existingBook != null) {
                    id = existingBook.getId(); // Giữ ID cũ khi edit
                } else { // Sách mới
                    id = isbnField.getText().trim();
                    if (id.isEmpty() || !(id.length() == 13 && id.matches("\\d+")) && !(id.length() <= 10 && id.matches("\\d+X?"))) {
                        // Nếu ISBN không hợp lệ hoặc rỗng, tạo UUID
                        id = UUID.randomUUID().toString();
                        System.out.println("DEBUG_BMC_DIALOG: ISBN from field is invalid or empty, generated UUID for new book: " + id);
                    }
                }

                String finalIsbn10 = null;
                String finalIsbn13 = null;
                String isbnFromField = isbnField.getText().trim();
                if (!isbnFromField.isEmpty()){
                    if(isbnFromField.length() == 13 && isbnFromField.matches("\\d+")) finalIsbn13 = isbnFromField;
                    else if (isbnFromField.length() <=10 && isbnFromField.matches("\\d+X?")) finalIsbn10 = isbnFromField;
                }


                List<String> authorsList = Arrays.stream(authorsField.getText().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
                List<String> categoriesList = Arrays.stream(categoriesField.getText().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());

                // Sử dụng constructor mới của Book, truyền parsedTotalQuantity làm initialQuantity
                Book book = new Book(
                        id, titleField.getText().trim(), authorsList, publisherField.getText().trim(),
                        publishedDateField.getText().trim(), descriptionArea.getText().trim(), categoriesList,
                        thumbnailUrlField.getText().trim(),
                        (existingBook != null ? existingBook.getInfoLink() : null), // Giữ infoLink nếu edit
                        finalIsbn10, finalIsbn13, // Lấy từ isbnField đã validate (nếu có)
                        parsedPageCount,
                        (existingBook != null ? existingBook.getAverageRating() : null), // Giữ rating nếu edit
                        (existingBook != null ? existingBook.getRatingsCount() : null), // Giữ rating count nếu edit
                        parsedTotalQuantity // Đây sẽ là initialQuantity (totalQuantity và availableQuantity ban đầu)
                );
                book.setShelfLocation(locationField.getText().trim());

                // Nếu là edit, chúng ta cần xử lý availableQuantity một cách đặc biệt
                // BookManagementService.updateBookInLibrary sẽ làm việc này dựa trên totalQuantity mới
                // và availableQuantity cũ của existingBook.
                // Ở đây, đối tượng 'book' trả về từ dialog sẽ có availableQuantity = totalQuantity (do constructor).
                // Khi gọi service.updateBookInLibrary(book), service sẽ dùng totalQuantity của 'book' này
                // và availableQuantity CŨ của sách trong danh sách để tính toán lại.
                if (existingBook != null) {
                    // Quan trọng: Để service biết được availableQuantity gốc trước khi thay đổi totalQuantity
                    // Chúng ta truyền một đối tượng Book mới với totalQuantity mới,
                    // nhưng service sẽ lấy availableQuantity cũ từ existingBook trong list của nó.
                    // Hoặc, chúng ta có thể set availableQuantity của đối tượng 'book' này bằng
                    // availableQuantity cũ của 'existingBook' *TRƯỚC KHI* trả về từ dialog,
                    // để service nhận được một 'updatedBookFromUI' có totalQuantity mới và availableQuantity cũ.
                    // Cách này có vẻ rõ ràng hơn.
                    book.setAvailableQuantity(existingBook.getAvailableQuantity());
                }
                return book;
            }
            return null;
        });
        return dialog.showAndWait();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null); // Không cần header
        alert.setContentText(message);
        URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
        if (cssUrl != null) {
            alert.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
        }
        alert.showAndWait();
    }
}
package com.lms.quanlythuvien.controllers.admin;

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.exceptions.DeletionRestrictedException;
import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.services.library.GoogleBooksService;
import com.lms.quanlythuvien.utils.session.SessionManager; // Vẫn cần nếu BookDetailView dùng

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader; // Thêm để load FXML cho dialog
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent; // Thêm để lấy root của FXML
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
// import javafx.stage.Modality; // Nếu muốn dùng Stage thay Dialog
// import javafx.stage.Stage;    // Nếu muốn dùng Stage thay Dialog
// import javafx.scene.Scene;    // Nếu muốn dùng Stage thay Dialog


import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminBookManagementController implements Initializable {

    @FXML private TableView<Book> booksTableView;
    @FXML private TableColumn<Book, String> idColumn;
    @FXML private TableColumn<Book, String> titleColumn;
    @FXML private TableColumn<Book, String> authorsColumn;
    @FXML private TableColumn<Book, String> publisherColumn;
    @FXML private TableColumn<Book, String> publishedDateColumn;
    @FXML private TableColumn<Book, Integer> quantityColumn;
    @FXML private TableColumn<Book, String> locationColumn;

    @FXML private Button addBookButton;
    @FXML private Button editBookButton;
    @FXML private Button deleteBookButton;
    @FXML private TextField searchInLibraryField;

    private BookManagementService bookManagementService;
    private GoogleBooksService googleBooksService;

    private ObservableList<Book> observableBookList;
    private FilteredList<Book> filteredBookList;

    private AdminDashboardController dashboardController; // Để AdminBookCardController có thể gọi lại

    public void setDashboardController(AdminDashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bookManagementService = BookManagementService.getInstance();
        googleBooksService = new GoogleBooksService();

        observableBookList = FXCollections.observableArrayList();
        setupTableColumns();
        loadBooksToTable();

        booksTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean bookSelected = (newSelection != null);
            editBookButton.setDisable(!bookSelected);
            deleteBookButton.setDisable(!bookSelected);
        });

        searchInLibraryField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterBookTable(newValue);
        });

        editBookButton.setDisable(true);
        deleteBookButton.setDisable(true);
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleColumn.setCellFactory(column -> {
            return new TableCell<Book, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                        setGraphic(null);
                        setOnMouseClicked(null);
                    } else {
                        setText(item);
                        setStyle("-fx-cursor: hand; -fx-text-fill: #007bff;");
                        setOnMouseClicked(event -> {
                            if (event.getButton() == MouseButton.PRIMARY) {
                                Book selectedBook = getTableRow().getItem();
                                if (selectedBook != null) {
                                    // Gọi phương thức mở cửa sổ AdminBookCardView
                                    showAdminBookCardWindow(selectedBook);
                                }
                            }
                        });
                    }
                }
            };
        });

        authorsColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                (cellData.getValue().getAuthors() != null && !cellData.getValue().getAuthors().isEmpty()) ?
                        String.join(", ", cellData.getValue().getAuthors()) : "N/A"));
        publisherColumn.setCellValueFactory(new PropertyValueFactory<>("publisher"));
        publishedDateColumn.setCellValueFactory(new PropertyValueFactory<>("publishedDate"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("availableQuantity"));
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("shelfLocation"));
    }

    // --- PHƯƠNG THỨC MỚI/SỬA ĐỔI CHO VẤN ĐỀ 2 ---
    private void showAdminBookCardWindow(Book book) {
        if (book == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/quanlythuvien/fxml/admin/AdminBookCardView.fxml"));
            Parent bookCardNode = loader.load();

            AdminBookCardController cardController = loader.getController();
            // Truyền AdminBookManagementController (this) để cardController có thể gọi lại
            cardController.setData(book, this);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Thông Tin Sách (Admin Card)");
            dialog.getDialogPane().setContent(bookCardNode);
            applyDialogStyles(dialog); // Áp dụng CSS cho dialog

            // Thêm nút đóng (Close hoặc Cancel) cho Dialog
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            // Node closeButton = dialog.getDialogPane().lookupButton(ButtonType.CLOSE); // Nếu cần style riêng cho nút close

            dialog.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi Tải Giao Diện", "Không thể mở cửa sổ thông tin sách (AdminBookCardView).");
        }
    }
    // --- KẾT THÚC PHƯƠNG THỨC MỚI/SỬA ĐỔI ---


    private void loadBooksToTable() {
        List<Book> allBooks = bookManagementService.getAllBooksInLibrary();
        observableBookList.setAll(allBooks);
        if (filteredBookList == null) {
            filteredBookList = new FilteredList<>(observableBookList, p -> true);
            booksTableView.setItems(filteredBookList);
        }
        booksTableView.setPlaceholder(new Label(allBooks.isEmpty() ? "Thư viện chưa có sách." : "Không tìm thấy sách nào khớp."));
        System.out.println("DEBUG_BMC_LOAD: Books loaded. Count: " + observableBookList.size());
    }

    private void filterBookTable(String searchText) {
        if (filteredBookList == null) return;
        String lowerCaseFilter = (searchText == null) ? "" : searchText.toLowerCase().trim();
        filteredBookList.setPredicate(book -> {
            if (lowerCaseFilter.isEmpty()) return true;
            if (book.getTitle() != null && book.getTitle().toLowerCase().contains(lowerCaseFilter)) return true;
            if (book.getAuthors() != null && book.getAuthors().stream().anyMatch(author -> author.toLowerCase().contains(lowerCaseFilter))) return true;
            if (book.getId() != null && book.getId().toLowerCase().contains(lowerCaseFilter)) return true;
            if (book.getIsbn10() != null && book.getIsbn10().toLowerCase().contains(lowerCaseFilter)) return true;
            if (book.getCustomDisplayId() != null && book.getCustomDisplayId().toLowerCase().contains(lowerCaseFilter)) return true;
            if (book.getPublisher() != null && book.getPublisher().toLowerCase().contains(lowerCaseFilter)) return true;
            if (book.getShelfLocation() != null && book.getShelfLocation().toLowerCase().contains(lowerCaseFilter)) return true;
            if (book.getCategories() != null && book.getCategories().stream().anyMatch(cat -> cat.toLowerCase().contains(lowerCaseFilter))) return true;
            return false;
        });
    }

    @FXML
    private void handleAddBookAction(ActionEvent event) {
        openBookFormDialog(null);
    }

    @FXML
    private void handleEditBookAction(ActionEvent event) {
        Book selectedBook = booksTableView.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn sách", "Vui lòng chọn một cuốn sách để sửa.");
            return;
        }
        openBookFormDialog(selectedBook);
    }

    @FXML
    private void handleDeleteBookAction(ActionEvent event) {
        Book selectedBook = booksTableView.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn sách", "Vui lòng chọn một cuốn sách để xóa.");
            return;
        }
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Xác nhận Xóa");
        confirmationDialog.setHeaderText("Xóa Sách: " + selectedBook.getTitle());
        confirmationDialog.setContentText("Bạn có chắc chắn muốn xóa cuốn sách này không?");
        applyDialogStyles(confirmationDialog);
        Optional<ButtonType> result = confirmationDialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (bookManagementService.deleteBookFromLibrary(selectedBook.getId())) {
                    refreshBookDisplay();
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã xóa sách thành công!");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi Xóa Sách", "Không thể xóa sách. Sách có thể không còn tồn tại.");
                }
            } catch (DeletionRestrictedException e) {
                showAlert(Alert.AlertType.WARNING, "Không thể xóa", e.getMessage());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi Hệ thống", "Lỗi không mong muốn: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleSearchInLibraryAction(ActionEvent event) {
        filterBookTable(searchInLibraryField.getText());
    }

    public AdminDashboardController getDashboardController() {
        return this.dashboardController;
    }

    public void openBookFormDialog(Book bookToEdit) {
        Optional<Book> resultBookOptional = showBookDialog(bookToEdit);
        resultBookOptional.ifPresent(bookFromDialog -> {
            boolean success;
            String actionVerb = (bookToEdit == null) ? "thêm" : "cập nhật";
            String successMessage = "Đã " + actionVerb + " sách thành công: " + bookFromDialog.getTitle();
            String errorMessage = "Không thể " + actionVerb + " sách. ";

            if (bookToEdit == null) {
                Optional<Book> addedBook = bookManagementService.addBookToLibrary(bookFromDialog);
                success = addedBook.isPresent();
                if(!success) errorMessage += "Mã sách/ISBN có thể đã tồn tại, dữ liệu không hợp lệ, hoặc lỗi ràng buộc UNIQUE.";
            } else {
                bookFromDialog.setInternalId(bookToEdit.getInternalId());
                bookFromDialog.setAverageRating(bookToEdit.getAverageRating());
                bookFromDialog.setRatingsCount(bookToEdit.getRatingsCount());
                success = bookManagementService.updateBookInLibrary(bookFromDialog);
                if(!success) errorMessage += "ISBN mới có thể bị trùng, sách không còn tồn tại, hoặc lỗi ràng buộc UNIQUE.";
            }

            if (success) {
                refreshBookDisplay();
                showAlert(Alert.AlertType.INFORMATION, "Thành công", successMessage);
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", errorMessage);
            }
        });
    }

    public void refreshBookDisplay() {
        loadBooksToTable();
    }

    private Optional<Book> showBookDialog(Book existingBook) {
        Dialog<Book> dialog = new Dialog<>();
        dialog.setTitle(existingBook == null ? "Thêm Sách Mới" : "Chỉnh Sửa Thông Tin Sách");
        dialog.setHeaderText(existingBook == null ? "Nhập chi tiết sách:" : "Chỉnh sửa chi tiết cho: " + existingBook.getTitle());
        applyDialogStyles(dialog);

        ButtonType saveButtonType = ButtonType.OK;
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField isbnField = new TextField(); isbnField.setPromptText("ISBN-13 (Mã sách chính)");
        Button fetchButton = new Button("Lấy từ Google"); fetchButton.getStyleClass().add("primary-button-small");
        HBox isbnBox = new HBox(5, isbnField, fetchButton); HBox.setHgrow(isbnField, Priority.ALWAYS);

        TextField titleField = new TextField(); titleField.setPromptText("Tiêu đề sách (*)");
        TextField authorsField = new TextField(); authorsField.setPromptText("Tác giả (cách bởi dấu phẩy)");
        TextField publisherField = new TextField(); publisherField.setPromptText("Nhà xuất bản");
        TextField publishedDateField = new TextField(); publishedDateField.setPromptText("Năm XB (YYYY hoặc yy-MM-dd)");
        TextField categoriesField = new TextField(); categoriesField.setPromptText("Thể loại (cách bởi dấu phẩy)");
        TextArea descriptionArea = new TextArea(); descriptionArea.setPromptText("Mô tả chi tiết"); descriptionArea.setPrefRowCount(4); descriptionArea.setWrapText(true);
        TextField thumbnailUrlField = new TextField(); thumbnailUrlField.setPromptText("URL ảnh bìa");
        TextField pageCountField = new TextField(); pageCountField.setPromptText("Số trang (số nguyên)");
        TextField totalQuantityField = new TextField(); totalQuantityField.setPromptText("Tổng số lượng (*)");
        TextField shelfLocationField = new TextField(); shelfLocationField.setPromptText("Vị trí trên kệ");
        TextField customDisplayIdField = new TextField(); customDisplayIdField.setPromptText("Mã hiển thị phụ (nếu có)");
        TextField isbn10Field = new TextField(); isbn10Field.setPromptText("ISBN-10 (nếu có)");

        int rowIndex = 0;
        grid.add(new Label("Mã Sách/ISBN-13:"), 0, rowIndex); grid.add(isbnBox, 1, rowIndex++, 2, 1);
        grid.add(new Label("Tiêu đề (*):"), 0, rowIndex); grid.add(titleField, 1, rowIndex++, 2, 1);
        grid.add(new Label("Tác giả:"), 0, rowIndex); grid.add(authorsField, 1, rowIndex++, 2, 1);
        grid.add(new Label("Nhà XB:"), 0, rowIndex); grid.add(publisherField, 1, rowIndex++, 2, 1);
        grid.add(new Label("Năm XB:"), 0, rowIndex); grid.add(publishedDateField, 1, rowIndex++, 2, 1);
        grid.add(new Label("Thể loại:"), 0, rowIndex); grid.add(categoriesField, 1, rowIndex++, 2, 1);
        grid.add(new Label("Mô tả:"), 0, rowIndex); grid.add(descriptionArea, 1, rowIndex++, 2, 1);
        grid.add(new Label("URL Ảnh bìa:"), 0, rowIndex); grid.add(thumbnailUrlField, 1, rowIndex++, 2, 1);
        grid.add(new Label("Số trang:"), 0, rowIndex); grid.add(pageCountField, 1, rowIndex++, 2, 1);
        grid.add(new Label("Tổng SL (*):"), 0, rowIndex); grid.add(totalQuantityField, 1, rowIndex++, 2, 1);
        grid.add(new Label("Vị trí kệ:"), 0, rowIndex); grid.add(shelfLocationField, 1, rowIndex++, 2, 1);
        grid.add(new Label("Mã hiển thị phụ:"), 0, rowIndex); grid.add(customDisplayIdField, 1, rowIndex++, 2, 1);
        grid.add(new Label("ISBN-10:"), 0, rowIndex); grid.add(isbn10Field, 1, rowIndex, 2, 1);

        dialog.getDialogPane().setContent(grid);
        Node saveButtonNode = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButtonNode.setDisable(true);

        Runnable validateFields = () -> {
            boolean disable = titleField.getText().trim().isEmpty() || totalQuantityField.getText().trim().isEmpty();
            try {
                if (!totalQuantityField.getText().trim().isEmpty() && Integer.parseInt(totalQuantityField.getText().trim()) < 0) disable = true;
                if (!pageCountField.getText().trim().isEmpty() && Integer.parseInt(pageCountField.getText().trim()) < 0) disable = true;
            } catch (NumberFormatException e) { disable = true; }
            saveButtonNode.setDisable(disable);
        };
        titleField.textProperty().addListener((obs, oldV, newV) -> validateFields.run());
        totalQuantityField.textProperty().addListener((obs, oldV, newV) -> validateFields.run());
        pageCountField.textProperty().addListener((obs, oldV, newV) -> validateFields.run());

        fetchButton.setOnAction(e -> {
            String isbnToFetch = isbnField.getText().trim();
            if (isbnToFetch.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Thiếu ISBN", "Vui lòng nhập ISBN để lấy thông tin.");
                return;
            }
            fetchButton.setDisable(true);
            dialog.setHeaderText("Đang tải thông tin từ Google Books...");
            Task<Optional<Book>> fetchTask = new Task<>() {
                @Override protected Optional<Book> call() { return googleBooksService.getBookDetailsByISBN(isbnToFetch); }
            };
            fetchTask.setOnSucceeded(ev -> Platform.runLater(() -> {
                dialog.setHeaderText(existingBook == null ? "Nhập chi tiết sách:" : "Chỉnh sửa chi tiết cho: " + (existingBook!=null?existingBook.getTitle():""));
                fetchButton.setDisable(false);
                Optional<Book> gBookOpt = fetchTask.getValue();
                if (gBookOpt.isPresent()) {
                    Book gBook = gBookOpt.get();
                    titleField.setText(gBook.getTitleOrDefault(""));
                    authorsField.setText(gBook.getAuthorsFormatted(""));
                    publisherField.setText(gBook.getPublisherOrDefault(""));
                    publishedDateField.setText(gBook.getPublishedDateOrDefault(""));
                    categoriesField.setText(gBook.getCategoriesFormatted(""));
                    descriptionArea.setText(gBook.getDescriptionOrDefault(""));
                    thumbnailUrlField.setText(gBook.getThumbnailUrl() != null ? gBook.getThumbnailUrl() : "");
                    pageCountField.setText(gBook.getPageCount() != null ? String.valueOf(gBook.getPageCount()) : "");
                    if (existingBook == null) {
                        isbnField.setText(gBook.getId() != null ? gBook.getId() : isbnToFetch);
                        isbn10Field.setText(gBook.getIsbn10() != null ? gBook.getIsbn10() : "");
                    }
                } else { showAlert(Alert.AlertType.INFORMATION, "Không tìm thấy", "Không có thông tin cho ISBN: " + isbnToFetch); }
                validateFields.run();
            }));
            fetchTask.setOnFailed(ev -> Platform.runLater(() -> {
                dialog.setHeaderText(existingBook == null ? "Nhập chi tiết sách:" : "Chỉnh sửa chi tiết cho: " + (existingBook!=null?existingBook.getTitle():""));
                fetchButton.setDisable(false);
                showAlert(Alert.AlertType.ERROR, "Lỗi API", "Không thể lấy thông tin từ Google Books.");
            }));
            new Thread(fetchTask).start();
        });

        if (existingBook != null) {
            isbnField.setText(existingBook.getId());
            isbnField.setEditable(false); fetchButton.setDisable(true);
            titleField.setText(existingBook.getTitle());
            authorsField.setText(existingBook.getAuthorsFormatted(""));
            publisherField.setText(existingBook.getPublisher());
            publishedDateField.setText(existingBook.getPublishedDate());
            categoriesField.setText(existingBook.getCategoriesFormatted(""));
            descriptionArea.setText(existingBook.getDescription());
            thumbnailUrlField.setText(existingBook.getThumbnailUrl());
            pageCountField.setText(existingBook.getPageCount() != null ? String.valueOf(existingBook.getPageCount()) : "");
            totalQuantityField.setText(String.valueOf(existingBook.getTotalQuantity()));
            shelfLocationField.setText(existingBook.getShelfLocation());
            customDisplayIdField.setText(existingBook.getCustomDisplayId());
            isbn10Field.setText(existingBook.getIsbn10());
        }
        validateFields.run();
        Platform.runLater(titleField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                int parsedTotalQuantity; Integer parsedPageCount = null;
                try {
                    parsedTotalQuantity = Integer.parseInt(totalQuantityField.getText().trim());
                    if (parsedTotalQuantity < 0) throw new NumberFormatException("Số lượng không được âm");
                    if (!pageCountField.getText().trim().isEmpty()) {
                        parsedPageCount = Integer.parseInt(pageCountField.getText().trim());
                        if (parsedPageCount < 0) throw new NumberFormatException("Số trang không được âm");
                    }
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi Dữ Liệu", "Số lượng và Số trang (nếu có) phải là số nguyên không âm.");
                    return null;
                }

                String bookPrimaryId = isbnField.getText().trim();
                if (existingBook == null) {
                    if (bookPrimaryId.isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Thiếu thông tin", "Mã sách ISBN-13 là bắt buộc cho sách mới.");
                        return null;
                    }
                } else {
                    bookPrimaryId = existingBook.getId();
                }

                List<String> authorsList = Arrays.stream(authorsField.getText().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
                List<String> categoriesList = Arrays.stream(categoriesField.getText().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());

                Book resultBook = new Book(
                        bookPrimaryId, titleField.getText().trim(), authorsList,
                        publisherField.getText().trim(), publishedDateField.getText().trim(),
                        descriptionArea.getText().trim(), categoriesList,
                        thumbnailUrlField.getText().trim(),
                        (existingBook != null ? existingBook.getInfoLink() : null),
                        isbn10Field.getText().trim(),
                        parsedPageCount,
                        (existingBook != null ? existingBook.getAverageRating() : 0.0),
                        (existingBook != null ? existingBook.getRatingsCount() : 0),
                        parsedTotalQuantity
                );

                String customIdText = customDisplayIdField.getText().trim();
                resultBook.setCustomDisplayId(customIdText.isEmpty() ? null : customIdText); // Đã sửa ở đây

                resultBook.setShelfLocation(shelfLocationField.getText().trim());

                if (existingBook != null) {
                    resultBook.setInternalId(existingBook.getInternalId());
                }
                return resultBook;
            }
            return null;
        });
        return dialog.showAndWait();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyDialogStyles(alert);
        alert.showAndWait();
    }

    private void applyDialogStyles(Dialog<?> dialog) {
        try {
            URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
            if (cssUrl != null) {
                dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) { System.err.println("WARN_DIALOG_CSS: Failed to load CSS for dialog: " + e.getMessage()); }
    }
}
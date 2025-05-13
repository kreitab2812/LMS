package com.lms.quanlythuvien.controllers;

import com.lms.quanlythuvien.models.Book;
import com.lms.quanlythuvien.services.BookManagementService;
import com.lms.quanlythuvien.services.GoogleBooksService;
import javafx.application.Platform;
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

    //<editor-fold desc="FXML Injections for BookManagementView.fxml">
    @FXML private TableView<Book> booksTableView;
    @FXML private TableColumn<Book, String> idColumn;
    @FXML private TableColumn<Book, String> titleColumn;
    @FXML private TableColumn<Book, String> authorsColumn;
    @FXML private TableColumn<Book, String> publisherColumn;
    @FXML private TableColumn<Book, String> publishedDateColumn;
    // UPDATED: Đổi tên FXML hoặc ý nghĩa cột này cho rõ ràng hơn.
    // Hiện tại sẽ cho nó hiển thị availableQuantity.
    @FXML private TableColumn<Book, Integer> quantityColumn; // Sẽ hiển thị số lượng có sẵn (availableQuantity)
    // NEW: Nếu muốn hiển thị cả tổng số lượng, cậu cần thêm một TableColumn mới trong FXML
    // và khai báo nó ở đây, ví dụ:
    // @FXML private TableColumn<Book, Integer> totalQuantityColumn;

    @FXML private TableColumn<Book, String> locationColumn;

    @FXML private Button addBookButton;
    @FXML private Button editBookButton;
    @FXML private Button deleteBookButton;

    @FXML private TextField searchInLibraryField;
    //</editor-fold>

    private BookManagementService bookManagementService;
    private GoogleBooksService googleBooksService;
    private ObservableList<Book> observableBookList;
    private FilteredList<Book> filteredBookList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bookManagementService = new BookManagementService();
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
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorsColumn.setCellValueFactory(cellData -> {
            List<String> authors = cellData.getValue().getAuthors();
            return new javafx.beans.property.SimpleStringProperty(
                    (authors != null && !authors.isEmpty()) ? String.join(", ", authors) : "N/A"
            );
        });
        publisherColumn.setCellValueFactory(new PropertyValueFactory<>("publisher"));
        publishedDateColumn.setCellValueFactory(new PropertyValueFactory<>("publishedDate"));

        // UPDATED: quantityColumn giờ sẽ hiển thị availableQuantity
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("availableQuantity"));
        // NEW: Nếu cậu thêm cột totalQuantityColumn:
        // totalQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("totalQuantity"));

        locationColumn.setCellValueFactory(new PropertyValueFactory<>("shelfLocation"));
    }

    private void loadBooksToTable() {
        observableBookList.setAll(bookManagementService.getAllBooksInLibrary());
        filteredBookList = new FilteredList<>(observableBookList, p -> true);
        booksTableView.setItems(filteredBookList);
        System.out.println("Books loaded into table: " + observableBookList.size());
    }

    private void filterBookTable(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            filteredBookList.setPredicate(p -> true);
        } else {
            String lowerCaseFilter = searchText.toLowerCase();
            filteredBookList.setPredicate(book -> {
                if (book.getTitle() != null && book.getTitle().toLowerCase().contains(lowerCaseFilter)) return true;
                if (book.getAuthors() != null && book.getAuthors().stream().anyMatch(author -> author.toLowerCase().contains(lowerCaseFilter))) return true;
                if (book.getIsbn13() != null && book.getIsbn13().contains(lowerCaseFilter)) return true;
                if (book.getIsbn10() != null && book.getIsbn10().contains(lowerCaseFilter)) return true;
                return false;
            });
        }
    }


    @FXML
    private void handleAddBookAction(ActionEvent event) {
        System.out.println("Add New Book action triggered.");
        Optional<Book> newBookOptional = showBookDialog(null);
        newBookOptional.ifPresent(book -> {
            if (book.getId() == null || book.getId().trim().isEmpty()) {
                if (book.getIsbn13() != null && !book.getIsbn13().trim().isEmpty()) {
                    book.setId(book.getIsbn13());
                } else if (book.getIsbn10() != null && !book.getIsbn10().trim().isEmpty()) {
                    book.setId(book.getIsbn10());
                } else {
                    book.setId(UUID.randomUUID().toString());
                }
            }

            if (bookManagementService.addBookToLibrary(book)) {
                observableBookList.add(book);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Book added successfully: " + book.getTitle());
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add book. Book ID/ISBN '" + book.getId() + "' might already exist.");
            }
        });
    }

    @FXML
    private void handleEditBookAction(ActionEvent event) {
        Book selectedBook = booksTableView.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a book to edit.");
            return;
        }
        Optional<Book> updatedBookOptional = showBookDialog(selectedBook);
        updatedBookOptional.ifPresent(updatedBook -> {
            // Logic cập nhật trong BookManagementService cần đảm bảo tính nhất quán
            // của totalQuantity và availableQuantity.
            if (bookManagementService.updateBookInLibrary(updatedBook)) {
                int index = observableBookList.indexOf(selectedBook);
                if (index != -1) {
                    observableBookList.set(index, updatedBook);
                    // NEW: Có thể cần refresh TableView để đảm bảo các cột được cập nhật đúng
                    // booksTableView.refresh();
                }
                showAlert(Alert.AlertType.INFORMATION, "Success", "Book updated successfully: " + updatedBook.getTitle());
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update book.");
            }
        });
    }

    @FXML
    private void handleDeleteBookAction(ActionEvent event) {
        Book selectedBook = booksTableView.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a book to delete.");
            return;
        }
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Confirm Deletion");
        confirmationDialog.setHeaderText("Delete Book: " + selectedBook.getTitle());
        confirmationDialog.setContentText("Are you sure you want to delete this book?");
        Optional<ButtonType> result = confirmationDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (bookManagementService.deleteBookFromLibrary(selectedBook.getId())) {
                observableBookList.remove(selectedBook);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Book deleted successfully!");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete book.");
            }
        }
    }

    @FXML
    private void handleSearchInLibraryAction(ActionEvent event) {
        filterBookTable(searchInLibraryField.getText());
        if (filteredBookList.isEmpty() && !searchInLibraryField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION,"Search Result", "No books found in library matching your query.");
        }
    }

    private Optional<Book> showBookDialog(Book existingBook) {
        Dialog<Book> dialog = new Dialog<>();
        dialog.setTitle(existingBook == null ? "Add New Book" : "Edit Book");
        dialog.setHeaderText(existingBook == null ? "Enter book details:" : "Edit details for: " + (existingBook.getTitle() != null ? existingBook.getTitle() : "Selected Book"));
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/lms/quanlythuvien/css/styles.css").toExternalForm());

        ButtonType saveButtonType = ButtonType.OK;
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        TextField isbnField = new TextField(); isbnField.setPromptText("ISBN-13 (used as Book ID if new)");
        Button fetchButton = new Button("Fetch from Google"); fetchButton.getStyleClass().add("primary-button-small");

        TextField titleField = new TextField(); titleField.setPromptText("Title*");
        TextField authorsField = new TextField(); authorsField.setPromptText("Authors (comma-separated)");
        TextField publisherField = new TextField(); publisherField.setPromptText("Publisher");
        TextField publishedDateField = new TextField(); publishedDateField.setPromptText("Published Date (YYYY or YYYY-MM-DD)");
        TextField categoriesField = new TextField(); categoriesField.setPromptText("Categories (comma-separated)");
        TextArea descriptionArea = new TextArea(); descriptionArea.setPromptText("Description"); descriptionArea.setPrefRowCount(4); descriptionArea.setWrapText(true);
        TextField thumbnailUrlField = new TextField(); thumbnailUrlField.setPromptText("Cover Image URL");
        TextField pageCountField = new TextField(); pageCountField.setPromptText("Page Count");
        // UPDATED: TextField này giờ sẽ nhập tổng số lượng (totalQuantity)
        TextField quantityField = new TextField(); quantityField.setPromptText("Total Quantity in Stock*");
        TextField locationField = new TextField(); locationField.setPromptText("Shelf Location");

        grid.add(new Label("ISBN:"), 0, 0);
        HBox isbnBox = new HBox(5, isbnField, fetchButton); HBox.setHgrow(isbnField, Priority.ALWAYS);
        grid.add(isbnBox, 1, 0, 2, 1);

        grid.add(new Label("Title*:"), 0, 1); grid.add(titleField, 1, 1, 2, 1);
        grid.add(new Label("Authors:"), 0, 2); grid.add(authorsField, 1, 2, 2, 1);
        grid.add(new Label("Publisher:"), 0, 3); grid.add(publisherField, 1, 3, 2, 1);
        grid.add(new Label("Published Date:"), 0, 4); grid.add(publishedDateField, 1, 4, 2, 1);
        grid.add(new Label("Categories:"), 0, 5); grid.add(categoriesField, 1, 5, 2, 1);
        grid.add(new Label("Description:"), 0, 6); grid.add(descriptionArea, 1, 6, 2, 1);
        grid.add(new Label("Cover URL:"), 0, 7); grid.add(thumbnailUrlField, 1, 7, 2, 1);
        grid.add(new Label("Page Count:"), 0, 8); grid.add(pageCountField, 1, 8, 2, 1);
        // UPDATED: Label cho quantity field
        grid.add(new Label("Total Quantity*:"), 0, 9); grid.add(quantityField, 1, 9, 2, 1);
        grid.add(new Label("Location:"), 0, 10); grid.add(locationField, 1, 10, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(500);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        Runnable validateSaveButton = () -> {
            boolean disable = titleField.getText().trim().isEmpty() ||
                    quantityField.getText().trim().isEmpty(); // quantityField là totalQuantity
            try {
                if (!quantityField.getText().trim().isEmpty()) Integer.parseInt(quantityField.getText().trim());
            } catch (NumberFormatException e) {
                disable = true;
            }
            saveButton.setDisable(disable);
        };
        titleField.textProperty().addListener((obs, oldVal, newVal) -> validateSaveButton.run());
        quantityField.textProperty().addListener((obs, oldVal, newVal) -> validateSaveButton.run());


        fetchButton.setOnAction(e -> {
            String isbn = isbnField.getText().trim();
            if (isbn.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Input Needed", "Please enter an ISBN to fetch details.");
                return;
            }
            fetchButton.setDisable(true);
            Label loadingLabel = new Label("Fetching...");
            grid.add(loadingLabel, 1, 11, 2, 1);

            Task<Optional<Book>> fetchTask = new Task<>() {
                @Override protected Optional<Book> call() { return googleBooksService.getBookDetailsByISBN(isbn); }
            };
            fetchTask.setOnSucceeded(workerStateEvent -> {
                Platform.runLater(() -> {
                    grid.getChildren().remove(loadingLabel);
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
                        isbnField.setText(isbn);
                        validateSaveButton.run();
                    } else {
                        showAlert(Alert.AlertType.INFORMATION, "Not Found", "No book details found on Google Books for ISBN: " + isbn);
                    }
                });
            });
            fetchTask.setOnFailed(workerStateEvent -> {
                Platform.runLater(() -> {
                    grid.getChildren().remove(loadingLabel);
                    fetchButton.setDisable(false);
                    showAlert(Alert.AlertType.ERROR, "API Error", "Error fetching book details from Google Books.");
                    if(fetchTask.getException() != null) fetchTask.getException().printStackTrace();
                });
            });
            new Thread(fetchTask).start();
        });

        if (existingBook != null) {
            isbnField.setText(existingBook.getIsbn13() != null ? existingBook.getIsbn13() : existingBook.getIsbn10());
            isbnField.setEditable(false); // ISBN không nên sửa khi edit, vì nó là ID
            fetchButton.setDisable(true);
            titleField.setText(existingBook.getTitle());
            authorsField.setText(existingBook.getAuthors() != null ? String.join(", ", existingBook.getAuthors()) : "");
            publisherField.setText(existingBook.getPublisher());
            publishedDateField.setText(existingBook.getPublishedDate());
            categoriesField.setText(existingBook.getCategories() != null ? String.join(", ", existingBook.getCategories()) : "");
            descriptionArea.setText(existingBook.getDescription());
            thumbnailUrlField.setText(existingBook.getThumbnailUrl());
            pageCountField.setText(existingBook.getPageCount() != null ? String.valueOf(existingBook.getPageCount()) : "");
            // UPDATED: Hiển thị totalQuantity khi edit
            quantityField.setText(String.valueOf(existingBook.getTotalQuantity()));
            locationField.setText(existingBook.getShelfLocation());
            validateSaveButton.run();
        } else {
            validateSaveButton.run();
        }


        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (titleField.getText().trim().isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Validation Error", "Title cannot be empty.");
                    return null;
                }
                int parsedTotalQuantity; // UPDATED: Biến này giờ là totalQuantity
                try {
                    parsedTotalQuantity = quantityField.getText().trim().isEmpty() ? 0 : Integer.parseInt(quantityField.getText().trim());
                    if (parsedTotalQuantity < 0) throw new NumberFormatException("Quantity cannot be negative.");
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Input", "Total Quantity must be a valid non-negative number.");
                    return null;
                }
                Integer parsedPageCount = null;
                if (!pageCountField.getText().trim().isEmpty()) {
                    try {
                        parsedPageCount = Integer.parseInt(pageCountField.getText().trim());
                        if (parsedPageCount < 0) throw new NumberFormatException("Page count cannot be negative.");
                    } catch (NumberFormatException e) {
                        showAlert(Alert.AlertType.ERROR, "Invalid Input", "Page count must be a valid non-negative number.");
                        return null;
                    }
                }

                String id = (existingBook != null) ? existingBook.getId() :
                        (isbnField.getText().trim().isEmpty() ? UUID.randomUUID().toString() : isbnField.getText().trim());

                List<String> authorsList = Arrays.stream(authorsField.getText().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
                List<String> categoriesList = Arrays.stream(categoriesField.getText().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());

                String currentIsbn10 = null;
                String currentIsbn13 = null;
                if(id.length() == 13 && id.matches("\\d+")) currentIsbn13 = id;
                else if (id.length() <= 10 && id.matches("\\d+X?")) currentIsbn10 = id;

                if (currentIsbn13 == null && isbnField.getText().trim().length() == 13 && isbnField.getText().trim().matches("\\d+")) currentIsbn13 = isbnField.getText().trim();
                if (currentIsbn10 == null && isbnField.getText().trim().length() <=10 && isbnField.getText().trim().matches("\\d+X?")) currentIsbn10 = isbnField.getText().trim();

                // UPDATED: Gọi constructor mới của Book với parsedTotalQuantity là initialQuantity
                Book book = new Book(
                        id, titleField.getText().trim(), authorsList, publisherField.getText().trim(),
                        publishedDateField.getText().trim(), descriptionArea.getText().trim(), categoriesList,
                        thumbnailUrlField.getText().trim(),
                        (existingBook != null ? existingBook.getInfoLink() : null),
                        currentIsbn10, currentIsbn13,
                        parsedPageCount,
                        (existingBook != null ? existingBook.getAverageRating() : null),
                        (existingBook != null ? existingBook.getRatingsCount() : null),
                        parsedTotalQuantity // Đây là initialQuantity, sẽ set cả totalQuantity và availableQuantity
                );
                // NEW: Nếu là edit, và totalQuantity thay đổi, BookManagementService cần logic để cập nhật availableQuantity
                // một cách chính xác (ví dụ, không thể nhỏ hơn số sách đang cho mượn).
                // Đối với trường hợp edit, chúng ta đã có đối tượng 'existingBook'.
                // Nếu chỉ thay đổi totalQuantity, availableQuantity cần được điều chỉnh cẩn thận.
                // Cách đơn giản nhất là BookManagementService sẽ xử lý việc này.
                // Hoặc, nếu Book object tự quản lý, thì setter cho totalQuantity trong Book.java cần logic đó.
                // Hiện tại, constructor của Book sẽ đặt availableQuantity = totalQuantity.
                // Nếu đây là sách đang edit (existingBook != null), chúng ta cần bảo toàn availableQuantity
                // hoặc tính toán lại nó dựa trên totalQuantity mới và số sách đang thực sự được mượn.

                // Tạm thời, nếu là edit, ta sẽ set lại totalQuantity, còn availableQuantity sẽ được
                // service xử lý hoặc giữ nguyên nếu logic cho phép.
                // Tuy nhiên, constructor mới của Book đã tự động set availableQuantity = totalQuantity.
                // Điều này có thể không đúng cho sách đang được edit và có sách đang mượn.
                // -> Cần một cách tốt hơn để cập nhật Book khi edit.
                if (existingBook != null) {
                    // Giữ lại các thông tin không thay đổi từ existingBook
                    book.setAvailableQuantity(existingBook.getAvailableQuantity()); // QUAN TRỌNG: Giữ lại số lượng đang có sẵn
                    // Sau đó điều chỉnh availableQuantity nếu totalQuantity mới < availableQuantity hiện tại
                    if (book.getTotalQuantity() < book.getAvailableQuantity()) {
                        // Logic này nên ở service, hoặc setter của Book.
                        // Ví dụ: book.setAvailableQuantity(book.getTotalQuantity());
                        // Hoặc báo lỗi nếu không hợp lệ (ví dụ tổng mới < số đang mượn)
                    }
                }
                // else: sách mới thì availableQuantity = totalQuantity là đúng


                book.setShelfLocation(locationField.getText().trim());
                // Không còn book.setQuantityInStock(parsedQuantity); nữa vì constructor đã xử lý
                return book;
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
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/lms/quanlythuvien/css/styles.css").toExternalForm());
        alert.showAndWait();
    }
}
package com.lms.quanlythuvien.controllers;

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.models.Book;
import com.lms.quanlythuvien.models.Quote;
import com.lms.quanlythuvien.services.GoogleBooksService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML private ComboBox<String> searchTypeComboBox;
    @FXML private TextField searchField;
    @FXML private Label timeLabel;
    @FXML private Label dateLabel;
    @FXML private ImageView userAvatar;
    @FXML private Label usernameLabel;
    @FXML private HBox userInfoArea;

    // Khai báo các nút menu
    @FXML private Button homeButton;
    @FXML private Button bookManagementButton;
    @FXML private Button userManagementButton;
    @FXML private Button loanManagementButton; // ĐÃ ĐỔI TÊN TỪ analyticsButton

    @FXML private StackPane mainContentArea;

    // Các biến cho HomeView
    private Label greetingLabel;
    private Label quoteTextLabel;
    private Label quoteAuthorLabel;

    private GoogleBooksService googleBooksService;
    private List<Quote> quotesList; // Danh sách các Quote
    private Timeline clockTimeline;
    private Image defaultBookCoverImage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        googleBooksService = new GoogleBooksService();
        searchTypeComboBox.setItems(FXCollections.observableArrayList("All", "Title", "Author", "Subjects", "Text"));
        searchTypeComboBox.setValue("All");
        usernameLabel.setText("Admin"); // Hoặc lấy từ User session nếu có

        try {
            // Nên kiểm tra null cho getResourceAsStream
            if (getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_avatar.png") != null) {
                userAvatar.setImage(new Image(getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_avatar.png")));
            }
            if (getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_book_cover.png") != null) {
                defaultBookCoverImage = new Image(getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_book_cover.png"));
            }
        } catch (Exception e) {
            System.err.println("AdminDashboard: Error loading default images: " + e.getMessage());
            e.printStackTrace(); // In chi tiết lỗi
        }

        setupClock();
        loadHomeView(); // Tải HomeView làm mặc định
        setActiveMenuButton(homeButton); // Đặt nút Home là active ban đầu
    }

    private void setupClock() {
        clockTimeline = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            LocalTime currentTime = LocalTime.now();
            LocalDateTime currentDateTime = LocalDateTime.now();
            timeLabel.setText(currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            dateLabel.setText(currentDateTime.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")));
            if (greetingLabel != null) { // greetingLabel chỉ có khi HomeView được load
                updateGreeting(currentTime);
            }
        }), new KeyFrame(Duration.seconds(1)));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    private void updateGreeting(LocalTime time) {
        if (greetingLabel == null) return;
        int hour = time.getHour();
        String greetingText = "Good Evening, Admin!"; // Mặc định
        if (hour >= 5 && hour < 12) {
            greetingText = "Good Morning, Admin!";
        } else if (hour >= 12 && hour < 18) {
            greetingText = "Good Afternoon, Admin!";
        }
        greetingLabel.setText(greetingText);
    }

    private void loadHomeView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/quanlythuvien/fxml/HomeView.fxml"));
            Node homeViewNode = loader.load();
            mainContentArea.getChildren().setAll(homeViewNode);

            // Lấy các Label từ HomeView (nếu HomeView.fxml có các fx:id này)
            greetingLabel = (Label) loader.getNamespace().get("greetingLabel");
            quoteTextLabel = (Label) loader.getNamespace().get("quoteTextLabel");
            quoteAuthorLabel = (Label) loader.getNamespace().get("quoteAuthorLabel");

            if (greetingLabel != null) updateGreeting(LocalTime.now()); // Cập nhật greeting ngay
            displayRandomQuote(); // Hiển thị quote

        } catch (Exception e) {
            e.printStackTrace();
            mainContentArea.getChildren().setAll(new Label("Error loading Home content: " + e.getMessage()));
        }
    }

    private void displayRandomQuote() {
        if (quoteTextLabel == null || quoteAuthorLabel == null) {
            System.err.println("AdminDashboard: Quote labels not found in HomeView.fxml's namespace for dynamic display.");
            // Nếu không tìm thấy, không làm gì cả hoặc hiển thị thông báo lỗi tĩnh
            return;
        }
        // Khởi tạo danh sách quotes
        quotesList = List.of(
                new Quote("The only way to do great work is to love what you do.", "Steve Jobs"),
                new Quote("A reader lives a thousand lives before he dies. The man who never reads lives only one.", "George R.R. Martin"),
                new Quote("Books are a uniquely portable magic.", "Stephen King"),
                new Quote("There is no friend as loyal as a book.", "Ernest Hemingway"),
                new Quote("So many books, so little time.", "Frank Zappa")
        );
        if (!quotesList.isEmpty()) {
            Random random = new Random();
            Quote randomQuote = quotesList.get(random.nextInt(quotesList.size()));
            quoteTextLabel.setText("\"" + randomQuote.getText() + "\"");
            quoteAuthorLabel.setText("- " + randomQuote.getAuthor());
        } else {
            quoteTextLabel.setText("No quotes available today.");
            quoteAuthorLabel.setText("");
        }
    }

    private VBox createBookDisplay(Book book) {
        VBox bookPane = new VBox(3); // Giảm spacing một chút nếu cần
        bookPane.setPadding(new Insets(8));
        bookPane.getStyleClass().add("book-item-pane");
        bookPane.setAlignment(Pos.TOP_CENTER);

        ImageView coverImage = new ImageView();
        coverImage.setFitHeight(130); // Chiều cao mong muốn
        coverImage.setFitWidth(85);   // Chiều rộng mong muốn
        coverImage.setPreserveRatio(true); // Giữ tỉ lệ ảnh
        VBox.setMargin(coverImage, new Insets(0, 0, 5, 0)); // Margin dưới ảnh
        loadBookCoverAsync(book.getThumbnailUrl(), coverImage);

        Label titleLabel = new Label(book.getTitle() != null ? book.getTitle() : "N/A");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(115); // Giới hạn chiều rộng để wrap text
        titleLabel.setMinHeight(28); // Đủ cho 2 dòng text
        titleLabel.getStyleClass().add("book-item-title");
        titleLabel.setTextAlignment(TextAlignment.CENTER);

        Label authorsLabel = new Label((book.getAuthors() != null && !book.getAuthors().isEmpty()) ? String.join(", ", book.getAuthors()) : "N/A");
        authorsLabel.setWrapText(true);
        authorsLabel.setMaxWidth(115);
        authorsLabel.setMinHeight(15); // Đủ cho 1-2 dòng
        authorsLabel.getStyleClass().add("book-item-author");
        authorsLabel.setTextAlignment(TextAlignment.CENTER);

        bookPane.getChildren().addAll(coverImage, titleLabel, authorsLabel);
        return bookPane;
    }

    private void loadBookCoverAsync(String imageUrl, ImageView imageView) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Task<Image> loadImageTask = new Task<>() {
                @Override
                protected Image call() throws Exception {
                    String finalImageUrl = imageUrl;
                    // Google Books API đôi khi trả về URL thiếu "https:"
                    if (finalImageUrl.startsWith("//")) {
                        finalImageUrl = "https:" + finalImageUrl;
                    }
                    // Yêu cầu ảnh với kích thước nhỏ hơn nếu có thể để tải nhanh hơn (tùy API)
                    return new Image(finalImageUrl, 85, 130, true, true, true); // width, height, preserveRatio, smooth, backgroundLoading
                }
            };
            loadImageTask.setOnSucceeded(event -> imageView.setImage(loadImageTask.getValue()));
            loadImageTask.setOnFailed(event -> {
                if (defaultBookCoverImage != null) { // Sử dụng ảnh mặc định nếu tải lỗi
                    imageView.setImage(defaultBookCoverImage);
                }
                System.err.println("AdminDashboard: Failed to load image: " + imageUrl +
                        (loadImageTask.getException() != null ? " - " + loadImageTask.getException().getMessage() : ""));
            });
            new Thread(loadImageTask).start();
        } else {
            if (defaultBookCoverImage != null) { // Sử dụng ảnh mặc định nếu URL rỗng
                imageView.setImage(defaultBookCoverImage);
            }
        }
    }


    @FXML
    private void handleSearchAction(ActionEvent event) {
        String query = searchField.getText().trim();
        // String searchType = searchTypeComboBox.getValue(); // searchType chưa được sử dụng trong googleBooksService.searchBooks
        if (query.isEmpty()) {
            mainContentArea.getChildren().setAll(new Label("Please enter a search query."));
            setActiveMenuButton(null); // Không có menu nào active khi hiển thị kết quả tìm kiếm
            return;
        }
        Label loadingLabel = new Label("Searching for books, please wait...");
        loadingLabel.getStyleClass().add("greeting-title"); // Tận dụng style đã có hoặc tạo style mới
        mainContentArea.getChildren().setAll(loadingLabel);
        setActiveMenuButton(null);

        Task<List<Book>> searchTask = new Task<>() {
            @Override
            protected List<Book> call() throws Exception {
                // googleBooksService.searchBooks hiện chỉ nhận query và maxResults
                return googleBooksService.searchBooks(query, 20); // Tìm 20 kết quả
            }
        };
        searchTask.setOnSucceeded(e -> {
            List<Book> searchResults = searchTask.getValue();
            FlowPane resultsPane = new FlowPane(15, 15); // Tăng khoảng cách giữa các item
            resultsPane.setPadding(new Insets(10));
            resultsPane.setAlignment(Pos.TOP_LEFT); // Căn lề
            if (searchResults == null || searchResults.isEmpty()) { // Kiểm tra cả null
                resultsPane.getChildren().add(new Label("No books found for your search: '" + query + "'"));
            } else {
                for (Book book : searchResults) {
                    resultsPane.getChildren().add(createBookDisplay(book));
                }
            }
            ScrollPane scrollPane = new ScrollPane(resultsPane);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;"); // Xóa viền/nền của ScrollPane
            mainContentArea.getChildren().setAll(scrollPane);
        });
        searchTask.setOnFailed(e -> {
            mainContentArea.getChildren().setAll(new Label("Error during search. Please try again."));
            if (searchTask.getException() != null) searchTask.getException().printStackTrace();
        });
        new Thread(searchTask).start();
    }


    @FXML
    private void handleHomeAction(ActionEvent event) {
        System.out.println("Home button clicked");
        loadHomeView();
        setActiveMenuButton(homeButton);
    }

    @FXML
    private void handleBookManagementAction(ActionEvent event) {
        System.out.println("Book Management button clicked");
        loadViewIntoMainContent("/com/lms/quanlythuvien/fxml/BookManagementView.fxml");
        setActiveMenuButton(bookManagementButton);
    }

    @FXML
    private void handleUserManagementAction(ActionEvent event) {
        System.out.println("User Management button clicked");
        // Giả sử bạn đã có hoặc sẽ tạo UserManagementView.fxml
        loadViewIntoMainContent("/com/lms/quanlythuvien/fxml/UserManagementView.fxml");
        setActiveMenuButton(userManagementButton);
    }

    @FXML
    private void handleLoanManagementAction(ActionEvent event) { // Đã đổi tên phương thức này trong FXML (onAction)
        System.out.println("Loan Management button clicked"); // Sửa log
        loadViewIntoMainContent("/com/lms/quanlythuvien/fxml/LoanManagementView.fxml"); // Tên file FXML mới
        setActiveMenuButton(loanManagementButton); // SỬA THÀNH loanManagementButton
    }

    private void loadViewIntoMainContent(String fxmlPath) {
        try {
            // System.out.println("AdminDashboard: Loading view: " + fxmlPath); // Debug
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            mainContentArea.getChildren().setAll(view);
        } catch (Exception e) {
            System.err.println("AdminDashboard: Error loading view: " + fxmlPath);
            e.printStackTrace();
            mainContentArea.getChildren().setAll(new Label("Error loading view: " + fxmlPath + "\n" + e.getMessage()));
        }
    }

    private void setActiveMenuButton(Button activeButton) {
        // Sử dụng mảng hoặc List các nút để dễ quản lý hơn nếu có nhiều nút
        Button[] menuButtons = {homeButton, bookManagementButton, userManagementButton, loanManagementButton};
        for (Button btn : menuButtons) {
            if (btn != null) { // Kiểm tra null trước khi thao tác
                btn.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("active"), btn == activeButton);
            }
        }
    }

    @FXML
    private void handleUserInfoAreaClick(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            ContextMenu userMenu = new ContextMenu();
            MenuItem profileItem = new MenuItem("View Profile");
            profileItem.setOnAction(e -> System.out.println("View Profile clicked (Not implemented)"));
            MenuItem settingsItem = new MenuItem("Settings");
            settingsItem.setOnAction(e -> System.out.println("Settings clicked (Not implemented)"));
            MenuItem logoutItem = new MenuItem("Logout");
            logoutItem.setOnAction(e -> {
                System.out.println("Logout selected from context menu");
                if (clockTimeline != null) { // Dừng timeline trước khi chuyển scene
                    clockTimeline.stop();
                }
                MainApp.loadScene("LoginScreen.fxml"); // Gọi phương thức load scene của MainApp
            });

            userMenu.getItems().addAll(profileItem, settingsItem, new javafx.scene.control.SeparatorMenuItem(), logoutItem);
            // Tính toán vị trí hiển thị context menu cho phù hợp hơn
            userMenu.show(userInfoArea, event.getScreenX() - event.getX() + userInfoArea.getWidth() - (userMenu.getWidth() > 0 ? userMenu.getWidth() : 150) , event.getScreenY() - event.getY() + userInfoArea.getHeight() + 5);
        }
    }
}
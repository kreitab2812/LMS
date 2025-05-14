package com.lms.quanlythuvien.controllers;

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.models.Book;
import com.lms.quanlythuvien.models.Quote;
import com.lms.quanlythuvien.models.User; // <<<--- THÊM IMPORT NÀY
import com.lms.quanlythuvien.services.GoogleBooksService;
// CÁC SERVICE SINGLETON SẼ KHÔNG CẦN KHỞI TẠO Ở ĐÂY TRỪ KHI CÓ LOGIC ĐẶC BIỆT NGAY TRONG DASHBOARD
// import com.lms.quanlythuvien.services.UserService;
// import com.lms.quanlythuvien.services.BookManagementService;
// import com.lms.quanlythuvien.services.BorrowingRecordService;
import com.lms.quanlythuvien.utils.SessionManager; // <<<--- THÊM IMPORT NÀY


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
import javafx.scene.control.Alert; // <<<--- THÊM IMPORT NÀY (cho showAlert ví dụ)
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

import java.io.IOException; // <<<--- THÊM IMPORT NÀY
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
    @FXML private Button searchButton; // Thêm fx:id cho nút tìm kiếm nếu chưa có trong FXML
    @FXML private Label timeLabel;
    @FXML private Label dateLabel;
    @FXML private ImageView userAvatar;
    @FXML private Label usernameLabel;
    @FXML private HBox userInfoArea;

    @FXML private Button homeButton;
    @FXML private Button bookManagementButton;
    @FXML private Button userManagementButton;
    @FXML private Button loanManagementButton;

    @FXML private StackPane mainContentArea;

    // Các biến cho HomeView
    private Label greetingLabel; // Sẽ được lấy từ HomeView.fxml khi nó được load
    private Label quoteTextLabel;
    private Label quoteAuthorLabel;

    private GoogleBooksService googleBooksService;
    private List<Quote> quotesList;
    private Timeline clockTimeline;
    private Image defaultBookCoverImage;
    // private UserService userService; // Không cần khởi tạo ở đây nếu chỉ lấy user từ SessionManager

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("DEBUG_ADC_INIT: AdminDashboardController initialize() started.");
        googleBooksService = new GoogleBooksService(); // GoogleBooksService có thể không cần là Singleton nếu nó không giữ trạng thái
        // userService = UserService.getInstance(); // Không cần thiết nếu chỉ lấy từ session

        searchTypeComboBox.setItems(FXCollections.observableArrayList("Tất cả", "Tiêu đề", "Tác giả", "Chủ đề", "Nội dung"));
        searchTypeComboBox.setValue("Tất cả"); // Giá trị mặc định

        // Lấy thông tin người dùng từ SessionManager
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            usernameLabel.setText(currentUser.getUsername());
            // TODO: Có thể hiển thị avatar dựa trên thông tin user nếu có
        } else {
            usernameLabel.setText("Admin"); // Fallback
            System.err.println("WARN_ADC_INIT: No user found in session for AdminDashboard.");
        }

        try {
            if (userAvatar != null && getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_avatar.png") != null) {
                userAvatar.setImage(new Image(getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_avatar.png")));
            }
            if (getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_book_cover.png") != null) {
                defaultBookCoverImage = new Image(getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_book_cover.png"));
            }
        } catch (Exception e) {
            System.err.println("ERROR_ADC_INIT: AdminDashboard - Error loading default images: " + e.getMessage());
            e.printStackTrace();
        }

        setupClock();
        loadHomeView(); // Tải HomeView làm mặc định
        setActiveMenuButton(homeButton);
        System.out.println("DEBUG_ADC_INIT: AdminDashboardController initialize() finished.");

        // (Tùy chọn) Gọi initializeSampleData cho BorrowingRecordService nếu muốn có dữ liệu mẫu
        // và đảm bảo nó chỉ chạy một lần khi ứng dụng khởi động (có thể đặt ở MainApp tốt hơn)
        // BorrowingRecordService.getInstance().initializeSampleData();
    }

    private void setupClock() {
        clockTimeline = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            LocalTime currentTime = LocalTime.now();
            LocalDateTime currentDateTime = LocalDateTime.now();
            if (timeLabel != null) timeLabel.setText(currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            if (dateLabel != null) dateLabel.setText(currentDateTime.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")));
            if (greetingLabel != null) {
                updateGreeting(currentTime);
            }
        }), new KeyFrame(Duration.seconds(1)));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    private void updateGreeting(LocalTime time) {
        if (greetingLabel == null) return;
        int hour = time.getHour();
        String username = (SessionManager.getInstance().getCurrentUser() != null) ?
                SessionManager.getInstance().getCurrentUser().getUsername() : "Admin";
        String greetingText = "Chào buổi tối, " + username + "!";
        if (hour >= 5 && hour < 12) {
            greetingText = "Chào buổi sáng, " + username + "!";
        } else if (hour >= 12 && hour < 18) {
            greetingText = "Chào buổi chiều, " + username + "!";
        }
        greetingLabel.setText(greetingText);
    }

    private void loadHomeView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/quanlythuvien/fxml/HomeView.fxml"));
            Node homeViewNode = loader.load();
            mainContentArea.getChildren().setAll(homeViewNode);

            greetingLabel = (Label) loader.getNamespace().get("greetingLabel");
            quoteTextLabel = (Label) loader.getNamespace().get("quoteTextLabel");
            quoteAuthorLabel = (Label) loader.getNamespace().get("quoteAuthorLabel");

            if (greetingLabel != null) updateGreeting(LocalTime.now());
            displayRandomQuote();

        } catch (Exception e) {
            System.err.println("ERROR_ADC_LOAD_HOME: Error loading Home content.");
            e.printStackTrace();
            mainContentArea.getChildren().setAll(new Label("Lỗi tải nội dung Trang chủ: " + e.getMessage()));
        }
    }

    private void displayRandomQuote() {
        if (quoteTextLabel == null || quoteAuthorLabel == null) {
            System.err.println("WARN_ADC_QUOTE: Quote labels not found in HomeView.fxml for dynamic display.");
            return;
        }
        quotesList = List.of(
                new Quote("Cách duy nhất để làm việc lớn là yêu những gì bạn làm.", "Steve Jobs"),
                new Quote("Người đọc sách sống cả ngàn cuộc đời trước khi chết. Người không bao giờ đọc sách chỉ sống một lần.", "George R.R. Martin"),
                new Quote("Sách là một loại ma thuật độc đáo có thể mang theo.", "Stephen King"),
                new Quote("Không có người bạn nào trung thành như một cuốn sách.", "Ernest Hemingway"),
                new Quote("Quá nhiều sách, quá ít thời gian.", "Frank Zappa")
        );
        if (!quotesList.isEmpty()) {
            Random random = new Random();
            Quote randomQuote = quotesList.get(random.nextInt(quotesList.size()));
            quoteTextLabel.setText("\"" + randomQuote.getText() + "\"");
            quoteAuthorLabel.setText("- " + randomQuote.getAuthor());
        } else {
            quoteTextLabel.setText("Không có trích dẫn nào hôm nay.");
            quoteAuthorLabel.setText("");
        }
    }

    private VBox createBookDisplay(Book book) {
        // Giữ nguyên logic của cậu, chỉ đảm bảo các styleClass được định nghĩa trong CSS
        VBox bookPane = new VBox(3);
        bookPane.setPadding(new Insets(8));
        bookPane.getStyleClass().add("book-item-pane"); // CSS: .book-item-pane
        bookPane.setAlignment(Pos.TOP_CENTER);

        ImageView coverImage = new ImageView();
        coverImage.setFitHeight(130); coverImage.setFitWidth(85);
        coverImage.setPreserveRatio(true);
        VBox.setMargin(coverImage, new Insets(0, 0, 5, 0));
        loadBookCoverAsync(book.getThumbnailUrl(), coverImage);

        Label titleLabel = new Label(book.getTitle() != null ? book.getTitle() : "N/A");
        titleLabel.setWrapText(true); titleLabel.setMaxWidth(115); titleLabel.setMinHeight(28);
        titleLabel.getStyleClass().add("book-item-title"); // CSS: .book-item-title
        titleLabel.setTextAlignment(TextAlignment.CENTER);

        Label authorsLabel = new Label((book.getAuthors() != null && !book.getAuthors().isEmpty()) ? String.join(", ", book.getAuthors()) : "N/A");
        authorsLabel.setWrapText(true); authorsLabel.setMaxWidth(115); authorsLabel.setMinHeight(15);
        authorsLabel.getStyleClass().add("book-item-author"); // CSS: .book-item-author
        authorsLabel.setTextAlignment(TextAlignment.CENTER);

        bookPane.getChildren().addAll(coverImage, titleLabel, authorsLabel);
        return bookPane;
    }

    private void loadBookCoverAsync(String imageUrl, ImageView imageView) {
        if (imageView == null) { // Thêm kiểm tra null cho imageView
            System.err.println("ERROR_ADC_LOAD_COVER: ImageView is null for URL: " + imageUrl);
            return;
        }
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Task<Image> loadImageTask = new Task<Image>() { // Sửa <Image> ở đây
                @Override // <<--- BẮT BUỘC PHẢI CÓ TRIỂN KHAI NÀY
                protected Image call() throws Exception {
                    String finalImageUrl = imageUrl;
                    // Google Books API đôi khi trả về URL thiếu "https:"
                    if (finalImageUrl.startsWith("//")) {
                        finalImageUrl = "https:" + finalImageUrl;
                    }
                    // Yêu cầu ảnh với kích thước nhỏ hơn nếu có thể để tải nhanh hơn (tùy API)
                    // Tham số cuối true để tải ảnh ở background thread
                    System.out.println("DEBUG_ADC_LOAD_COVER: Loading image from URL: " + finalImageUrl);
                    return new Image(finalImageUrl, 85, 130, true, true, true); // width, height, preserveRatio, smooth, backgroundLoading
                }
            };

            loadImageTask.setOnSucceeded(event -> {
                Image loadedImage = loadImageTask.getValue();
                if (loadedImage != null && !loadedImage.isError()) {
                    imageView.setImage(loadedImage);
                    System.out.println("DEBUG_ADC_LOAD_COVER: Successfully loaded image: " + imageUrl);
                } else {
                    if (loadedImage != null && loadedImage.getException() != null) {
                        System.err.println("ERROR_ADC_LOAD_COVER: Image loaded with error for URL: " + imageUrl + " - " + loadedImage.getException().getMessage());
                    } else {
                        System.err.println("ERROR_ADC_LOAD_COVER: Image loaded is null or has an unspecified error for URL: " + imageUrl);
                    }
                    if (defaultBookCoverImage != null) {
                        imageView.setImage(defaultBookCoverImage); // Sử dụng ảnh mặc định nếu tải lỗi
                    }
                }
            });

            loadImageTask.setOnFailed(event -> {
                // Lỗi này xảy ra nếu phương thức call() ném ra một Exception không được bắt
                System.err.println("ERROR_ADC_LOAD_COVER: Task failed to load image: " + imageUrl);
                Throwable throwable = loadImageTask.getException();
                if (throwable != null) {
                    throwable.printStackTrace();
                }
                if (defaultBookCoverImage != null) {
                    imageView.setImage(defaultBookCoverImage); // Sử dụng ảnh mặc định
                }
            });
            new Thread(loadImageTask).start();
        } else {
            System.out.println("DEBUG_ADC_LOAD_COVER: Image URL is null or empty, using default book cover.");
            if (defaultBookCoverImage != null) {
                imageView.setImage(defaultBookCoverImage);
            } else {
                System.err.println("ERROR_ADC_LOAD_COVER: Default book cover is also null.");
                // Cân nhắc đặt một ảnh placeholder mặc định cứng nếu defaultBookCoverImage cũng có thể null
            }
        }
    }

    @FXML
    private void handleSearchAction(ActionEvent event) {
        String query = searchField.getText().trim();
        // String searchType = searchTypeComboBox.getValue(); // Cần tích hợp searchType vào logic gọi service
        if (query.isEmpty()) {
            mainContentArea.getChildren().setAll(new Label("Vui lòng nhập từ khóa tìm kiếm."));
            setActiveMenuButton(null);
            return;
        }
        Label loadingLabel = new Label("Đang tìm kiếm sách, vui lòng đợi...");
        loadingLabel.getStyleClass().add("view-title"); // Dùng tạm view-title hoặc tạo style riêng
        mainContentArea.getChildren().setAll(loadingLabel);
        setActiveMenuButton(null);

        Task<List<Book>> searchTask = new Task<>() {
            @Override
            protected List<Book> call() throws Exception {
                return googleBooksService.searchBooks(query, 20);
            }
        };
        searchTask.setOnSucceeded(e -> {
            List<Book> searchResults = searchTask.getValue();
            FlowPane resultsPane = new FlowPane(15, 15);
            resultsPane.setPadding(new Insets(10));
            resultsPane.setAlignment(Pos.TOP_LEFT);
            if (searchResults == null || searchResults.isEmpty()) {
                resultsPane.getChildren().add(new Label("Không tìm thấy sách nào cho từ khóa: '" + query + "'"));
            } else {
                for (Book book : searchResults) {
                    resultsPane.getChildren().add(createBookDisplay(book));
                }
            }
            ScrollPane scrollPane = new ScrollPane(resultsPane);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
            mainContentArea.getChildren().setAll(scrollPane);
        });
        searchTask.setOnFailed(e -> {
            mainContentArea.getChildren().setAll(new Label("Lỗi trong quá trình tìm kiếm. Vui lòng thử lại."));
            if (searchTask.getException() != null) searchTask.getException().printStackTrace();
        });
        new Thread(searchTask).start();
    }

    @FXML
    private void handleHomeAction(ActionEvent event) {
        loadHomeView();
        setActiveMenuButton(homeButton);
    }

    @FXML
    private void handleBookManagementAction(ActionEvent event) {
        loadViewIntoMainContent("/com/lms/quanlythuvien/fxml/BookManagementView.fxml");
        setActiveMenuButton(bookManagementButton);
    }

    @FXML
    private void handleUserManagementAction(ActionEvent event) {
        loadViewIntoMainContent("/com/lms/quanlythuvien/fxml/UserManagementView.fxml");
        setActiveMenuButton(userManagementButton);
    }

    @FXML
    private void handleLoanManagementAction(ActionEvent event) {
        loadViewIntoMainContent("/com/lms/quanlythuvien/fxml/LoanManagementView.fxml");
        setActiveMenuButton(loanManagementButton);
    }

    private void loadViewIntoMainContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            mainContentArea.getChildren().setAll(view);
        } catch (IOException e) { // Nên bắt IOException cụ thể
            System.err.println("ERROR_ADC_LOAD_VIEW: Error loading view: " + fxmlPath);
            e.printStackTrace();
            mainContentArea.getChildren().setAll(new Label("Lỗi tải giao diện: " + fxmlPath.substring(fxmlPath.lastIndexOf("/") + 1) + "\nChi tiết: " + e.getMessage()));
        }
    }

    private void setActiveMenuButton(Button activeButton) {
        Button[] menuButtons = {homeButton, bookManagementButton, userManagementButton, loanManagementButton};
        for (Button btn : menuButtons) {
            if (btn != null) {
                btn.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("active"), btn == activeButton);
            }
        }
    }

    @FXML
    private void handleUserInfoAreaClick(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            ContextMenu userMenu = new ContextMenu();
            MenuItem profileItem = new MenuItem("Xem Hồ Sơ"); // Dịch
            profileItem.setOnAction(e -> System.out.println("Xem Hồ Sơ clicked (Not implemented)"));
            MenuItem settingsItem = new MenuItem("Cài Đặt"); // Dịch
            settingsItem.setOnAction(e -> System.out.println("Cài Đặt clicked (Not implemented)"));
            MenuItem logoutItem = new MenuItem("Đăng xuất"); // Dịch
            logoutItem.setOnAction(e -> {
                if (clockTimeline != null) {
                    clockTimeline.stop();
                }
                SessionManager.getInstance().clearSession(); // Xóa session khi logout
                MainApp.loadScene("LoginScreen.fxml");
            });

            userMenu.getItems().addAll(profileItem, settingsItem, new javafx.scene.control.SeparatorMenuItem(), logoutItem);
            userMenu.show(userInfoArea, event.getScreenX() - event.getX() + userInfoArea.getWidth() - (userMenu.getWidth() > 0 ? userMenu.getWidth() : 150) , event.getScreenY() - event.getY() + userInfoArea.getHeight() + 5);
        }
    }
}
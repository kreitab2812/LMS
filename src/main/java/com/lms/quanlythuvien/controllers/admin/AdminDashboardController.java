package com.lms.quanlythuvien.controllers.admin; // Giả sử cậu đã chuyển vào package admin

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.models.item.Book; // Đường dẫn model mới
import com.lms.quanlythuvien.models.item.Quote; // Đường dẫn model mới
import com.lms.quanlythuvien.models.user.User;   // Đường dẫn model mới
import com.lms.quanlythuvien.services.library.GoogleBooksService; // Đường dẫn service mới
import com.lms.quanlythuvien.utils.session.SessionManager;     // Đường dẫn util mới

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

import java.io.IOException;
import java.io.InputStream; // Thêm import này nếu bị thiếu
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
    @FXML private Button searchButton;
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

    private Label greetingLabel;
    private Label quoteTextLabel;
    private Label quoteAuthorLabel;

    private GoogleBooksService googleBooksService;
    private List<Quote> quotesList;
    private Timeline clockTimeline;
    private Image defaultBookCoverImage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("DEBUG_ADC_INIT: AdminDashboardController initialize() started.");
        // GoogleBooksService có thể vẫn được tạo mới nếu nó không quản lý trạng thái cần chia sẻ
        // Hoặc nếu cậu muốn nó là Singleton, thì dùng GoogleBooksService.getInstance()
        googleBooksService = new GoogleBooksService();
        System.out.println("DEBUG_ADC_INIT: GoogleBooksService instantiated/retrieved.");

        searchTypeComboBox.setItems(FXCollections.observableArrayList("Tất cả", "Tiêu đề", "Tác giả", "Chủ đề", "Nội dung"));
        searchTypeComboBox.setValue("Tất cả");

        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            usernameLabel.setText(currentUser.getUsername());
            // TODO: Load avatar của currentUser nếu có trường avatarUrl
        } else {
            usernameLabel.setText("Admin"); // Fallback
            System.err.println("WARN_ADC_INIT: No user found in session for AdminDashboard. Displaying default username.");
        }

        try {
            InputStream avatarStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_avatar.png");
            if (userAvatar != null && avatarStream != null) {
                userAvatar.setImage(new Image(avatarStream));
            } else if (userAvatar == null) {
                System.err.println("ERROR_ADC_INIT: userAvatar ImageView is null.");
            } else {
                System.err.println("ERROR_ADC_INIT: Default avatar image not found.");
            }

            InputStream bookCoverStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_book_cover.png");
            if (bookCoverStream != null) {
                defaultBookCoverImage = new Image(bookCoverStream);
            } else {
                System.err.println("ERROR_ADC_INIT: Default book cover image not found.");
            }
        } catch (Exception e) {
            System.err.println("ERROR_ADC_INIT: AdminDashboard - Error loading default images: " + e.getMessage());
            e.printStackTrace();
        }

        setupClock();
        loadHomeView();
        setActiveMenuButton(homeButton);
        System.out.println("DEBUG_ADC_INIT: AdminDashboardController initialize() finished.");
    }

    private void setupClock() {
        // Giữ nguyên logic của cậu
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
        // Giữ nguyên logic của cậu, nhưng lấy username từ SessionManager
        if (greetingLabel == null) return;
        int hour = time.getHour();
        String username = "Admin"; // Default
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            username = currentUser.getUsername();
        }
        String greetingText = "Chào buổi tối, " + username + "!";
        if (hour >= 5 && hour < 12) {
            greetingText = "Chào buổi sáng, " + username + "!";
        } else if (hour >= 12 && hour < 18) {
            greetingText = "Chào buổi chiều, " + username + "!";
        }
        greetingLabel.setText(greetingText);
    }

    private void loadHomeView() {
        // Giả sử HomeView.fxml nằm trong fxml/admin/
        // (Đảm bảo đường dẫn này đúng với cấu trúc thư mục FXML mới của cậu)
        loadViewIntoMainContent("/com/lms/quanlythuvien/fxml/admin/HomeView.fxml");
        // Logic lấy greetingLabel, quoteTextLabel, quoteAuthorLabel từ loader.getNamespace()
        // cần được đặt bên trong loadViewIntoMainContent nếu muốn nó áp dụng cho mọi view
        // hoặc xử lý riêng sau khi loadHomeView thành công.
        // Hiện tại, loadViewIntoMainContent chỉ tải Parent, không trả về loader.
        // Để lấy các control từ view con, cậu cần sửa loadViewIntoMainContent để trả về loader
        // hoặc để HomeViewController tự quản lý các label đó.
        // Cách đơn giản là để HomeViewController tự quản lý.
        // Nếu các label đó là của AdminDashboard thì phải đảm bảo chúng không bị null khi HomeView chưa được load
        // Hoặc chỉ gọi updateGreeting và displayRandomQuote sau khi loader.getNamespace() thành công.

        // Để code hiện tại hoạt động (lấy label từ namespace), loadViewIntoMainContent cần trả về Node
        // và logic lấy label từ namespace sẽ ở đây.
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lms/quanlythuvien/fxml/admin/HomeView.fxml"));
            Node homeViewNode = loader.load();
            mainContentArea.getChildren().setAll(homeViewNode);

            // Logic này chỉ hoạt động nếu HomeView.fxml KHÔNG CÓ fx:controller riêng
            // và các fx:id đó được định nghĩa trực tiếp trong HomeView.fxml
            // Nếu HomeView.fxml có controller riêng, thì HomeViewController sẽ quản lý các label đó.
            // Giả sử HomeView.fxml không có controller riêng và các label đó là của AdminDashboard
            // thì việc này sẽ gây lỗi nếu các label này không phải là con trực tiếp của homeViewNode
            // hoặc nếu namespace không chứa chúng.
            // TỐT NHẤT: HomeViewController nên tự quản lý greeting và quote của nó.
            // Bỏ các dòng lấy label này nếu chúng thuộc về HomeViewController.
            // greetingLabel = (Label) loader.getNamespace().get("greetingLabel");
            // quoteTextLabel = (Label) loader.getNamespace().get("quoteTextLabel");
            // quoteAuthorLabel = (Label) loader.getNamespace().get("quoteAuthorLabel");
            // if (greetingLabel != null) updateGreeting(LocalTime.now());
            // displayRandomQuote();

        } catch (Exception e) {
            System.err.println("ERROR_ADC_LOAD_HOME: Error loading Home content.");
            e.printStackTrace();
            mainContentArea.getChildren().setAll(new Label("Lỗi tải nội dung Trang chủ: " + e.getMessage()));
        }
    }

    private void displayRandomQuote() {
        // Phương thức này nên nằm trong HomeViewController nếu quoteTextLabel và quoteAuthorLabel là của HomeView
        // Giả sử chúng là của AdminDashboard (ít khả năng)
        if (quoteTextLabel == null || quoteAuthorLabel == null) {
            System.err.println("WARN_ADC_QUOTE: Quote labels are null. Cannot display quote.");
            return;
        }
        quotesList = List.of( /* ... danh sách quote của cậu ... */ );
        if (!quotesList.isEmpty()) {
            Random random = new Random();
            Quote randomQuote = quotesList.get(random.nextInt(quotesList.size()));
            quoteTextLabel.setText("\"" + randomQuote.getText() + "\"");
            quoteAuthorLabel.setText("- " + randomQuote.getAuthor());
        } else {
            // ...
        }
    }

    private VBox createBookDisplay(Book book) {
        // Giữ nguyên logic, đảm bảo Book model có đường dẫn đúng
        VBox bookPane = new VBox(3);
        bookPane.setPadding(new Insets(8));
        bookPane.getStyleClass().add("book-item-pane");
        bookPane.setAlignment(Pos.TOP_CENTER);
        ImageView coverImage = new ImageView();
        coverImage.setFitHeight(130); coverImage.setFitWidth(85);
        coverImage.setPreserveRatio(true);
        VBox.setMargin(coverImage, new Insets(0, 0, 5, 0));
        loadBookCoverAsync(book.getThumbnailUrl(), coverImage);
        Label titleLabel = new Label(book.getTitle() != null ? book.getTitle() : "N/A");
        titleLabel.setWrapText(true); titleLabel.setMaxWidth(115); titleLabel.setMinHeight(28);
        titleLabel.getStyleClass().add("book-item-title");
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        Label authorsLabel = new Label((book.getAuthors() != null && !book.getAuthors().isEmpty()) ? String.join(", ", book.getAuthors()) : "N/A");
        authorsLabel.setWrapText(true); authorsLabel.setMaxWidth(115); authorsLabel.setMinHeight(15);
        authorsLabel.getStyleClass().add("book-item-author");
        authorsLabel.setTextAlignment(TextAlignment.CENTER);
        bookPane.getChildren().addAll(coverImage, titleLabel, authorsLabel);
        return bookPane;
    }

    private void loadBookCoverAsync(String imageUrl, ImageView imageView) {
        // Giữ nguyên logic, đảm bảo Image model có đường dẫn đúng
        if (imageView == null) {
            System.err.println("ERROR_ADC_LOAD_COVER: ImageView is null for URL: " + imageUrl);
            return;
        }
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Task<Image> loadImageTask = new Task<>() {
                @Override
                protected Image call() throws Exception {
                    String finalImageUrl = imageUrl;
                    if (finalImageUrl.startsWith("//")) {
                        finalImageUrl = "https:" + finalImageUrl;
                    }
                    return new Image(finalImageUrl, 85, 130, true, true, true);
                }
            };
            loadImageTask.setOnSucceeded(event -> {
                Image loadedImage = loadImageTask.getValue();
                if (loadedImage != null && !loadedImage.isError()) {
                    imageView.setImage(loadedImage);
                } else {
                    if (defaultBookCoverImage != null) imageView.setImage(defaultBookCoverImage);
                    if (loadedImage != null && loadedImage.getException()!=null) System.err.println("ERROR_ADC_LOAD_COVER_TASK: Image loaded with error for " + imageUrl + " - " + loadedImage.getException().getMessage()); else System.err.println("ERROR_ADC_LOAD_COVER_TASK: Image loaded null or unspecified error for " + imageUrl);
                }
            });
            loadImageTask.setOnFailed(event -> {
                if (defaultBookCoverImage != null) imageView.setImage(defaultBookCoverImage);
                System.err.println("ERROR_ADC_LOAD_COVER_TASK: Task failed for " + imageUrl);
                if(loadImageTask.getException()!=null) loadImageTask.getException().printStackTrace();
            });
            new Thread(loadImageTask).start();
        } else {
            if (defaultBookCoverImage != null) imageView.setImage(defaultBookCoverImage);
        }
    }

    @FXML
    private void handleSearchAction(ActionEvent event) {
        // Giữ nguyên logic, đảm bảo Book model có đường dẫn đúng
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            mainContentArea.getChildren().setAll(new Label("Vui lòng nhập từ khóa tìm kiếm."));
            setActiveMenuButton(null);
            return;
        }
        Label loadingLabel = new Label("Đang tìm kiếm sách, vui lòng đợi...");
        loadingLabel.getStyleClass().add("view-title");
        mainContentArea.getChildren().setAll(loadingLabel);
        setActiveMenuButton(null);

        Task<List<Book>> searchTask = new Task<>() {
            @Override
            protected List<Book> call() throws Exception {
                return googleBooksService.searchBooks(query, 20);
            }
        };
        searchTask.setOnSucceeded(e -> { /* ... giữ nguyên ... */
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
        searchTask.setOnFailed(e -> { /* ... giữ nguyên ... */
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

    // QUAN TRỌNG: Cập nhật đường dẫn FXML ở đây nếu cậu đã chia package FXML
    @FXML
    private void handleBookManagementAction(ActionEvent event) {
        loadViewIntoMainContent("/com/lms/quanlythuvien/fxml/admin/BookManagementView.fxml");
        setActiveMenuButton(bookManagementButton);
    }

    @FXML
    private void handleUserManagementAction(ActionEvent event) {
        loadViewIntoMainContent("/com/lms/quanlythuvien/fxml/admin/UserManagementView.fxml");
        setActiveMenuButton(userManagementButton);
    }

    @FXML
    private void handleLoanManagementAction(ActionEvent event) {
        loadViewIntoMainContent("/com/lms/quanlythuvien/fxml/admin/LoanManagementView.fxml");
        setActiveMenuButton(loanManagementButton);
    }

    private void loadViewIntoMainContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            mainContentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("ERROR_ADC_LOAD_VIEW: Error loading view: " + fxmlPath);
            e.printStackTrace();
            mainContentArea.getChildren().setAll(new Label("Lỗi tải giao diện: " + fxmlPath.substring(fxmlPath.lastIndexOf("/") + 1) + "\nChi tiết: " + e.getMessage()));
        } catch (NullPointerException e) { // Bắt lỗi nếu getResource trả về null
            System.err.println("ERROR_ADC_LOAD_VIEW: Cannot find FXML file at path: " + fxmlPath);
            e.printStackTrace();
            mainContentArea.getChildren().setAll(new Label("Lỗi: Không tìm thấy file giao diện tại đường dẫn "  + fxmlPath));
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
            MenuItem profileItem = new MenuItem("Xem Hồ Sơ");
            profileItem.setOnAction(e -> System.out.println("Xem Hồ Sơ clicked (Not implemented)"));
            MenuItem settingsItem = new MenuItem("Cài Đặt");
            settingsItem.setOnAction(e -> System.out.println("Cài Đặt clicked (Not implemented)"));
            MenuItem logoutItem = new MenuItem("Đăng xuất");
            logoutItem.setOnAction(e -> {
                if (clockTimeline != null) {
                    clockTimeline.stop();
                }
                SessionManager.getInstance().clearSession();
                MainApp.loadScene("/com/lms/quanlythuvien/fxml/common/LoginScreen.fxml"); // Đường dẫn mới
            });
            userMenu.getItems().addAll(profileItem, settingsItem, new javafx.scene.control.SeparatorMenuItem(), logoutItem);
            userMenu.show(userInfoArea, event.getScreenX() - event.getX() + userInfoArea.getWidth() - 150 , event.getScreenY() - event.getY() + userInfoArea.getHeight() + 5); // Điều chỉnh vị trí menu
        }
    }
}
package com.lms.quanlythuvien.controllers.admin;

import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.item.Quote;
import com.lms.quanlythuvien.models.transaction.BorrowingRecord;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.library.BookManagementService;
// import com.lms.quanlythuvien.services.system.NotificationService; // Bạn có thể cần service này sau
import com.lms.quanlythuvien.services.transaction.BorrowingRecordService;
import com.lms.quanlythuvien.services.transaction.BorrowingRequestService; // Cho pending requests
import com.lms.quanlythuvien.services.user.UserService;
import com.lms.quanlythuvien.utils.session.SessionManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AdminHomeContentController implements Initializable {

    //<editor-fold desc="FXML Injections - Admin Info & Quote">
    @FXML private ImageView adminAvatarImageView;
    @FXML private Label adminGreetingLabel;
    @FXML private Label adminUserNameLabel;
    @FXML private Label adminRoleInfoLabel;
    @FXML private VBox quoteCardAdminContainer;
    @FXML private Label quoteTextLabelAdmin;
    @FXML private Label quoteAuthorLabelAdmin;
    //</editor-fold>

    //<editor-fold desc="FXML Injections - Stats">
    @FXML private Label activeLoansCountLabel;
    @FXML private Label overdueLoansCountLabel;
    @FXML private Label pendingRequestsCountLabel;
    //</editor-fold>

    //<editor-fold desc="FXML Injections - Upcoming Due Books">
    @FXML private ListView<String> upcomingDueBooksListView;
    //</editor-fold>

    private User currentAdmin;
    private List<Quote> quotesList;
    private final Random random = new Random();
    private final DateTimeFormatter displayDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Services
    private BorrowingRecordService borrowingRecordService;
    private BorrowingRequestService borrowingRequestService;
    private BookManagementService bookManagementService;
    private UserService userService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentAdmin = SessionManager.getInstance().getCurrentUser();

        // Initialize services
        borrowingRecordService = BorrowingRecordService.getInstance();
        borrowingRequestService = BorrowingRequestService.getInstance();
        bookManagementService = BookManagementService.getInstance();
        userService = UserService.getInstance();

        initializeQuotes();

        if (currentAdmin != null && currentAdmin.getRole() == User.Role.ADMIN) {
            Platform.runLater(() -> {
                populateAdminInfoSection();
                loadQuoteOfTheDayForAdmin();
                loadDashboardStats();
                loadUpcomingDueBooksData();
            });
        } else {
            handleNoAdmin();
        }
    }

    private void handleNoAdmin() {
        System.err.println("AdminHomeContentController: No admin user found or not an ADMIN role.");
        if (adminGreetingLabel != null) adminGreetingLabel.setText("Lỗi! Không tìm thấy Admin.");
        if (adminUserNameLabel != null) adminUserNameLabel.setText("");
        if (adminRoleInfoLabel != null) adminRoleInfoLabel.setText("");
        if (activeLoansCountLabel != null) activeLoansCountLabel.setText("0");
        if (overdueLoansCountLabel != null) overdueLoansCountLabel.setText("0");
        if (pendingRequestsCountLabel != null) pendingRequestsCountLabel.setText("0");
        if (upcomingDueBooksListView != null) upcomingDueBooksListView.getItems().clear();
        if (quoteCardAdminContainer != null) quoteCardAdminContainer.setVisible(false);
        loadAdminAvatar(); // Sẽ load default avatar
    }

    private void initializeQuotes() {
        quotesList = Arrays.asList(
                new Quote("Quản lý là làm mọi việc đúng cách; lãnh đạo là làm những việc đúng đắn.", "Peter Drucker"),
                new Quote("Thư viện là nơi tự do cuối cùng của chúng ta.", "Isaac Asimov"),
                new Quote("Một cuốn sách hay có thể thay đổi cuộc đời bạn.", " Oprah Winfrey"),
                new Quote("Cách duy nhất để làm một công việc tuyệt vời là yêu những gì bạn làm.", "Steve Jobs")
        );
    }

    private void populateAdminInfoSection() {
        if (currentAdmin == null) return;

        if (adminGreetingLabel != null) {
            int hour = LocalTime.now().getHour();
            if (hour < 5 || hour >= 22) adminGreetingLabel.setText("Admin ơi, làm việc khuya thế!");
            else if (hour < 12) adminGreetingLabel.setText("Chào buổi sáng, Admin!");
            else if (hour < 18) adminGreetingLabel.setText("Chào buổi chiều, Admin!");
            else adminGreetingLabel.setText("Chào buổi tối, Admin!");
        }
        if (adminUserNameLabel != null) {
            adminUserNameLabel.setText(currentAdmin.getFullNameOrDefault(currentAdmin.getUsernameOrDefault("Admin")));
        }
        if (adminRoleInfoLabel != null) {
            adminRoleInfoLabel.setText("Vai trò: " + currentAdmin.getRole().toString());
        }
        loadAdminAvatar();
    }

    private void loadAdminAvatar() {
        if (adminAvatarImageView == null) return;
        Image imageToSet = null;
        String avatarPath = (currentAdmin != null) ? currentAdmin.getAvatarUrl() : null;

        if (avatarPath != null && !avatarPath.isEmpty()) {
            try {
                File avatarFile = new File(avatarPath);
                if (avatarFile.exists() && avatarFile.isFile() && avatarFile.canRead()) {
                    imageToSet = new Image(avatarFile.toURI().toString(), true);
                } else {
                    try {
                        new URL(avatarPath).toURI(); // Check if it's a valid URL structure
                        imageToSet = new Image(avatarPath, true);
                    } catch (Exception urlEx) {
                        System.err.println("AdminHomeContentController: Avatar path is not a valid file or URL: " + avatarPath);
                    }
                }
                if (imageToSet != null && imageToSet.isError()) {
                    System.err.println("AdminHomeContentController: Error loading admin avatar from path: " + avatarPath + " - " + imageToSet.getException().getMessage());
                    imageToSet = null;
                }
            } catch (Exception e) {
                System.err.println("AdminHomeContentController: Exception loading admin avatar: " + e.getMessage());
                imageToSet = null;
            }
        }

        if (imageToSet == null) {
            try (InputStream defaultStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_avatar.png")) {
                if (defaultStream != null) {
                    imageToSet = new Image(defaultStream);
                } else {
                    System.err.println("AdminHomeContentController: Default admin avatar resource not found.");
                }
            } catch (Exception e) {
                System.err.println("AdminHomeContentController: Failed to load default admin avatar: " + e.getMessage());
            }
        }
        adminAvatarImageView.setImage(imageToSet);
    }

    private void loadQuoteOfTheDayForAdmin() {
        if (quoteTextLabelAdmin == null || quoteAuthorLabelAdmin == null || quoteCardAdminContainer == null) return;
        if (quotesList != null && !quotesList.isEmpty()) {
            Quote randomQuote = quotesList.get(random.nextInt(quotesList.size()));
            quoteTextLabelAdmin.setText("\"" + randomQuote.getText() + "\"");
            quoteAuthorLabelAdmin.setText("— " + randomQuote.getAuthor());
            quoteCardAdminContainer.setVisible(true);
        } else {
            quoteCardAdminContainer.setVisible(false);
        }
    }

    private void loadDashboardStats() {
        // Các phương thức này đã được thêm/sửa trong BorrowingRecordService
        int activeLoans = borrowingRecordService.countActiveLoans();
        int overdueLoans = borrowingRecordService.countOverdueLoans();
        // Phương thức countPendingRequests() cần bạn triển khai trong BorrowingRequestService.java
        int pendingRequests = borrowingRequestService.countPendingRequests();

        if (activeLoansCountLabel != null) activeLoansCountLabel.setText(String.valueOf(activeLoans));
        if (overdueLoansCountLabel != null) overdueLoansCountLabel.setText(String.valueOf(overdueLoans));
        if (pendingRequestsCountLabel != null) pendingRequestsCountLabel.setText(String.valueOf(pendingRequests));
    }

    private void loadUpcomingDueBooksData() {
        if (upcomingDueBooksListView == null) return;

        // Phương thức này đã được thêm vào BorrowingRecordService
        List<BorrowingRecord> allActiveLoans = borrowingRecordService.getAllActiveLoans();
        if (allActiveLoans.isEmpty()) {
            upcomingDueBooksListView.setPlaceholder(new Label("Không có sách nào của người dùng đang được mượn."));
            upcomingDueBooksListView.getItems().clear();
            return;
        }

        // SỬA LỖI TYPE: userId là String
        Set<String> userIds = allActiveLoans.stream().map(BorrowingRecord::getUserId).collect(Collectors.toSet());
        // SỬA LỖI TYPE và TÊN PHƯƠNG THỨC:
        Map<String, User> usersMap = userService.getUsersMapByIds(userIds); // getUsersMapByIds trả về Map<String, User>

        Set<Integer> bookIds = allActiveLoans.stream().map(BorrowingRecord::getBookInternalId).collect(Collectors.toSet());
        Map<Integer, Book> booksMap = bookManagementService.getBooksByInternalIds(bookIds)
                .stream().collect(Collectors.toMap(Book::getInternalId, Function.identity()));


        LocalDate today = LocalDate.now();
        LocalDate sevenDaysFromNow = today.plusDays(7);

        List<String> dueSoonDetails = allActiveLoans.stream()
                .filter(record -> record.getDueDate() != null &&
                        !record.getDueDate().isBefore(today) &&
                        record.getDueDate().isBefore(sevenDaysFromNow.plusDays(1)))
                .sorted(Comparator.comparing(BorrowingRecord::getDueDate))
                .map(record -> {
                    Book book = booksMap.get(record.getBookInternalId());
                    User user = usersMap.get(record.getUserId()); // Lấy user bằng String userId
                    String bookTitle = (book != null) ? book.getTitleOrDefault("Sách ID#" + record.getBookInternalId()) : "Sách ID#" + record.getBookInternalId();
                    String userName = (user != null) ? user.getUsernameOrDefault("User ID#" + record.getUserId()) : "User ID#" + record.getUserId();
                    long daysUntilDue = ChronoUnit.DAYS.between(today, record.getDueDate());
                    String dueStatus;
                    if (daysUntilDue == 0) dueStatus = "(Hạn hôm nay)";
                    else if (daysUntilDue == 1) dueStatus = "(Còn 1 ngày)";
                    else dueStatus = "(Còn " + daysUntilDue + " ngày)";

                    return String.format("%s - Sách: %s - Hạn: %s %s",
                            userName, bookTitle, record.getDueDate().format(displayDateFormatter), dueStatus);
                })
                .collect(Collectors.toList());

        if (dueSoonDetails.isEmpty()) {
            upcomingDueBooksListView.setPlaceholder(new Label("Không có sách nào của người dùng sắp đến hạn trong 7 ngày tới."));
        }
        upcomingDueBooksListView.setItems(FXCollections.observableArrayList(dueSoonDetails));
    }
}
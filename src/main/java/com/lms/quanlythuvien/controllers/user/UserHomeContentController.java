package com.lms.quanlythuvien.controllers.user;

import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.item.Quote;
import com.lms.quanlythuvien.models.transaction.BorrowingRecord;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.services.transaction.BorrowingRecordService;
import com.lms.quanlythuvien.utils.session.SessionManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class UserHomeContentController implements Initializable {

    //<editor-fold desc="FXML Injections - Profile Section">
    @FXML private Label greetingLabel;
    @FXML private ImageView avatarImageView;
    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private Label userPhoneLabel;
    @FXML private Label userDobLabel;
    @FXML private Label currentLoansCountLabel;
    //</editor-fold>

    //<editor-fold desc="FXML Injections - Charts">
    @FXML private BarChart<Number, String> loanHistoryLast14DaysChart;
    @FXML private CategoryAxis dateAxis14Days;
    @FXML private NumberAxis loanCountAxis14Days;
    @FXML private Label loanHistoryChartPlaceholder; // << THÊM CHO PLACEHOLDER

    @FXML private BarChart<Number, String> loanedGenresChart;
    @FXML private CategoryAxis genreCategoryAxis;
    @FXML private NumberAxis genreLoanCountAxis;
    @FXML private Label loanedGenresChartPlaceholder; // << THÊM CHO PLACEHOLDER
    //</editor-fold>

    //<editor-fold desc="FXML Injections - Due Soon & Quote">
    @FXML private ListView<String> dueSoonListView;
    @FXML private VBox quoteCardUser;
    @FXML private Label quoteTextLabel;
    @FXML private Label quoteAuthorLabel;
    //</editor-fold>

    private BorrowingRecordService borrowingRecordService;
    private BookManagementService bookManagementService;
    private User currentUser;
    private List<Quote> quotesList;

    public UserHomeContentController() {
        borrowingRecordService = BorrowingRecordService.getInstance();
        bookManagementService = BookManagementService.getInstance();
        initializeQuotes();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getInstance().getCurrentUser();

        // Thiết lập placeholder cho ListView (BarChart sẽ xử lý placeholder riêng)
        if (dueSoonListView != null) {
            dueSoonListView.setPlaceholder(new Label("Không có sách nào sắp đến hạn trong 5 ngày tới."));
        }

        // Ban đầu ẩn chart, hiện placeholder của chart
        setChartVisibility(loanHistoryLast14DaysChart, loanHistoryChartPlaceholder, false);
        setChartVisibility(loanedGenresChart, loanedGenresChartPlaceholder, false);


        if (currentUser != null) {
            Platform.runLater(() -> {
                populateProfileSectionUserInfo();
                loadCurrentLoansSummary();
                loadDueSoonBooks();
                loadLoanHistoryChartData();
                loadLoanedGenresChartData();
            });
        } else {
            if (greetingLabel != null) greetingLabel.setText("Không thể tải thông tin người dùng.");
            // Các chart và listview đã có placeholder hoặc sẽ hiển thị thông báo rỗng
        }
        loadQuoteOfTheDay();
    }

    private void initializeQuotes() {
        quotesList = Arrays.asList(
                new Quote("Sách là nguồn của cải quý báu của thế giới và là di sản xứng đáng của các thế hệ và các quốc gia.", "Henry David Thoreau"),
                new Quote("Đừng đọc những gì giải trí. Hãy đọc những gì khiến bạn phải suy nghĩ.", "Farah Gray"),
                new Quote("Một cuốn sách hay trên giá sách là một người bạn dù quay lưng lại nhưng vẫn là bạn tốt.", "Ngạn ngữ Việt Nam"),
                new Quote("Đọc sách không đảm bảo bạn sẽ thành công, nhưng mọi người thành công đều đọc sách rất nhiều.", "Warren Buffett"),
                new Quote("Thư viện là ngôi đền của học vấn, và học vấn đã tạo nên những con người vĩ đại.", "Ngạn ngữ")
        );
    }

    // Helper để ẩn/hiện chart và placeholder của nó
    private void setChartVisibility(BarChart<?,?> chart, Label placeholder, boolean showChart) {
        if (chart != null && placeholder != null) {
            chart.setVisible(showChart);
            chart.setManaged(showChart);
            placeholder.setVisible(!showChart);
            placeholder.setManaged(!showChart);
        }
    }

    private void populateProfileSectionUserInfo() {
        if (currentUser == null) return;
        if (greetingLabel != null) {
            int hour = java.time.LocalTime.now().getHour();
            if (hour < 5 || hour >= 22) greetingLabel.setText("Khuya rồi, nghỉ sớm nhé,");
            else if (hour < 12) greetingLabel.setText("Chào buổi sáng,");
            else if (hour < 18) greetingLabel.setText("Chào buổi chiều,");
            else greetingLabel.setText("Chào buổi tối,");
        }
        if (userNameLabel != null) userNameLabel.setText(currentUser.getUsername());
        if (userEmailLabel != null) userEmailLabel.setText("Email: " + currentUser.getEmail());

        if (userPhoneLabel != null) userPhoneLabel.setText("SĐT: (chưa có)"); // Cập nhật khi User model có
        if (userDobLabel != null) userDobLabel.setText("Năm sinh: (chưa có)"); // Cập nhật khi User model có

        loadDefaultAvatarForProfile();
    }

    private void loadDefaultAvatarForProfile() {
        if (avatarImageView == null) return;
        try (InputStream defaultStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_avatar.png")) {
            if (defaultStream != null) {
                avatarImageView.setImage(new Image(defaultStream));
            } else { System.err.println("Default avatar for profile not found.");}
        } catch (Exception e) { System.err.println("Failed to load default avatar for profile: " + e.getMessage());}
    }

    private void loadCurrentLoansSummary() {
        if (currentUser == null || currentLoansCountLabel == null) return;
        List<BorrowingRecord> activeLoans = borrowingRecordService.getLoansByUserId(currentUser.getUserId(), true);
        currentLoansCountLabel.setText(String.valueOf(activeLoans.size()));
    }

    private void loadDueSoonBooks() {
        if (currentUser == null || dueSoonListView == null) return;
        List<BorrowingRecord> activeLoans = borrowingRecordService.getLoansByUserId(currentUser.getUserId(), true);
        LocalDate today = LocalDate.now();
        LocalDate fiveDaysFromNow = today.plusDays(5);

        List<String> dueSoonDetails = activeLoans.stream()
                .filter(record -> !record.getDueDate().isBefore(today) && record.getDueDate().isBefore(fiveDaysFromNow.plusDays(1)))
                .sorted(Comparator.comparing(BorrowingRecord::getDueDate))
                .map(record -> {
                    Optional<Book> bookOpt = bookManagementService.findBookByInternalId(record.getBookInternalId());
                    String title = bookOpt.map(Book::getTitle).orElse("Sách (ID nội bộ: " + record.getBookInternalId() + ")");
                    long daysUntilDue = ChronoUnit.DAYS.between(today, record.getDueDate());
                    String dueStatus = "";
                    if (daysUntilDue < 0) dueStatus = " (Đã trễ hạn!)";
                    else if (daysUntilDue == 0) dueStatus = " (Hạn hôm nay)";
                    else if (daysUntilDue == 1) dueStatus = " (Còn 1 ngày)";
                    else dueStatus = " (Còn " + daysUntilDue + " ngày)";
                    return title + " - Hạn trả: " + record.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + dueStatus;
                })
                .collect(Collectors.toList());

        if (!dueSoonDetails.isEmpty()) {
            dueSoonListView.setItems(FXCollections.observableArrayList(dueSoonDetails));
        } else {
            dueSoonListView.getItems().clear(); // Xóa items cũ, placeholder sẽ hiển thị
        }
        // TODO: Implement custom CellFactory for dueSoonListView
    }

    private void loadQuoteOfTheDay() {
        if (quoteTextLabel == null || quoteAuthorLabel == null || quoteCardUser == null) return;
        if (quotesList != null && !quotesList.isEmpty()) {
            Random random = new Random();
            Quote randomQuote = quotesList.get(random.nextInt(quotesList.size()));
            quoteTextLabel.setText("\"" + randomQuote.getText() + "\"");
            quoteAuthorLabel.setText("— " + randomQuote.getAuthor());
            quoteCardUser.setVisible(true);
        } else {
            quoteCardUser.setVisible(false);
        }
    }

    private void loadLoanHistoryChartData() {
        if (currentUser == null || loanHistoryLast14DaysChart == null || dateAxis14Days == null || loanCountAxis14Days == null || loanHistoryChartPlaceholder == null) {
            setChartVisibility(loanHistoryLast14DaysChart, loanHistoryChartPlaceholder, false);
            return;
        }
        loanHistoryLast14DaysChart.getData().clear();
        XYChart.Series<Number, String> series = new XYChart.Series<>();

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(13);
        Map<String, Integer> dailyLoanCounts = new LinkedHashMap<>();
        DateTimeFormatter dayMonthFormatter = DateTimeFormatter.ofPattern("dd/MM");

        for (int i = 0; i < 14; i++) {
            dailyLoanCounts.put(startDate.plusDays(i).format(dayMonthFormatter), 0);
        }

        List<BorrowingRecord> allUserLoansInPeriod = borrowingRecordService.getLoansByUserId(currentUser.getUserId(), false)
                .stream()
                .filter(r -> r.getBorrowDate() != null && !r.getBorrowDate().isBefore(startDate) && !r.getBorrowDate().isAfter(today))
                .collect(Collectors.toList());

        boolean hasData = false;
        for (BorrowingRecord record : allUserLoansInPeriod) {
            String formattedDate = record.getBorrowDate().format(dayMonthFormatter);
            int newCount = dailyLoanCounts.getOrDefault(formattedDate, 0) + 1;
            dailyLoanCounts.put(formattedDate, newCount);
            if (newCount > 0) hasData = true;
        }

        ObservableList<String> dateCategories = FXCollections.observableArrayList();
        dailyLoanCounts.forEach((dateStr, count) -> {
            dateCategories.add(dateStr);
            series.getData().add(new XYChart.Data<>(count, dateStr));
        });

        if (hasData) {
            dateAxis14Days.setCategories(dateCategories);
            loanHistoryLast14DaysChart.getData().add(series);
            setChartVisibility(loanHistoryLast14DaysChart, loanHistoryChartPlaceholder, true);
        } else {
            setChartVisibility(loanHistoryLast14DaysChart, loanHistoryChartPlaceholder, false);
        }
    }

    private void loadLoanedGenresChartData() {
        if (currentUser == null || loanedGenresChart == null || genreCategoryAxis == null || genreLoanCountAxis == null || loanedGenresChartPlaceholder == null) {
            setChartVisibility(loanedGenresChart, loanedGenresChartPlaceholder, false);
            return;
        }
        loanedGenresChart.getData().clear();
        XYChart.Series<Number, String> series = new XYChart.Series<>();

        Map<String, Integer> genreCounts = new HashMap<>();
        List<BorrowingRecord> allUserLoans = borrowingRecordService.getLoansByUserId(currentUser.getUserId(), false);

        for (BorrowingRecord record : allUserLoans) {
            Optional<Book> bookOpt = bookManagementService.findBookByInternalId(record.getBookInternalId());
            if (bookOpt.isPresent() && bookOpt.get().getCategories() != null) {
                for (String category : bookOpt.get().getCategories()) {
                    String cleanCategory = category.trim();
                    if (!cleanCategory.isEmpty()) {
                        genreCounts.put(cleanCategory, genreCounts.getOrDefault(cleanCategory, 0) + 1);
                    }
                }
            }
        }

        List<Map.Entry<String, Integer>> sortedGenres = genreCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0) // Chỉ lấy thể loại có lượt mượn > 0
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(7)
                .collect(Collectors.toList());

        if (!sortedGenres.isEmpty()) {
            ObservableList<String> genreCategoriesList = FXCollections.observableArrayList();
            Collections.reverse(sortedGenres);
            for (Map.Entry<String, Integer> entry : sortedGenres) {
                genreCategoriesList.add(entry.getKey());
                series.getData().add(new XYChart.Data<>(entry.getValue(), entry.getKey()));
            }
            genreCategoryAxis.setCategories(genreCategoriesList);
            loanedGenresChart.getData().add(series);
            setChartVisibility(loanedGenresChart, loanedGenresChartPlaceholder, true);
        } else {
            setChartVisibility(loanedGenresChart, loanedGenresChartPlaceholder, false);
        }
    }
}
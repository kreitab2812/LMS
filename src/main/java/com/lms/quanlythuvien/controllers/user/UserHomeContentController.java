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

import java.io.File; // Thêm cho loadUserAvatar
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
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
    @FXML private NumberAxis loanCountAxis14Days;
    @FXML private CategoryAxis dateAxis14Days;
    @FXML private Label loanHistoryChartPlaceholder;

    @FXML private BarChart<Number, String> loanedGenresChart;
    @FXML private NumberAxis genreLoanCountAxis;
    @FXML private CategoryAxis genreCategoryAxis;
    @FXML private Label loanedGenresChartPlaceholder;
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
    private final Random random = new Random();
    private final DateTimeFormatter displayDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        borrowingRecordService = BorrowingRecordService.getInstance();
        bookManagementService = BookManagementService.getInstance();
        currentUser = SessionManager.getInstance().getCurrentUser();
        initializeQuotes();

        if (dueSoonListView != null) {
            dueSoonListView.setPlaceholder(new Label("Không có sách nào sắp đến hạn trả."));
        }
        setChartVisibility(loanHistoryLast14DaysChart, loanHistoryChartPlaceholder, false);
        setChartVisibility(loanedGenresChart, loanedGenresChartPlaceholder, false);

        if (currentUser != null) {
            Platform.runLater(() -> {
                populateProfileSectionUserInfo();
                loadCurrentLoansSummary();
                loadDueSoonBooksData();
                loadLoanActivityChartData();
                loadLoanedGenresChartData();
            });
        } else {
            handleNoUser();
        }
        loadQuoteOfTheDay();
    }

    private void handleNoUser(){
        if (greetingLabel != null) greetingLabel.setText("Xin chào!");
        if (userNameLabel != null) userNameLabel.setText("Vui lòng đăng nhập");
        if (userEmailLabel != null) userEmailLabel.setText("");
        if (userPhoneLabel != null) userPhoneLabel.setText("");
        if (userDobLabel != null) userDobLabel.setText("");
        if (currentLoansCountLabel != null) currentLoansCountLabel.setText("0");
        if (dueSoonListView != null) dueSoonListView.getItems().clear();
        // Không cần load avatar vì đã có default
        loadUserAvatar(); // Sẽ load default avatar
    }


    private void initializeQuotes() {
        quotesList = Arrays.asList(
                new Quote("Sách là nguồn của cải quý báu của thế giới và là di sản xứng đáng của các thế hệ và các quốc gia.", "Henry David Thoreau"),
                new Quote("Đọc sách không những để nâng cao trí tuệ mà còn để nâng cao tâm hồn.", "Maxim Gorky"),
                new Quote("Một cuốn sách hay trên giá sách là một người bạn dù quay lưng lại nhưng vẫn là bạn tốt.", "Ngạn ngữ Việt Nam")
        );
    }

    private void setChartVisibility(BarChart<?,?> chart, Label placeholder, boolean showChart) {
        if (chart != null && placeholder != null) {
            chart.setVisible(showChart); chart.setManaged(showChart);
            placeholder.setVisible(!showChart); placeholder.setManaged(!showChart);
        }
    }

    private void populateProfileSectionUserInfo() {
        if (currentUser == null) return;
        if (greetingLabel != null) {
            int hour = LocalTime.now().getHour();
            if (hour < 5 || hour >= 22) greetingLabel.setText("Khuya rồi, nghỉ ngơi nhé,");
            else if (hour < 12) greetingLabel.setText("Chào buổi sáng,");
            else if (hour < 18) greetingLabel.setText("Chào buổi chiều,");
            else greetingLabel.setText("Chào buổi tối,");
        }
        if (userNameLabel != null) userNameLabel.setText(currentUser.getFullNameOrDefault(currentUser.getUsernameOrDefault("Bạn")));
        if (userEmailLabel != null) userEmailLabel.setText("Email: " + currentUser.getEmailOrDefault("N/A"));
        if (userPhoneLabel != null) userPhoneLabel.setText("SĐT: " + currentUser.getPhoneNumberOrDefault("(chưa cập nhật)"));
        if (userDobLabel != null) userDobLabel.setText("Ngày sinh: " + currentUser.getDateOfBirthFormattedOrDefault("(chưa cập nhật)", displayDateFormatter));
        loadUserAvatar();
    }

    private void loadUserAvatar() {
        if (avatarImageView == null) return;
        Image imageToSet = null; String avatarPath = (currentUser != null) ? currentUser.getAvatarUrl() : null;
        if (avatarPath != null && !avatarPath.isEmpty()) {
            try {
                File avatarFile = new File(avatarPath);
                if (avatarFile.exists() && avatarFile.isFile() && avatarFile.canRead()) imageToSet = new Image(avatarFile.toURI().toString(), true);
                else imageToSet = new Image(avatarPath, true);
                if (imageToSet.isError()) imageToSet = null;
            } catch (Exception e) { imageToSet = null; }
        }
        if (imageToSet == null) {
            try (InputStream defaultStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/default_avatar.png")) {
                if (defaultStream != null) imageToSet = new Image(defaultStream);
            } catch (Exception e) { System.err.println("ERROR_UHCC_AVATAR_DEFAULT: " + e.getMessage());}
        }
        avatarImageView.setImage(imageToSet);
    }

    private void loadCurrentLoansSummary() {
        if (currentUser == null || currentLoansCountLabel == null) return;
        List<BorrowingRecord> activeLoans = borrowingRecordService.getLoansByUserId(currentUser.getUserId(), true);
        currentLoansCountLabel.setText(String.valueOf(activeLoans.size()));
    }

    private void loadDueSoonBooksData() {
        if (currentUser == null || dueSoonListView == null) return;
        List<BorrowingRecord> activeLoans = borrowingRecordService.getLoansByUserId(currentUser.getUserId(), true);
        if (activeLoans.isEmpty()) { dueSoonListView.getItems().clear(); return; }

        Set<Integer> bookIds = activeLoans.stream().map(BorrowingRecord::getBookInternalId).collect(Collectors.toSet());
        Map<Integer, Book> booksMap = bookManagementService.getBooksByInternalIds(bookIds)
                .stream().collect(Collectors.toMap(Book::getInternalId, Function.identity()));
        LocalDate today = LocalDate.now();
        LocalDate fiveDaysFromNow = today.plusDays(5);

        List<String> dueSoonDetails = activeLoans.stream()
                .filter(record -> record.getDueDate() != null && !record.getDueDate().isBefore(today) && record.getDueDate().isBefore(fiveDaysFromNow.plusDays(1)))
                .sorted(Comparator.comparing(BorrowingRecord::getDueDate))
                .map(record -> {
                    Book book = booksMap.get(record.getBookInternalId());
                    String title = (book != null) ? book.getTitleOrDefault("Sách ID: " + record.getBookInternalId()) : "Sách ID: " + record.getBookInternalId();
                    long daysUntilDue = ChronoUnit.DAYS.between(today, record.getDueDate());
                    String dueStatus = (daysUntilDue == 0) ? "(Hạn hôm nay)" : (daysUntilDue == 1 ? "(Còn 1 ngày)" : "(Còn " + daysUntilDue + " ngày)");
                    return title + " - Hạn: " + record.getDueDate().format(displayDateFormatter) + " " + dueStatus;
                })
                .collect(Collectors.toList());
        dueSoonListView.setItems(FXCollections.observableArrayList(dueSoonDetails));
    }

    private void loadQuoteOfTheDay() {
        if (quoteTextLabel == null || quoteAuthorLabel == null || quoteCardUser == null) return;
        if (quotesList != null && !quotesList.isEmpty()) {
            Quote randomQuote = quotesList.get(random.nextInt(quotesList.size()));
            quoteTextLabel.setText("\"" + randomQuote.getText() + "\"");
            quoteAuthorLabel.setText("— " + randomQuote.getAuthor());
            quoteCardUser.setVisible(true);
        } else {
            quoteCardUser.setVisible(false);
        }
    }

    private void loadLoanActivityChartData() {
        if (currentUser == null || loanHistoryLast14DaysChart == null || dateAxis14Days == null || loanCountAxis14Days == null || loanHistoryChartPlaceholder == null) {
            setChartVisibility(loanHistoryLast14DaysChart, loanHistoryChartPlaceholder, false); return;
        }
        loanHistoryLast14DaysChart.getData().clear();
        XYChart.Series<Number, String> series = new XYChart.Series<>();

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(13);
        Map<String, Integer> dailyLoanCounts = new LinkedHashMap<>();
        DateTimeFormatter chartDayFormatter = DateTimeFormatter.ofPattern("dd/MM");

        for (int i = 0; i < 14; i++) dailyLoanCounts.put(startDate.plusDays(i).format(chartDayFormatter), 0);

        List<BorrowingRecord> loansInPeriod = borrowingRecordService.getLoansByUserId(currentUser.getUserId(), false)
                .stream()
                .filter(r -> r.getBorrowDate() != null && !r.getBorrowDate().isBefore(startDate) && !r.getBorrowDate().isAfter(today))
                .collect(Collectors.toList());

        boolean hasData = false;
        for (BorrowingRecord record : loansInPeriod) {
            String formattedDate = record.getBorrowDate().format(chartDayFormatter);
            dailyLoanCounts.compute(formattedDate, (key, val) -> (val == null ? 0 : val) + 1);
            hasData = true; // Chỉ cần có một record là có data
        }

        if (hasData) {
            ObservableList<String> dateCategories = FXCollections.observableArrayList();
            List<Map.Entry<String, Integer>> entries = new ArrayList<>(dailyLoanCounts.entrySet());
            Collections.reverse(entries);
            for (Map.Entry<String, Integer> entry : entries) {
                dateCategories.add(entry.getKey());
                series.getData().add(new XYChart.Data<>(entry.getValue(), entry.getKey()));
            }
            dateAxis14Days.setCategories(dateCategories);
            loanHistoryLast14DaysChart.getData().add(series);
            setChartVisibility(loanHistoryLast14DaysChart, loanHistoryChartPlaceholder, true);
        } else {
            setChartVisibility(loanHistoryLast14DaysChart, loanHistoryChartPlaceholder, false);
        }
    }

    private void loadLoanedGenresChartData() {
        if (currentUser == null || loanedGenresChart == null || genreCategoryAxis == null || genreLoanCountAxis == null || loanedGenresChartPlaceholder == null) {
            setChartVisibility(loanedGenresChart, loanedGenresChartPlaceholder, false); return;
        }
        loanedGenresChart.getData().clear();
        XYChart.Series<Number, String> series = new XYChart.Series<>();

        LocalDate last30Days = LocalDate.now().minusDays(30);
        List<BorrowingRecord> recentLoans = borrowingRecordService.getLoansByUserId(currentUser.getUserId(), false)
                .stream().filter(r -> r.getBorrowDate() != null && !r.getBorrowDate().isBefore(last30Days))
                .collect(Collectors.toList());

        if (recentLoans.isEmpty()) { setChartVisibility(loanedGenresChart, loanedGenresChartPlaceholder, false); return; }

        Set<Integer> bookIdsInHistory = recentLoans.stream().map(BorrowingRecord::getBookInternalId).collect(Collectors.toSet());
        Map<Integer, Book> booksMap = bookManagementService.getBooksByInternalIds(bookIdsInHistory)
                .stream().collect(Collectors.toMap(Book::getInternalId, Function.identity()));
        Map<String, Integer> genreCounts = new HashMap<>();
        for (BorrowingRecord record : recentLoans) {
            Book book = booksMap.get(record.getBookInternalId());
            if (book != null && book.getCategories() != null) {
                for (String category : book.getCategories()) {
                    String cleanCategory = category.trim();
                    if (!cleanCategory.isEmpty()) genreCounts.put(cleanCategory, genreCounts.getOrDefault(cleanCategory, 0) + 1);
                }
            }
        }
        if (genreCounts.isEmpty()) { setChartVisibility(loanedGenresChart, loanedGenresChartPlaceholder, false); return; }

        List<Map.Entry<String, Integer>> sortedGenres = genreCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(7)
                .collect(Collectors.toList());

        if (!sortedGenres.isEmpty()) {
            Collections.reverse(sortedGenres);
            ObservableList<String> genreCategoriesList = FXCollections.observableArrayList();
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
package com.lms.quanlythuvien.controllers.common;

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.transaction.BorrowingRecord;
import com.lms.quanlythuvien.models.transaction.LoanStatus;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.services.transaction.BorrowingRecordService;
import com.lms.quanlythuvien.utils.session.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.InputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional; // Đã có từ code của cậu
import java.util.ResourceBundle;
import java.util.Set;     // <<< THÊM IMPORT NÀY
import java.util.Map;     // <<< THÊM IMPORT NÀY
import java.util.HashSet; // <<< THÊM IMPORT NÀY (nếu dùng new HashSet<>())
import java.util.HashMap; // <<< THÊM IMPORT NÀY (nếu dùng new HashMap<>())
import java.util.stream.Collectors; // Đã có từ code của cậu

public class LockedAccountController implements Initializable {

    @FXML private Label lockReasonMessageLabel;
    @FXML private VBox overdueBooksSection;
    @FXML private ListView<String> overdueBooksListView;
    @FXML private Label fineAmountLabel;
    @FXML private Button acknowledgeButton;
    @FXML private ImageView warningIconImageView;

    private User lockedUser;
    private BorrowingRecordService borrowingRecordService;
    private BookManagementService bookManagementService;
    private NumberFormat currencyFormatter;
    private final DateTimeFormatter displayDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public LockedAccountController() {
        // Constructor
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        borrowingRecordService = BorrowingRecordService.getInstance();
        bookManagementService = BookManagementService.getInstance();
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        this.lockedUser = SessionManager.getInstance().getLockedUserAccountInfo();
        // SessionManager.getInstance().setLockedUserAccountInfo(null); // Cân nhắc xóa sau khi lấy

        if (lockedUser != null) {
            populateLockedInfo();
            if (warningIconImageView != null) {
                try (InputStream iconStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/warning_icon_large.png")) {
                    if (iconStream != null) {
                        warningIconImageView.setImage(new Image(iconStream));
                    } else {
                        System.err.println("ERROR_LOCKED_ACC: Warning icon not found.");
                    }
                } catch (Exception e) {
                    System.err.println("ERROR_LOCKED_ACC: Failed to load warning icon: " + e.getMessage());
                }
            }
        } else {
            lockReasonMessageLabel.setText("Lỗi: Không xác định được thông tin tài khoản bị khóa.");
            fineAmountLabel.setText("Phí phạt: Không xác định");
            if (overdueBooksSection != null) {
                overdueBooksSection.setVisible(false);
                overdueBooksSection.setManaged(false);
            }
            if (acknowledgeButton != null) acknowledgeButton.setText("Thoát");
        }
        if(overdueBooksListView != null) {
            overdueBooksListView.setPlaceholder(new Label("Không có sách nào đang quá hạn."));
        }
    }

    private void populateLockedInfo() {
        if (lockedUser == null) return;

        String reason = "Tài khoản của bạn (" + lockedUser.getUsernameOrDefault("N/A") + ") đã bị tạm khóa.";
        boolean hasFine = lockedUser.getCurrentFineAmount() > 0;
        boolean lowReputation = lockedUser.getReputationScore() < 50;

        if (hasFine && lowReputation) {
            reason += "\nLý do: Vi phạm quy định (điểm uy tín thấp và còn phí phạt).";
        } else if (hasFine) {
            reason += "\nLý do: Còn phí phạt chưa thanh toán.";
        } else if (lowReputation) {
            reason += "\nLý do: Điểm uy tín của bạn quá thấp (" + lockedUser.getReputationScore() + "/100).";
        } else {
            reason += "\nLý do: Vi phạm các quy định khác của thư viện.";
        }
        lockReasonMessageLabel.setText(reason);

        fineAmountLabel.setText("Tổng phí phạt cần thanh toán: " + currencyFormatter.format(lockedUser.getCurrentFineAmount()));
        fineAmountLabel.setVisible(hasFine); // Chỉ hiển thị nếu có phí phạt
        fineAmountLabel.setManaged(hasFine);


        List<BorrowingRecord> overdueRecords = borrowingRecordService.getLoansByUserId(lockedUser.getUserId(), true)
                .stream()
                .filter(record -> record.getStatus() == LoanStatus.OVERDUE)
                .collect(Collectors.toList());

        if (!overdueRecords.isEmpty()) {
            ObservableList<String> overdueBookDetails = FXCollections.observableArrayList();

            // Tối ưu N+1: Lấy danh sách Book một lần
            Set<Integer> bookIds = overdueRecords.stream() // Sử dụng Set
                    .map(BorrowingRecord::getBookInternalId)
                    .collect(Collectors.toSet());
            // BookManagementService.getBooksByInternalIds trả về List<Book>, cần chuyển sang Map
            Map<Integer, Book> booksMap = bookManagementService.getBooksByInternalIds(bookIds) // Sử dụng Set<Integer>
                    .stream()
                    .collect(Collectors.toMap(Book::getInternalId, book -> book, (b1, b2) -> b1)); // Xử lý key trùng (nếu có)


            for (BorrowingRecord record : overdueRecords) {
                Book book = booksMap.get(record.getBookInternalId()); // Sử dụng Map
                String bookTitle = book != null ? book.getTitleOrDefault("Sách ID: " + record.getBookInternalId()) : "Sách ID: " + record.getBookInternalId();
                overdueBookDetails.add("• " + bookTitle + " (Hạn trả: " + record.getDueDate().format(displayDateFormatter) + ")");
            }
            overdueBooksListView.setItems(overdueBookDetails);
            overdueBooksSection.setVisible(true);
            overdueBooksSection.setManaged(true);
        } else {
            overdueBooksSection.setVisible(false);
            overdueBooksSection.setManaged(false);
        }
    }

    @FXML
    void handleAcknowledgeAction(ActionEvent event) {
        System.out.println("User acknowledged account lock. They should contact library staff.");
        showAlert(Alert.AlertType.INFORMATION, "Thông báo",
                "Tài khoản của bạn hiện đang bị khóa.\n" +
                        "Vui lòng liên hệ Quản trị viên thư viện (tại quầy hoặc qua email uet.library.contact@vnu.edu.vn) " +
                        "để được hướng dẫn giải quyết và mở khóa tài khoản.");
        // Không nên tự động đóng ứng dụng hoặc quay lại login, vì tài khoản vẫn bị khóa.
        // Người dùng cần đọc kỹ thông báo.
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyDialogStyles(alert.getDialogPane());
        alert.showAndWait();
    }

    private void applyDialogStyles(DialogPane dialogPane) {
        try {
            URL cssUrl = getClass().getResource("/com/lms/quanlythuvien/css/styles.css");
            if (cssUrl != null) {
                dialogPane.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) { System.err.println("WARN_DIALOG_CSS: Failed to load CSS for dialog: " + e.getMessage()); }
    }
}
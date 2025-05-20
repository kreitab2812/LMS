package com.lms.quanlythuvien.controllers.admin;

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.system.Notification;
import com.lms.quanlythuvien.models.transaction.DonationRequest;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.library.BookManagementService;
import com.lms.quanlythuvien.services.system.NotificationService;
import com.lms.quanlythuvien.services.transaction.DonationRequestService;
import com.lms.quanlythuvien.services.user.UserService;
import com.lms.quanlythuvien.utils.session.SessionManager;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.util.StringConverter;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos; // Thêm cho HBox alignment
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage; // Thêm nếu dùng Stage cho Dialog
import javafx.util.Callback; // Thêm cho TableColumn Action

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.Comparator;

public class AdminDonationManagementController implements Initializable {

    // FXML Fields cho TableView và các cột
    @FXML private TableView<DonationRequest> donationsTableView;
    @FXML private TableColumn<DonationRequest, String> requestIdColumn;
    @FXML private TableColumn<DonationRequest, String> drBookNameColumn;
    @FXML private TableColumn<DonationRequest, String> drAuthorNameColumn;
    // @FXML private TableColumn<DonationRequest, String> drCategoryColumn; // Có thể bỏ nếu không hiển thị trực tiếp
    @FXML private TableColumn<DonationRequest, String> donatorNameColumn; // Hiển thị tên người dùng
    @FXML private TableColumn<DonationRequest, LocalDate> drRequestDateColumn;
    @FXML private TableColumn<DonationRequest, DonationRequest.DonationStatus> drStatusColumn;
    @FXML private TableColumn<DonationRequest, Void> donationActionsColumn; // Cột chứa nút hành động

    // FXML Fields cho bộ lọc và tìm kiếm
    @FXML private ComboBox<DonationRequest.DonationStatus> donationStatusFilterComboBox;
    @FXML private TextField donationSearchField;
    @FXML private Button searchDonationsButton; // Nút "Lọc/Tìm"

    // FXML Fields cho các nút hành động chung (nếu vẫn giữ trong FXML)
    // Nếu chỉ dùng action trong TableView, các nút này có thể không cần thiết
    @FXML private Button approveDonationButton;
    @FXML private Button rejectDonationButton;
    @FXML private Button refreshDonationsButton;

    private DonationRequestService donationRequestService;
    private BookManagementService bookManagementService;
    private NotificationService notificationService;
    private UserService userService;
    private User currentAdmin;

    private ObservableList<DonationRequest> observableDonationList;
    private FilteredList<DonationRequest> filteredDonationList;

    private AdminDashboardController dashboardController; // Để gọi lại dashboard nếu cần

    public void setDashboardController(AdminDashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        donationRequestService = DonationRequestService.getInstance();
        bookManagementService = BookManagementService.getInstance();
        notificationService = NotificationService.getInstance();
        userService = UserService.getInstance();
        currentAdmin = SessionManager.getInstance().getCurrentUser(); // Lấy admin hiện tại

        observableDonationList = FXCollections.observableArrayList();
        filteredDonationList = new FilteredList<>(observableDonationList, p -> true);

        setupDonationsTableColumns();
        donationsTableView.setItems(filteredDonationList);
        donationsTableView.setPlaceholder(new Label("Không có yêu cầu quyên góp nào."));

        setupFilterControls();
        setupMainActionButtons();

        loadAllDonationRequests();
    }

    private void setupFilterControls() {
        if (donationStatusFilterComboBox != null) {
            ObservableList<DonationRequest.DonationStatus> statuses = FXCollections.observableArrayList();
            statuses.add(null); // Lựa chọn "Tất cả trạng thái"
            statuses.addAll(DonationRequest.DonationStatus.values());
            donationStatusFilterComboBox.setItems(statuses);
            donationStatusFilterComboBox.setConverter(new StringConverter<DonationRequest.DonationStatus>() {
                @Override
                public String toString(DonationRequest.DonationStatus status) {
                    if (status == null) return "-- Tất cả trạng thái --";
                    // Có thể thêm logic dịch tên Enum sang Tiếng Việt ở đây
                    return status.name();
                }
                @Override
                public DonationRequest.DonationStatus fromString(String string) {
                    return null; // Không cần thiết cho ComboBox chỉ hiển thị
                }
            });
            donationStatusFilterComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
        if (donationSearchField != null) {
            donationSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
    }

    private void setupMainActionButtons(){
        donationsTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean requestSelectedAndPending = (newSelection != null && newSelection.getStatus() == DonationRequest.DonationStatus.PENDING_APPROVAL);
            if (approveDonationButton != null) approveDonationButton.setDisable(!requestSelectedAndPending);
            if (rejectDonationButton != null) rejectDonationButton.setDisable(!requestSelectedAndPending);
        });
        if (approveDonationButton != null) approveDonationButton.setDisable(true);
        if (rejectDonationButton != null) rejectDonationButton.setDisable(true);
    }


    private void setupDonationsTableColumns() {
        if (requestIdColumn != null) requestIdColumn.setCellValueFactory(new PropertyValueFactory<>("requestId"));
        drBookNameColumn.setCellValueFactory(new PropertyValueFactory<>("bookName"));
        drAuthorNameColumn.setCellValueFactory(new PropertyValueFactory<>("authorName"));
        // drCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));

        if (donatorNameColumn != null) {
            donatorNameColumn.setCellValueFactory(cellData -> {
                DonationRequest request = cellData.getValue();
                if (request != null && request.getUserId() != null && userService != null) { // Thêm kiểm tra userService
                    Optional<User> userOpt = userService.findUserById(request.getUserId());
                    // Sử dụng getUsernameOrDefault từ User model
                    return new SimpleStringProperty(userOpt.map(user -> user.getUsernameOrDefault(request.getUserId())).orElse(request.getUserId()));
                }
                return new SimpleStringProperty(request != null ? request.getUserId() : "N/A");
            });
        }

        drRequestDateColumn.setCellValueFactory(new PropertyValueFactory<>("requestDate"));
        drStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        if (donationActionsColumn != null) {
            Callback<TableColumn<DonationRequest, Void>, TableCell<DonationRequest, Void>> cellFactory = param -> new TableCell<>() {
                private final Button btnApproveCell = new Button("Duyệt");
                private final Button btnRejectCell = new Button("Từ chối");
                private final HBox pane = new HBox(5, btnApproveCell, btnRejectCell);
                {
                    btnApproveCell.getStyleClass().add("success-button-small");
                    btnRejectCell.getStyleClass().add("danger-button-small");
                    pane.setAlignment(Pos.CENTER);

                    btnApproveCell.setOnAction(event -> {
                        DonationRequest req = getTableView().getItems().get(getIndex());
                        approveSelectedRequest(req);
                    });
                    btnRejectCell.setOnAction(event -> {
                        DonationRequest req = getTableView().getItems().get(getIndex());
                        rejectSelectedRequest(req);
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                    } else {
                        DonationRequest req = getTableRow().getItem();
                        boolean isPending = req.getStatus() == DonationRequest.DonationStatus.PENDING_APPROVAL;
                        btnApproveCell.setDisable(!isPending);
                        btnRejectCell.setDisable(!isPending);
                        setGraphic(pane);
                    }
                }
            };
            donationActionsColumn.setCellFactory(cellFactory);
        }
    }

    private void loadAllDonationRequests() {
        List<DonationRequest> allRequests = donationRequestService.getAllRequests(); // Giả sử có hàm này
        observableDonationList.setAll(allRequests != null ? allRequests : new ArrayList<>());
        applyFilters(); // Áp dụng filter để hiển thị đúng
        System.out.println("DEBUG_ADMC: Loaded all donation requests. Count: " + observableDonationList.size());
    }

    @FXML
    private void handleRefreshDonationsAction(ActionEvent event) {
        loadAllDonationRequests();
        showAlert(Alert.AlertType.INFORMATION, "Làm mới", "Đã cập nhật danh sách yêu cầu quyên góp.");
    }

    @FXML
    private void handleSearchDonationsAction(ActionEvent event) {
        applyFilters(); // Nút này chỉ cần gọi lại applyFilters
    }

    private void applyFilters() {
        if (filteredDonationList == null) return;
        DonationRequest.DonationStatus selectedStatus = (donationStatusFilterComboBox != null) ? donationStatusFilterComboBox.getValue() : null;
        String keyword = (donationSearchField != null && donationSearchField.getText() != null) ? donationSearchField.getText().toLowerCase().trim() : "";

        filteredDonationList.setPredicate(request -> {
            boolean statusMatch = (selectedStatus == null) || (request.getStatus() == selectedStatus);
            if (!statusMatch) return false;

            if (keyword.isEmpty()) return true;

            boolean keywordMatch = (request.getBookName() != null && request.getBookName().toLowerCase().contains(keyword)) ||
                    (request.getAuthorName() != null && request.getAuthorName().toLowerCase().contains(keyword)) ||
                    (request.getUserId() != null && request.getUserId().toLowerCase().contains(keyword));

            if (!keywordMatch && request.getUserId() != null && userService != null) {
                Optional<User> userOpt = userService.findUserById(request.getUserId());
                if (userOpt.isPresent() && userOpt.get().getUsernameOrDefault("").toLowerCase().contains(keyword)) {
                    keywordMatch = true;
                }
            }
            return keywordMatch;
        });
        donationsTableView.setPlaceholder(new Label(filteredDonationList.isEmpty() ? "Không có yêu cầu nào khớp." : ""));
        donationsTableView.refresh();
    }

    // Xử lý cho các nút @FXML approve/reject chung
    @FXML
    private void handleApproveDonationAction(ActionEvent event) {
        DonationRequest selectedRequest = donationsTableView.getSelectionModel().getSelectedItem();
        if (selectedRequest != null) {
            approveSelectedRequest(selectedRequest);
        } else {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn một yêu cầu để duyệt.");
        }
    }

    @FXML
    private void handleRejectDonationAction(ActionEvent event) {
        DonationRequest selectedRequest = donationsTableView.getSelectionModel().getSelectedItem();
        if (selectedRequest != null) {
            rejectSelectedRequest(selectedRequest);
        } else {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn một yêu cầu để từ chối.");
        }
    }

    private void approveSelectedRequest(DonationRequest requestToApprove) {
        if (requestToApprove == null || requestToApprove.getStatus() != DonationRequest.DonationStatus.PENDING_APPROVAL) {
            showAlert(Alert.AlertType.WARNING, "Không hợp lệ", "Chỉ có thể duyệt các yêu cầu đang chờ.");
            return;
        }
        Optional<Book> bookDetailsOptional = showCompleteBookInfoDialog(requestToApprove);
        bookDetailsOptional.ifPresent(completedBook -> {
            Optional<Book> addedBookOpt = bookManagementService.addBookToLibrary(completedBook);
            if (addedBookOpt.isPresent()) {
                boolean statusUpdated = donationRequestService.updateRequestStatus(
                        requestToApprove.getRequestId(), DonationRequest.DonationStatus.COMPLETED,
                        "Đã duyệt. Sách được thêm với ID nội bộ: " + addedBookOpt.get().getInternalId(), LocalDate.now()
                );
                if (statusUpdated) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã duyệt và thêm sách '" + completedBook.getTitleOrDefault("N/A") + "' vào thư viện.");
                    if (notificationService != null && requestToApprove.getUserId() != null) {
                        notificationService.createNotification(
                                requestToApprove.getUserId(),
                                "Yêu cầu quyên góp sách '" + requestToApprove.getBookName() + "' của bạn đã được duyệt và thêm vào thư viện. Xin cảm ơn!",
                                Notification.NotificationType.DONATION_PROCESSED_USER, // Cần type này
                                requestToApprove.getRequestId(), null);
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi Cập Nhật", "Sách đã thêm, nhưng không thể cập nhật trạng thái yêu cầu.");
                }
                loadAllDonationRequests();
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi Thêm Sách", "Không thể thêm sách. Mã sách/ISBN có thể đã tồn tại.");
            }
        });
    }

    private void rejectSelectedRequest(DonationRequest requestToReject) {
        if (requestToReject == null || requestToReject.getStatus() != DonationRequest.DonationStatus.PENDING_APPROVAL) {
            showAlert(Alert.AlertType.WARNING, "Không hợp lệ", "Chỉ có thể từ chối các yêu cầu đang chờ.");
            return;
        }
        TextInputDialog reasonDialog = new TextInputDialog("Sách không phù hợp với tiêu chí của thư viện.");
        reasonDialog.setTitle("Từ chối Yêu cầu Quyên góp");
        reasonDialog.setHeaderText("Từ chối quyên góp sách: " + requestToReject.getBookName());
        reasonDialog.setContentText("Nhập lý do từ chối (*):");
        applyDialogStyles(reasonDialog.getDialogPane());
        Optional<String> reasonResult = reasonDialog.showAndWait();

        if (reasonResult.isPresent() && !reasonResult.get().trim().isEmpty()) {
            String reason = reasonResult.get().trim();
            boolean success = donationRequestService.updateRequestStatus(
                    requestToReject.getRequestId(), DonationRequest.DonationStatus.REJECTED,
                    reason, null
            );
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Đã từ chối yêu cầu: '" + requestToReject.getBookName() + "'.");
                if (notificationService != null && requestToReject.getUserId() != null) {
                    notificationService.createNotification(
                            requestToReject.getUserId(),
                            "Yêu cầu quyên góp sách '" + requestToReject.getBookName() + "' của bạn đã bị từ chối. Lý do: " + reason,
                            Notification.NotificationType.DONATION_PROCESSED_USER, // Dùng chung type này
                            requestToReject.getRequestId(), null);
                }
                loadAllDonationRequests();
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể từ chối yêu cầu.");
            }
        } else if (reasonResult.isPresent()) { // Người dùng nhấn OK nhưng không nhập lý do
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập lý do từ chối.");
        }
    }

    private Optional<Book> showCompleteBookInfoDialog(DonationRequest donationRequest) {
        Dialog<Book> dialog = new Dialog<>();
        dialog.setTitle("Hoàn Thiện Thông Tin Sách Quyên Góp");
        dialog.setHeaderText("Sách đề xuất: " + donationRequest.getBookName() + " (TG: " + (donationRequest.getAuthorName() != null ? donationRequest.getAuthorName() : "N/A") + ")");
        applyDialogStyles(dialog.getDialogPane());

        ButtonType saveButtonType = ButtonType.OK;
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField isbnField = new TextField(); isbnField.setPromptText("ISBN-13 (nếu có)");
        TextField titleField = new TextField(donationRequest.getBookName());
        TextField authorsField = new TextField(donationRequest.getAuthorName());
        TextField publisherField = new TextField(); publisherField.setPromptText("Nhà xuất bản");
        TextField publishedDateField = new TextField(); publishedDateField.setPromptText("Năm XB (YYYY)");
        TextField categoriesField = new TextField(donationRequest.getCategory());
        TextArea descriptionArea = new TextArea(); descriptionArea.setPromptText("Mô tả chi tiết"); descriptionArea.setPrefRowCount(3); descriptionArea.setWrapText(true);
        TextField thumbnailUrlField = new TextField(); thumbnailUrlField.setPromptText("URL ảnh bìa");
        TextField pageCountField = new TextField(); pageCountField.setPromptText("Số trang");
        TextField totalQuantityField = new TextField("1"); totalQuantityField.setPromptText("Số lượng nhận (*)");
        TextField shelfLocationField = new TextField(); shelfLocationField.setPromptText("Vị trí kệ");
        TextField customDisplayIdField = new TextField(); customDisplayIdField.setPromptText("Mã hiển thị phụ");
        TextField isbn10Field = new TextField(); isbn10Field.setPromptText("ISBN-10 (nếu có)");

        int rowIndex = 0;
        grid.add(new Label("ISBN-13/Mã sách:"), 0, rowIndex); grid.add(isbnField, 1, rowIndex++, 2, 1);
        grid.add(new Label("Tiêu đề (*):"), 0, rowIndex); grid.add(titleField, 1, rowIndex++, 2, 1);
        grid.add(new Label("Tác giả:"), 0, rowIndex); grid.add(authorsField, 1, rowIndex++, 2, 1);
        grid.add(new Label("Thể loại:"), 0, rowIndex); grid.add(categoriesField, 1, rowIndex++, 2, 1);
        grid.add(new Label("Nhà XB:"), 0, rowIndex); grid.add(publisherField, 1, rowIndex++, 2, 1);
        grid.add(new Label("Năm XB:"), 0, rowIndex); grid.add(publishedDateField, 1, rowIndex++, 2, 1);
        grid.add(new Label("Mô tả:"), 0, rowIndex); grid.add(descriptionArea, 1, rowIndex++, 2, 1);
        grid.add(new Label("URL Ảnh bìa:"), 0, rowIndex); grid.add(thumbnailUrlField, 1, rowIndex++, 2, 1);
        grid.add(new Label("Số trang:"), 0, rowIndex); grid.add(pageCountField, 1, rowIndex++, 2, 1);
        grid.add(new Label("Số lượng (*):"), 0, rowIndex); grid.add(totalQuantityField, 1, rowIndex++, 2, 1);
        grid.add(new Label("Vị trí kệ:"), 0, rowIndex); grid.add(shelfLocationField, 1, rowIndex++, 2, 1);
        grid.add(new Label("Mã hiển thị phụ:"), 0, rowIndex); grid.add(customDisplayIdField, 1, rowIndex++, 2, 1);
        grid.add(new Label("ISBN-10:"), 0, rowIndex); grid.add(isbn10Field, 1, rowIndex, 2, 1);

        dialog.getDialogPane().setContent(grid);
        Node saveButtonNode = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButtonNode.setDisable(true);

        Runnable validateFields = () -> {
            boolean disable = titleField.getText().trim().isEmpty() || totalQuantityField.getText().trim().isEmpty();
            try {
                if (!totalQuantityField.getText().trim().isEmpty() && Integer.parseInt(totalQuantityField.getText().trim()) <= 0) disable = true;
                if (!pageCountField.getText().trim().isEmpty() && Integer.parseInt(pageCountField.getText().trim()) < 0) disable = true;
            } catch (NumberFormatException e) { disable = true; }
            saveButtonNode.setDisable(disable);
        };
        titleField.textProperty().addListener((obs, o, n) -> validateFields.run());
        totalQuantityField.textProperty().addListener((obs, o, n) -> validateFields.run());
        pageCountField.textProperty().addListener((obs, o, n) -> validateFields.run());
        validateFields.run();

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                int parsedTotalQuantity; Integer parsedPageCount = null;
                try {
                    parsedTotalQuantity = Integer.parseInt(totalQuantityField.getText().trim());
                    if (parsedTotalQuantity <= 0) { showAlert(Alert.AlertType.ERROR, "Lỗi", "Số lượng phải lớn hơn 0."); return null; }
                    if (!pageCountField.getText().trim().isEmpty()) {
                        parsedPageCount = Integer.parseInt(pageCountField.getText().trim());
                        if (parsedPageCount < 0) { showAlert(Alert.AlertType.ERROR, "Lỗi", "Số trang không được âm."); return null; }
                    }
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi Dữ Liệu", "Số lượng và Số trang phải là số hợp lệ."); return null;
                }
                String bookId = isbnField.getText().trim(); // Đây sẽ là ISBN-13 nếu có
                if (bookId.isEmpty() && !customDisplayIdField.getText().trim().isEmpty()){ bookId = customDisplayIdField.getText().trim(); }
                // Nếu bookId vẫn rỗng, nó sẽ là null, BookManagementService.addBookToLibrary sẽ không dùng nó làm ID chính

                List<String> authorsList = Arrays.stream(authorsField.getText().split(",")) .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
                List<String> categoriesList = Arrays.stream(categoriesField.getText().split(",")) .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());

                // Dùng constructor Book đã sửa
                Book newBook = new Book(
                        (bookId != null && !bookId.isEmpty()) ? bookId : null, // id (ISBN-13)
                        titleField.getText().trim(), authorsList,
                        publisherField.getText().trim(), publishedDateField.getText().trim(),
                        descriptionArea.getText().trim(), categoriesList,
                        thumbnailUrlField.getText().trim(), null, // infoLink
                        isbn10Field.getText().trim(),
                        parsedPageCount, 0.0, 0, parsedTotalQuantity
                );
                newBook.setCustomDisplayId(customDisplayIdField.getText().trim());
                newBook.setShelfLocation(shelfLocationField.getText().trim());
                return newBook;
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
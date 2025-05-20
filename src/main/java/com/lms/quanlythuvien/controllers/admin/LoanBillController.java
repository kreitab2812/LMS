package com.lms.quanlythuvien.controllers.admin;

import com.lms.quanlythuvien.models.item.Book;
import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.utils.helpers.QRUtils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LoanBillController {

    @FXML private Label bookTitleLabel;
    @FXML private Label bookAuthorsLabel;
    @FXML private Label bookIsbnLabel; // Nên hiển thị ID chính của sách (ISBN13/CustomID)
    @FXML private Label userNameLabel;
    @FXML private Label userIdLabel;
    @FXML private Label borrowDateLabel;
    @FXML private Label dueDateLabel;
    @FXML private ImageView qrCodeImageView;
    @FXML private Label qrDataLabel; // Hiển thị dữ liệu được mã hóa trong QR
    @FXML private Button closeBillButton;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void setBillData(Book book, User user, LocalDate borrowDate, LocalDate dueDate) {
        if (book != null) {
            bookTitleLabel.setText(book.getTitle() != null ? book.getTitle() : "N/A");
            bookAuthorsLabel.setText(book.getAuthors() != null && !book.getAuthors().isEmpty() ?
                    String.join(", ", book.getAuthors()) : "N/A");
            // Hiển thị ID chính của sách (có thể là ISBN13, CustomDisplayID, hoặc internalId nếu các cái kia null)
            String displayId = book.getId(); // Book.getId() trả về ISBN13 hoặc CustomID (từ Document.id)
            if (displayId == null || displayId.trim().isEmpty()) {
                displayId = book.getCustomDisplayId();
            }
            if (displayId == null || displayId.trim().isEmpty()) {
                displayId = String.valueOf(book.getInternalId()); // Fallback cuối cùng
            }
            bookIsbnLabel.setText(displayId != null ? displayId : "N/A");

            String qrDataForImage = book.getQrCodeData(); // qrCodeData đã được AdminLoanManagementController chuẩn bị
            if (qrDataForImage != null && !qrDataForImage.trim().isEmpty()) {
                Image qrImage = QRUtils.generateQRCodeImage(qrDataForImage, 150, 150);
                if (qrImage != null) {
                    qrCodeImageView.setImage(qrImage);
                    qrDataLabel.setText("QR Data: " + qrDataForImage); // Hiển thị data của QR
                } else {
                    qrDataLabel.setText("Lỗi tạo mã QR");
                    // qrCodeImageView.setImage(null); // Hoặc ảnh lỗi
                }
            } else {
                qrDataLabel.setText("Sách không có dữ liệu QR hợp lệ.");
                // qrCodeImageView.setImage(null); // Hoặc ảnh lỗi
            }
        } else {
            bookTitleLabel.setText("N/A");
            bookAuthorsLabel.setText("N/A");
            bookIsbnLabel.setText("N/A");
            qrDataLabel.setText("Không có thông tin sách");
        }

        if (user != null) {
            userNameLabel.setText(user.getFullName() != null ? user.getFullName() : user.getUsername());
            userIdLabel.setText(user.getUserId());
        } else {
            userNameLabel.setText("N/A");
            userIdLabel.setText("N/A");
        }

        if (borrowDate != null) borrowDateLabel.setText(borrowDate.format(DATE_FORMATTER));
        else borrowDateLabel.setText("N/A");

        if (dueDate != null) dueDateLabel.setText(dueDate.format(DATE_FORMATTER));
        else dueDateLabel.setText("N/A");
    }

    @FXML
    private void handleCloseBill(ActionEvent event) {
        closeDialog();
    }

    private void closeDialog() {
        if (closeBillButton != null && closeBillButton.getScene() != null) {
            Stage stage = (Stage) closeBillButton.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        } else {
            System.err.println("ERROR_LBC_CLOSE: Cannot get stage from closeBillButton.");
        }
    }
}
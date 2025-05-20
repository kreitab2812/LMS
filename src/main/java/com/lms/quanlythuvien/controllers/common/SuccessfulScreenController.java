package com.lms.quanlythuvien.controllers.common;

import com.lms.quanlythuvien.MainApp;
import com.lms.quanlythuvien.utils.session.SessionManager; // Thêm để lấy thông điệp

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label; // Thêm nếu cần truy cập messageLabel

import java.net.URL;
import java.util.ResourceBundle;

public class SuccessfulScreenController implements Initializable {

    @FXML private Label messageLine1Label; // Giả sử FXML có fx:id này
    @FXML private Label messageLine2Label; // Giả sử FXML có fx:id này
    @FXML private Button actionButton;     // Đảm bảo FXML có fx:id này

    private String nextScreenFxmlPath = "common/LoginScreen.fxml"; // Mặc định
    private String buttonText = "Tiếp tục"; // Mặc định

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("DEBUG_SSC: SuccessfulScreenController initialized.");

        // Lấy thông điệp và cấu hình nút từ SessionManager
        String msg1 = SessionManager.getInstance().getSuccessMessageLine1();
        String msg2 = SessionManager.getInstance().getSuccessMessageLine2();
        String btnText = SessionManager.getInstance().getSuccessButtonText();
        String nextScreenPath = SessionManager.getInstance().getSuccessButtonNextScreen();

        if (messageLine1Label != null && msg1 != null) {
            messageLine1Label.setText(msg1);
        } else if (messageLine1Label != null) {
            messageLine1Label.setText("Thao tác thành công!"); // Mặc định nếu không có gì trong session
        }

        if (messageLine2Label != null && msg2 != null) {
            messageLine2Label.setText(msg2);
            messageLine2Label.setVisible(true);
            messageLine2Label.setManaged(true);
        } else if (messageLine2Label != null) {
            messageLine2Label.setVisible(false);
            messageLine2Label.setManaged(false);
        }

        if (btnText != null && !btnText.isEmpty()) {
            this.buttonText = btnText;
        }
        if (actionButton != null) {
            actionButton.setText(this.buttonText);
        }

        if (nextScreenPath != null && !nextScreenPath.isEmpty()) {
            this.nextScreenFxmlPath = nextScreenPath;
        }

        // Xóa thông tin khỏi session sau khi đã sử dụng
        SessionManager.getInstance().clearSuccessScreenData();
    }

    // Các phương thức set này có thể không cần nữa nếu dùng SessionManager
    // public void setNextScreen(String targetScreen) {
    //     if (targetScreen != null && !targetScreen.isEmpty()) this.nextScreenFxmlPath = targetScreen;
    // }
    // public void setActionButtonText(String text) {
    //     this.buttonText = text;
    //     if (actionButton != null) actionButton.setText(this.buttonText);
    // }

    @FXML
    private void handleActionButtonAction(ActionEvent event) {
        System.out.println("DEBUG_SSC: Action button ('" + (actionButton != null ? actionButton.getText() : "N/A") + "') clicked. Navigating to: " + nextScreenFxmlPath);
        MainApp.loadScene(nextScreenFxmlPath);
    }
}
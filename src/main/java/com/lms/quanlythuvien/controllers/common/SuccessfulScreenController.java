package com.lms.quanlythuvien.controllers.common;

import com.lms.quanlythuvien.MainApp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML; // Đảm bảo import này có
import javafx.fxml.Initializable;
import javafx.scene.control.Button; // Đảm bảo import này có
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class SuccessfulScreenController implements Initializable {

    // Bỏ comment các @FXML cho Label nếu bạn muốn truy cập chúng
    // @FXML
    // private Label messageLine1Label;
    // @FXML
    // private Label messageLine2Label;

    @FXML
    private Button actionButton; // <<<--- BỎ COMMENT HOẶC THÊM LẠI DÒNG NÀY

    private String nextScreen = "common/LoginScreen.fxml";
    private String actionButtonText = "Đăng nhập"; // Text mặc định, sẽ được FXML ghi đè nếu FXML có text

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("SuccessfulScreenController initialized.");
        // Nếu bạn muốn đặt text cho nút từ code Java (ví dụ, dựa trên logic nào đó)
        // thì mới cần dòng dưới. Nếu FXML đã đặt text rồi thì không cần.
        // if (actionButton != null && actionButtonText != null) {
        //     actionButton.setText(actionButtonText);
        // }
    }

    public void setNextScreen(String targetScreen) {
        if (targetScreen != null && !targetScreen.isEmpty()) {
            this.nextScreen = targetScreen;
        }
    }

    // Nếu bạn muốn có thể tùy chỉnh text của nút từ controller gọi đến màn hình này
    public void setActionButtonText(String text) {
        this.actionButtonText = text;
        if (actionButton != null) { // Nếu view đã được load
            actionButton.setText(this.actionButtonText);
        }
    }


    @FXML
    private void handleActionButtonAction(ActionEvent event) {
        String currentButtonText = "nút hành động"; // Giá trị mặc định nếu actionButton là null
        if (actionButton != null) {
            currentButtonText = actionButton.getText();
        }
        System.out.println("Action button ('" + currentButtonText + "') clicked from Successful screen. Proceeding to: " + nextScreen);
        MainApp.loadScene(nextScreen);
    }
}
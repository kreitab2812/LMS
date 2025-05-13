package com.lms.quanlythuvien.controllers; // Nhớ thay yourdomain

import com.lms.quanlythuvien.MainApp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable; // Có thể không cần nếu initialize() trống

import java.net.URL;
import java.util.ResourceBundle;

public class RegistrationSuccessfulScreenController implements Initializable {

    // Không cần @FXML cho Button nếu chỉ dùng onAction trong FXML
    // @FXML
    // private Button loginButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Không có gì cần khởi tạo đặc biệt ở đây
        System.out.println("RegistrationSuccessfulScreenController initialized.");
    }

    @FXML
    private void handleLoginButtonAction(ActionEvent event) { // Có thể bỏ ActionEvent nếu không dùng
        System.out.println("Proceed to Login button clicked from Registration Successful screen.");
        MainApp.loadScene("LoginScreen.fxml"); // Chuyển về màn hình Đăng nhập
    }

    // Hoặc nếu không dùng ActionEvent:
    /*
    @FXML
    private void handleLoginButtonAction() {
        System.out.println("Proceed to Login button clicked from Registration Successful screen.");
        MainApp.loadScene("LoginScreen.fxml"); // Chuyển về màn hình Đăng nhập
    }
    */
}
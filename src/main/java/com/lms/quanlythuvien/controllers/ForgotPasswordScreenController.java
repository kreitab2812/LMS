package com.lms.quanlythuvien.controllers; // Nhớ thay yourdomain

import com.lms.quanlythuvien.MainApp; // Để gọi phương thức loadScene
import javafx.event.ActionEvent; // Import nếu bạn dùng (ActionEvent event)
import javafx.fxml.FXML;
import javafx.fxml.Initializable; // Có thể không cần nếu initialize() trống
import javafx.scene.control.Button; // Import nếu bạn có fx:id cho Button

import java.net.URL;
import java.util.ResourceBundle;

public class ForgotPasswordScreenController implements Initializable { // Hoặc không cần implements Initializable nếu không dùng

    // Không bắt buộc phải khai báo @FXML cho Button nếu bạn chỉ dùng onAction trong FXML
    // và không cần truy cập nút này trong mã Java của controller.
    // @FXML
    // private Button backButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Hiện tại không có gì cần khởi tạo đặc biệt cho màn hình này
        // Nếu có, bạn có thể thêm vào đây
        System.out.println("ForgotPasswordScreenController initialized.");
    }

    @FXML
    private void handleBackButtonAction(ActionEvent event) { // Có thể bỏ ActionEvent event nếu không dùng
        System.out.println("Back button on Forgot Password screen clicked.");
        MainApp.loadScene("LoginScreen.fxml"); // Quay lại màn hình Đăng nhập
    }

    // Nếu bạn không muốn tham số ActionEvent:
    /*
    @FXML
    private void handleBackButtonAction() {
        System.out.println("Back button on Forgot Password screen clicked.");
        MainApp.loadScene("LoginScreen.fxml"); // Quay lại màn hình Đăng nhập
    }
    */
}
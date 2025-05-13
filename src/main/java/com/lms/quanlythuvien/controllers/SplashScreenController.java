package com.lms.quanlythuvien.controllers; // Nhớ thay yourdomain

import com.lms.quanlythuvien.MainApp; // Để gọi phương thức loadScene
import javafx.animation.PauseTransition;
import javafx.fxml.Initializable;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class SplashScreenController implements Initializable {

    // Thời gian hiển thị Splash Screen (tính bằng giây)
    private static final double SPLASH_DURATION_SECONDS = 1.0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Tải font Comfortaa (nên thực hiện một lần trong MainApp nếu dùng nhiều nơi)
        // Nếu bạn đã đặt font Comfortaa-Regular.ttf vào src/main/resources/com/yourdomain/quanlythuvien/fonts/
        try {
            Font.loadFont(getClass().getResourceAsStream("/com/yourdomain/quanlythuvien/fonts/Comfortaa-Regular.ttf"), 10);
            // Số 10 chỉ là kích thước placeholder, font sẽ được áp dụng theo FXML
            System.out.println("Comfortaa font loaded successfully.");
        } catch (Exception e) {
            System.err.println("Could not load Comfortaa font: " + e.getMessage());
            // Font sẽ fallback về font hệ thống nếu không tải được
        }

        // Tạo một đối tượng PauseTransition
        PauseTransition pause = new PauseTransition(Duration.seconds(SPLASH_DURATION_SECONDS));

        // Thiết lập hành động sẽ xảy ra sau khi thời gian chờ kết thúc
        pause.setOnFinished(event -> {
            System.out.println("Splash screen finished. Loading Login Screen...");
            // Gọi phương thức trong MainApp để chuyển sang màn hình Login
            // Đảm bảo LoginScreen.fxml tồn tại trong thư mục fxml
            MainApp.loadScene("LoginScreen.fxml");
        });

        // Bắt đầu thực hiện PauseTransition
        pause.play();
    }
}
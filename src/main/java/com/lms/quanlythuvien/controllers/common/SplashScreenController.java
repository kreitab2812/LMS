package com.lms.quanlythuvien.controllers.common;

import com.lms.quanlythuvien.MainApp;
import javafx.animation.PauseTransition;
import javafx.fxml.Initializable;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class SplashScreenController implements Initializable {

    private static final double SPLASH_DURATION_SECONDS = 1.5; // Tăng nhẹ thời gian nếu muốn

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Phần tải font đã được khuyến nghị chuyển sang MainApp.start()
        // Nếu cậu vẫn muốn giữ ở đây cho mục đích cụ thể, đảm bảo đường dẫn đúng.

        PauseTransition pause = new PauseTransition(Duration.seconds(SPLASH_DURATION_SECONDS));
        pause.setOnFinished(event -> {
            System.out.println("DEBUG_SPLASH: Splash screen finished. Loading Login Screen...");
            // Đường dẫn này đã được xác nhận là đúng từ các controller khác
            MainApp.loadScene("common/LoginScreen.fxml");
        });
        pause.play();
    }
}
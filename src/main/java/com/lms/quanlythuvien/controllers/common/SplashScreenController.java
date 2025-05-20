package com.lms.quanlythuvien.controllers.common; // Gói của bạn

import com.lms.quanlythuvien.MainApp; // Để gọi phương thức loadScene
import javafx.animation.PauseTransition;
import javafx.fxml.Initializable;
// import javafx.scene.text.Font; // Không cần import Font ở đây nữa nếu chuyển sang MainApp
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class SplashScreenController implements Initializable {

    // Thời gian hiển thị Splash Screen (tính bằng giây)
    private static final double SPLASH_DURATION_SECONDS = 1.0; // Bạn có thể điều chỉnh nếu muốn

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // --- PHẦN TẢI FONT NÊN ĐƯỢC CHUYỂN SANG MAINAPP.START() ---
        // try {
        //     // Font.loadFont(getClass().getResourceAsStream("/com/lms/quanlythuvien/fonts/Comfortaa-Regular.ttf"), 10);
        //     // System.out.println("Comfortaa font loaded successfully (from SplashScreenController)."); // Chỉ để debug
        // } catch (Exception e) {
        //     // System.err.println("Could not load Comfortaa font (from SplashScreenController): " + e.getMessage());
        // }
        // --- KẾT THÚC PHẦN TẢI FONT ---

        PauseTransition pause = new PauseTransition(Duration.seconds(SPLASH_DURATION_SECONDS));

        pause.setOnFinished(event -> {
            System.out.println("DEBUG_SPLASH: Splash screen finished. Loading Login Screen...");
            // Gọi phương thức trong MainApp để chuyển sang màn hình Login
            // Đảm bảo LoginScreen.fxml tồn tại trong thư mục fxml/common/
            MainApp.loadScene("common/LoginScreen.fxml"); // <<<--- SỬA ĐƯỜNG DẪN Ở ĐÂY
        });

        pause.play();
    }
}
package com.lms.quanlythuvien;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class MainApp extends Application {

    private static Stage primaryStage; // Để có thể truy cập từ các controller nếu cần thay đổi scene

    @Override
    public void start(Stage stage) {
        primaryStage = stage; // Gán stage chính
        primaryStage.setTitle("UET-VNU Library Management");

        // Tải màn hình Splash Screen đầu tiên
        loadSplashScreen();
        primaryStage.show();
    }

    public void loadSplashScreen() {
        try {
            // Tạo đường dẫn tới tệp FXML cho Splash Screen
            // Đảm bảo đường dẫn này chính xác với vị trí tệp SplashScreen.fxml trong thư mục resources
            URL fxmlLocation = getClass().getResource("/com/yourdomain/quanlythuvien/fxml/SplashScreen.fxml");
            if (fxmlLocation == null) {
                System.err.println("Cannot find FXML file. Check the path.");
                // Có thể hiển thị một thông báo lỗi cho người dùng hoặc thoát ứng dụng
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load(); // Tải FXML

            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            // Kích thước ban đầu cho cửa sổ, bạn có thể điều chỉnh
            // primaryStage.setWidth(800);
            // primaryStage.setHeight(600);
            // primaryStage.setResizable(false); // Có thể không cho phép thay đổi kích thước splash screen
        } catch (IOException e) {
            System.err.println("Error loading Splash Screen FXML: " + e.getMessage());
            e.printStackTrace();
            // Xử lý lỗi tải FXML (ví dụ: hiển thị thông báo lỗi)
        }
    }

    // Phương thức để thay đổi Scene (màn hình)
    public static void loadScene(String fxmlFile) {
        try {
            System.out.println("Attempting to load FXML: " + fxmlFile);
            URL fxmlLocation = MainApp.class.getResource("/com/yourdomain/quanlythuvien/fxml/" + fxmlFile);
            if (fxmlLocation == null) {
                System.err.println("Cannot find FXML file: " + fxmlFile + ". Check the path.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
        } catch (IOException e) {
            System.err.println("Error loading scene " + fxmlFile + ": " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}
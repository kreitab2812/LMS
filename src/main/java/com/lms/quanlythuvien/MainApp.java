package com.lms.quanlythuvien; // Hoặc package của bạn

import com.lms.quanlythuvien.utils.DatabaseManager; // <<<--- THÊM IMPORT NÀY
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
// import javafx.stage.Screen; // Import Screen không thấy được sử dụng, có thể bỏ nếu không cần
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        System.out.println("DEBUG_MAINAPP: MainApp start() called.");
        primaryStage = stage;
        primaryStage.setTitle("UET-VNU Library Management");

        // KHỞI TẠO DATABASE VÀ CÁC BẢNG (NẾU CHƯA CÓ)
        try {
            System.out.println("DEBUG_MAINAPP: Initializing database...");
            DatabaseManager.initializeDatabase();
            System.out.println("DEBUG_MAINAPP: Database initialization should be complete.");
        } catch (Exception e) {
            System.err.println("CRITICAL_MAINAPP: Failed to initialize database!");
            e.printStackTrace();
            // Cân nhắc hiển thị một Alert lỗi ở đây và có thể thoát ứng dụng
            // nếu database là bắt buộc để ứng dụng hoạt động.
            // Ví dụ:
            // Alert alert = new Alert(Alert.AlertType.ERROR);
            // alert.setTitle("Database Error");
            // alert.setHeaderText("Could not initialize the application database.");
            // alert.setContentText("The application will now exit. Please contact support.\nError: " + e.getMessage());
            // alert.showAndWait();
            // Platform.exit(); // Thoát ứng dụng
            // return; // Không tiếp tục nếu database lỗi
        }


        // Tùy chọn: Đặt trạng thái phóng to ngay từ đầu
        primaryStage.setMaximized(true); // Phóng to cửa sổ

        System.out.println("DEBUG_MAINAPP: Loading splash screen...");
        loadSplashScreen(); // Tải màn hình Splash Screen
        // primaryStage.show(); // Không cần gọi show() ở đây nữa nếu loadSplashScreen đã show
        System.out.println("DEBUG_MAINAPP: MainApp start() finished.");
    }

    public void loadSplashScreen() {
        try {
            String splashPath = "/com/lms/quanlythuvien/fxml/SplashScreen.fxml";
            URL fxmlLocation = getClass().getResource(splashPath);

            if (fxmlLocation == null) {
                System.err.println("ERROR_MAINAPP: Cannot find Splash Screen FXML file. Check the path: " + splashPath);
                // Có thể load thẳng LoginScreen nếu Splash lỗi
                // loadScene("LoginScreen.fxml");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            Scene scene = new Scene(root);
            primaryStage.setScene(scene);

            if (!primaryStage.isShowing()) {
                primaryStage.show();
            }
            primaryStage.setMaximized(true);
            System.out.println("DEBUG_MAINAPP: Splash screen loaded successfully.");

        } catch (IOException e) {
            System.err.println("ERROR_MAINAPP: Error loading Splash Screen FXML: " + e.getMessage());
            e.printStackTrace();
            // Xử lý lỗi, ví dụ load thẳng LoginScreen
            // System.out.println("DEBUG_MAINAPP: Attempting to load LoginScreen directly due to SplashScren error.");
            // loadScene("LoginScreen.fxml");
        }
    }

    public static void loadScene(String fxmlFile) {
        try {
            String fullPath = "/com/lms/quanlythuvien/fxml/" + fxmlFile;
            System.out.println("DEBUG_MAINAPP_LOADSCENE: Attempting to load FXML from: " + fullPath);

            URL fxmlLocation = MainApp.class.getResource(fullPath);
            if (fxmlLocation == null) {
                System.err.println("ERROR_MAINAPP_LOADSCENE: Cannot find FXML file: " + fullPath + ". Check the path.");
                // Hiển thị lỗi cho người dùng ở đây nếu cần, ví dụ qua một Alert tĩnh
                // hoặc ghi log và không làm gì cả (màn hình hiện tại sẽ giữ nguyên)
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();
            Scene scene = new Scene(root);

            primaryStage.setScene(scene);
            primaryStage.setMaximized(true); // Đảm bảo cửa sổ vẫn maximized

            if (!primaryStage.isShowing()) { // Hiển thị nếu chưa hiển thị (thường không cần thiết nếu đã show từ splash)
                primaryStage.show();
            }
            System.out.println("DEBUG_MAINAPP_LOADSCENE: Scene " + fxmlFile + " loaded successfully.");

        } catch (IOException e) {
            System.err.println("ERROR_MAINAPP_LOADSCENE: Error loading scene " + fxmlFile + ": " + e.getMessage());
            e.printStackTrace();
            // Có thể hiển thị một Alert lỗi ở đây
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
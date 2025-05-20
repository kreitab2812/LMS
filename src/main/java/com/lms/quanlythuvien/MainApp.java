package com.lms.quanlythuvien;

import com.lms.quanlythuvien.utils.database.DatabaseManager;
import javafx.application.Application;
import javafx.application.Platform; // Thêm để dùng Platform.exit() nếu cần
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;   // Thêm để hiển thị lỗi nghiêm trọng
import javafx.scene.control.ButtonType; // Thêm cho Alert
import javafx.scene.image.Image;     // Thêm để set icon cho ứng dụng
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream; // Thêm để load icon
import java.net.URL;

public class MainApp extends Application {

    private static Stage primaryStage;
    private static final String APP_TITLE = "Quản Lý Thư Viện - UETLIB"; // Tên ứng dụng

    @Override
    public void start(Stage stage) {
        System.out.println("INFO_MAINAPP: Application starting...");
        primaryStage = stage;
        primaryStage.setTitle(APP_TITLE);

        // Set application icon (thay "app_icon.png" bằng tên file icon của cậu)
        try (InputStream iconStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/images/app_icon.png")) {
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            } else {
                System.err.println("WARN_MAINAPP: Application icon not found.");
            }
        } catch (Exception e) {
            System.err.println("ERROR_MAINAPP: Failed to load application icon: " + e.getMessage());
        }

        // TẢI CUSTOM FONTS (nếu có)
        // Ví dụ: Font chữ Comfortaa
        try {
            // Đường dẫn nên bắt đầu bằng "/" nếu từ root của resources
            InputStream fontStream = getClass().getResourceAsStream("/com/lms/quanlythuvien/fonts/Comfortaa-Regular.ttf");
            if (fontStream != null) {
                Font.loadFont(fontStream, 10); // Kích thước không quan trọng khi load, chỉ cần load được font
                System.out.println("INFO_MAINAPP: Custom font 'Comfortaa-Regular.ttf' loaded successfully.");
                fontStream.close(); // Nhớ đóng stream
            } else {
                System.err.println("WARN_MAINAPP: Custom font 'Comfortaa-Regular.ttf' not found.");
            }
        } catch (Exception e) {
            System.err.println("ERROR_MAINAPP: Could not load custom font: " + e.getMessage());
        }


        // KHỞI TẠO DATABASE
        try {
            System.out.println("INFO_MAINAPP: Initializing database...");
            DatabaseManager.getInstance(); // Gọi getInstance() để trigger constructor và initializeDatabase()
            System.out.println("INFO_MAINAPP: Database initialization process completed.");
        } catch (RuntimeException e) { // Bắt RuntimeException mà DatabaseManager có thể ném ra
            System.err.println("CRITICAL_MAINAPP: Database initialization failed. Application will exit.");
            e.printStackTrace();
            showCriticalErrorAndExit("Lỗi Khởi Tạo Cơ Sở Dữ Liệu",
                    "Không thể kết nối hoặc khởi tạo cơ sở dữ liệu.\n" +
                            "Ứng dụng không thể tiếp tục. Vui lòng kiểm tra lại file 'library.db'.\n" +
                            "Chi tiết lỗi: " + e.getMessage());
            return; // Không load splash screen nếu DB lỗi
        }

        System.out.println("INFO_MAINAPP: Loading splash screen...");
        loadSplashScreen(); // Load và hiển thị màn hình splash
        System.out.println("INFO_MAINAPP: Application start() method finished.");
    }

    private static void centerStageOnScreen(Stage stage, Parent rootNode) {
        if (stage == null) return;

        // Lấy kích thước màn hình chính
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        // Lấy kích thước của scene từ rootNode nếu có, hoặc từ stage nếu đã có scene
        double sceneWidth = (rootNode != null && rootNode.prefWidth(-1) > 0) ? rootNode.prefWidth(-1) : stage.getWidth();
        double sceneHeight = (rootNode != null && rootNode.prefHeight(-1) > 0) ? rootNode.prefHeight(-1) : stage.getHeight();

        // Nếu stage chưa có kích thước (thường xảy ra trước khi show), thử sizeToScene
        if (Double.isNaN(sceneWidth) || Double.isNaN(sceneHeight) || sceneWidth <= 0 || sceneHeight <= 0) {
            if (stage.getScene() != null) {
                stage.sizeToScene(); // Cập nhật kích thước stage theo scene hiện tại
                sceneWidth = stage.getWidth();
                sceneHeight = stage.getHeight();
            }
        }

        // Nếu vẫn không có kích thước hợp lệ, không căn giữa
        if (Double.isNaN(sceneWidth) || Double.isNaN(sceneHeight) || sceneWidth <= 0 || sceneHeight <= 0) {
            System.err.println("WARN_MAINAPP_CENTER: Stage/Scene width/height not properly initialized for centering. Centering skipped.");
            return;
        }

        stage.setX((screenBounds.getWidth() - sceneWidth) / 2 + screenBounds.getMinX());
        stage.setY((screenBounds.getHeight() - sceneHeight) / 2 + screenBounds.getMinY());
        System.out.println("DEBUG_MAINAPP_CENTER: Stage centered. X=" + stage.getX() + ", Y=" + stage.getY() + ", W=" + sceneWidth + ", H=" + sceneHeight);
    }


    public void loadSplashScreen() {
        try {
            String splashPath = "/com/lms/quanlythuvien/fxml/common/SplashScreen.fxml";
            URL fxmlLocation = MainApp.class.getResource(splashPath); // Dùng MainApp.class để an toàn hơn

            if (fxmlLocation == null) {
                System.err.println("CRITICAL_MAINAPP_SPLASH: SplashScreen FXML not found: " + splashPath);
                loadScene("common/LoginScreen.fxml"); // Fallback nếu splash lỗi
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            // Áp dụng CSS cho scene (nếu SplashScreen có style riêng hoặc để đảm bảo)
            // scene.getStylesheets().add(getClass().getResource("/com/lms/quanlythuvien/css/styles.css").toExternalForm());

            primaryStage.setScene(scene);

            if (!primaryStage.isShowing()) {
                primaryStage.show(); // Hiển thị stage LẦN ĐẦU TIÊN
            }
            // Quan trọng: sizeToScene() sau khi setScene và TRƯỚC khi show() hoặc ngay sau show() lần đầu.
            // Đối với splash screen, kích thước thường cố định trong FXML.
            primaryStage.sizeToScene();
            centerStageOnScreen(primaryStage, root); // Căn giữa dựa trên kích thước của root node

            System.out.println("INFO_MAINAPP: Splash screen loaded successfully.");

        } catch (IOException e) {
            System.err.println("CRITICAL_MAINAPP_SPLASH_IO: Error loading SplashScreen FXML: " + e.getMessage());
            e.printStackTrace();
            loadScene("common/LoginScreen.fxml"); // Fallback
        }
    }

    public static void loadScene(String fxmlFile) {
        try {
            String fullPath = "/com/lms/quanlythuvien/fxml/" + fxmlFile;
            System.out.println("INFO_MAINAPP_LOADSCENE: Loading FXML: " + fullPath);

            URL fxmlLocation = MainApp.class.getResource(fullPath);
            if (fxmlLocation == null) {
                System.err.println("CRITICAL_MAINAPP_LOADSCENE: FXML file not found: " + fullPath);
                showCriticalErrorAndExit("Lỗi Tải Giao Diện", "Không tìm thấy tệp giao diện chính: " + fxmlFile);
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            Scene currentScene = primaryStage.getScene();
            if (currentScene == null) { // Nếu chưa có scene nào (ví dụ khi splash lỗi và gọi trực tiếp)
                currentScene = new Scene(root);
                primaryStage.setScene(currentScene);
            } else {
                currentScene.setRoot(root); // Thay đổi root của scene hiện tại để giữ nguyên Stage
            }

            // Áp dụng lại stylesheet cho scene nếu cần (thường thì stylesheet đã được load từ FXML)
            // Hoặc đảm bảo FXML đã link đúng stylesheet
            // String cssPath = MainApp.class.getResource("/com/lms/quanlythuvien/css/styles.css").toExternalForm();
            // if (!currentScene.getStylesheets().contains(cssPath)) {
            //     currentScene.getStylesheets().add(cssPath);
            // }


            // Căn giữa sau khi đã có root mới và scene đã được cập nhật
            // Stage cần được hiển thị để getWidth/getHeight chính xác nếu Scene thay đổi kích thước
            primaryStage.sizeToScene(); // Cập nhật kích thước Stage theo Scene/Root mới
            centerStageOnScreen(primaryStage, root);

            if (!primaryStage.isShowing()) {
                primaryStage.show();
            }
            System.out.println("INFO_MAINAPP_LOADSCENE: Scene " + fxmlFile + " loaded successfully.");

        } catch (IOException e) {
            System.err.println("CRITICAL_MAINAPP_LOADSCENE_IO: Error loading scene " + fxmlFile + ": " + e.getMessage());
            e.printStackTrace();
            showCriticalErrorAndExit("Lỗi Tải Giao Diện", "Không thể tải giao diện: " + fxmlFile + "\nChi tiết: " + e.getMessage());
        }
    }

    private static void showCriticalErrorAndExit(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        // Optional: Apply styles to the alert
        // try {
        //     URL cssUrl = MainApp.class.getResource("/com/lms/quanlythuvien/css/styles.css");
        //     if (cssUrl != null && alert.getDialogPane() != null) {
        //         alert.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
        //     }
        // } catch (Exception e) { /* ignore */ }
        alert.showAndWait();
        Platform.exit();
        System.exit(1); // Đảm bảo ứng dụng thoát hoàn toàn
    }


    public static void main(String[] args) {
        launch(args);
    }
}
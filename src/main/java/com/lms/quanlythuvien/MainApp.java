package com.lms.quanlythuvien; // Hoặc package của bạn

import com.lms.quanlythuvien.utils.database.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D; // <<<--- THÊM IMPORT NÀY
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Screen; // <<<--- THÊM IMPORT NÀY
import javafx.stage.Stage;
// import javafx.scene.control.Alert;
// import javafx.application.Platform;

import java.io.IOException;
import java.net.URL;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        System.out.println("DEBUG_MAINAPP: MainApp start() called.");
        primaryStage = stage;
        primaryStage.setTitle("UET-VNU Library Management");

        // TẢI CUSTOM FONTS
        try {
            Font.loadFont(getClass().getResourceAsStream("/com/lms/quanlythuvien/fonts/Comfortaa-Regular.ttf"), 10);
            System.out.println("DEBUG_MAINAPP: Comfortaa font loaded successfully.");
        } catch (Exception e) {
            System.err.println("ERROR_MAINAPP: Could not load Comfortaa font: " + e.getMessage());
        }

        // KHỞI TẠO DATABASE
        try {
            System.out.println("DEBUG_MAINAPP: Initializing database by getting instance...");
            DatabaseManager.getInstance(); // <<--- SỬA THÀNH DÒNG NÀY
            // Việc gọi getInstance() lần đầu sẽ tự động chạy initializeDatabase() bên trong DatabaseManager
            System.out.println("DEBUG_MAINAPP: Database initialization triggered via Singleton instantiation.");
        } catch (Exception e) { // Bắt Exception chung nếu getInstance() hoặc constructor có thể ném lỗi (hiếm khi nếu chỉ là SQLException đã được xử lý bên trong)
            // Hoặc cụ thể hơn là RuntimeException nếu DatabaseManager ném ra khi khởi tạo thất bại.
            System.err.println("CRITICAL_MAINAPP: Failed to initialize database via DatabaseManager.getInstance()!");
            e.printStackTrace();
            // Hiển thị Alert cho người dùng và thoát ứng dụng nếu DB là thiết yếu
            // Alert alert = new Alert(Alert.AlertType.ERROR, "Không thể khởi tạo cơ sở dữ liệu. Ứng dụng sẽ thoát.");
            // alert.showAndWait();
            // Platform.exit();
        }

        // KHÔNG CÒN primaryStage.setMaximized(true); NỮA

        System.out.println("DEBUG_MAINAPP: Loading splash screen...");
        loadSplashScreen(); // Phương thức này sẽ xử lý việc show và căn giữa scene đầu tiên
        System.out.println("DEBUG_MAINAPP: MainApp start() finished.");
    }

    // Phương thức mới để căn giữa Stage
    private static void centerStageOnScreen(Stage stage) {
        if (stage == null) return;

        // Lấy kích thước màn hình chính (có tính đến thanh taskbar, v.v.)
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

        // Đảm bảo stage có kích thước hợp lệ trước khi căn giữa
        // (getWidth/Height có thể trả về NaN nếu stage chưa được hiển thị hoặc chưa có scene)
        double stageWidth = stage.getWidth();
        double stageHeight = stage.getHeight();

        if (Double.isNaN(stageWidth) || Double.isNaN(stageHeight) || stageWidth <= 0 || stageHeight <= 0) {
            // Nếu kích thước chưa sẵn sàng, thử gọi sizeToScene() một lần nữa
            // Điều này thường xảy ra nếu stage chưa được show()
            stage.sizeToScene();
            stageWidth = stage.getWidth();
            stageHeight = stage.getHeight();
        }

        // Kiểm tra lại sau khi sizeToScene()
        if (Double.isNaN(stageWidth) || Double.isNaN(stageHeight) || stageWidth <= 0 || stageHeight <= 0) {
            System.err.println("DEBUG_MAINAPP_CENTER: Stage width/height not properly initialized for centering. Centering skipped.");
            return; // Không thể căn giữa nếu không có kích thước
        }

        // Tính toán vị trí
        double posX = primaryScreenBounds.getMinX() + (primaryScreenBounds.getWidth() - stageWidth) / 2;
        double posY = primaryScreenBounds.getMinY() + (primaryScreenBounds.getHeight() - stageHeight) / 2;

        stage.setX(posX);
        stage.setY(posY);
        System.out.println("DEBUG_MAINAPP_CENTER: Stage centered. X=" + stage.getX() + ", Y=" + stage.getY() + ", W=" + stageWidth + ", H=" + stageHeight);
    }


    public void loadSplashScreen() {
        try {
            String splashPath = "/com/lms/quanlythuvien/fxml/common/SplashScreen.fxml";
            URL fxmlLocation = getClass().getResource(splashPath);

            if (fxmlLocation == null) {
                System.err.println("ERROR_MAINAPP: Cannot find Splash Screen FXML file: " + splashPath);
                loadScene("common/LoginScreen.fxml"); // Load Login nếu Splash lỗi (loadScene cũng sẽ căn giữa)
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();
            Scene scene = new Scene(root);

            primaryStage.setScene(scene);
            // KHÔNG CÒN primaryStage.setMaximized(true);

            if (!primaryStage.isShowing()) {
                primaryStage.show(); // Hiển thị Stage lần đầu
            }
            primaryStage.sizeToScene(); // Đảm bảo Stage có kích thước của Scene
            centerStageOnScreen(primaryStage); // Căn giữa Stage

            System.out.println("DEBUG_MAINAPP: Splash screen loaded successfully.");

        } catch (IOException e) {
            System.err.println("ERROR_MAINAPP: Error loading Splash Screen FXML: " + e.getMessage());
            e.printStackTrace();
            System.out.println("DEBUG_MAINAPP: Attempting to load LoginScreen directly due to SplashScreen error.");
            loadScene("common/LoginScreen.fxml"); // loadScene cũng sẽ căn giữa
        }
    }

    public static void loadScene(String fxmlFile) { // fxmlFile là đường dẫn tương đối từ thư mục /fxml/
        try {
            String fullPath = "/com/lms/quanlythuvien/fxml/" + fxmlFile;
            System.out.println("DEBUG_MAINAPP_LOADSCENE: Attempting to load FXML from: " + fullPath);

            URL fxmlLocation = MainApp.class.getResource(fullPath);
            if (fxmlLocation == null) {
                System.err.println("ERROR_MAINAPP_LOADSCENE: Cannot find FXML file: " + fullPath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();
            Scene scene = new Scene(root);

            primaryStage.setScene(scene);
            // KHÔNG CÒN primaryStage.setMaximized(true);

            primaryStage.sizeToScene(); // Cập nhật kích thước Stage theo Scene mới
            centerStageOnScreen(primaryStage); // Căn giữa Stage

            if (!primaryStage.isShowing()) { // Nếu Stage chưa hiển thị (ví dụ, loadScene được gọi từ start() khi splash lỗi)
                primaryStage.show();
                // centerStageOnScreen(primaryStage); // Có thể gọi lại nếu show lần đầu ở đây, nhưng thường là đã đủ
            }
            System.out.println("DEBUG_MAINAPP_LOADSCENE: Scene " + fxmlFile + " loaded successfully.");

        } catch (IOException e) {
            System.err.println("ERROR_MAINAPP_LOADSCENE: Error loading scene " + fxmlFile + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
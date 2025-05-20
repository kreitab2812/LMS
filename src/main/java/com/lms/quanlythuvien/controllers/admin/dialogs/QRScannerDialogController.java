package com.lms.quanlythuvien.controllers.admin.dialogs;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource; // <<< THÊM IMPORT NÀY
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javafx.application.Platform;
// import javafx.beans.property.ObjectProperty; // Không được sử dụng, có thể bỏ
// import javafx.beans.property.SimpleObjectProperty; // Không được sử dụng, có thể bỏ
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image; // Image của JavaFX đã được import ngầm khi dùng ImageView
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.awt.Dimension;
import java.util.List;
import java.awt.image.BufferedImage;
import java.util.Optional; // <<< THÊM IMPORT NÀY
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class QRScannerDialogController {

    @FXML private ImageView cameraView;
    @FXML private Label statusLabel;
    @FXML private Button cancelButton;

    private Webcam webcam;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Task<Void> scanningTask;
    private AtomicReference<String> scannedQrCode = new AtomicReference<>(null);

    public void initialize() {
        Platform.runLater(() -> statusLabel.setText("Đang tìm camera..."));
        initWebcam();
    }

    private void initWebcam() {
        try {
            Dimension size = WebcamResolution.VGA.getSize();
            webcam = Webcam.getDefault(); // Thử lấy webcam mặc định
            if (webcam == null) { // Kiểm tra nếu không có webcam mặc định (hoặc không có webcam nào)
                List<Webcam> webcams = Webcam.getWebcams();
                if (!webcams.isEmpty()) {
                    webcam = webcams.get(0); // Lấy webcam đầu tiên trong danh sách nếu có
                    System.out.println("QRScanner: No default webcam, using first available: " + webcam.getName());
                }
            }

            if (webcam != null) {
                webcam.setViewSize(size); // Chỉ đặt view size nếu webcam đã được chọn
                // webcam.setCustomViewSizes(new Dimension[]{size}); // Cách khác để set size
                webcam.open(true); // Mở non-blocking, rồi bắt đầu task
                Platform.runLater(() -> statusLabel.setText("Camera: " + webcam.getName() + " - Sẵn sàng. Đang quét..."));
                startScanning();
            } else {
                System.err.println("QRScanner: Không tìm thấy camera nào.");
                Platform.runLater(() -> statusLabel.setText("Lỗi: Không tìm thấy camera."));
            }
        } catch (Exception e) { // Bắt lỗi chung khi khởi tạo webcam
            System.err.println("QRScanner: Không thể khởi tạo hoặc mở camera: " + e.getMessage());
            // e.printStackTrace();
            Platform.runLater(() -> statusLabel.setText("Lỗi: Không thể khởi tạo camera."));
            if (webcam != null && webcam.isOpen()) { // Đảm bảo đóng nếu đã mở được một phần
                webcam.close();
            }
            webcam = null;
        }
    }

    private void startScanning() {
        if (webcam == null || !webcam.isOpen()) {
            Platform.runLater(() -> statusLabel.setText("Camera không hoạt động. Không thể quét."));
            return;
        }

        scanningTask = new Task<>() {
            @Override
            protected Void call() throws Exception { // Cho phép ném Exception để onFailed xử lý
                while (webcam.isOpen() && !isCancelled()) { // Kiểm tra isCancelled()
                    BufferedImage image = webcam.getImage();
                    if (image == null) {
                        Thread.sleep(100); // Đợi nếu chưa có ảnh
                        continue;
                    }

                    // Hiển thị frame lên ImageView
                    Platform.runLater(() -> {
                        // Chỉ cập nhật ImageView nếu nó vẫn còn hiển thị và có scene
                        if (cameraView != null && cameraView.getScene() != null && cameraView.getScene().getWindow() != null && cameraView.getScene().getWindow().isShowing()) {
                            cameraView.setImage(SwingFXUtils.toFXImage(image, null));
                        }
                    });

                    // Giải mã QR code
                    try {
                        LuminanceSource source = new BufferedImageLuminanceSource(image);
                        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                        Result result = new MultiFormatReader().decode(bitmap); // Có thể ném NotFoundException

                        if (result != null) {
                            String qrText = result.getText();
                            scannedQrCode.set(qrText);
                            Platform.runLater(() -> {
                                statusLabel.setText("Đã quét được: " + qrText);
                                closeDialog(); // Tự động đóng khi quét thành công
                            });
                            return null; // Kết thúc Task thành công
                        }
                    } catch (NotFoundException e) {
                        // Không tìm thấy QR code trong frame này, không làm gì cả, vòng lặp tiếp tục
                    }
                    // Có thể thêm một khoảng nghỉ nhỏ ở đây nếu cần thiết, nhưng webcam.getImage() có thể đã block đủ
                    Thread.sleep(50); // Giảm tần suất quét một chút
                }
                System.out.println("QRScanner: Scanning loop ended. Cancelled: " + isCancelled() + ", Webcam open: " + (webcam !=null && webcam.isOpen()));
                return null;
            }
        };

        scanningTask.setOnFailed(event -> {
            Throwable exc = scanningTask.getException();
            System.err.println("QRScanner: Scanning task failed.");
            if (exc != null) {
                exc.printStackTrace();
            }
            Platform.runLater(() -> statusLabel.setText("Lỗi quét. Vui lòng thử lại."));
            stopCameraAndTask(); // Dừng camera nếu task thất bại
        });

        // Không cần setOnSucceeded vì đã xử lý trong call() và Platform.runLater

        executor.submit(scanningTask);
    }

    /**
     * Trả về Optional chứa mã QR đã quét được.
     * Trả về Optional.empty() nếu không quét được hoặc bị hủy.
     */
    public Optional<String> getScannedQrCode() {
        return Optional.ofNullable(scannedQrCode.get());
    }

    /**
     * Dừng camera và task quét. Được gọi khi dialog đóng hoặc hủy.
     */
    public void stopCameraAndTask() {
        System.out.println("QRScanner: Executing stopCameraAndTask().");
        if (scanningTask != null && !scanningTask.isDone()) {
            System.out.println("QRScanner: Cancelling scanning task.");
            scanningTask.cancel(true); // Gửi yêu cầu hủy task
        }
        if (webcam != null && webcam.isOpen()) {
            System.out.println("QRScanner: Closing webcam " + webcam.getName());
            webcam.close();
        }
        // Không nên shutdown executor ở đây nếu nó được dùng chung hoặc dialog có thể mở lại.
        // Nếu executor chỉ dành cho dialog này, thì shutdown là đúng.
        // Hiện tại, executor được tạo mỗi khi controller tạo ra, nên shutdown là hợp lý.
        if (executor != null && !executor.isShutdown()) {
            System.out.println("QRScanner: Shutting down executor.");
            executor.shutdownNow();
        }
    }

    private void closeDialog() {
        // Đã gọi stopCameraAndTask() từ nơi gọi closeDialog (ví dụ: trong Task hoặc handleCancelAction)
        // Nếu chưa, cần gọi ở đây:
        // stopCameraAndTask();

        if (cancelButton != null && cancelButton.getScene() != null && cancelButton.getScene().getWindow() != null) {
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            if (stage.isShowing()) { // Chỉ đóng nếu đang hiển thị
                System.out.println("QRScanner: Closing dialog stage.");
                stage.close();
            }
        } else {
            System.out.println("QRScanner: Could not get stage from cancelButton to close dialog, or it's already closed.");
        }
    }

    @FXML
    void handleCancelAction(ActionEvent event) {
        System.out.println("QRScanner: Cancel button clicked.");
        scannedQrCode.set(null); // Đảm bảo kết quả là null/empty khi hủy
        stopCameraAndTask(); // Dừng mọi thứ trước
        closeDialog();       // Rồi đóng dialog
    }
}
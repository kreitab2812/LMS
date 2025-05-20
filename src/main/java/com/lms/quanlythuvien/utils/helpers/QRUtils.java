package com.lms.quanlythuvien.utils.helpers;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException; // Import cụ thể hơn
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;
import java.util.HashMap; // Sử dụng HashMap thay vì Map nếu chỉ cần triển khai cụ thể này
import java.util.Map;

public class QRUtils {

    /**
     * Tạo một đối tượng javafx.scene.image.Image chứa mã QR từ một chuỗi text.
     * @param text Chuỗi dữ liệu cần mã hóa (ví dụ: ISBN, ID sách).
     * @param width Chiều rộng mong muốn của ảnh QR.
     * @param height Chiều cao mong muốn của ảnh QR.
     * @return Đối tượng Image chứa mã QR, hoặc null nếu có lỗi.
     */
    public static Image generateQRCodeImage(String text, int width, int height) {
        if (text == null || text.trim().isEmpty()) {
            System.err.println("QRUtils: Text to encode is null or empty. Cannot generate QR code.");
            return null;
        }
        if (width <= 0 || height <= 0) {
            System.err.println("QRUtils: QR code dimensions (width, height) must be positive.");
            return null;
        }

        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L); // Mức sửa lỗi thấp nhất, cho phép QR nhỏ hơn/đơn giản hơn
            hints.put(EncodeHintType.MARGIN, 1); // Lề tối thiểu (1 block)

            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    text,
                    BarcodeFormat.QR_CODE,
                    width,
                    height,
                    hints
            );

            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            return SwingFXUtils.toFXImage(bufferedImage, null); // Chuyển từ AWT Image sang JavaFX Image

        } catch (WriterException e) { // Bắt WriterException cụ thể từ zxing
            System.err.println("QRUtils: Failed to generate QR Code for text '" + text + "'. Error: " + e.getMessage());
            // e.printStackTrace(); // Có thể bật để debug
            return null;
        } catch (Exception e) { // Bắt các lỗi không mong muốn khác
            System.err.println("QRUtils: An unexpected error occurred while generating QR Code for text '" + text + "'. Error: " + e.getMessage());
            // e.printStackTrace();
            return null;
        }
    }
}
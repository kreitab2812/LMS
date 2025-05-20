package com.lms.quanlythuvien.services.auth;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailService {

    // Cấu hình thông tin SMTP - NÊN ĐỌC TỪ FILE CONFIG HOẶC BIẾN MÔI TRƯỜNG
    // TUYỆT ĐỐI KHÔNG HARDCODE MẬT KHẨU VÀO CODE NẾU ĐƯA LÊN GIT!!!
    private final String SMTP_HOST = "smtp.gmail.com"; // Ví dụ: smtp.gmail.com
    private final String SMTP_PORT = "587"; // Ví dụ: 587 (TLS) hoặc 465 (SSL)
    private final String SMTP_AUTH_USER = "aovproject2020@gmail.com"; // Email của bạn
    private final String SMTP_AUTH_PWD = "arvr lyzp jrte eohf"; // Mật khẩu ứng dụng hoặc mật khẩu email

    public boolean sendVerificationCode(String toEmail, String subject, String body) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true"); // Cho TLS
        // props.put("mail.smtp.ssl.enable", "true"); // Bỏ comment dòng này và comment dòng trên nếu dùng SSL port 465
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        // props.put("mail.smtp.ssl.trust", SMTP_HOST); // Có thể cần cho một số cấu hình SSL

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_AUTH_USER, SMTP_AUTH_PWD);
            }
        });

        // Bên trong phương thức sendVerificationCode của EmailService.java

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_AUTH_USER)); // Email người gửi
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail)); // Email người nhận

            // SỬ DỤNG THAM SỐ TRUYỀN VÀO
            message.setSubject(subject); // <<<--- SỬA Ở ĐÂY: Dùng subject từ tham số
            message.setText(body);       // <<<--- SỬA Ở ĐÂY: Dùng body từ tham số (body này đã chứa mã code)

            Transport.send(message);
            System.out.println("Gửi email thành công tới " + toEmail); // Đã sửa lại log
            return true;
        } catch (MessagingException e) {
            System.err.println("Gửi email thất bại tới " + toEmail + ": " + e.getMessage()); // Đã sửa lại log
            e.printStackTrace();
            return false;
        }
    }
}
package j2ee_backend.nhom05.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("TechStore - Mã xác thực đặt lại mật khẩu");

            String htmlContent = """
            <div style="font-family: Arial, sans-serif; background-color:#f4f6f8; padding:30px;">
                <div style="max-width:600px; margin:auto; background:white; border-radius:8px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                    
                    <div style="background:#0d6efd; color:white; padding:20px; text-align:center;">
                        <h2 style="margin:0;">TechStore</h2>
                        <p style="margin:0; font-size:14px;">Hệ thống mua sắm công nghệ</p>
                    </div>

                    <div style="padding:30px;">
                        <h3 style="color:#333;">Yêu cầu đặt lại mật khẩu</h3>
                        
                        <p>Xin chào,</p>
                        
                        <p>Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản tại <b>TechStore</b>.</p>
                        
                        <p>Mã xác thực của bạn là:</p>

                        <div style="text-align:center; margin:25px 0;">
                            <span style="font-size:32px; font-weight:bold; letter-spacing:6px; color:#0d6efd;">
                                """ + resetToken + """
                            </span>
                        </div>

                        <p>Mã này sẽ hết hạn trong <b>15 phút</b>.</p>

                        <p>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.</p>

                        <p style="margin-top:30px;">
                            Trân trọng,<br>
                            <b>TechStore Team</b>
                        </p>
                    </div>

                    <div style="background:#f1f1f1; text-align:center; padding:15px; font-size:12px; color:#777;">
                        © 2026 TechStore. All rights reserved.
                    </div>

                </div>
            </div>
            """;

            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email: " + e.getMessage());
        }
    }
    
    public void sendTwoFactorCode(String toEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("TechStore - Mã xác thực đăng nhập");

            String htmlContent = """
            <div style="font-family: Arial, sans-serif; background-color:#f4f6f8; padding:30px;">
                <div style="max-width:600px; margin:auto; background:white; border-radius:8px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                    
                    <div style="background:#0d6efd; color:white; padding:20px; text-align:center;">
                        <h2 style="margin:0;">TechStore</h2>
                        <p style="margin:0; font-size:14px;">Hệ thống mua sắm công nghệ</p>
                    </div>

                    <div style="padding:30px;">
                        <h3 style="color:#333;">Mã xác thực đăng nhập 2 bước</h3>
                        
                        <p>Xin chào,</p>
                        
                        <p>Bạn đang đăng nhập vào tài khoản <b>TechStore</b> với xác thực 2 bước.</p>
                        
                        <p>Mã xác thực của bạn là:</p>

                        <div style="text-align:center; margin:25px 0;">
                            <span style="font-size:32px; font-weight:bold; letter-spacing:6px; color:#0d6efd;">
                                """ + code + """
                            </span>
                        </div>

                        <p>Mã này sẽ hết hạn trong <b>5 phút</b>.</p>

                        <p>Nếu bạn không thực hiện đăng nhập này, vui lòng đổi mật khẩu ngay lập tức.</p>

                        <p style="margin-top:30px;">
                            Trân trọng,<br>
                            <b>TechStore Team</b>
                        </p>
                    </div>

                    <div style="background:#f1f1f1; text-align:center; padding:15px; font-size:12px; color:#777;">
                        © 2026 TechStore. All rights reserved.
                    </div>

                </div>
            </div>
            """;

            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email: " + e.getMessage());
        }
    }
}

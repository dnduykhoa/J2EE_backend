package j2ee_backend.nhom05.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    
    // Gửi email đặt lại mật khẩu với mã xác thực
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
    
    // Gửi mã 2FA cho email người dùng khi đăng nhập
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

    // Gửi email thông báo đơn hàng chờ thanh toán lại
    public void sendPaymentPendingEmail(
            String toEmail,
            String fullName,
            String orderCode,
            LocalDateTime paymentDeadline,
            BigDecimal totalAmount,
            String paymentMethod) {
        if (toEmail == null || toEmail.isBlank() || paymentDeadline == null) {
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("TechStore - Đơn hàng chờ thanh toán lại");

            String deadlineText = paymentDeadline.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
            String amountText = totalAmount != null ? String.format("%,.0f", totalAmount) : "0";
            String customerName = (fullName != null && !fullName.isBlank()) ? fullName : "bạn";

            String htmlContent =
                "<div style='font-family: Arial, sans-serif; background-color:#f4f6f8; padding:30px;'>"
                + "<div style='max-width:600px; margin:auto; background:white; border-radius:8px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.1);'>"
                + "<div style='background:#0d6efd; color:white; padding:20px; text-align:center;'>"
                + "<h2 style='margin:0;'>TechStore</h2>"
                + "<p style='margin:0; font-size:14px;'>Thông báo thanh toán đơn hàng</p>"
                + "</div>"
                + "<div style='padding:30px;'>"
                + "<p>Xin chào <b>" + customerName + "</b>,</p>"
                + "<p>Đơn hàng <b>" + orderCode + "</b> của bạn chưa thanh toán thành công qua <b>" + paymentMethod + "</b>.</p>"
                + "<p>Hệ thống vẫn giữ đơn ở trạng thái <b>Chờ xác nhận</b> để bạn có thể thanh toán lại.</p>"
                + "<div style='background:#fff7ed; border:1px solid #fed7aa; border-radius:8px; padding:14px; margin:16px 0;'>"
                + "<p style='margin:0 0 6px 0; color:#9a3412;'><b>Thời hạn thanh toán:</b> " + deadlineText + "</p>"
                + "<p style='margin:0; color:#9a3412;'><b>Tổng tiền:</b> " + amountText + " ₫</p>"
                + "</div>"
                + "<p>Nếu bạn không thanh toán lại trước thời gian trên, đơn hàng sẽ tự động bị huỷ.</p>"
                + "<p style='margin-top:30px;'>Trân trọng,<br><b>TechStore Team</b></p>"
                + "</div>"
                + "</div>"
                + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email: " + e.getMessage());
        }
    }

    // Gửi email xác nhận đặt hàng thành công
    public void sendOrderConfirmationEmail(
            String toEmail,
            String fullName,
            String orderCode,
            BigDecimal totalAmount,
            String shippingAddress,
            String phone,
            String paymentMethod) {
        if (toEmail == null || toEmail.isBlank()) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("TechStore - Đặt hàng thành công #" + orderCode);

            String amountText = totalAmount != null ? String.format("%,.0f", totalAmount) : "0";
            String customerName = (fullName != null && !fullName.isBlank()) ? fullName : "bạn";
            String paymentLabel = "VNPAY".equalsIgnoreCase(paymentMethod) ? "VNPay"
                : "MOMO".equalsIgnoreCase(paymentMethod) ? "MoMo" : "Tiền mặt (COD)";

            String htmlContent =
                "<div style='font-family: Arial, sans-serif; background-color:#f4f6f8; padding:30px;'>"
                + "<div style='max-width:600px; margin:auto; background:white; border-radius:8px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.1);'>"
                + "<div style='background:#16a34a; color:white; padding:20px; text-align:center;'>"
                + "<h2 style='margin:0;'>TechStore</h2>"
                + "<p style='margin:4px 0 0 0; font-size:14px;'>Xác nhận đặt hàng thành công</p>"
                + "</div>"
                + "<div style='padding:30px;'>"
                + "<p>Xin chào <b>" + customerName + "</b>,</p>"
                + "<p>Cảm ơn bạn đã đặt hàng tại <b>TechStore</b>! Đơn hàng của bạn đã được ghi nhận.</p>"
                + "<div style='background:#f0fdf4; border:1px solid #bbf7d0; border-radius:8px; padding:16px; margin:16px 0;'>"
                + "<p style='margin:0 0 8px 0;'><b>Mã đơn hàng:</b> " + orderCode + "</p>"
                + "<p style='margin:0 0 8px 0;'><b>Tổng tiền:</b> " + amountText + " ₫</p>"
                + "<p style='margin:0 0 8px 0;'><b>Phương thức thanh toán:</b> " + paymentLabel + "</p>"
                + "<p style='margin:0 0 8px 0;'><b>Địa chỉ giao hàng:</b> " + (shippingAddress != null ? shippingAddress : "") + "</p>"
                + "<p style='margin:0;'><b>Số điện thoại:</b> " + (phone != null ? phone : "") + "</p>"
                + "</div>"
                + "<p>Chúng tôi sẽ xử lý và giao hàng đến bạn trong thời gian sớm nhất.</p>"
                + "<p style='margin-top:30px;'>Trân trọng,<br><b>TechStore Team</b></p>"
                + "</div>"
                + "<div style='background:#f8fafc; padding:16px; text-align:center; font-size:12px; color:#64748b;'>"
                + "© 2026 TechStore. All rights reserved."
                + "</div>"
                + "</div>"
                + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email xác nhận đơn hàng: " + e.getMessage());
        }
    }

    // Gửi email thông báo đơn hàng đã bị huỷ (do người dùng hoặc admin)
    public void sendOrderCancelledEmail(
            String toEmail,
            String fullName,
            String orderCode,
            BigDecimal totalAmount,
            String cancelReason) {
        if (toEmail == null || toEmail.isBlank()) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("TechStore - Đơn hàng #" + orderCode + " đã bị huỷ");

            String amountText = totalAmount != null ? String.format("%,.0f", totalAmount) : "0";
            String customerName = (fullName != null && !fullName.isBlank()) ? fullName : "bạn";
            String reason = (cancelReason != null && !cancelReason.isBlank()) ? cancelReason : "Không có lý do cụ thể";

            String htmlContent =
                "<div style='font-family: Arial, sans-serif; background-color:#f4f6f8; padding:30px;'>"
                + "<div style='max-width:600px; margin:auto; background:white; border-radius:8px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.1);'>"
                + "<div style='background:#dc2626; color:white; padding:20px; text-align:center;'>"
                + "<h2 style='margin:0;'>TechStore</h2>"
                + "<p style='margin:4px 0 0 0; font-size:14px;'>Thông báo huỷ đơn hàng</p>"
                + "</div>"
                + "<div style='padding:30px;'>"
                + "<p>Xin chào <b>" + customerName + "</b>,</p>"
                + "<p>Đơn hàng <b>#" + orderCode + "</b> của bạn đã được huỷ.</p>"
                + "<div style='background:#fef2f2; border:1px solid #fecaca; border-radius:8px; padding:16px; margin:16px 0;'>"
                + "<p style='margin:0 0 8px 0;'><b>Mã đơn hàng:</b> " + orderCode + "</p>"
                + "<p style='margin:0 0 8px 0;'><b>Tổng tiền đơn hàng:</b> " + amountText + " ₫</p>"
                + "<p style='margin:0;'><b>Lý do huỷ:</b> " + reason + "</p>"
                + "</div>"
                + "<p>Nếu bạn có thắc mắc, vui lòng liên hệ với chúng tôi để được hỗ trợ.</p>"
                + "<p style='margin-top:30px;'>Trân trọng,<br><b>TechStore Team</b></p>"
                + "</div>"
                + "<div style='background:#f8fafc; padding:16px; text-align:center; font-size:12px; color:#64748b;'>"
                + "© 2026 TechStore. All rights reserved."
                + "</div>"
                + "</div>"
                + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email huỷ đơn hàng: " + e.getMessage());
        }
    }

    // Gửi email thông báo đơn hàng đã bị huỷ do quá hạn thanh toán
    public void sendPaymentExpiredEmail(
            String toEmail,
            String fullName,
            String orderCode,
            BigDecimal totalAmount) {
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("TechStore - Đơn hàng đã bị huỷ do quá hạn thanh toán");

            String amountText = totalAmount != null ? String.format("%,.0f", totalAmount) : "0";
            String customerName = (fullName != null && !fullName.isBlank()) ? fullName : "bạn";

            String htmlContent =
                "<div style='font-family: Arial, sans-serif; background-color:#f4f6f8; padding:30px;'>"
                + "<div style='max-width:600px; margin:auto; background:white; border-radius:8px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.1);'>"
                + "<div style='background:#dc2626; color:white; padding:20px; text-align:center;'>"
                + "<h2 style='margin:0;'>TechStore</h2>"
                + "<p style='margin:0; font-size:14px;'>Thông báo huỷ đơn tự động</p>"
                + "</div>"
                + "<div style='padding:30px;'>"
                + "<p>Xin chào <b>" + customerName + "</b>,</p>"
                + "<p>Đơn hàng <b>" + orderCode + "</b> đã bị huỷ vì quá thời hạn thanh toán online.</p>"
                + "<p><b>Tổng tiền đơn hàng:</b> " + amountText + " ₫</p>"
                + "<p>Nếu bạn vẫn có nhu cầu mua hàng, vui lòng đặt lại đơn mới trên hệ thống.</p>"
                + "<p style='margin-top:30px;'>Trân trọng,<br><b>TechStore Team</b></p>"
                + "</div>"
                + "</div>"
                + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email: " + e.getMessage());
        }
    }
}

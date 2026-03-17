package j2ee_backend.nhom05.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    // DTO nội bộ để truyền thông tin sản phẩm vào email
    public static class EmailOrderItem {
        public final String productName;
        public final String variantInfo;  // ví dụ: "Màu: Đen, Size: XL"
        public final String imageUrl;     // URL ảnh có thể truy cập từ internet
        public final int quantity;
        public final BigDecimal unitPrice;
        public final BigDecimal subtotal;

        public EmailOrderItem(String productName, String variantInfo, String imageUrl,
                              int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
            this.productName = productName;
            this.variantInfo = variantInfo;
            this.imageUrl    = imageUrl;
            this.quantity    = quantity;
            this.unitPrice   = unitPrice;
            this.subtotal    = subtotal;
        }
    }

    // Tạo HTML bảng danh sách sản phẩm trong email
    private String buildItemsTableHtml(List<EmailOrderItem> items) {
        if (items == null || items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("<table style='width:100%; border-collapse:collapse; margin:16px 0;'>");
        sb.append("<thead><tr style='background:#f1f5f9;'>");
        sb.append("<th style='text-align:left; padding:10px 8px; font-size:12px; color:#475569; font-weight:600; border-bottom:1px solid #e2e8f0;'>Sản phẩm</th>");
        sb.append("<th style='text-align:center; padding:10px 8px; font-size:12px; color:#475569; font-weight:600; border-bottom:1px solid #e2e8f0;'>Số lượng</th>");
        sb.append("<th style='text-align:right; padding:10px 8px; font-size:12px; color:#475569; font-weight:600; border-bottom:1px solid #e2e8f0;'>Thành tiền</th>");
        sb.append("</tr></thead><tbody>");
        for (EmailOrderItem item : items) {
            String imgTag = (item.imageUrl != null && !item.imageUrl.isBlank())
                ? "<img src='" + item.imageUrl + "' alt='" + item.productName + "' "
                    + "style='width:52px; height:52px; object-fit:cover; border-radius:6px; border:1px solid #e2e8f0; display:block;' />"
                : "<div style='width:52px; height:52px; background:#f1f5f9; border-radius:6px; border:1px solid #e2e8f0; display:flex; align-items:center; justify-content:center; font-size:20px;'>📦</div>";
            String variantHtml = (item.variantInfo != null && !item.variantInfo.isBlank())
                ? "<p style='margin:2px 0 0 0; font-size:11px; color:#64748b;'>" + item.variantInfo + "</p>"
                : "";
            sb.append("<tr style='border-bottom:1px solid #f1f5f9;'>");
            sb.append("<td style='padding:10px 8px; vertical-align:middle;'>");
            sb.append("<div style='display:flex; align-items:center; gap:10px;'>");
            sb.append(imgTag);
            sb.append("<div>");
            sb.append("<p style='margin:0; font-size:13px; font-weight:600; color:#1e293b;'>" + item.productName + "</p>");
            sb.append(variantHtml);
            sb.append("<p style='margin:2px 0 0 0; font-size:11px; color:#94a3b8;'>" + String.format("%,.0f", item.unitPrice) + " ₫ / cái</p>");
            sb.append("</div></div></td>");
            sb.append("<td style='padding:10px 8px; text-align:center; font-size:13px; color:#475569; vertical-align:middle;'>" + item.quantity + "</td>");
                sb.append("<td style='padding:10px 8px; text-align:right; font-size:13px; font-weight:700; color:#222; vertical-align:middle;'>" + String.format("%,.0f", item.subtotal) + " ₫</td>");
            sb.append("</tr>");
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

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
            String paymentMethod,
            List<EmailOrderItem> items) {
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
                + "<div style='max-width:620px; margin:auto; background:white; border-radius:8px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.1);'>"
                + "<div style='background:#16a34a; color:white; padding:20px; text-align:center;'>"
                + "<h2 style='margin:0;'>TechStore</h2>"
                + "<p style='margin:4px 0 0 0; font-size:14px;'>Xác nhận đặt hàng thành công</p>"
                + "</div>"
                + "<div style='padding:28px;'>"
                + "<p style='margin:0 0 8px 0;'>Xin chào <b>" + customerName + "</b>,</p>"
                + "<p style='margin:0 0 16px 0;'>Cảm ơn bạn đã đặt hàng tại <b>TechStore</b>! Đơn hàng của bạn đã được ghi nhận.</p>"
                // Thông tin đơn hàng
                + "<div style='background:#f0fdf4; border:1px solid #bbf7d0; border-radius:8px; padding:14px 16px; margin-bottom:16px;'>"
                + "<p style='margin:0 0 6px 0;'><b>Mã đơn hàng:</b> " + orderCode + "</p>"
                + "<p style='margin:0 0 6px 0;'><b>Phương thức thanh toán:</b> " + paymentLabel + "</p>"
                + "<p style='margin:0 0 6px 0;'><b>Địa chỉ giao hàng:</b> " + (shippingAddress != null ? shippingAddress : "") + "</p>"
                + "<p style='margin:0;'><b>Số điện thoại:</b> " + (phone != null ? phone : "") + "</p>"
                + "</div>"
                // Bảng sản phẩm
                + "<p style='margin:0 0 4px 0; font-weight:600; color:#1e293b;'>Chi tiết đơn hàng</p>"
                + buildItemsTableHtml(items)
                // Tổng tiền
                + "<div style='border-top:2px solid #e2e8f0; padding-top:12px; text-align:right;'>"
                + "<p style='margin:0; font-size:15px;'>Tổng cộng: <b style='color:#e60012; font-size:17px;'>" + amountText + " ₫</b></p>"
                + "</div>"
                + "<p style='margin-top:20px;'>Chúng tôi sẽ xử lý và giao hàng đến bạn trong thời gian sớm nhất.</p>"
                + "<p style='margin-top:24px;'>Trân trọng,<br><b>TechStore Team</b></p>"
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
            String cancelReason,
            List<EmailOrderItem> items) {
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
                + "<p style='margin:0;'><b>Lý do huỷ:</b> " + reason + "</p>"
                + "</div>"
                + buildItemsTableHtml(items)
                + "<p style='margin:0; font-size:15px;'>Tổng tiền đơn hàng: <b style='color:#222; font-size:17px;'>" + amountText + " ₫</b></p>"
                + "<p style='margin-top:16px;'>Nếu bạn có thắc mắc, vui lòng liên hệ với chúng tôi để được hỗ trợ.</p>"
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

    // Gửi email xác nhận thanh toán online thành công (VNPAY / MoMo)
    public void sendPaymentConfirmedEmail(
            String toEmail,
            String fullName,
            String orderCode,
            BigDecimal totalAmount,
            String shippingAddress,
            String phone,
            String paymentMethod,
            List<EmailOrderItem> items) {
        if (toEmail == null || toEmail.isBlank()) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("TechStore - Thanh toán thành công #" + orderCode);

            String amountText = totalAmount != null ? String.format("%,.0f", totalAmount) : "0";
            String customerName = (fullName != null && !fullName.isBlank()) ? fullName : "bạn";
            String paymentLabel = "VNPAY".equalsIgnoreCase(paymentMethod) ? "VNPay"
                : "MOMO".equalsIgnoreCase(paymentMethod) ? "MoMo" : paymentMethod;

            String htmlContent =
                "<div style='font-family: Arial, sans-serif; background-color:#f4f6f8; padding:30px;'>"
                + "<div style='max-width:620px; margin:auto; background:white; border-radius:8px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.1);'>"
                + "<div style='background:#16a34a; color:white; padding:20px; text-align:center;'>"
                + "<h2 style='margin:0;'>TechStore</h2>"
                + "<p style='margin:4px 0 0 0; font-size:14px;'>Xác nhận thanh toán thành công</p>"
                + "</div>"
                + "<div style='padding:28px;'>"
                + "<p style='margin:0 0 8px 0;'>Xin chào <b>" + customerName + "</b>,</p>"
                + "<p style='margin:0 0 16px 0;'>🎉 Thanh toán qua <b>" + paymentLabel + "</b> cho đơn hàng của bạn đã thành công!</p>"
                // Thông tin đơn hàng
                + "<div style='background:#f0fdf4; border:1px solid #bbf7d0; border-radius:8px; padding:14px 16px; margin-bottom:16px;'>"
                + "<p style='margin:0 0 6px 0;'><b>Mã đơn hàng:</b> " + orderCode + "</p>"
                + "<p style='margin:0 0 6px 0;'><b>Phương thức thanh toán:</b> " + paymentLabel + "</p>"
                + "<p style='margin:0 0 6px 0;'><b>Địa chỉ giao hàng:</b> " + (shippingAddress != null ? shippingAddress : "") + "</p>"
                + "<p style='margin:0;'><b>Số điện thoại:</b> " + (phone != null ? phone : "") + "</p>"
                + "</div>"
                // Bảng sản phẩm
                + "<p style='margin:0 0 4px 0; font-weight:600; color:#1e293b;'>Chi tiết đơn hàng</p>"
                + buildItemsTableHtml(items)
                // Tổng tiền
                + "<div style='border-top:2px solid #e2e8f0; padding-top:12px; text-align:right;'>"
                + "<p style='margin:0; font-size:15px;'>Tổng đã thanh toán: <b style='color:#e60012; font-size:17px;'>" + amountText + " ₫</b></p>"
                + "</div>"
                + "<p style='margin-top:20px;'>Đơn hàng của bạn đang được xử lý và sẽ được giao trong thời gian sớm nhất.</p>"
                + "<p style='margin-top:24px;'>Trân trọng,<br><b>TechStore Team</b></p>"
                + "</div>"
                + "<div style='background:#f8fafc; padding:16px; text-align:center; font-size:12px; color:#64748b;'>"
                + "© 2026 TechStore. All rights reserved."
                + "</div>"
                + "</div>"
                + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email xác nhận thanh toán: " + e.getMessage());
        }
    }

    public void sendPreorderConfirmationEmail(
            String toEmail,
            String fullName,
            String productName,
            String variantInfo,
            Integer desiredQuantity) {
        if (toEmail == null || toEmail.isBlank()) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("TechStore - Xác nhận đăng ký chờ hàng");

            String customerName = (fullName != null && !fullName.isBlank()) ? fullName : "bạn";
            String quantityText = desiredQuantity != null ? String.valueOf(desiredQuantity) : "1";
            String variantHtml = (variantInfo != null && !variantInfo.isBlank())
                ? "<p style='margin:0 0 8px 0;'><b>Phân loại:</b> " + variantInfo + "</p>"
                : "";

            String htmlContent =
                "<div style='font-family: Arial, sans-serif; background:#f4f6f8; padding:30px;'>"
                + "<div style='max-width:620px; margin:auto; background:white; border-radius:10px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.08);'>"
                + "<div style='background:#0f766e; color:white; padding:22px; text-align:center;'>"
                + "<h2 style='margin:0;'>TechStore</h2>"
                + "<p style='margin:4px 0 0 0; font-size:14px;'>Xác nhận đăng ký chờ hàng</p>"
                + "</div>"
                + "<div style='padding:28px;'>"
                + "<p style='margin:0 0 12px 0;'>Xin chào <b>" + customerName + "</b>,</p>"
                + "<p style='margin:0 0 16px 0;'>Chúng tôi đã ghi nhận yêu cầu chờ hàng của bạn tại TechStore.</p>"
                + "<div style='background:#f0fdfa; border:1px solid #99f6e4; border-radius:8px; padding:16px;'>"
                + "<p style='margin:0 0 8px 0;'><b>Sản phẩm:</b> " + productName + "</p>"
                + variantHtml
                + "<p style='margin:0;'><b>Số lượng mong muốn:</b> " + quantityText + "</p>"
                + "</div>"
                + "<p style='margin:18px 0 0 0;'>Khi hàng về, hệ thống sẽ tự động gửi email thông báo cho bạn theo thứ tự đăng ký.</p>"
                + "<p style='margin-top:24px;'>Trân trọng,<br><b>TechStore Team</b></p>"
                + "</div>"
                + "</div>"
                + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email xác nhận chờ hàng: " + e.getMessage());
        }
    }

    public void sendPreorderAvailableEmail(
            String toEmail,
            String fullName,
            String productName,
            String variantInfo,
            Integer desiredQuantity) {
        if (toEmail == null || toEmail.isBlank()) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("TechStore - Sản phẩm bạn chờ đã có hàng");

            String customerName = (fullName != null && !fullName.isBlank()) ? fullName : "bạn";
            String quantityText = desiredQuantity != null ? String.valueOf(desiredQuantity) : "1";
            String variantHtml = (variantInfo != null && !variantInfo.isBlank())
                ? "<p style='margin:0 0 8px 0;'><b>Phân loại:</b> " + variantInfo + "</p>"
                : "";

            String htmlContent =
                "<div style='font-family: Arial, sans-serif; background:#f4f6f8; padding:30px;'>"
                + "<div style='max-width:620px; margin:auto; background:white; border-radius:10px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.08);'>"
                + "<div style='background:#ea580c; color:white; padding:22px; text-align:center;'>"
                + "<h2 style='margin:0;'>TechStore</h2>"
                + "<p style='margin:4px 0 0 0; font-size:14px;'>Thông báo hàng đã về</p>"
                + "</div>"
                + "<div style='padding:28px;'>"
                + "<p style='margin:0 0 12px 0;'>Xin chào <b>" + customerName + "</b>,</p>"
                + "<p style='margin:0 0 16px 0;'>Sản phẩm bạn đã đăng ký chờ tại TechStore hiện đã có hàng.</p>"
                + "<div style='background:#fff7ed; border:1px solid #fdba74; border-radius:8px; padding:16px;'>"
                + "<p style='margin:0 0 8px 0;'><b>Sản phẩm:</b> " + productName + "</p>"
                + variantHtml
                + "<p style='margin:0;'><b>Số lượng bạn đã đăng ký:</b> " + quantityText + "</p>"
                + "</div>"
                + "<p style='margin:18px 0 0 0;'>Bạn có thể quay lại website để đặt mua ngay khi còn hàng.</p>"
                + "<p style='margin-top:24px;'>Trân trọng,<br><b>TechStore Team</b></p>"
                + "</div>"
                + "</div>"
                + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email báo hàng về: " + e.getMessage());
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

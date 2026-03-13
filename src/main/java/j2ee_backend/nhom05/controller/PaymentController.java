package j2ee_backend.nhom05.controller;

import j2ee_backend.nhom05.service.MomoService;
import j2ee_backend.nhom05.service.OrderService;
import j2ee_backend.nhom05.service.VnpayService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@RestController
public class PaymentController {

    @Autowired
    private VnpayService vnpayService;

    @Autowired
    private MomoService momoService;

    @Autowired
    private OrderService orderService;

    @Value("${vnpay.frontend-url}")
    private String frontendUrl;

    // ═══════════════════════════════════════════════════════════════════
    // VNPAY
    // ═══════════════════════════════════════════════════════════════════

    /**
     * VNPAY redirect về đây sau khi người dùng hoàn tất thanh toán.
     * Backend xác minh chữ ký rồi redirect browser về frontend.
     */
    @GetMapping("/api/vnpay/callback")
    public void vnpayCallback(@RequestParam Map<String, String> params,
                              HttpServletResponse response) throws IOException {
        String responseCode = params.get("vnp_ResponseCode");
        String orderCode    = params.get("vnp_TxnRef");
        boolean isValid     = vnpayService.verifyCallback(params);

        if (isValid && "00".equals(responseCode) && orderCode != null) {
            // Thanh toán thành công → xác nhận đơn hàng
            try {
                orderService.confirmVnpayPayment(orderCode);
            } catch (Exception ignored) {
                // Đơn có thể đã được xác nhận trước đó
            }
            response.sendRedirect(frontendUrl + "/orders?vnpay=success&orderCode=" + orderCode);
        } else {
            // Thất bại hoặc bị huỷ → hoàn kho, huỷ đơn
            if (orderCode != null) {
                try {
                    orderService.cancelVnpayPayment(orderCode);
                } catch (Exception ignored) {}
            }
            String code = responseCode != null ? responseCode : "unknown";
            response.sendRedirect(frontendUrl + "/checkout?vnpay=failed&code=" + code);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // MOMO
    // ═══════════════════════════════════════════════════════════════════

    /**
     * MoMo redirect về đây sau khi người dùng hoàn tất thanh toán.
     * Backend xác minh chữ ký rồi redirect browser về frontend.
     */
    @GetMapping("/api/momo/callback")
    public void momoCallback(@RequestParam Map<String, String> params,
                             HttpServletResponse response) throws IOException {
        String resultCode = params.get("resultCode");
        String orderCode  = params.get("orderId");
        boolean isValid   = momoService.verifyCallback(params);

        if (isValid && "0".equals(resultCode) && orderCode != null) {
            // Thanh toán thành công → xác nhận đơn hàng
            try {
                orderService.confirmMomoPayment(orderCode);
            } catch (Exception ignored) {
                // Đơn có thể đã được xác nhận trước đó
            }
            response.sendRedirect(frontendUrl + "/orders?momo=success&orderCode=" + orderCode);
        } else {
            // Thất bại hoặc bị huỷ → hoàn kho, huỷ đơn
            if (orderCode != null) {
                try {
                    orderService.cancelMomoPayment(orderCode);
                } catch (Exception ignored) {}
            }
            String code = resultCode != null ? resultCode : "unknown";
            response.sendRedirect(frontendUrl + "/checkout?momo=failed&code=" + code);
        }
    }

    /**
     * MoMo IPN (Instant Payment Notification) — MoMo server gọi về để xác nhận.
     * Trả về HTTP 204 để MoMo biết đã nhận.
     */
    @PostMapping("/api/momo/ipn")
    public ResponseEntity<Void> momoIpn(@RequestBody Map<String, Object> body) {
        try {
            // Chuyển sang Map<String, String> để dùng chung verifyCallback
            Map<String, String> params = new java.util.HashMap<>();
            body.forEach((k, v) -> params.put(k, v != null ? v.toString() : ""));

            boolean isValid  = momoService.verifyCallback(params);
            String resultCode = params.get("resultCode");
            String orderCode  = params.get("orderId");

            if (isValid && "0".equals(resultCode) && orderCode != null) {
                try {
                    orderService.confirmMomoPayment(orderCode);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return ResponseEntity.noContent().build();
    }
}

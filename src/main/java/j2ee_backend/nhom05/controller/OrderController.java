package j2ee_backend.nhom05.controller;

import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.dto.OrderRequest;
import j2ee_backend.nhom05.dto.OrderResponse;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.service.MomoService;
import j2ee_backend.nhom05.service.OrderService;
import j2ee_backend.nhom05.service.VnpayService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private VnpayService vnpayService;

    @Autowired
    private MomoService momoService;

    // ── POST /api/orders ──────────────────────────────────────────────────────
    // Tạo đơn hàng từ giỏ hàng (yêu cầu đăng nhập)
    @PostMapping
    public ResponseEntity<?> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OrderRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = ((User) userDetails).getId();
            OrderResponse order = orderService.createOrder(userId, request);

            // Nếu chọn VNPAY → tạo URL thanh toán VNPAY
            if ("VNPAY".equalsIgnoreCase(request.getPaymentMethod())) {
                String ip = resolveClientIp(httpRequest);
                String vnpayUrl = vnpayService.createPaymentUrl(
                    order.getOrderCode(), order.getTotalAmount(), ip);
                order.setVnpayUrl(vnpayUrl);
            }

            // Nếu chọn MOMO → tạo URL thanh toán MoMo
            if ("MOMO".equalsIgnoreCase(request.getPaymentMethod())) {
                String momoUrl = momoService.createPaymentUrl(
                    order.getOrderCode(), order.getTotalAmount());
                order.setMomoUrl(momoUrl);
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Đặt hàng thành công", order));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Helper lấy IP client
    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip != null ? ip : "127.0.0.1";
    }

    // ── GET /api/orders/my ────────────────────────────────────────────────────
    // Lấy danh sách đơn hàng của user hiện tại
    @GetMapping("/my")
    public ResponseEntity<?> getMyOrders(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = ((User) userDetails).getId();
            List<OrderResponse> orders = orderService.getOrdersByUser(userId);
            return ResponseEntity.ok(new ApiResponse("Lấy danh sách đơn hàng thành công", orders));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // ── GET /api/orders/{id} ──────────────────────────────────────────────────
    // Lấy chi tiết đơn hàng (user chỉ xem đơn của mình, ADMIN xem tất cả)
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = ((User) userDetails).getId();
            boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));
            OrderResponse order = orderService.getOrderById(id, userId, isAdmin);
            return ResponseEntity.ok(new ApiResponse("Lấy thông tin đơn hàng thành công", order));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // ── POST /api/orders/{id}/cancel ──────────────────────────────────────────
    // User tự huỷ đơn hàng (chỉ khi đơn đang PENDING)
    // Body (optional): { "cancelReason": "..." }
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = ((User) userDetails).getId();
            String cancelReason = (body != null) ? body.get("cancelReason") : null;
            OrderResponse order = orderService.cancelOrder(id, userId, cancelReason);
            return ResponseEntity.ok(new ApiResponse("Huỷ đơn hàng thành công", order));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // ── GET /api/orders  (ADMIN) ──────────────────────────────────────────────
    // Admin lấy tất cả đơn hàng
    @GetMapping
    public ResponseEntity<?> getAllOrders(@AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse("Bạn không có quyền thực hiện thao tác này", null));
        }
        try {
            List<OrderResponse> orders = orderService.getAllOrders();
            return ResponseEntity.ok(new ApiResponse("Lấy tất cả đơn hàng thành công", orders));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // ── PATCH /api/orders/{id}/status  (ADMIN) ────────────────────────────────
    // Admin cập nhật trạng thái đơn hàng
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse("Bạn không có quyền thực hiện thao tác này", null));
        }
        try {
            String newStatus = body.get("status");
            String cancelReason = body.get("cancelReason");
            if (newStatus == null || newStatus.isBlank()) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse("Thiếu trường 'status'", null));
            }
            OrderResponse order = orderService.updateOrderStatus(id, newStatus, cancelReason);
            return ResponseEntity.ok(new ApiResponse("Cập nhật trạng thái thành công", order));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
}


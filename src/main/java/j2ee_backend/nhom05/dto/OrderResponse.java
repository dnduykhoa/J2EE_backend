package j2ee_backend.nhom05.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {

    private Long id;
    private String orderCode;
    private Long userId;
    private String fullName;
    private String phone;
    private String email;
    private String shippingAddress;
    private String note;
    private String paymentMethod;
    private String status;
    private BigDecimal totalAmount;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private String vnpayUrl;  // Chỉ có giá trị khi paymentMethod = VNPAY
    private String momoUrl;   // Chỉ có giá trị khi paymentMethod = MOMO

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String productImageUrl; // URL ảnh đại diện sản phẩm
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}

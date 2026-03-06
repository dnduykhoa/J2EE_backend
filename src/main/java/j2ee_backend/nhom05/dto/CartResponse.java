package j2ee_backend.nhom05.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import j2ee_backend.nhom05.model.Product;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartResponse {

    private Long id;
    private Long userId;
    private List<CartItemResponse> items;
    private Integer totalItems;
    private BigDecimal totalAmount;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CartItemResponse {
        private Long id;
        private Product product;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
        private boolean inStock;
        private Integer availableStock;
    }
}

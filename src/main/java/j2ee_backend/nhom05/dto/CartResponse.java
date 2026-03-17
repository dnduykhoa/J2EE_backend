package j2ee_backend.nhom05.dto;

import java.math.BigDecimal;
import java.util.List;

import j2ee_backend.nhom05.model.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
        private Long variantId;
        private String variantSku;
        private String variantImageUrl;
        private List<String> variantOptions;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
        private boolean inStock;
        private Integer availableStock;
        private boolean preorder;
    }
}

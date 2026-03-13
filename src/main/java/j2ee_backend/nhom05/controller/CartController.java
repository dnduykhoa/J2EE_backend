package j2ee_backend.nhom05.controller;

import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.dto.CartItemRequest;
import j2ee_backend.nhom05.dto.CartResponse;
import j2ee_backend.nhom05.dto.UpdateCartItemRequest;
import j2ee_backend.nhom05.service.CartService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class CartController {

    @Autowired
    private CartService cartService;

    // GET /api/cart?userId={userId}
    // Lấy giỏ hàng của user (tạo mới nếu chưa có)
    @GetMapping("")
    public ResponseEntity<?> getCart(@RequestParam Long userId) {
        try {
            CartResponse cartResponse = cartService.getCartByUserId(userId);
            return ResponseEntity.ok(new ApiResponse("Lấy giỏ hàng thành công", cartResponse));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // POST /api/cart/items?userId={userId}
    // Thêm sản phẩm vào giỏ hàng
    @PostMapping("/items")
    public ResponseEntity<?> addToCart(@RequestParam Long userId,
                                       @RequestBody CartItemRequest request) {
        try {
            CartResponse cartResponse = cartService.addToCart(
                userId,
                request.getProductId(),
                request.getVariantId(),
                request.getQuantity()
            );
            return ResponseEntity.ok(new ApiResponse("Thêm vào giỏ hàng thành công", cartResponse));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // PUT /api/cart/items/{itemId}?userId={userId}
    // Cập nhật số lượng sản phẩm trong giỏ hàng, body: { "quantity": N }
    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> updateCartItem(
            @PathVariable Long itemId,
            @RequestParam Long userId,
            @RequestBody UpdateCartItemRequest request) {
        try {
            CartResponse cartResponse = cartService.updateCartItem(userId, itemId, request.getQuantity());
            return ResponseEntity.ok(new ApiResponse("Cập nhật giỏ hàng thành công", cartResponse));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // DELETE /api/cart/items/{itemId}?userId={userId}
    // Xóa một sản phẩm khỏi giỏ hàng
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> removeCartItem(
            @PathVariable Long itemId,
            @RequestParam Long userId) {
        try {
            CartResponse cartResponse = cartService.removeCartItem(userId, itemId);
            return ResponseEntity.ok(new ApiResponse("Xóa sản phẩm khỏi giỏ hàng thành công", cartResponse));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // DELETE /api/cart?userId={userId}
    // Xóa toàn bộ giỏ hàng
    @DeleteMapping("")
    public ResponseEntity<?> clearCart(@RequestParam Long userId) {
        try {
            cartService.clearCart(userId);
            return ResponseEntity.ok(new ApiResponse("Đã xóa toàn bộ giỏ hàng", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // GET /api/cart/validate?userId={userId}
    // Kiểm tra giỏ hàng trước khi thanh toán
    @GetMapping("/validate")
    public ResponseEntity<?> validateCart(@RequestParam Long userId) {
        try {
            List<String> errors = cartService.validateCartForCheckout(userId);
            if (errors.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse("Giỏ hàng hợp lệ, có thể thanh toán", null));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Giỏ hàng có sản phẩm không thể thanh toán", errors));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
}

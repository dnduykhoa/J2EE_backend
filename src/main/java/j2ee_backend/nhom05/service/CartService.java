package j2ee_backend.nhom05.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import j2ee_backend.nhom05.dto.CartResponse;
import j2ee_backend.nhom05.dto.CartResponse.CartItemResponse;
import j2ee_backend.nhom05.model.Cart;
import j2ee_backend.nhom05.model.CartItem;
import j2ee_backend.nhom05.model.Product;
import j2ee_backend.nhom05.model.ProductStatus;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.repository.ICartItemRepository;
import j2ee_backend.nhom05.repository.ICartRepository;
import j2ee_backend.nhom05.repository.IProductRepository;
import j2ee_backend.nhom05.repository.IUserRepository;

@Service
public class CartService {

    @Autowired
    private ICartRepository cartRepository;

    @Autowired
    private ICartItemRepository cartItemRepository;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IUserRepository userRepository;

    // Lấy hoặc tạo mới giỏ hàng cho user
    @Transactional
    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));
            Cart cart = new Cart();
            cart.setUser(user);
            return cartRepository.save(cart);
        });
    }

    // Thêm sản phẩm vào giỏ hàng (nếu đã có thì tăng số lượng)
    @Transactional
    public CartResponse addToCart(Long userId, Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Số lượng phải lớn hơn 0");
        }

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new RuntimeException("Sản phẩm không còn hoạt động");
        }

        if (product.getStockQuantity() <= 0) {
            throw new RuntimeException("Sản phẩm '" + product.getName() + "' đã hết hàng");
        }

        Cart cart = getOrCreateCart(userId);

        // Kiểm tra nếu sản phẩm đã có trong giỏ hàng
        CartItem cartItem = cartItemRepository
            .findByCartIdAndProductId(cart.getId(), productId)
            .orElse(null);

        if (cartItem != null) {
            int newQuantity = cartItem.getQuantity() + quantity;
            if (newQuantity > product.getStockQuantity()) {
                newQuantity = product.getStockQuantity();
            }
            cartItem.setQuantity(newQuantity);
        } else {
            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            int actualQuantity = Math.min(quantity, product.getStockQuantity());
            cartItem.setQuantity(actualQuantity);
            cart.getItems().add(cartItem);
        }

        cartItemRepository.save(cartItem);
        return buildCartResponse(cart);
    }

    // Cập nhật số lượng sản phẩm trong giỏ hàng
    @Transactional
    public CartResponse updateCartItem(Long userId, Long cartItemId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Số lượng phải lớn hơn 0");
        }

        CartItem cartItem = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng"));

        if (!cartItem.getCart().getUser().getId().equals(userId)) {
            throw new RuntimeException("Không có quyền chỉnh sửa giỏ hàng này");
        }

        Product product = cartItem.getProduct();
        int actualQuantity = Math.min(quantity, product.getStockQuantity());
        if (actualQuantity <= 0) {
            throw new RuntimeException("Sản phẩm '" + product.getName() + "' đã hết hàng");
        }

        cartItem.setQuantity(actualQuantity);
        cartItemRepository.save(cartItem);

        Cart cart = getOrCreateCart(userId);
        return buildCartResponse(cart);
    }

    // Xóa một sản phẩm khỏi giỏ hàng
    @Transactional
    public CartResponse removeCartItem(Long userId, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng"));

        if (!cartItem.getCart().getUser().getId().equals(userId)) {
            throw new RuntimeException("Không có quyền chỉnh sửa giỏ hàng này");
        }

        Cart cart = cartItem.getCart();
        cart.getItems().removeIf(item -> item.getId().equals(cartItemId)); // safe by ID
        cartRepository.save(cart); // orphanRemoval deletes from cart_items

        return buildCartResponse(cart);
    }

    // Xóa toàn bộ giỏ hàng
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));
        cartRepository.delete(cart); // cascade CascadeType.ALL xóa cả cart_items
    }

    // Lấy thông tin giỏ hàng
    @Transactional(readOnly = true)
    public CartResponse getCartByUserId(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return buildCartResponse(cart);
    }

    // Kiểm tra giỏ hàng trước khi thanh toán — trả về danh sách lỗi nếu có
    @Transactional(readOnly = true)
    public List<String> validateCartForCheckout(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

        List<String> errors = new ArrayList<>();

        if (cart.getItems().isEmpty()) {
            errors.add("Giỏ hàng trống");
            return errors;
        }

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (product.getStatus() != ProductStatus.ACTIVE) {
                errors.add("Sản phẩm '" + product.getName() + "' hiện không còn bán");
            } else if (product.getStockQuantity() <= 0) {
                errors.add("Sản phẩm '" + product.getName() + "' đã hết hàng");
            } else if (item.getQuantity() > product.getStockQuantity()) {
                errors.add("Sản phẩm '" + product.getName() + "' chỉ còn " + product.getStockQuantity() + " sản phẩm trong kho");
            }
        }

        return errors;
    }

    // Build CartResponse DTO từ Cart entity
    private CartResponse buildCartResponse(Cart cart) {
        // Tải lại giỏ hàng mới nhất từ DB để đồng bộ trạng thái
        cart = cartRepository.findById(cart.getId())
            .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

        List<CartItemResponse> itemResponses = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            boolean inStock = product.getStatus() == ProductStatus.ACTIVE && product.getStockQuantity() > 0;
            int availableStock = product.getStockQuantity();

            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            itemResponses.add(new CartItemResponse(
                item.getId(),
                product,
                item.getQuantity(),
                unitPrice,
                subtotal,
                inStock,
                availableStock
            ));
        }

        return new CartResponse(
            cart.getId(),
            cart.getUser().getId(),
            itemResponses,
            itemResponses.size(),
            totalAmount
        );
    }
}

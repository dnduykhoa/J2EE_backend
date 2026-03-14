package j2ee_backend.nhom05.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import j2ee_backend.nhom05.dto.OrderRequest;
import j2ee_backend.nhom05.dto.OrderResponse;
import j2ee_backend.nhom05.dto.OrderResponse.OrderItemResponse;
import j2ee_backend.nhom05.model.Cart;
import j2ee_backend.nhom05.model.CartItem;
import j2ee_backend.nhom05.model.Order;
import j2ee_backend.nhom05.model.OrderItem;
import j2ee_backend.nhom05.model.OrderStatus;
import j2ee_backend.nhom05.model.PaymentMethod;
import j2ee_backend.nhom05.model.Product;
import j2ee_backend.nhom05.model.ProductStatus;
import j2ee_backend.nhom05.model.ProductVariant;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.repository.ICartRepository;
import j2ee_backend.nhom05.repository.IOrderRepository;
import j2ee_backend.nhom05.repository.IProductRepository;
import j2ee_backend.nhom05.repository.IUserRepository;

@Service
public class OrderService {

    @Autowired
    private IOrderRepository orderRepository;

    @Autowired
    private ICartRepository cartRepository;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IUserRepository userRepository;

    /**
     * Tạo đơn hàng từ giỏ hàng hiện tại của user.
     * Sau khi tạo thành công: trừ tồn kho và xoá giỏ hàng.
     */
    @Transactional
    public OrderResponse createOrder(Long userId, OrderRequest request) {
        // 1. Lấy user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // 2. Lấy giỏ hàng
        Cart cart = cartRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Giỏ hàng trống, không thể đặt hàng"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống, không thể đặt hàng");
        }

        // 3. Validate từng sản phẩm trong giỏ
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            ProductVariant variant = cartItem.getVariant();
            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new RuntimeException("Sản phẩm '" + product.getName() + "' hiện không còn bán");
            }

            if (variant != null) {
                if (!Boolean.TRUE.equals(variant.getIsActive())) {
                    throw new RuntimeException("Biến thể của sản phẩm '" + product.getName() + "' hiện không còn bán");
                }
                if (variant.getStockQuantity() <= 0) {
                    throw new RuntimeException("Biến thể của sản phẩm '" + product.getName() + "' đã hết hàng");
                }
                if (cartItem.getQuantity() > variant.getStockQuantity()) {
                    throw new RuntimeException("Biến thể của sản phẩm '" + product.getName()
                        + "' chỉ còn " + variant.getStockQuantity() + " sản phẩm trong kho");
                }
            } else {
                if (product.getStockQuantity() <= 0) {
                    throw new RuntimeException("Sản phẩm '" + product.getName() + "' đã hết hàng");
                }
                if (cartItem.getQuantity() > product.getStockQuantity()) {
                    throw new RuntimeException("Sản phẩm '" + product.getName()
                        + "' chỉ còn " + product.getStockQuantity() + " sản phẩm trong kho");
                }
            }
        }

        // 4. Parse paymentMethod
        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Phương thức thanh toán không hợp lệ: " + request.getPaymentMethod());
        }

        // 5. Tạo Order
        Order order = new Order();
        order.setUser(user);
        order.setFullName(request.getFullName());
        order.setPhone(request.getPhone());
        order.setEmail(request.getEmail());
        order.setShippingAddress(request.getShippingAddress());
        order.setNote(request.getNote());
        order.setPaymentMethod(paymentMethod);
        order.setStatus(OrderStatus.PENDING);

        // 6. Tạo OrderItem và tính tổng tiền
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            ProductVariant variant = cartItem.getVariant();
            BigDecimal unitPrice = variant != null ? variant.getPrice() : product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setVariant(variant);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setSubtotal(subtotal);
            orderItems.add(orderItem);

            // Trừ tồn kho
            if (variant != null) {
                variant.setStockQuantity(variant.getStockQuantity() - cartItem.getQuantity());
            } else {
                product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
                productRepository.save(product);
            }
        }

        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);
        Order savedOrder = orderRepository.save(order);

        // 7. Xoá giỏ hàng sau khi đặt hàng thành công
        cartRepository.delete(cart);

        return buildOrderResponse(savedOrder);
    }

    /**
     * Lấy danh sách đơn hàng của user.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<OrderResponse> result = new ArrayList<>();
        for (Order order : orders) {
            result.add(buildOrderResponse(order));
        }
        return result;
    }

    /**
     * Lấy chi tiết một đơn hàng (chỉ user sở hữu hoặc ADMIN mới xem được).
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId, boolean isAdmin) {
        Order order;
        if (isAdmin) {
            order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        } else {
            order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng hoặc bạn không có quyền xem"));
        }
        return buildOrderResponse(order);
    }

    /**
     * Admin: lấy tất cả đơn hàng, sắp xếp mới nhất trước.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        List<OrderResponse> result = new ArrayList<>();
        for (Order order : orders) {
            result.add(buildOrderResponse(order));
        }
        return result;
    }

    /**
     * Cập nhật trạng thái đơn hàng (dành cho admin).
     * Nếu chuyển sang CANCELLED: hoàn lại tồn kho (trừ DELIVERED vì hàng đã giao xong).
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, String newStatus, String cancelReason) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        OrderStatus newOrderStatus;
        try {
            newOrderStatus = OrderStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Trạng thái không hợp lệ: " + newStatus);
        }

        OrderStatus currentStatus = order.getStatus();

        // Nếu admin chuyển sang CANCELLED và đơn chưa bị huỷ/chưa giao xong
        // → hoàn lại tồn kho
        if (newOrderStatus == OrderStatus.CANCELLED
                && currentStatus != OrderStatus.CANCELLED
                && currentStatus != OrderStatus.DELIVERED) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                ProductVariant variant = item.getVariant();
                if (variant != null) {
                    variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
                } else {
                    product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                    productRepository.save(product);
                }
            }
            order.setCancelledAt(java.time.LocalDateTime.now());
            order.setCancelReason(
                (cancelReason != null && !cancelReason.isBlank()) ? cancelReason : "Admin huỷ đơn hàng"
            );
        }

        order.setStatus(newOrderStatus);
        return buildOrderResponse(orderRepository.save(order));
    }

    /**
     * User huỷ đơn hàng (chỉ khi còn PENDING).
     * cancelReason: lý do huỷ (từ danh sách mặc định hoặc người dùng tự nhập)
     */
    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long userId, String cancelReason) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể huỷ đơn hàng đang ở trạng thái chờ xác nhận");
        }
        // Hoàn lại tồn kho
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            ProductVariant variant = item.getVariant();
            if (variant != null) {
                variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
            } else {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(java.time.LocalDateTime.now());
        order.setCancelReason(cancelReason != null && !cancelReason.isBlank()
            ? cancelReason : "Người dùng huỷ đơn");
        return buildOrderResponse(orderRepository.save(order));
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    /**
     * VNPAY callback: thanh toán thành công → chuyển CONFIRMED.
     */
    @Transactional
    public void confirmVnpayPayment(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderCode));
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
        }
    }

    /**
     * VNPAY callback: thanh toán thất bại / bị huỷ → hoàn kho, huỷ đơn.
     */
    @Transactional
    public void cancelVnpayPayment(String orderCode) {
        orderRepository.findByOrderCode(orderCode).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.PENDING) {
                for (OrderItem item : order.getItems()) {
                    Product product = item.getProduct();
                    ProductVariant variant = item.getVariant();
                    if (variant != null) {
                        variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
                    } else {
                        product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                        productRepository.save(product);
                    }
                }
                order.setStatus(OrderStatus.CANCELLED);
                order.setCancelledAt(java.time.LocalDateTime.now());
                order.setCancelReason("Thanh toán VNPay thất bại hoặc bị huỷ");
                orderRepository.save(order);
            }
        });
    }

    /**
     * MoMo callback: thanh toán thành công → chuyển CONFIRMED.
     */
    @Transactional
    public void confirmMomoPayment(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderCode));
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
        }
    }

    /**
     * MoMo callback: thanh toán thất bại / bị huỷ → hoàn kho, huỷ đơn.
     */
    @Transactional
    public void cancelMomoPayment(String orderCode) {
        orderRepository.findByOrderCode(orderCode).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.PENDING) {
                for (OrderItem item : order.getItems()) {
                    Product product = item.getProduct();
                    ProductVariant variant = item.getVariant();
                    if (variant != null) {
                        variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
                    } else {
                        product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                        productRepository.save(product);
                    }
                }
                order.setStatus(OrderStatus.CANCELLED);
                order.setCancelledAt(java.time.LocalDateTime.now());
                order.setCancelReason("Thanh toán MoMo thất bại hoặc bị huỷ");
                orderRepository.save(order);
            }
        });
    }

    // ── Helper ────────────────────────────────────────────────────────────────────────────
    private OrderResponse buildOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();

            // Lấy URL ảnh đại diện sản phẩm
            String productImageUrl = null;
            if (product.getMedia() != null) {
                productImageUrl = product.getMedia().stream()
                    .filter(m -> Boolean.TRUE.equals(m.getIsPrimary()) && "IMAGE".equals(m.getMediaType()))
                    .findFirst()
                    .map(m -> m.getMediaUrl())
                    .orElse(product.getMedia().stream()
                        .filter(m -> "IMAGE".equals(m.getMediaType()))
                        .findFirst()
                        .map(m -> m.getMediaUrl())
                        .orElse(null));
            }

            itemResponses.add(new OrderItemResponse(
                item.getId(),
                product.getId(),
                product.getName(),
                productImageUrl,
                item.getVariant() != null ? item.getVariant().getId() : null,
                item.getVariant() != null ? item.getVariant().getSku() : null,
                item.getVariant() != null
                    ? item.getVariant().getValues().stream()
                        .map(v -> {
                            String key = v.getAttributeDefinition() != null ? v.getAttributeDefinition().getName() : v.getAttrKey();
                            return (key != null ? key : "") + ": " + v.getDisplayValue();
                        })
                        .collect(Collectors.toList())
                    : null,
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
            ));
        }

        return new OrderResponse(
            order.getId(),
            order.getOrderCode(),
            order.getUser().getId(),
            order.getFullName(),
            order.getPhone(),
            order.getEmail(),
            order.getShippingAddress(),
            order.getNote(),
            order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null,
            order.getStatus() != null ? order.getStatus().name() : null,
            order.getTotalAmount(),
            itemResponses,
            order.getCreatedAt(),
            order.getUpdatedAt(),
            order.getCancelledAt(),
            order.getCancelReason(),
            null,  // vnpayUrl — được gán bởi controller nếu cần
            null   // momoUrl  — được gán bởi controller nếu cần
        );
    }
}

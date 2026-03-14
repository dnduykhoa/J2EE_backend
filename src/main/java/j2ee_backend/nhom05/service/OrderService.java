package j2ee_backend.nhom05.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import j2ee_backend.nhom05.dto.OrderItemRequest;
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
import j2ee_backend.nhom05.repository.IProductVariantRepository;
import j2ee_backend.nhom05.repository.IUserRepository;
import j2ee_backend.nhom05.validator.PhoneValidator;

@Service
public class OrderService {

    @Autowired
    private IOrderRepository orderRepository;

    @Autowired
    private ICartRepository cartRepository;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IProductVariantRepository productVariantRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private EmailService emailService;

    private static class OrderLineSource {
        private final Product product;
        private final ProductVariant variant;
        private final int quantity;

        private OrderLineSource(Product product, ProductVariant variant, int quantity) {
            this.product = product;
            this.variant = variant;
            this.quantity = quantity;
        }
    }

    /**
     * Tạo đơn hàng từ giỏ hàng hiện tại của user.
     * Sau khi tạo thành công: trừ tồn kho và xoá giỏ hàng.
     */
    @Transactional
    public OrderResponse createOrder(Long userId, OrderRequest request) {
        // 1. Lấy user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // 2. Resolve nguồn item: request.items (mua ngay) hoặc cart hiện tại
        List<OrderLineSource> orderLines = new ArrayList<>();
        boolean useCartSource = request.getItems() == null || request.getItems().isEmpty();
        Cart cart = null;

        if (!useCartSource) {
            for (OrderItemRequest item : request.getItems()) {
                Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + item.getProductId()));

                ProductVariant variant = null;
                if (item.getVariantId() != null) {
                    variant = productVariantRepository.findByIdAndProductId(item.getVariantId(), product.getId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể hợp lệ cho sản phẩm '" + product.getName() + "'"));
                }

                int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                if (qty <= 0) {
                    throw new RuntimeException("Số lượng sản phẩm phải lớn hơn 0");
                }

                orderLines.add(new OrderLineSource(product, variant, qty));
            }
        } else {
            cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng trống, không thể đặt hàng"));

            if (cart.getItems() == null || cart.getItems().isEmpty()) {
                throw new RuntimeException("Giỏ hàng trống, không thể đặt hàng");
            }

            for (CartItem cartItem : cart.getItems()) {
                orderLines.add(new OrderLineSource(
                    cartItem.getProduct(),
                    cartItem.getVariant(),
                    cartItem.getQuantity()
                ));
            }
        }

        // 3. Validate từng dòng sản phẩm
        for (OrderLineSource line : orderLines) {
            Product product = line.product;
            ProductVariant variant = line.variant;
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
                if (line.quantity > variant.getStockQuantity()) {
                    throw new RuntimeException("Biến thể của sản phẩm '" + product.getName()
                        + "' chỉ còn " + variant.getStockQuantity() + " sản phẩm trong kho");
                }
            } else {
                if (product.getStockQuantity() <= 0) {
                    throw new RuntimeException("Sản phẩm '" + product.getName() + "' đã hết hàng");
                }
                if (line.quantity > product.getStockQuantity()) {
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
        order.setPhone(PhoneValidator.normalize(request.getPhone()));
        order.setEmail(request.getEmail());
        order.setShippingAddress(request.getShippingAddress());
        order.setNote(request.getNote());
        order.setPaymentMethod(paymentMethod);
        order.setStatus(OrderStatus.PENDING);
        // VNPAY/MOMO: đặt deadline thanh toán = 30 phút kể từ lúc tạo đơn
        if (paymentMethod == PaymentMethod.VNPAY || paymentMethod == PaymentMethod.MOMO) {
            order.setPaymentDeadline(LocalDateTime.now().plusMinutes(30));
        }

        // 6. Tạo OrderItem và tính tổng tiền
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderLineSource line : orderLines) {
            Product product = line.product;
            ProductVariant variant = line.variant;
            BigDecimal unitPrice = variant != null ? variant.getPrice() : product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(line.quantity));
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setVariant(variant);
            orderItem.setQuantity(line.quantity);
            orderItem.setUnitPrice(unitPrice);
            orderItem.setSubtotal(subtotal);
            orderItems.add(orderItem);

            // Trừ tồn kho và cập nhật trạng thái nếu hết hàng
            deductStock(product, variant, line.quantity);
        }

        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);
        Order savedOrder = orderRepository.save(order);

        // 7. Nếu lấy dữ liệu từ giỏ hàng thì xoá giỏ sau khi đặt thành công
        if (useCartSource && cart != null) {
            cartRepository.delete(cart);
        }

        // 8. Gửi email xác nhận đặt hàng
        try {
            emailService.sendOrderConfirmationEmail(
                savedOrder.getEmail(), savedOrder.getFullName(), savedOrder.getOrderCode(),
                savedOrder.getTotalAmount(), savedOrder.getShippingAddress(),
                savedOrder.getPhone(), savedOrder.getPaymentMethod().name());
        } catch (Exception ignored) {}

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
                restoreStock(product, variant, item.getQuantity());
            }
            order.setCancelledAt(LocalDateTime.now());
            order.setCancelReason(
                (cancelReason != null && !cancelReason.isBlank()) ? cancelReason : "Admin huỷ đơn hàng"
            );
        }

        order.setStatus(newOrderStatus);
        Order savedOrder = orderRepository.save(order);

        // Gửi email thông báo huỷ đơn (nếu admin chuyển sang CANCELLED)
        if (newOrderStatus == OrderStatus.CANCELLED) {
            try {
                emailService.sendOrderCancelledEmail(
                    savedOrder.getEmail(), savedOrder.getFullName(), savedOrder.getOrderCode(),
                    savedOrder.getTotalAmount(), savedOrder.getCancelReason());
            } catch (Exception ignored) {}
        }

        return buildOrderResponse(savedOrder);
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
            restoreStock(product, variant, item.getQuantity());
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason(cancelReason != null && !cancelReason.isBlank()
            ? cancelReason : "Người dùng huỷ đơn");
        Order savedOrder = orderRepository.save(order);

        // Gửi email thông báo huỷ đơn
        try {
            emailService.sendOrderCancelledEmail(
                savedOrder.getEmail(), savedOrder.getFullName(), savedOrder.getOrderCode(),
                savedOrder.getTotalAmount(), savedOrder.getCancelReason());
        } catch (Exception ignored) {}

        return buildOrderResponse(savedOrder);
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
            order.setPaymentDeadline(null);
            orderRepository.save(order);
        }
    }

    /**
     * VNPAY callback: thanh toán thất bại / bị huỷ → giữ đơn PENDING, đặt lại deadline và gửi email nhắc.
     */
    @Transactional
    public void cancelVnpayPayment(String orderCode) {
        orderRepository.findByOrderCode(orderCode).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setPaymentDeadline(LocalDateTime.now().plusMinutes(30));
                orderRepository.save(order);
                try {
                    emailService.sendPaymentPendingEmail(
                        order.getEmail(),
                        order.getFullName(),
                        order.getOrderCode(),
                        order.getPaymentDeadline(),
                        order.getTotalAmount(),
                        "VNPAY"
                    );
                } catch (Exception ignored) {}
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
            order.setPaymentDeadline(null);
            orderRepository.save(order);
        }
    }

    /**
     * MoMo callback: thanh toán thất bại / bị huỷ → giữ đơn PENDING, đặt lại deadline và gửi email nhắc.
     */
    @Transactional
    public void cancelMomoPayment(String orderCode) {
        orderRepository.findByOrderCode(orderCode).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setPaymentDeadline(LocalDateTime.now().plusMinutes(30));
                orderRepository.save(order);
                try {
                    emailService.sendPaymentPendingEmail(
                        order.getEmail(),
                        order.getFullName(),
                        order.getOrderCode(),
                        order.getPaymentDeadline(),
                        order.getTotalAmount(),
                        "MoMo"
                    );
                } catch (Exception ignored) {}
            }
        });
    }

    /**
     * User thanh toán lại đơn hàng online (VNPAY/MOMO).
     * - Chỉ áp dụng cho đơn PENDING
     * - Gia hạn deadline thêm 30 phút từ hiện tại
     */
    @Transactional
    public OrderResponse retryPayment(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Đơn hàng không ở trạng thái chờ xác nhận");
        }

        if (order.getPaymentMethod() != PaymentMethod.VNPAY && order.getPaymentMethod() != PaymentMethod.MOMO) {
            throw new RuntimeException("Đơn hàng này không áp dụng thanh toán lại online");
        }

        if (order.getPaymentDeadline() == null || order.getPaymentDeadline().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Đơn hàng đã hết hạn thanh toán");
        }

        order.setPaymentDeadline(LocalDateTime.now().plusMinutes(30));
        return buildOrderResponse(orderRepository.save(order));
    }

    /**
     * Tự động huỷ các đơn online chưa thanh toán khi quá hạn deadline.
     */
    @Transactional
    public void expireUnpaidOrders() {
        List<Long> expiredIds = orderRepository.findExpiredUnpaidOrderIds(
            OrderStatus.PENDING,
            List.of(PaymentMethod.VNPAY, PaymentMethod.MOMO),
            LocalDateTime.now()
        );

        for (Long orderId : expiredIds) {
            Order order = orderRepository.findByIdWithItems(orderId).orElse(null);
            if (order == null) continue;

            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                ProductVariant variant = item.getVariant();
                restoreStock(product, variant, item.getQuantity());
            }

            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelledAt(LocalDateTime.now());
            order.setCancelReason("Hết hạn thanh toán online");
            order.setPaymentDeadline(null);
            orderRepository.save(order);

            try {
                emailService.sendPaymentExpiredEmail(
                    order.getEmail(),
                    order.getFullName(),
                    order.getOrderCode(),
                    order.getTotalAmount()
                );
            } catch (Exception ignored) {}
        }
    }

    // ── Stock helpers ─────────────────────────────────────────────────────────────────────
    /**
     * Trừ tồn kho và tự động chuyển trạng thái khi hết hàng.
     * - Variant: isActive = false khi stockQuantity về 0
     * - Product : status = OUT_OF_STOCK khi stockQuantity về 0
     */
    private void deductStock(Product product, ProductVariant variant, int qty) {
        if (variant != null) {
            int newStock = variant.getStockQuantity() - qty;
            variant.setStockQuantity(newStock);
            if (newStock <= 0) {
                variant.setIsActive(false);
            }
        } else {
            int newStock = product.getStockQuantity() - qty;
            product.setStockQuantity(newStock);
            if (newStock <= 0) {
                product.setStatus(ProductStatus.OUT_OF_STOCK);
            }
            productRepository.save(product);
        }
    }

    /**
     * Hoàn tồn kho và tự động kích hoạt lại khi có hàng trở lại.
     * - Variant: isActive = true khi stockQuantity > 0
     * - Product : status = ACTIVE khi stockQuantity > 0 và đang là OUT_OF_STOCK
     */
    private void restoreStock(Product product, ProductVariant variant, int qty) {
        if (variant != null) {
            int newStock = variant.getStockQuantity() + qty;
            variant.setStockQuantity(newStock);
            if (newStock > 0) {
                variant.setIsActive(true);
            }
        } else {
            int newStock = product.getStockQuantity() + qty;
            product.setStockQuantity(newStock);
            if (product.getStatus() == ProductStatus.OUT_OF_STOCK && newStock > 0) {
                product.setStatus(ProductStatus.ACTIVE);
            }
            productRepository.save(product);
        }
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
            null,  // momoUrl  — được gán bởi controller nếu cần
            order.getPaymentDeadline()
        );
    }
}

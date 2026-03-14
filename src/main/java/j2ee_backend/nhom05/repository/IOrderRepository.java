package j2ee_backend.nhom05.repository;

import j2ee_backend.nhom05.model.Order;
import j2ee_backend.nhom05.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IOrderRepository extends JpaRepository<Order, Long> {

    // Tìm theo mã đơn hàng (ví dụ: 20260312GPH8F2)
    Optional<Order> findByOrderCode(String orderCode);

    // Lấy tất cả đơn hàng của một user, sắp xếp mới nhất trước
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Lấy đơn hàng theo id và userId (đảm bảo chỉ xem đơn của mình)
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    // Lấy đơn hàng theo trạng thái (dành cho admin)
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    // Lấy tất cả đơn hàng, sắp xếp mới nhất trước (admin)
    List<Order> findAllByOrderByCreatedAtDesc();

    // Đếm số đơn hàng của user
    long countByUserId(Long userId);

    // Tìm đơn có load luôn items (tránh N+1)
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items oi LEFT JOIN FETCH oi.product WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);
}

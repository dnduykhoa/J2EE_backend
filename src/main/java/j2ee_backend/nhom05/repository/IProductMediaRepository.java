package j2ee_backend.nhom05.repository;

import j2ee_backend.nhom05.model.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IProductMediaRepository extends JpaRepository<ProductMedia, Long> {
    List<ProductMedia> findByProductId(Long productId);
    void deleteByProductId(Long productId);

    // Lấy displayOrder lớn nhất của sản phẩm (để tiếp tục đánh số)
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(MAX(m.displayOrder), -1) FROM ProductMedia m WHERE m.product.id = :productId")
    int findMaxDisplayOrderByProductId(Long productId);
}

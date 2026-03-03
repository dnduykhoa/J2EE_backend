package j2ee_backend.nhom05.repository;

import j2ee_backend.nhom05.model.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IProductMediaRepository extends JpaRepository<ProductMedia, Long> {
    List<ProductMedia> findByProductId(Long productId);
    void deleteByProductId(Long productId);
}

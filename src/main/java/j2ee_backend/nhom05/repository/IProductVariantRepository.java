package j2ee_backend.nhom05.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import j2ee_backend.nhom05.model.ProductVariant;

@Repository
public interface IProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductIdOrderByDisplayOrderAsc(Long productId);
    List<ProductVariant> findByProductIdAndIsActiveTrueOrderByDisplayOrderAsc(Long productId);
    Optional<ProductVariant> findByIdAndProductId(Long variantId, Long productId);
}

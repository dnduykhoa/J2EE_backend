package j2ee_backend.nhom05.repository;

import j2ee_backend.nhom05.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IProductRepository extends JpaRepository<Product, Long> {
    
    // Tìm sản phẩm theo tên (tìm kiếm không phân biệt hoa thường)
    List<Product> findByNameContainingIgnoreCase(String name);
    
    // Tìm sản phẩm theo danh mục
    List<Product> findByCategoryId(Long categoryId);
    
    // Đếm số sản phẩm theo danh mục
    long countByCategoryId(Long categoryId);
    
    // Đếm số sản phẩm theo thương hiệu
    long countByBrandId(Long brandId);
    
    // Tìm sản phẩm theo thương hiệu
    List<Product> findByBrandId(Long brandId);
    
    // Tìm sản phẩm đang hoạt động
    List<Product> findByIsActiveTrue();
    
    // Tìm sản phẩm theo khoảng giá
    List<Product> findByPriceBetween(java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice);
}

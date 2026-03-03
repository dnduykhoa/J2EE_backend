package j2ee_backend.nhom05.repository;

import j2ee_backend.nhom05.model.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface IBrandRepository extends JpaRepository<Brand, Long> {
    
    // Tìm brand theo tên
    Optional<Brand> findByName(String name);
    
    // Lấy brand đang hoạt động
    List<Brand> findByIsActiveTrue();
    
    // Tìm brand theo tên (tìm kiếm)
    List<Brand> findByNameContainingIgnoreCase(String name);
}

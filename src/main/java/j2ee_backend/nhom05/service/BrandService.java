package j2ee_backend.nhom05.service;

import j2ee_backend.nhom05.model.Brand;
import j2ee_backend.nhom05.repository.IBrandRepository;
import j2ee_backend.nhom05.repository.IProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class BrandService {
    
    @Autowired
    private IBrandRepository brandRepository;
    
    @Autowired
    private IProductRepository productRepository;
    
    // Lấy tất cả brand
    public List<Brand> getAllBrands() {
        return brandRepository.findAll();
    }
    
    // Lấy brand theo ID
    public Optional<Brand> getBrandById(Long id) {
        return brandRepository.findById(id);
    }
    
    // Lấy brand đang hoạt động
    public List<Brand> getActiveBrands() {
        return brandRepository.findByIsActiveTrue();
    }
    
    // Tạo brand mới
    public Brand createBrand(Brand brand) {
        return brandRepository.save(brand);
    }
    
    // Cập nhật brand
    public Brand updateBrand(Long id, Brand brandDetails) {
        Brand brand = brandRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu với ID: " + id));
        
        brand.setName(brandDetails.getName());
        brand.setLogoUrl(brandDetails.getLogoUrl());
        brand.setDescription(brandDetails.getDescription());
        brand.setDisplayOrder(brandDetails.getDisplayOrder());
        brand.setIsActive(brandDetails.getIsActive());
        
        return brandRepository.save(brand);
    }
    
    // Xóa brand
    public void deleteBrand(Long id) {
        Brand brand = brandRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu với ID: " + id));
        
        // Kiểm tra xem có sản phẩm nào thuộc thương hiệu này không
        long productCount = productRepository.countByBrandId(id);
        if (productCount > 0) {
            throw new RuntimeException("Không thể xóa thương hiệu này vì còn " + productCount + " sản phẩm. Vui lòng chuyển hoặc xóa các sản phẩm trước.");
        }
        
        brandRepository.delete(brand);
    }
    
    // Tìm kiếm brand theo tên
    public List<Brand> searchBrands(String name) {
        return brandRepository.findByNameContainingIgnoreCase(name);
    }
}

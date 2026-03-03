package j2ee_backend.nhom05.service;

import j2ee_backend.nhom05.model.Product;
import j2ee_backend.nhom05.model.ProductMedia;
import j2ee_backend.nhom05.model.ProductSpecification;
import j2ee_backend.nhom05.repository.IProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    
    @Autowired
    private IProductRepository productRepository;
    
    @Autowired
    private ProductMediaService productMediaService;
    
    // Lấy tất cả sản phẩm
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    // Lấy sản phẩm theo ID
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }
    
    // Tạo sản phẩm mới
    public Product createProduct(Product product) {
        // Thiết lập quan hệ hai chiều cho media
        if (product.getMedia() != null) {
            for (ProductMedia media : product.getMedia()) {
                media.setProduct(product);
            }
        }
        
        // Thiết lập quan hệ hai chiều cho specifications
        if (product.getSpecifications() != null) {
            for (ProductSpecification spec : product.getSpecifications()) {
                spec.setProduct(product);
            }
        }
        
        return productRepository.save(product);
    }
    
    // Cập nhật sản phẩm (chỉ thông tin cơ bản)
    public Product updateProduct(Long id, Product productDetails) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
        
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setStockQuantity(productDetails.getStockQuantity());
        product.setCategory(productDetails.getCategory());
        product.setBrand(productDetails.getBrand());
        product.setIsActive(productDetails.getIsActive());
        
        return productRepository.save(product);
    }
    
    // Cập nhật sản phẩm với media (thông tin + hình ảnh/video)
    public Product updateProductWithMedia(Long id, Product productDetails, MultipartFile[] files, boolean replaceMedia) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
        
        // Cập nhật thông tin cơ bản
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setStockQuantity(productDetails.getStockQuantity());
        product.setCategory(productDetails.getCategory());
        product.setBrand(productDetails.getBrand());
        product.setIsActive(productDetails.getIsActive());
        
        // Xử lý media nếu có files mới
        if (files != null && files.length > 0) {
            // Xóa media cũ nếu replaceMedia = true
            if (replaceMedia) {
                productMediaService.deleteAllProductMedia(id);
            }
            
            // Upload media mới
            productMediaService.uploadProductMedia(id, files, false);
        }
        
        return productRepository.save(product);
    }
    
    // Xóa sản phẩm
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
        
        // Xóa tất cả media (bao gồm cả file vật lý) trước khi xóa product
        productMediaService.deleteAllProductMedia(id);
        
        // Xóa product
        productRepository.delete(product);
    }
    
    // Tìm kiếm sản phẩm theo tên
    public List<Product> searchProductsByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }
    
    // Lấy sản phẩm theo danh mục
    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }
    
    // Lấy sản phẩm theo thương hiệu
    public List<Product> getProductsByBrand(Long brandId) {
        return productRepository.findByBrandId(brandId);
    }
    
    // Lấy sản phẩm đang hoạt động
    public List<Product> getActiveProducts() {
        return productRepository.findByIsActiveTrue();
    }
    
    // Lấy sản phẩm theo khoảng giá
    public List<Product> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return productRepository.findByPriceBetween(minPrice, maxPrice);
    }
}

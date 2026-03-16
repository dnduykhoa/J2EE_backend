package j2ee_backend.nhom05.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import j2ee_backend.nhom05.model.Category;
import j2ee_backend.nhom05.model.Product;
import j2ee_backend.nhom05.model.ProductMedia;
import j2ee_backend.nhom05.model.ProductSpecification;
import j2ee_backend.nhom05.model.ProductStatus;
import j2ee_backend.nhom05.repository.ICategoryRepository;
import j2ee_backend.nhom05.repository.IProductRepository;

@Service
public class ProductService {

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private ICategoryRepository categoryRepository;

    @Autowired
    private ProductMediaService productMediaService;

    @Autowired
    private SseService sseService;

    // Lấy tất cả sản phẩm
    public List<Product> getAllProducts() {
        return deduplicateById(productRepository.findAll());
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
        product.setStatus(productDetails.getStatus());

        // Tự động đồng bộ trạng thái dựa trên tồn kho
        if (product.getStockQuantity() > 0 && product.getStatus() == ProductStatus.OUT_OF_STOCK) {
            product.setStatus(ProductStatus.ACTIVE);
        } else if (product.getStockQuantity() <= 0 && product.getStatus() == ProductStatus.ACTIVE) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        }

        Product saved = productRepository.save(product);
        sseService.broadcastProductUpdate(saved.getId(), saved.getStatus().name(), saved.getStockQuantity());
        return saved;
    }

    // Cập nhật sản phẩm với media (thông tin + hình ảnh/video)
    public Product updateProductWithMedia(Long id, Product productDetails, MultipartFile[] files,
            boolean replaceMedia) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));

        // Cập nhật thông tin cơ bản
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setStockQuantity(productDetails.getStockQuantity());
        product.setCategory(productDetails.getCategory());
        product.setBrand(productDetails.getBrand());
        product.setStatus(productDetails.getStatus());

        // Tự động đồng bộ trạng thái dựa trên tồn kho
        if (product.getStockQuantity() > 0 && product.getStatus() == ProductStatus.OUT_OF_STOCK) {
            product.setStatus(ProductStatus.ACTIVE);
        } else if (product.getStockQuantity() <= 0 && product.getStatus() == ProductStatus.ACTIVE) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        }

        // Xử lý media nếu có files mới
        if (files != null && files.length > 0) {
            // Xóa media cũ nếu replaceMedia = true
            if (replaceMedia) {
                productMediaService.deleteAllProductMedia(id);
            }

            // Upload media mới
            productMediaService.uploadProductMedia(id, files, false);
        }

        Product saved = productRepository.save(product);
        sseService.broadcastProductUpdate(saved.getId(), saved.getStatus().name(), saved.getStockQuantity());
        return saved;
    }

    // Ngừng kinh doanh sản phẩm (xóa mềm - chuyển status = INACTIVE)
    public Product deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
        product.setStatus(ProductStatus.INACTIVE);
        Product saved = productRepository.save(product);
        sseService.broadcastProductUpdate(saved.getId(), "INACTIVE", saved.getStockQuantity());
        return saved;
    }

    // Đánh dấu hàng sắp về (chuyển status = OUT_OF_STOCK)
    public Product markOutOfStock(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
        product.setStatus(ProductStatus.OUT_OF_STOCK);
        Product saved = productRepository.save(product);
        sseService.broadcastProductUpdate(saved.getId(), "OUT_OF_STOCK", saved.getStockQuantity());
        return saved;
    }

    // Kích hoạt lại sản phẩm (chuyển status = ACTIVE)
    public Product restoreProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
        product.setStatus(ProductStatus.ACTIVE);
        Product saved = productRepository.save(product);
        sseService.broadcastProductUpdate(saved.getId(), "ACTIVE", saved.getStockQuantity());
        return saved;
    }

    // Tìm kiếm sản phẩm theo tên
    public List<Product> searchProductsByName(String name) {
        return deduplicateById(productRepository.findByNameContainingIgnoreCase(name));
    }

    // Lấy sản phẩm theo danh mục
    public List<Product> getProductsByCategory(Long categoryId) {
        return deduplicateById(productRepository.findByCategoryId(categoryId));
    }

    // Lấy sản phẩm theo thương hiệu
    public List<Product> getProductsByBrand(Long brandId) {
        return deduplicateById(productRepository.findByBrandId(brandId));
    }

    // Lấy sản phẩm đang hoạt động (status = ACTIVE)
    public List<Product> getActiveProducts() {
        return deduplicateById(productRepository.findByStatus(ProductStatus.ACTIVE));
    }

    // Lấy sản phẩm hàng sắp về (status = OUT_OF_STOCK)
    public List<Product> getOutOfStockProducts() {
        return deduplicateById(productRepository.findByStatus(ProductStatus.OUT_OF_STOCK));
    }

    // Lấy sản phẩm ngừng kinh doanh (status = INACTIVE)
    public List<Product> getInactiveProducts() {
        return deduplicateById(productRepository.findByStatus(ProductStatus.INACTIVE));
    }

    // Lấy sản phẩm theo khoảng giá
    public List<Product> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return deduplicateById(productRepository.findByPriceBetween(minPrice, maxPrice));
    }

    // Lấy sản phẩm theo danh sách nhiều categoryId
    public List<Product> getProductsByCategories(List<Long> categoryIds) {
        return deduplicateById(productRepository.findByCategoryIdIn(categoryIds));
    }

    // Lấy sản phẩm theo danh mục và tất cả danh mục con của nó
    public List<Product> getProductsByCategoryTree(Long categoryId) {
        List<Long> ids = new ArrayList<>();
        ids.add(categoryId);
        // Lấy tất cả danh mục con trực tiếp
        List<Category> children = categoryRepository.findByParentId(categoryId);
        for (Category child : children) {
            ids.add(child.getId());
        }
        return deduplicateById(productRepository.findByCategoryIdIn(ids));
    }

    // Lọc sản phẩm theo nhiều tiêu chí kết hợp (danh mục, thương hiệu, giá, tên)
    public List<Product> filterProducts(List<Long> categoryIds, List<Long> brandIds,
            BigDecimal minPrice, BigDecimal maxPrice, String name) {
        return deduplicateById(productRepository.findAll(
                ProductFilterSpec.build(categoryIds, brandIds, minPrice, maxPrice, name)));
    }

    private List<Product> deduplicateById(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return products;
        }
        return new ArrayList<>(new LinkedHashMap<>(
                products.stream().collect(java.util.stream.Collectors.toMap(
                        Product::getId,
                        product -> product,
                        (first, second) -> first,
                        LinkedHashMap::new)))
                .values());
    }
}

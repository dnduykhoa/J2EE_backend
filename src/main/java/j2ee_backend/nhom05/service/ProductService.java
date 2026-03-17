package j2ee_backend.nhom05.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Autowired
    private PreorderRequestService preorderRequestService;

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

        normalizeStatusByStock(product, null, true);
        Product saved = productRepository.save(product);
        preorderRequestService.notifyProductAvailability(saved);
        return saved;
    }

    // Cập nhật sản phẩm (chỉ thông tin cơ bản)
    public Product updateProduct(Long id, Product productDetails) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));

        ProductStatus previousStatus = product.getStatus();

        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setStockQuantity(productDetails.getStockQuantity());
        product.setCategory(productDetails.getCategory());
        product.setBrand(productDetails.getBrand());
        product.setStatus(productDetails.getStatus());

        normalizeStatusByStock(product, previousStatus, false);

        Product saved = productRepository.save(product);
        sseService.broadcastProductUpdate(saved.getId(), saved.getStatus().name(), saved.getStockQuantity());
        preorderRequestService.notifyProductAvailability(saved);
        return saved;
    }

    // Cập nhật sản phẩm với media (thông tin + hình ảnh/video)
    public Product updateProductWithMedia(Long id, Product productDetails, MultipartFile[] files,
            boolean replaceMedia) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));

        ProductStatus previousStatus = product.getStatus();

        // Cập nhật thông tin cơ bản
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setStockQuantity(productDetails.getStockQuantity());
        product.setCategory(productDetails.getCategory());
        product.setBrand(productDetails.getBrand());
        product.setStatus(productDetails.getStatus());

        normalizeStatusByStock(product, previousStatus, false);

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
        preorderRequestService.notifyProductAvailability(saved);
        return saved;
    }

    // Ngừng kinh doanh sản phẩm (xóa mềm - chuyển status = INACTIVE)
    public Product deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
        product.setStatus(ProductStatus.INACTIVE);
        product.setNewArrivalAt(null);
        Product saved = productRepository.save(product);
        sseService.broadcastProductUpdate(saved.getId(), "INACTIVE", saved.getStockQuantity());
        return saved;
    }

    // Đánh dấu hàng sắp về (chuyển status = OUT_OF_STOCK)
    // Đánh dấu hàng mới về (chuyển status = NEW_ARRIVAL)
    public Product markNewArrival(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
        if (product.getStatus() != ProductStatus.OUT_OF_STOCK) {
            throw new RuntimeException("Chỉ có thể đánh dấu hàng mới về từ trạng thái Hàng sắp về");
        }
        if (product.getStockQuantity() == null || product.getStockQuantity() <= 0) {
            throw new RuntimeException("Cần nhập tồn kho lớn hơn 0 trước khi chuyển sang Hàng mới về");
        }
        product.setStatus(ProductStatus.NEW_ARRIVAL);
        product.setNewArrivalAt(LocalDateTime.now());
        Product saved = productRepository.save(product);
        sseService.broadcastProductUpdate(saved.getId(), "NEW_ARRIVAL", saved.getStockQuantity());
        preorderRequestService.notifyProductAvailability(saved);
        return saved;
    }

    // Đánh dấu hết hàng (chuyển status = OUT_OF_STOCK)
    public Product markOutOfStock(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
        if (product.getStockQuantity() != null && product.getStockQuantity() > 0) {
            throw new RuntimeException("Không thể gán Hàng sắp về khi tồn kho vẫn lớn hơn 0");
        }
        product.setStatus(ProductStatus.OUT_OF_STOCK);
        product.setNewArrivalAt(null);
        Product saved = productRepository.save(product);
        sseService.broadcastProductUpdate(saved.getId(), "OUT_OF_STOCK", saved.getStockQuantity());
        return saved;
    }

    // Kích hoạt lại sản phẩm (chuyển status = ACTIVE)
    public Product restoreProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
        product.setStatus(ProductStatus.ACTIVE);
        product.setNewArrivalAt(null);
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
    // Lấy sản phẩm hàng mới về (status = NEW_ARRIVAL)
    public List<Product> getNewArrivalProducts() {
        return deduplicateById(productRepository.findByStatus(ProductStatus.NEW_ARRIVAL));
    }

    // Lấy sản phẩm hết hàng (status = OUT_OF_STOCK)
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

    private void normalizeStatusByStock(Product product, ProductStatus previousStatus, boolean isCreate) {
        if (product.getStatus() == ProductStatus.INACTIVE) {
            product.setNewArrivalAt(null);
            return;
        }

        Integer stockValue = product.getStockQuantity();
        int stock = stockValue != null ? stockValue.intValue() : 0;
        if (stock <= 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
            product.setNewArrivalAt(null);
            return;
        }

        if (isCreate) {
            product.setStatus(ProductStatus.NEW_ARRIVAL);
            product.setNewArrivalAt(LocalDateTime.now());
            return;
        }

        if (previousStatus == ProductStatus.OUT_OF_STOCK) {
            product.setStatus(ProductStatus.NEW_ARRIVAL);
            product.setNewArrivalAt(LocalDateTime.now());
            return;
        }

        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.ACTIVE);
        }

        if (product.getStatus() == ProductStatus.NEW_ARRIVAL) {
            if (product.getNewArrivalAt() == null) {
                product.setNewArrivalAt(LocalDateTime.now());
            }
        } else {
            product.setNewArrivalAt(null);
        }
    }

    public void expireNewArrivalProducts() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(5);
        List<Product> expiredProducts = productRepository.findByStatusAndNewArrivalAtBefore(
            ProductStatus.NEW_ARRIVAL,
            threshold
        );
        for (Product product : expiredProducts) {
            product.setStatus(ProductStatus.ACTIVE);
            product.setNewArrivalAt(null);
            Product saved = productRepository.save(product);
            sseService.broadcastProductUpdate(saved.getId(), "ACTIVE", saved.getStockQuantity());
        }
    }
}

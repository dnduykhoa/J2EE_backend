package j2ee_backend.nhom05.service;

import j2ee_backend.nhom05.model.Product;
import j2ee_backend.nhom05.model.ProductMedia;
import j2ee_backend.nhom05.repository.IProductMediaRepository;
import j2ee_backend.nhom05.repository.IProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductMediaService {
    
    @Autowired
    private IProductMediaRepository productMediaRepository;
    
    @Autowired
    private IProductRepository productRepository;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    /**
     * Upload media cho sản phẩm
     */
    @Transactional
    public List<ProductMedia> uploadProductMedia(Long productId, MultipartFile[] files, boolean isPrimary) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
        
        List<ProductMedia> uploadedMedia = new ArrayList<>();
        
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            
            // Xác định loại media
            String mediaType;
            if (fileStorageService.isImageFile(file)) {
                mediaType = "IMAGE";
            } else if (fileStorageService.isVideoFile(file)) {
                mediaType = "VIDEO";
            } else {
                throw new RuntimeException("File không hợp lệ. Chỉ chấp nhận hình ảnh hoặc video");
            }
            
            // Upload file và lấy đường dẫn
            String filePath = fileStorageService.storeFile(file, "products");
            
            // Tạo ProductMedia
            ProductMedia media = new ProductMedia();
            media.setProduct(product);
            media.setMediaUrl("/images/" + filePath);
            media.setMediaType(mediaType);
            media.setIsPrimary(isPrimary && i == 0); // Chỉ file đầu tiên được set làm primary
            media.setDisplayOrder(i);
            
            uploadedMedia.add(productMediaRepository.save(media));
        }
        
        return uploadedMedia;
    }
    
    /**
     * Xóa media của sản phẩm
     */
    @Transactional
    public void deleteProductMedia(Long mediaId) {
        ProductMedia media = productMediaRepository.findById(mediaId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy media"));
        
        // Xóa file vật lý
        String filePath = media.getMediaUrl().replace("/images/", "");
        fileStorageService.deleteFile(filePath);
        
        // Xóa record trong DB
        productMediaRepository.delete(media);
    }
    
    /**
     * Lấy tất cả media của sản phẩm
     */
    public List<ProductMedia> getProductMedia(Long productId) {
        return productMediaRepository.findByProductId(productId);
    }
    
    /**
     * Xóa tất cả media của sản phẩm
     */
    @Transactional
    public void deleteAllProductMedia(Long productId) {
        List<ProductMedia> mediaList = productMediaRepository.findByProductId(productId);
        
        for (ProductMedia media : mediaList) {
            // Xóa file vật lý
            String filePath = media.getMediaUrl().replace("/images/", "");
            fileStorageService.deleteFile(filePath);
        }
        
        // Xóa tất cả record trong DB
        productMediaRepository.deleteByProductId(productId);
    }

    /**
     * Đặt một media làm ảnh chính (isPrimary = true).
     * Tự động bỏ chọn primary của các media khác cùng sản phẩm.
     */
    @Transactional
    public ProductMedia setPrimaryMedia(Long productId, Long mediaId) {
        ProductMedia target = productMediaRepository.findById(mediaId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy media với ID: " + mediaId));
        if (!target.getProduct().getId().equals(productId)) {
            throw new RuntimeException("Media không thuộc sản phẩm này");
        }
        // Bỏ primary tất cả media cùng sản phẩm
        List<ProductMedia> all = productMediaRepository.findByProductId(productId);
        for (ProductMedia m : all) {
            m.setIsPrimary(false);
        }
        productMediaRepository.saveAll(all);
        // Đặt target làm primary
        target.setIsPrimary(true);
        return productMediaRepository.save(target);
    }
}

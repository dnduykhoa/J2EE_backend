package j2ee_backend.nhom05.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200, columnDefinition = "NVARCHAR(200)")
    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    @Column(name = "description", columnDefinition = "NTEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 18, scale = 2)
    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    @Min(value = 0, message = "Số lượng không được âm")
    private Integer stockQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @ToString.Exclude
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    @ToString.Exclude
    private Brand brand;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<ProductMedia> media = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<ProductSpecification> specifications = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Phương thức lấy ảnh chính
    public ProductMedia getPrimaryImage() {
        return media.stream()
            .filter(m -> "IMAGE".equals(m.getMediaType()) && Boolean.TRUE.equals(m.getIsPrimary()))
            .findFirst()
            .orElse(null);
    }
    
    // Phương thức lấy tất cả ảnh phụ
    public List<ProductMedia> getSecondaryImages() {
        return media.stream()
            .filter(m -> "IMAGE".equals(m.getMediaType()) && !Boolean.TRUE.equals(m.getIsPrimary()))
            .toList();
    }
    
    // Phương thức lấy tất cả ảnh (chính + phụ)
    public List<ProductMedia> getAllImages() {
        return media.stream()
            .filter(m -> "IMAGE".equals(m.getMediaType()))
            .toList();
    }
    
    // Phương thức lấy tất cả video
    public List<ProductMedia> getVideos() {
        return media.stream()
            .filter(m -> "VIDEO".equals(m.getMediaType()))
            .toList();
    }
}

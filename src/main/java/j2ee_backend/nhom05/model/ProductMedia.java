package j2ee_backend.nhom05.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "product_media")
public class ProductMedia {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;
    
    @Column(name = "media_url", nullable = false, length = 500)
    private String mediaUrl;
    
    @Column(name = "media_type", nullable = false, length = 20)
    private String mediaType; // "IMAGE" hoặc "VIDEO"
    
    @Column(name = "is_primary")
    private Boolean isPrimary = false; // Ảnh chính của sản phẩm
    
    @Column(name = "display_order")
    private Integer displayOrder = 0;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

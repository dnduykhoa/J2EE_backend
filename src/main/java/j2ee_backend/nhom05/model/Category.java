package j2ee_backend.nhom05.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "categories")
public class Category {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    @NotBlank(message = "Tên danh mục không được để trống")
    private String name;
    
    @Column(name = "description", columnDefinition = "NTEXT")
    private String description;
    
    @Column(name = "display_order")
    private Integer displayOrder = 0;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    // Danh mục cha (null nếu là danh mục gốc)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @ToString.Exclude
    @JsonIgnore
    private Category parent;
    
    // Danh mục con
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @ToString.Exclude
    @JsonIgnore
    private List<Category> children = new ArrayList<>();
    
    // Sản phẩm thuộc danh mục này
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    @ToString.Exclude
    @JsonIgnore
    private List<Product> products = new ArrayList<>();
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Kiểm tra có phải danh mục gốc không
    public boolean isRoot() {
        return parent == null;
    }
    
    // Lấy cấp độ của danh mục (0 = root)
    public int getLevel() {
        int level = 0;
        Category current = this.parent;
        while (current != null) {
            level++;
            current = current.getParent();
        }
        return level;
    }
}

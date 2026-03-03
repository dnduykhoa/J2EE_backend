package j2ee_backend.nhom05.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "product_specifications")
public class ProductSpecification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;
    
    @Column(name = "spec_key", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String specKey; // Ví dụ: "CPU", "RAM", "Mainboard", "Thương hiệu"...
    
    @Column(name = "spec_value", nullable = false, columnDefinition = "NVARCHAR(500)")
    private String specValue; // Ví dụ: "AMD Ryzen 5 5600X", "8GB DDR4", "Asus"...
    
    @Column(name = "display_order")
    private Integer displayOrder = 0; // Thứ tự hiển thị
}

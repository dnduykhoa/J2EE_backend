package j2ee_backend.nhom05.dto.pcbuilder;

import java.math.BigDecimal;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PcBuilderOptionItemDto {
    private Long productId;
    private String name;
    private BigDecimal price;
    private Integer stockQuantity;
    private String brandName;
    private String categoryName;
    private Map<String, String> keySpecs;
}

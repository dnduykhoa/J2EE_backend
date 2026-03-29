package j2ee_backend.nhom05.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAttributeSchemaItemDto {

    private Long categoryAttributeId;
    private Long attrDefId;
    private String attrKey;
    private String attrName;
    private Boolean isRequired;
    private Integer displayOrder;
}

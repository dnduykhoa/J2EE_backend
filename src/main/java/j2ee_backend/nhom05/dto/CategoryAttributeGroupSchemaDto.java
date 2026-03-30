package j2ee_backend.nhom05.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAttributeGroupSchemaDto {

    private Long groupId;
    private String groupName;
    private Integer groupDisplayOrder;
    private List<CategoryAttributeSchemaItemDto> items = new ArrayList<>();
}

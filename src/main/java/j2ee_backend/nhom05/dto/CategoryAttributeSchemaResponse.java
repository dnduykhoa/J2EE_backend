package j2ee_backend.nhom05.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAttributeSchemaResponse {

    private Long categoryId;
    private List<CategoryAttributeGroupSchemaDto> groups = new ArrayList<>();
}

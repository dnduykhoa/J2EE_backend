package j2ee_backend.nhom05.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import j2ee_backend.nhom05.dto.CategoryAttributeGroupSchemaDto;
import j2ee_backend.nhom05.dto.CategoryAttributeSchemaItemDto;
import j2ee_backend.nhom05.dto.CategoryAttributeSchemaResponse;
import j2ee_backend.nhom05.model.AttributeDefinition;
import j2ee_backend.nhom05.model.AttributeGroup;
import j2ee_backend.nhom05.model.Category;
import j2ee_backend.nhom05.model.CategoryAttribute;
import j2ee_backend.nhom05.repository.IAttributeDefinitionRepository;
import j2ee_backend.nhom05.repository.IAttributeGroupRepository;
import j2ee_backend.nhom05.repository.ICategoryAttributeRepository;
import j2ee_backend.nhom05.repository.ICategoryRepository;

@Service
public class CategoryAttributeService {

    @Autowired
    private ICategoryAttributeRepository categoryAttributeRepository;

    @Autowired
    private ICategoryRepository categoryRepository;

    @Autowired
    private IAttributeDefinitionRepository attributeDefinitionRepository;

    @Autowired
    private IAttributeGroupRepository attributeGroupRepository;

    /** Lấy tất cả thuộc tính của một danh mục (đã sắp xếp theo display_order). */
    public List<CategoryAttribute> getAttributesByCategory(Long categoryId) {
        categoryRepository.findById(categoryId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + categoryId));
        return categoryAttributeRepository.findByCategoryIdOrderByDisplayOrderAsc(categoryId);
    }

    /** Gán một thuộc tính vào danh mục. */
    public CategoryAttribute assignAttribute(Long categoryId, Long attrDefId,
                                              Boolean isRequired, Integer displayOrder,
                                              Long groupId) {
        if (categoryAttributeRepository.existsByCategoryIdAndAttributeDefinitionId(categoryId, attrDefId)) {
            throw new RuntimeException("Thuộc tính này đã được gán cho danh mục.");
        }
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + categoryId));
        AttributeDefinition def = attributeDefinitionRepository.findById(attrDefId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy thuộc tính với ID: " + attrDefId));

        CategoryAttribute ca = new CategoryAttribute();
        ca.setCategory(category);
        ca.setAttributeDefinition(def);
        ca.setIsRequired(isRequired != null ? isRequired : false);
        ca.setDisplayOrder(displayOrder != null ? displayOrder : 0);
        ca.setAttributeGroup(resolveGroup(groupId));
        return categoryAttributeRepository.save(ca);
    }

    /** Cập nhật is_required / display_order / group của một liên kết Category-Attribute. */
    public CategoryAttribute updateAssignment(Long id, Boolean isRequired, Integer displayOrder, Long groupId) {
        CategoryAttribute ca = categoryAttributeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy liên kết với ID: " + id));
        if (isRequired != null) ca.setIsRequired(isRequired);
        if (displayOrder != null) ca.setDisplayOrder(displayOrder);
        if (groupId != null) {
            ca.setAttributeGroup(resolveGroup(groupId));
        }
        return categoryAttributeRepository.save(ca);
    }

    /** Trả về schema theo category đã group để frontend admin render trực tiếp. */
    public CategoryAttributeSchemaResponse getGroupedSchemaByCategory(Long categoryId) {
        List<CategoryAttribute> assignments = getAttributesByCategory(categoryId);

        Map<String, CategoryAttributeGroupSchemaDto> groupsByKey = new LinkedHashMap<>();

        for (CategoryAttribute assignment : assignments) {
            AttributeDefinition definition = assignment.getAttributeDefinition();
            AttributeGroup effectiveGroup = resolveEffectiveGroup(assignment);

            Long groupId = effectiveGroup != null ? effectiveGroup.getId() : null;
            String groupName = effectiveGroup != null ? effectiveGroup.getName() : "Chưa phân nhóm";
            Integer groupDisplayOrder = effectiveGroup != null && effectiveGroup.getDisplayOrder() != null
                ? effectiveGroup.getDisplayOrder() : Integer.MAX_VALUE;

            String groupKey = groupId != null ? "GROUP_" + groupId : "UNGROUPED";
            CategoryAttributeGroupSchemaDto groupDto = groupsByKey.computeIfAbsent(groupKey,
                key -> new CategoryAttributeGroupSchemaDto(groupId, groupName, groupDisplayOrder, new ArrayList<>()));

            CategoryAttributeSchemaItemDto itemDto = new CategoryAttributeSchemaItemDto(
                assignment.getId(),
                definition != null ? definition.getId() : null,
                definition != null ? definition.getAttrKey() : null,
                definition != null ? definition.getName() : null,
                assignment.getIsRequired(),
                assignment.getDisplayOrder()
            );
            groupDto.getItems().add(itemDto);
        }

        List<CategoryAttributeGroupSchemaDto> groups = new ArrayList<>(groupsByKey.values());
        for (CategoryAttributeGroupSchemaDto group : groups) {
            group.getItems().sort(Comparator
                .comparing((CategoryAttributeSchemaItemDto item) -> item.getDisplayOrder() == null
                    ? Integer.MAX_VALUE : item.getDisplayOrder())
                .thenComparing(item -> item.getAttrName() == null ? "" : item.getAttrName(), String.CASE_INSENSITIVE_ORDER));
        }
        groups.sort(Comparator
            .comparing((CategoryAttributeGroupSchemaDto group) -> group.getGroupDisplayOrder() == null
                ? Integer.MAX_VALUE : group.getGroupDisplayOrder())
            .thenComparing(group -> group.getGroupName() == null ? "" : group.getGroupName(), String.CASE_INSENSITIVE_ORDER));

        return new CategoryAttributeSchemaResponse(categoryId, groups);
    }

    /** Gỡ bỏ thuộc tính khỏi danh mục. */
    public void removeAttribute(Long id) {
        CategoryAttribute ca = categoryAttributeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy liên kết với ID: " + id));
        categoryAttributeRepository.delete(ca);
    }

    /** Gỡ bỏ thuộc tính khỏi danh mục theo categoryId + attrDefId. */
    public void removeAttributeByCategoryAndDef(Long categoryId, Long attrDefId) {
        CategoryAttribute ca = categoryAttributeRepository
            .findByCategoryIdAndAttributeDefinitionId(categoryId, attrDefId)
            .orElseThrow(() -> new RuntimeException("Thuộc tính này chưa được gán cho danh mục."));
        categoryAttributeRepository.delete(ca);
    }

    private AttributeGroup resolveGroup(Long groupId) {
        if (groupId == null) {
            return null;
        }
        return attributeGroupRepository.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm thuộc tính với ID: " + groupId));
    }

    private AttributeGroup resolveEffectiveGroup(CategoryAttribute assignment) {
        if (assignment.getAttributeGroup() != null) {
            return assignment.getAttributeGroup();
        }
        if (assignment.getAttributeDefinition() != null) {
            return assignment.getAttributeDefinition().getAttributeGroup();
        }
        return null;
    }
}

package j2ee_backend.nhom05.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import j2ee_backend.nhom05.dto.CategoryAttributeSchemaResponse;
import j2ee_backend.nhom05.model.AttributeDefinition;
import j2ee_backend.nhom05.model.AttributeGroup;
import j2ee_backend.nhom05.model.Category;
import j2ee_backend.nhom05.model.CategoryAttribute;
import j2ee_backend.nhom05.repository.IAttributeDefinitionRepository;
import j2ee_backend.nhom05.repository.IAttributeGroupRepository;
import j2ee_backend.nhom05.repository.ICategoryAttributeRepository;
import j2ee_backend.nhom05.repository.ICategoryRepository;

@ExtendWith(MockitoExtension.class)
class CategoryAttributeServiceTest {

    @Mock
    private ICategoryAttributeRepository categoryAttributeRepository;

    @Mock
    private ICategoryRepository categoryRepository;

    @Mock
    private IAttributeDefinitionRepository attributeDefinitionRepository;

    @Mock
    private IAttributeGroupRepository attributeGroupRepository;

    @InjectMocks
    private CategoryAttributeService categoryAttributeService;

    private Category category;
    private AttributeDefinition definition;
    private AttributeGroup group;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(10L);

        definition = new AttributeDefinition();
        definition.setId(20L);
        definition.setAttrKey("ram");
        definition.setName("RAM");

        group = new AttributeGroup();
        group.setId(30L);
        group.setName("Performance");
        group.setDisplayOrder(1);
    }

    @Test
    void assignAttribute_WithGroupId_ShouldAttachGroupOverride() {
        when(categoryAttributeRepository.existsByCategoryIdAndAttributeDefinitionId(10L, 20L)).thenReturn(false);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(attributeDefinitionRepository.findById(20L)).thenReturn(Optional.of(definition));
        when(attributeGroupRepository.findById(30L)).thenReturn(Optional.of(group));
        when(categoryAttributeRepository.save(any(CategoryAttribute.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CategoryAttribute result = categoryAttributeService.assignAttribute(10L, 20L, true, 2, 30L);

        assertSame(category, result.getCategory());
        assertSame(definition, result.getAttributeDefinition());
        assertSame(group, result.getAttributeGroup());
        assertEquals(true, result.getIsRequired());
        assertEquals(2, result.getDisplayOrder());
    }

    @Test
    void updateAssignment_WithGroupId_ShouldUpdateGroupAndFlags() {
        CategoryAttribute existing = new CategoryAttribute();
        existing.setId(100L);
        existing.setIsRequired(false);
        existing.setDisplayOrder(0);

        when(categoryAttributeRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(attributeGroupRepository.findById(30L)).thenReturn(Optional.of(group));
        when(categoryAttributeRepository.save(any(CategoryAttribute.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CategoryAttribute result = categoryAttributeService.updateAssignment(100L, true, 5, 30L);

        assertEquals(true, result.getIsRequired());
        assertEquals(5, result.getDisplayOrder());
        assertSame(group, result.getAttributeGroup());
    }

    @Test
    void getGroupedSchemaByCategory_ShouldFallbackToDefinitionGroup_WhenOverrideIsNull() {
        CategoryAttribute withFallbackGroup = new CategoryAttribute();
        withFallbackGroup.setId(101L);
        withFallbackGroup.setCategory(category);
        withFallbackGroup.setAttributeDefinition(definition);
        withFallbackGroup.setAttributeGroup(null);
        withFallbackGroup.setIsRequired(true);
        withFallbackGroup.setDisplayOrder(1);

        AttributeDefinition noGroupDef = new AttributeDefinition();
        noGroupDef.setId(21L);
        noGroupDef.setAttrKey("wireless");
        noGroupDef.setName("Wireless");
        noGroupDef.setAttributeGroup(null);

        CategoryAttribute ungrouped = new CategoryAttribute();
        ungrouped.setId(102L);
        ungrouped.setCategory(category);
        ungrouped.setAttributeDefinition(noGroupDef);
        ungrouped.setAttributeGroup(null);
        ungrouped.setIsRequired(false);
        ungrouped.setDisplayOrder(2);

        definition.setAttributeGroup(group);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(categoryAttributeRepository.findByCategoryIdOrderByDisplayOrderAsc(10L))
            .thenReturn(List.of(withFallbackGroup, ungrouped));

        CategoryAttributeSchemaResponse response = categoryAttributeService.getGroupedSchemaByCategory(10L);

        assertNotNull(response);
        assertEquals(10L, response.getCategoryId());
        assertEquals(2, response.getGroups().size());

        var firstGroup = response.getGroups().get(0);
        assertEquals(30L, firstGroup.getGroupId());
        assertEquals("Performance", firstGroup.getGroupName());
        assertEquals(1, firstGroup.getItems().size());
        assertEquals("ram", firstGroup.getItems().get(0).getAttrKey());

        var secondGroup = response.getGroups().get(1);
        assertNull(secondGroup.getGroupId());
        assertEquals("Chưa phân nhóm", secondGroup.getGroupName());
        assertEquals(1, secondGroup.getItems().size());
        assertEquals("wireless", secondGroup.getItems().get(0).getAttrKey());
    }
}

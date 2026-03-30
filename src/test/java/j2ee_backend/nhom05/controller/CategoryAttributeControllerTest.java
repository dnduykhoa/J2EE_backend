package j2ee_backend.nhom05.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.dto.CategoryAttributeSchemaResponse;
import j2ee_backend.nhom05.model.CategoryAttribute;
import j2ee_backend.nhom05.service.CategoryAttributeService;

@ExtendWith(MockitoExtension.class)
class CategoryAttributeControllerTest {

    @Mock
    private CategoryAttributeService categoryAttributeService;

    @InjectMocks
    private CategoryAttributeController categoryAttributeController;

    @Test
    void getSchemaByCategory_ShouldReturnGroupedSchema() throws Exception {
        CategoryAttributeSchemaResponse schema = new CategoryAttributeSchemaResponse(1L, new ArrayList<>());
        when(categoryAttributeService.getGroupedSchemaByCategory(1L)).thenReturn(schema);

        ResponseEntity<?> response = categoryAttributeController.getSchemaByCategory(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponse payload = (ApiResponse) response.getBody();
        assertNotNull(payload);
        assertEquals("Lấy schema thuộc tính theo danh mục thành công", payload.getMessage());
        CategoryAttributeSchemaResponse body = (CategoryAttributeSchemaResponse) payload.getData();
        assertEquals(1L, body.getCategoryId());
    }

    @Test
    void assign_ShouldAcceptOptionalGroupId() throws Exception {
        CategoryAttribute saved = new CategoryAttribute();
        saved.setId(99L);

        when(categoryAttributeService.assignAttribute(1L, 2L, true, 3, 4L)).thenReturn(saved);

        ResponseEntity<?> response = categoryAttributeController.assign(1L, 2L, true, 3, 4L);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        ApiResponse payload = (ApiResponse) response.getBody();
        assertNotNull(payload);
        assertEquals("Gán thuộc tính vào danh mục thành công", payload.getMessage());
        CategoryAttribute body = (CategoryAttribute) payload.getData();
        assertEquals(99L, body.getId());

        verify(categoryAttributeService).assignAttribute(1L, 2L, true, 3, 4L);
    }

    @Test
    void update_ShouldAcceptOptionalGroupId() throws Exception {
        CategoryAttribute updated = new CategoryAttribute();
        updated.setId(55L);

        when(categoryAttributeService.updateAssignment(55L, false, 8, 7L)).thenReturn(updated);

        ResponseEntity<?> response = categoryAttributeController.update(55L, false, 8, 7L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponse payload = (ApiResponse) response.getBody();
        assertNotNull(payload);
        assertEquals("Cập nhật liên kết thành công", payload.getMessage());
        CategoryAttribute body = (CategoryAttribute) payload.getData();
        assertEquals(55L, body.getId());

        verify(categoryAttributeService).updateAssignment(55L, false, 8, 7L);
    }
}

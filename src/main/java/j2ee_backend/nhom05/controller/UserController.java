package j2ee_backend.nhom05.controller;

import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.dto.auth.UpdateProfileRequest;
import j2ee_backend.nhom05.dto.auth.UserProfileResponse;
import j2ee_backend.nhom05.model.Role;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserService userService;

    // API lấy profile theo ID
    @GetMapping("/profile/{id}")
    public ResponseEntity<?> getProfile(@PathVariable Long id) {
        try {
            User user = userService.findById(id);
            
            UserProfileResponse response = new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getBirthDate(),
                user.getProvider(),                user.isTwoFactorEnabled(),                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
            );
            
            return ResponseEntity.ok(new ApiResponse("Lấy thông tin profile thành công", response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
    
    // API cập nhật profile
    @PutMapping("/profile/{id}")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @Valid @RequestBody UpdateProfileRequest request) {
        try {
            User user = userService.updateProfile(id, request);
            
            UserProfileResponse response = new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getBirthDate(),
                user.getProvider(),                user.isTwoFactorEnabled(),                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
            );
            
            return ResponseEntity.ok(new ApiResponse("Cập nhật profile thành công", response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // -----------------------------------------------------------------------
    // Admin APIs — quản lý người dùng
    // -----------------------------------------------------------------------

    // Lấy tất cả users (admin)
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            List<UserProfileResponse> result = users.stream()
                .map(u -> new UserProfileResponse(
                    u.getId(), u.getUsername(), u.getEmail(), u.getFullName(),
                    u.getPhone(), u.getBirthDate(), u.getProvider(),
                    u.isTwoFactorEnabled(),
                    u.getRoles().stream().map(Role::getName).collect(Collectors.toSet())))
                .toList();
            return ResponseEntity.ok(new ApiResponse("Lấy danh sách user thành công", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Tìm kiếm user theo username / email / họ tên (admin)
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String keyword) {
        try {
            List<User> users = userService.searchUsers(keyword);
            List<UserProfileResponse> result = users.stream()
                .map(u -> new UserProfileResponse(
                    u.getId(), u.getUsername(), u.getEmail(), u.getFullName(),
                    u.getPhone(), u.getBirthDate(), u.getProvider(),
                    u.isTwoFactorEnabled(),
                    u.getRoles().stream().map(Role::getName).collect(Collectors.toSet())))
                .toList();
            return ResponseEntity.ok(new ApiResponse("Tìm kiếm user thành công", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Xóa user theo ID (admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(new ApiResponse("Xóa user thành công", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // Cập nhật role cho user (admin)
    // Body: ["ADMIN", "USER"]
    @PutMapping("/{id}/roles")
    public ResponseEntity<?> updateUserRoles(@PathVariable Long id, @RequestBody Set<String> roleNames) {
        try {
            User user = userService.updateUserRoles(id, roleNames);
            UserProfileResponse response = new UserProfileResponse(
                user.getId(), user.getUsername(), user.getEmail(), user.getFullName(),
                user.getPhone(), user.getBirthDate(), user.getProvider(),
                user.isTwoFactorEnabled(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
            return ResponseEntity.ok(new ApiResponse("Cập nhật role thành công", response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
}

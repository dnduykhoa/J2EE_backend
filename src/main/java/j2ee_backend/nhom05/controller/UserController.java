package j2ee_backend.nhom05.controller;

import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.dto.auth.ChangePasswordRequest;
import j2ee_backend.nhom05.dto.auth.LoginRequest;
import j2ee_backend.nhom05.dto.auth.LoginResponse;
import j2ee_backend.nhom05.dto.auth.RegisterRequest;
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

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    // API đăng ký
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.register(request);
            
            LoginResponse response = new LoginResponse(
                "Đăng ký thành công",
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getBirthDate(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse("Đăng ký thành công", response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
    
    // API kiểm tra username đã tồn tại
    @GetMapping("/check-username/{username}")
    public ResponseEntity<?> checkUsername(@PathVariable String username) {
        try {
            userService.findByUsername(username);
            return ResponseEntity.ok().body(new ApiResponse("Username đã tồn tại", null));
        } catch (RuntimeException e) {
            return ResponseEntity.ok().body(new ApiResponse("Username khả dụng", null));
        }
    }
    
    // API kiểm tra email đã tồn tại
    @GetMapping("/check-email/{email}")
    public ResponseEntity<?> checkEmail(@PathVariable String email) {
        try {
            userService.findByEmail(email);
            return ResponseEntity.ok().body(new ApiResponse("Email đã được sử dụng", null));
        } catch (RuntimeException e) {
            return ResponseEntity.ok().body(new ApiResponse("Email khả dụng", null));
        }
    }
    
    // API đăng nhập
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            User user = userService.login(request);
            
            LoginResponse response = new LoginResponse(
                "Đăng nhập thành công",
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getBirthDate(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
            );
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
    
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
                user.getProvider(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
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
                user.getProvider(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
            );
            
            return ResponseEntity.ok(new ApiResponse("Cập nhật profile thành công", response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
    
    // API đổi mật khẩu
    @PutMapping("/change-password/{id}")
    public ResponseEntity<?> changePassword(@PathVariable Long id, @Valid @RequestBody ChangePasswordRequest request) {
        try {
            userService.changePassword(id, request);
            return ResponseEntity.ok(new ApiResponse("Đổi mật khẩu thành công", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
}

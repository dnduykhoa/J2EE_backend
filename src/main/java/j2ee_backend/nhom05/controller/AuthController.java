package j2ee_backend.nhom05.controller;

import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.dto.auth.ChangePasswordRequest;
import j2ee_backend.nhom05.dto.auth.ForgotPasswordRequest;
import j2ee_backend.nhom05.dto.auth.GoogleLoginRequest;
import j2ee_backend.nhom05.dto.auth.LoginRequest;
import j2ee_backend.nhom05.dto.auth.LoginResponse;
import j2ee_backend.nhom05.dto.auth.RegisterRequest;
import j2ee_backend.nhom05.dto.auth.ResetPasswordRequest;
import j2ee_backend.nhom05.dto.auth.Toggle2FARequest;
import j2ee_backend.nhom05.dto.auth.TwoFactorResponse;
import j2ee_backend.nhom05.dto.auth.Verify2FARequest;
import j2ee_backend.nhom05.model.Role;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.config.JwtUtil;
import j2ee_backend.nhom05.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;
    
    // API kiểm tra username đã tồn tại
    @GetMapping("/check-username/{username}")
    public ResponseEntity<?> checkUsername(@PathVariable String username) {
        try {
            authService.findByUsername(username);
            return ResponseEntity.ok().body(new ApiResponse("Thông tin đã được sử dụng, vui lòng điền lại thông tin", false));
        } catch (RuntimeException e) {
            return ResponseEntity.ok().body(new ApiResponse("Thông tin khả dụng", true));
        }
    }
    
    // API kiểm tra email đã tồn tại
    @GetMapping("/check-email/{email}")
    public ResponseEntity<?> checkEmail(@PathVariable String email) {
        try {
            authService.findByEmail(email);
            return ResponseEntity.ok().body(new ApiResponse("Thông tin đã được sử dụng, vui lòng điền lại thông tin", false));
        } catch (RuntimeException e) {
            return ResponseEntity.ok().body(new ApiResponse("Thông tin khả dụng", true));
        }
    }

    // API kiểm tra số điện thoại đã tồn tại
    @GetMapping("/check-phone/{phone}")
    public ResponseEntity<?> checkPhone(@PathVariable String phone) {
        try {
            authService.findByPhone(phone);
            return ResponseEntity.ok().body(new ApiResponse("Thông tin đã được sử dụng, vui lòng điền lại thông tin", false));
        } catch (RuntimeException e) {
            return ResponseEntity.ok().body(new ApiResponse("Thông tin khả dụng", true));
        }
    }
    
    // API đăng ký
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = authService.register(request);
            String token = jwtUtil.generateToken(user.getUsername(), false);
            LoginResponse response = new LoginResponse(
                "Đăng ký thành công",
                token,
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
    
    // API đăng nhập
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Object result = authService.login(request);
            
            // Nếu trả về TwoFactorResponse (yêu cầu xác thực 2 bước)
            if (result instanceof TwoFactorResponse) {
                return ResponseEntity.ok(result);
            }
            
            // Nếu trả về User (không cần xác thực 2 bước)
            User user = (User) result;
            String token = jwtUtil.generateToken(user.getUsername(), request.isRememberMe());
            LoginResponse response = new LoginResponse(
                "Đăng nhập thành công",
                token,
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
    
    // API đăng nhập bằng Google
    @PostMapping("/google")
    public ResponseEntity<?> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        try {
            User user = authService.loginWithGoogle(request.getIdToken());

            String token = jwtUtil.generateToken(user.getUsername(), false);
            LoginResponse response = new LoginResponse(
                "Đăng nhập Google thành công",
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getBirthDate(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
            );
            return ResponseEntity.ok(new ApiResponse("Đăng nhập Google thành công", response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // API đăng xuất
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Với JWT, logout thường được xử lý ở client bằng cách xóa token
        return ResponseEntity.ok(new ApiResponse("Đăng xuất thành công", null));
    }
    
    // API đổi mật khẩu
    @PutMapping("/change-password/{id}")
    public ResponseEntity<?> changePassword(@PathVariable Long id, @Valid @RequestBody ChangePasswordRequest request) {
        try {
            authService.changePassword(id, request);
            return ResponseEntity.ok(new ApiResponse("Đổi mật khẩu thành công", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // API quên mật khẩu (gửi mã 6 ký tự qua email)
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            authService.forgotPassword(request);
            return ResponseEntity.ok(new ApiResponse("Mã đặt lại mật khẩu đã được gửi qua email", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
    
    // API reset mật khẩu với mã xác thực
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request);
            return ResponseEntity.ok(new ApiResponse("Đặt lại mật khẩu thành công", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
    
    // API bật/tắt 2FA
    @PutMapping("/toggle-2fa/{id}")
    public ResponseEntity<?> toggle2FA(@PathVariable Long id, @Valid @RequestBody Toggle2FARequest request) {
        try {
            authService.toggle2FA(id, request.getEnabled());
            String message = request.getEnabled() 
                ? "Bật xác thực 2 bước thành công" 
                : "Tắt xác thực 2 bước thành công";
            return ResponseEntity.ok(new ApiResponse(message, null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(e.getMessage(), null));
        }
    }
    
    // API xác thực mã 2FA
    @PostMapping("/verify-2fa")
    public ResponseEntity<?> verify2FA(@Valid @RequestBody Verify2FARequest request) {
        try {
            User user = authService.verify2FACode(request.getEmailOrPhone(), request.getCode());
            
            String token = jwtUtil.generateToken(user.getUsername(), false);
            LoginResponse response = new LoginResponse(
                "Xác thực 2FA thành công",
                token,
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
}

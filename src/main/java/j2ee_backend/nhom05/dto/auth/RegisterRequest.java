package j2ee_backend.nhom05.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Data
public class RegisterRequest {
    
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập phải từ 3 đến 50 ký tự")
    private String username;
    
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;
    
    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPassword;
    
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
    
    private String fullName;
    
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại không hợp lệ")
    private String phone;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
}

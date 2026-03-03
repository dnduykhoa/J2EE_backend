package j2ee_backend.nhom05.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    
    private String fullName;
    
    @Email(message = "Email không hợp lệ")
    private String email;
    
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại không hợp lệ")
    private String phone;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
}

package j2ee_backend.nhom05.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequest {

    @NotBlank(message = "ID Token không được để trống")
    private String idToken;
}

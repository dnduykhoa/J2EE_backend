package j2ee_backend.nhom05.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private LocalDate birthDate;
    private String provider;
    private boolean twoFactorEnabled;
    private Set<String> roles;
}

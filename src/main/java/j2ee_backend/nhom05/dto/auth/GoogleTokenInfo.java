package j2ee_backend.nhom05.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO chứa thông tin trả về từ Google tokeninfo API.
 * GET https://oauth2.googleapis.com/tokeninfo?id_token={idToken}
 */
@Data
public class GoogleTokenInfo {

    /** Google user ID (Subject) */
    private String sub;

    private String email;

    @JsonProperty("email_verified")
    private String emailVerified;

    /** Họ tên đầy đủ */
    private String name;

    @JsonProperty("given_name")
    private String givenName;

    @JsonProperty("family_name")
    private String familyName;

    private String picture;

    /** Audience - phải khớp với Google Client ID của ứng dụng */
    private String aud;

    /** Thời điểm hết hạn (epoch seconds) */
    private String exp;
}

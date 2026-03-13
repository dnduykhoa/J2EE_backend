package j2ee_backend.nhom05.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vnpay")
@Data
public class VnpayConfig {
    private String tmnCode;
    private String hashSecret;
    private String baseUrl;
    private String returnUrl;
    private String frontendUrl;
}

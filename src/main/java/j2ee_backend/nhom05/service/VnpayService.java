package j2ee_backend.nhom05.service;

import j2ee_backend.nhom05.config.VnpayConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

@Service
public class VnpayService {

    @Autowired
    private VnpayConfig config;

    /**
     * Tạo URL thanh toán VNPAY.
     *
     * @param orderCode mã đơn hàng (dùng làm vnp_TxnRef)
     * @param amount    tổng tiền (VND)
     * @param ipAddr    IP client
     */
    public String createPaymentUrl(String orderCode, BigDecimal amount, String ipAddr) {
        String vnpCreateDate = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        // Số tiền × 100 theo quy định VNPAY
        String vnpAmount = String.valueOf(amount.multiply(BigDecimal.valueOf(100)).longValue());

        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version",   "2.1.0");
        vnpParams.put("vnp_Command",   "pay");
        vnpParams.put("vnp_TmnCode",   config.getTmnCode());
        vnpParams.put("vnp_Amount",    vnpAmount);
        vnpParams.put("vnp_CurrCode",  "VND");
        vnpParams.put("vnp_TxnRef",    orderCode);
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + orderCode);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale",    "vn");
        vnpParams.put("vnp_ReturnUrl", config.getReturnUrl());
        vnpParams.put("vnp_IpAddr",    ipAddr);
        vnpParams.put("vnp_CreateDate", vnpCreateDate);

        // Build hash data & query string (TreeMap đã sắp xếp theo key)
        StringBuilder hashData = new StringBuilder();
        StringBuilder queryString = new StringBuilder();
        Iterator<Map.Entry<String, String>> iter = vnpParams.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, String> entry = iter.next();
            String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII);
            hashData.append(entry.getKey()).append('=').append(encodedValue);
            queryString.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII))
                       .append('=').append(encodedValue);
            if (iter.hasNext()) {
                hashData.append('&');
                queryString.append('&');
            }
        }

        String secureHash = hmacSHA512(config.getHashSecret(), hashData.toString());
        queryString.append("&vnp_SecureHash=").append(secureHash);

        return config.getBaseUrl() + "?" + queryString;
    }

    /**
     * Xác minh chữ ký callback từ VNPAY.
     */
    public boolean verifyCallback(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) return false;

        // Loại bỏ vnp_SecureHash và vnp_SecureHashType trước khi tính
        Map<String, String> sortedParams = new TreeMap<>(params);
        sortedParams.remove("vnp_SecureHash");
        sortedParams.remove("vnp_SecureHashType");

        StringBuilder hashData = new StringBuilder();
        Iterator<Map.Entry<String, String>> iter = sortedParams.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, String> entry = iter.next();
            String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII);
            hashData.append(entry.getKey()).append('=').append(encodedValue);
            if (iter.hasNext()) hashData.append('&');
        }

        String computedHash = hmacSHA512(config.getHashSecret(), hashData.toString());
        return computedHash.equalsIgnoreCase(receivedHash);
    }

    // ── Helper HMAC-SHA512 ────────────────────────────────────────────────────

    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tính HMAC-SHA512", e);
        }
    }
}

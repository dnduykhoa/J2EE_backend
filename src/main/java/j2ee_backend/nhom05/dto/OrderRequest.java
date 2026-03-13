package j2ee_backend.nhom05.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "[0-9\\s\\+\\-]{9,15}", message = "Số điện thoại không hợp lệ")
    private String phone;

    private String email;

    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    private String shippingAddress;

    private String note;

    // "CASH" | "VNPAY" | "MOMO"
    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod;
}

package j2ee_backend.nhom05.controller;

import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.dto.VoucherRequest;
import j2ee_backend.nhom05.dto.VoucherResponse;
import j2ee_backend.nhom05.dto.VoucherValidateRequest;
import j2ee_backend.nhom05.dto.VoucherValidateResponse;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@CrossOrigin(origins = "*")
public class VoucherController {

    @Autowired
    private VoucherService voucherService;

    // ── Public / Authenticated: Kiểm tra voucher trước khi đặt hàng ──────────

    /**
     * POST /api/vouchers/validate
     * Người dùng kiểm tra voucher có hợp lệ không và xem số tiền giảm.
     * Yêu cầu đăng nhập.
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validate(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody VoucherValidateRequest request) {
        try {
            Long userId = userDetails != null ? ((User) userDetails).getId() : null;
            VoucherValidateResponse result = voucherService.validate(
                    request.getVoucherCode(), request.getOrderAmount(), userId);
            return ResponseEntity.ok(new ApiResponse(result.getMessage(), result));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(e.getMessage(), null));
        }
    }

    // ── Admin: Quản lý voucher ──────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<?> getAll(@AuthenticationPrincipal UserDetails userDetails) {
        if (!isAdmin(userDetails)) return forbidden();
        try {
            List<VoucherResponse> list = voucherService.getAll();
            return ResponseEntity.ok(new ApiResponse("Lấy danh sách voucher thành công", list));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActive() {
        try {
            return ResponseEntity.ok(new ApiResponse("OK", voucherService.getActive()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (!isAdmin(userDetails)) return forbidden();
        try {
            return ResponseEntity.ok(new ApiResponse("OK", voucherService.getById(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody VoucherRequest request) {
        if (!isAdmin(userDetails)) return forbidden();
        try {
            VoucherResponse res = voucherService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse("Tạo voucher thành công", res));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody VoucherRequest request) {
        if (!isAdmin(userDetails)) return forbidden();
        try {
            return ResponseEntity.ok(new ApiResponse("Cập nhật voucher thành công", voucherService.update(id, request)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @DeleteMapping({"/{id}", "/delete/{id}"})
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (!isAdmin(userDetails)) return forbidden();
        try {
            voucherService.delete(id);
            return ResponseEntity.ok(new ApiResponse("Xoá voucher thành công", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggle(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (!isAdmin(userDetails)) return forbidden();
        try {
            return ResponseEntity.ok(new ApiResponse("Cập nhật trạng thái voucher thành công", voucherService.toggleActive(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(e.getMessage(), null));
        }
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private boolean isAdmin(UserDetails userDetails) {
        return userDetails != null && userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
    }

    private ResponseEntity<ApiResponse> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse("Bạn không có quyền thực hiện thao tác này", null));
    }
}

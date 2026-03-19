package j2ee_backend.nhom05.controller;

import j2ee_backend.nhom05.dto.ApiResponse;
import j2ee_backend.nhom05.dto.ReviewRequest;
import j2ee_backend.nhom05.dto.ReviewResponse;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.service.ProductReviewService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class ProductReviewController {

    @Autowired
    private ProductReviewService reviewService;

    // ── POST /api/reviews ──────────────────────────────────────────────────────
    // Người dùng gửi đánh giá (multipart/form-data, hỗ trợ upload ảnh tối đa 5 ảnh)
    @PostMapping(value = "/api/reviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long orderItemId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment,
            @RequestPart(name = "images", required = false) List<MultipartFile> images) {
        try {
            if (rating == null || rating < 1 || rating > 5) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse("Đánh giá sao phải từ 1 đến 5", null));
            }
            ReviewRequest request = new ReviewRequest();
            request.setOrderItemId(orderItemId);
            request.setRating(rating);
            request.setComment(comment);

            Long userId = ((User) userDetails).getId();
            ReviewResponse review = reviewService.createReview(userId, request, images);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse("Đánh giá thành công", review));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // ── GET /api/products/{productId}/reviews ──────────────────────────────────
    // Lấy danh sách đánh giá của sản phẩm (công khai)
    @GetMapping("/api/products/{productId}/reviews")
    public ResponseEntity<?> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(required = false) Long variantId) {
        try {
            List<ReviewResponse> reviews;
            if (variantId != null) {
                reviews = reviewService.getReviewsByProductAndVariant(productId, variantId);
            } else {
                reviews = reviewService.getReviewsByProduct(productId);
            }
            return ResponseEntity.ok(new ApiResponse("Lấy danh sách đánh giá thành công", reviews));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // ── GET /api/products/{productId}/reviews/summary ──────────────────────────────────
    // Lấy điểm trung bình và số lượng đánh giá
    @GetMapping("/api/products/{productId}/reviews/summary")
    public ResponseEntity<?> getReviewSummary(
            @PathVariable Long productId,
            @RequestParam(required = false) Long variantId) {
        try {
            ProductReviewService.ReviewSummary summary;
            if (variantId != null) {
                summary = reviewService.getReviewSummaryByVariant(productId, variantId);
            } else {
                summary = reviewService.getReviewSummary(productId);
            }
            return ResponseEntity.ok(new ApiResponse("OK", Map.of(
                    "averageRating", summary.averageRating,
                    "totalReviews", summary.totalReviews)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // ── GET /api/admin/reviews ─────────────────────────────────────────────────
    // [ADMIN] Lấy tất cả đánh giá, hỗ trợ lọc keyword + rating
    @GetMapping("/api/admin/reviews")
    public ResponseEntity<?> getAllReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer rating) {
        if (!isAdmin(userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse("Không có quyền truy cập", null));
        }
        try {
            List<ReviewResponse> reviews = reviewService.getAllReviews(keyword, rating);
            return ResponseEntity.ok(new ApiResponse("OK", reviews));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // ── DELETE /api/admin/reviews/{id} ─────────────────────────────────────────
    // [ADMIN] Xóa một đánh giá
    @DeleteMapping("/api/admin/reviews/{id}")
    public ResponseEntity<?> deleteReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        if (!isAdmin(userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse("Không có quyền truy cập", null));
        }
        try {
            reviewService.deleteReview(id);
            return ResponseEntity.ok(new ApiResponse("Xóa đánh giá thành công", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    // ── PATCH /api/admin/reviews/{id}/toggle-hidden ────────────────────────────
    // [ADMIN] Ẩn / hiện một đánh giá
    @PatchMapping("/api/admin/reviews/{id}/toggle-hidden")
    public ResponseEntity<?> toggleHidden(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        if (!isAdmin(userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse("Không có quyền truy cập", null));
        }
        try {
            ReviewResponse updated = reviewService.toggleHidden(id);
            String msg = updated.isHidden() ? "Đã ẩn đánh giá" : "Đã hiện đánh giá";
            return ResponseEntity.ok(new ApiResponse(msg, updated));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    private boolean isAdmin(UserDetails userDetails) {
        return userDetails != null && userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
    }
}

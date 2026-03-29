package j2ee_backend.nhom05.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import j2ee_backend.nhom05.dto.auth.ChangePasswordRequest;
import j2ee_backend.nhom05.dto.auth.ForgotPasswordRequest;
import j2ee_backend.nhom05.dto.auth.GoogleTokenInfo;
import j2ee_backend.nhom05.dto.auth.LoginRequest;
import j2ee_backend.nhom05.dto.auth.RegisterRequest;
import j2ee_backend.nhom05.dto.auth.ResetPasswordRequest;
import j2ee_backend.nhom05.dto.auth.TwoFactorResponse;
import j2ee_backend.nhom05.model.PasswordResetToken;
import j2ee_backend.nhom05.model.Role;
import j2ee_backend.nhom05.model.TwoFactorCode;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.repository.IPasswordResetTokenRepository;
import j2ee_backend.nhom05.repository.IRoleRepository;
import j2ee_backend.nhom05.repository.ITwoFactorCodeRepository;
import j2ee_backend.nhom05.repository.IUserRepository;
import j2ee_backend.nhom05.validator.PasswordValidator;
import j2ee_backend.nhom05.validator.PhoneValidator;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthService {
    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private PasswordValidator passwordValidator;
    
    @Autowired
    private IPasswordResetTokenRepository resetTokenRepository;
    
    @Autowired
    private ITwoFactorCodeRepository twoFactorCodeRepository;
    
    @Autowired
    private EmailService emailService;

    @Autowired
    private AuthSessionService authSessionService;

    @Value("${google.client-id}")
    private String googleClientId;
    
    @Transactional
    public User register(RegisterRequest request) {
        // Kiểm tra password và confirmPassword có khớp nhau
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp");
        }
        
        // Validate password
        passwordValidator.validate(request.getPassword());
        
        // Kiểm tra username đã tồn tại
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Thông tin đã được sử dụng, vui lòng điền lại thông tin");
        }
        
        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Thông tin đã được sử dụng, vui lòng điền lại thông tin");
        }
        
        // Chuẩn hóa số điện thoại (+84/84 → 0, xóa khoảng trắng)
        String phone = PhoneValidator.normalize(request.getPhone());
        if (!PhoneValidator.isValid(phone)) {
            throw new RuntimeException("Số điện thoại Việt Nam không hợp lệ");
        }
        
        // Kiểm tra số điện thoại đã tồn tại
        if (userRepository.findByPhone(phone).isPresent()) {
            throw new RuntimeException("Thông tin đã được sử dụng, vui lòng điền lại thông tin");
        }
        
        // Tạo user mới
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Mã hóa password
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhone(phone);
        user.setBirthDate(request.getBirthDate());
        user.setProvider("local"); // Đăng ký thông thường
        
        // Gán role USER mặc định
        Role userRole = roleRepository.findByName("USER")
            .orElseThrow(() -> new RuntimeException("Role USER không tồn tại"));
        
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);
        
        // Lưu user
        return userRepository.save(user);
    }
    
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
    }
    
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
    }

    public User findByPhone(String phone) {
        return userRepository.findByPhone(phone)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
    }
    
    // Login user - Trả về TwoFactorResponse nếu cần xác thực 2 bước
    public Object login(LoginRequest request, HttpServletRequest httpRequest) {
        String input = request.getEmailOrPhone().trim();
        
        // Kiểm tra định dạng: phải là email hoặc số điện thoại
        boolean isEmail = input.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
        String normalizedPhone = PhoneValidator.normalize(input);
        boolean isPhone = PhoneValidator.isValid(normalizedPhone);
        
        if (!isEmail && !isPhone) {
            throw new RuntimeException("Vui lòng nhập đúng định dạng email hoặc số điện thoại");
        }
        
        // Tìm user theo email hoặc số điện thoại (phone đã chuẩn hóa thành 0XXXXXXXXX)
        User user = userRepository.findByEmail(input)
            .orElseGet(() -> userRepository.findByPhone(normalizedPhone)
                .orElseThrow(() -> new RuntimeException("Email hoặc số điện thoại không tồn tại")));

        if (!user.isEnabled()) {
            throw new RuntimeException("Tài khoản đã bị vô hiệu hóa");
        }

        // Kiểm tra tài khoản Google không thể đăng nhập bằng mật khẩu
        if ("google".equals(user.getProvider())) {
            throw new RuntimeException("Tài khoản này đăng nhập bằng Google, vui lòng dùng Google Sign-In");
        }
        
        // Kiểm tra password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu không chính xác");
        }

        authSessionService.validateAdminIpPolicyBeforeLogin(user, httpRequest);

        boolean requiresTwoFactor = authSessionService.shouldRequireTwoFactor(user, httpRequest);
        
        // Nếu user cần 2FA, gửi mã xác thực
        if (requiresTwoFactor) {
            String code = generateSixDigitCode();
            
            // Xóa các mã cũ
            twoFactorCodeRepository.deleteByEmailOrPhone(input);
            
            // Lưu mã mới với thời gian hết hạn 5 phút
            TwoFactorCode twoFactorCode = new TwoFactorCode();
            twoFactorCode.setEmailOrPhone(input);
            twoFactorCode.setCode(code);
            twoFactorCode.setExpiryDate(LocalDateTime.now().plusMinutes(5));
            twoFactorCode.setUsed(false);
            twoFactorCodeRepository.save(twoFactorCode);
            
            // Gửi mã qua email
            emailService.sendTwoFactorCode(user.getEmail(), code);
            
            // Trả về response yêu cầu nhập mã 2FA
            return new TwoFactorResponse(
                "Mã xác thực đã được gửi qua email",
                true,
                input
            );
        }
        
        // Nếu không bật 2FA, trả về user
        return user;
    }
    
    // Lấy user theo ID
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
    }
    
    // Đăng nhập bằng Google (xác thực ID Token phía backend)
    @Transactional
    public User loginWithGoogle(String idToken) {
        // 1. Gọi Google tokeninfo API để xác thực token
        GoogleTokenInfo tokenInfo = verifyGoogleToken(idToken);

        // 2. Kiểm tra audience khớp với Client ID của ứng dụng
        if (!googleClientId.equals(tokenInfo.getAud())) {
            throw new RuntimeException("Token Google không hợp lệ: audience không khớp");
        }

        // 3. Kiểm tra email đã được xác thực
        if (!"true".equals(tokenInfo.getEmailVerified())) {
            throw new RuntimeException("Email Google chưa được xác thực");
        }

        String googleId = tokenInfo.getSub();
        String email = tokenInfo.getEmail();

        // 4. Tìm user theo provider + providerId (đã đăng nhập Google trước đó)
        Optional<User> existingByProvider = userRepository.findByProviderAndProviderId("google", googleId);
        if (existingByProvider.isPresent()) {
            return existingByProvider.get();
        }

        // 5. Tìm user theo email (đã đăng ký bằng email thông thường → liên kết Google)
        Optional<User> existingByEmail = userRepository.findByEmail(email);
        if (existingByEmail.isPresent()) {
            User user = existingByEmail.get();
            user.setProvider("google");
            user.setProviderId(googleId);
            return userRepository.save(user);
        }

        // 6. Tạo tài khoản mới từ thông tin Google
        String fullName = tokenInfo.getName() != null ? tokenInfo.getName()
                : (tokenInfo.getGivenName() != null ? tokenInfo.getGivenName() : "Google User");
        String username = generateUsernameFromEmail(email);

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        // Mật khẩu ngẫu nhiên được encode - tài khoản Google không dùng mật khẩu để đăng nhập
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        newUser.setFullName(fullName);
        newUser.setProvider("google");
        newUser.setProviderId(googleId);

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Role USER không tồn tại"));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        newUser.setRoles(roles);

        return userRepository.save(newUser);
    }

    // Xác thực Google ID Token qua Google tokeninfo API
    private GoogleTokenInfo verifyGoogleToken(String idToken) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
        try {
            GoogleTokenInfo info = restTemplate.getForObject(url, GoogleTokenInfo.class);
            if (info == null || info.getSub() == null) {
                throw new RuntimeException("Không lấy được thông tin từ Google");
            }
            return info;
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Token Google không hợp lệ hoặc đã hết hạn");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Không thể xác thực token Google: " + e.getMessage());
        }
    }

    // Tạo username từ email (đảm bảo duy nhất)
    private String generateUsernameFromEmail(String email) {
        String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "_");
        // Đảm bảo độ dài tối thiểu 3
        if (baseUsername.length() < 3) {
            baseUsername = baseUsername + "_gg";
        }
        // Cắt nếu quá 45 ký tự (để còn chỗ thêm số)
        if (baseUsername.length() > 45) {
            baseUsername = baseUsername.substring(0, 45);
        }
        if (!userRepository.existsByUsername(baseUsername)) {
            return baseUsername;
        }
        int suffix = 1;
        while (userRepository.existsByUsername(baseUsername + suffix)) {
            suffix++;
        }
        return baseUsername + suffix;
    }

    // Đổi mật khẩu
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findById(userId);
        
        // Kiểm tra user có đăng nhập bằng Google không (không có password)
        if (user.getProvider() != null && user.getProvider().equals("google")) {
            throw new RuntimeException("Tài khoản Google không thể đổi mật khẩu");
        }
        
        // Kiểm tra mật khẩu cũ có đúng không
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu cũ không chính xác");
        }
        
        // Kiểm tra mật khẩu mới và xác nhận có khớp không
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu mới và xác nhận không khớp");
        }
        
        // Kiểm tra mật khẩu mới không được giống mật khẩu cũ
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new RuntimeException("Mật khẩu mới phải khác mật khẩu cũ");
        }
        
        // Validate mật khẩu mới
        passwordValidator.validate(request.getNewPassword());
        
        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
    
    // Quên mật khẩu - Gửi mã 6 ký tự qua email
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Kiểm tra email có tồn tại không
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống"));
        
        // Kiểm tra tài khoản Google không thể reset password
        if ("google".equals(user.getProvider())) {
            throw new RuntimeException("Tài khoản Google không thể đặt lại mật khẩu");
        }
        
        // Xóa các token cũ của email này
        resetTokenRepository.deleteByEmail(request.getEmail());
        
        // Tạo mã 6 ký tự ngẫu nhiên
        String token = generateSixDigitCode();
        
        // Lưu token vào database với thời gian hết hạn 15 phút
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setEmail(request.getEmail());
        resetToken.setToken(token);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        resetToken.setUsed(false);
        resetTokenRepository.save(resetToken);
        
        // Gửi email chứa mã
        emailService.sendPasswordResetEmail(request.getEmail(), token);
    }
    
    // Reset mật khẩu với mã xác thực
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Kiểm tra mật khẩu mới và xác nhận có khớp không
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu mới và xác nhận không khớp");
        }
        
        // Validate mật khẩu mới
        passwordValidator.validate(request.getNewPassword());
        
        // Tìm token hợp lệ (chưa dùng, chưa hết hạn)
        PasswordResetToken resetToken = resetTokenRepository
            .findByEmailAndTokenAndUsedFalseAndExpiryDateAfter(
                request.getEmail(), 
                request.getResetToken(), 
                LocalDateTime.now())
            .orElseThrow(() -> new RuntimeException("Mã xác thực không hợp lệ hoặc đã hết hạn"));
        
        // Tìm user theo email
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Email không tồn tại"));
        
        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        // Đánh dấu token đã được sử dụng
        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);
    }
    
    // Tạo mã 6 chữ số ngẫu nhiên
    private String generateSixDigitCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
    
    // Bật/tắt 2FA cho user
    @Transactional
    public void toggle2FA(Long userId, boolean enabled) {
        User user = findById(userId);
        
        // Kiểm tra tài khoản Google
        if ("google".equals(user.getProvider())) {
            throw new RuntimeException("Tài khoản Google không hỗ trợ 2FA qua email");
        }
        
        user.setTwoFactorEnabled(enabled);
        userRepository.save(user);
    }
    
    // Xác thực mã 2FA và hoàn tất đăng nhập
    @Transactional
    public User verify2FACode(String emailOrPhone, String code) {
        // Tìm mã xác thực hợp lệ
        TwoFactorCode twoFactorCode = twoFactorCodeRepository
            .findValidCode(
                emailOrPhone,
                code,
                LocalDateTime.now())
            .orElseThrow(() -> new RuntimeException("Mã xác thực không hợp lệ hoặc đã hết hạn"));
        
        // Tìm user
        User user = userRepository.findByEmail(emailOrPhone)
            .orElseGet(() -> userRepository.findByPhone(emailOrPhone)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user")));
        
        // Đánh dấu mã đã được sử dụng
        twoFactorCode.setUsed(true);
        twoFactorCodeRepository.save(twoFactorCode);
        
        return user;
    }
}

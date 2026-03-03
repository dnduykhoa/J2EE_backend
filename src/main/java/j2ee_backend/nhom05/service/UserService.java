package j2ee_backend.nhom05.service;
import j2ee_backend.nhom05.dto.auth.ChangePasswordRequest;
import j2ee_backend.nhom05.dto.auth.GoogleTokenInfo;
import j2ee_backend.nhom05.dto.auth.LoginRequest;
import j2ee_backend.nhom05.dto.auth.RegisterRequest;
import j2ee_backend.nhom05.dto.auth.UpdateProfileRequest;
import j2ee_backend.nhom05.model.Role;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.repository.IRoleRepository;
import j2ee_backend.nhom05.repository.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {
    
    @Autowired
    private IUserRepository userRepository;
    
    @Autowired
    private IRoleRepository roleRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${google.client-id}")
    private String googleClientId;
    
    @Transactional
    public User register(RegisterRequest request) {
        // Kiểm tra password và confirmPassword có khớp nhau
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp");
        }
        
        // Kiểm tra username đã tồn tại
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại");
        }
        
        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }
        
        // Tạo user mới
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Mã hóa password
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
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
    
    // Login user
    public User login(LoginRequest request) {
        // Tìm user theo username hoặc email
        User user = userRepository.findByUsername(request.getUsernameOrEmail())
            .orElseGet(() -> userRepository.findByEmail(request.getUsernameOrEmail())
                .orElseThrow(() -> new RuntimeException("Tên đăng nhập hoặc email không tồn tại")));

        // Kiểm tra tài khoản Google không thể đăng nhập bằng mật khẩu
        if ("google".equals(user.getProvider())) {
            throw new RuntimeException("Tài khoản này đăng nhập bằng Google, vui lòng dùng Google Sign-In");
        }
        
        // Kiểm tra password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu không chính xác");
        }
        
        return user;
    }
    
    // Lấy user theo ID
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
    }
    
    // Cập nhật profile (không cho update role)
    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findById(userId);
        
        // Kiểm tra email mới có trùng với user khác không
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email đã được sử dụng");
            }
            user.setEmail(request.getEmail());
        }
        
        // Cập nhật các field được phép
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        }
        
        return userRepository.save(user);
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
        
        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // -----------------------------------------------------------------------
    // Admin APIs
    // -----------------------------------------------------------------------

    /** Lấy danh sách tất cả user. */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /** Tìm kiếm user theo username, email hoặc họ tên. */
    public List<User> searchUsers(String keyword) {
        return userRepository
            .findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                keyword, keyword, keyword);
    }

    /** Xóa user theo ID (admin). */
    @Transactional
    public void deleteUser(Long id) {
        User user = findById(id);
        userRepository.delete(user);
    }

    /** Cập nhật danh sách role cho user (admin). */
    @Transactional
    public User updateUserRoles(Long id, Set<String> roleNames) {
        User user = findById(id);
        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy role: " + roleName));
            roles.add(role);
        }
        user.setRoles(roles);
        return userRepository.save(user);
    }
}

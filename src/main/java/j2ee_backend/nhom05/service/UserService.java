package j2ee_backend.nhom05.service;
import j2ee_backend.nhom05.dto.auth.ChangePasswordRequest;
import j2ee_backend.nhom05.dto.auth.LoginRequest;
import j2ee_backend.nhom05.dto.auth.RegisterRequest;
import j2ee_backend.nhom05.dto.auth.UpdateProfileRequest;
import j2ee_backend.nhom05.model.Role;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.repository.IRoleRepository;
import j2ee_backend.nhom05.repository.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class UserService {
    
    @Autowired
    private IUserRepository userRepository;
    
    @Autowired
    private IRoleRepository roleRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
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
}

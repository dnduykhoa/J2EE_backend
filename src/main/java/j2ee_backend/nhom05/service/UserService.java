package j2ee_backend.nhom05.service;
import j2ee_backend.nhom05.dto.auth.UpdateProfileRequest;
import j2ee_backend.nhom05.model.Role;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.repository.IRoleRepository;
import j2ee_backend.nhom05.repository.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {
    
    @Autowired
    private IUserRepository userRepository;
    
    @Autowired
    private IRoleRepository roleRepository;
    
    // Lấy user theo ID
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
    }
    
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
}

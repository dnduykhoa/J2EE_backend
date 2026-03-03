package j2ee_backend.nhom05.config;

import j2ee_backend.nhom05.model.Role;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.repository.IRoleRepository;
import j2ee_backend.nhom05.repository.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private IRoleRepository roleRepository;
    
    @Autowired
    private IUserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        initializeRoles();
        initializeAdminUser();
    }
    
    private void initializeRoles() {
        // Tạo role ADMIN nếu chưa tồn tại
        if (!roleRepository.existsByName("ADMIN")) {
            Role adminRole = new Role();
            adminRole.setName("ADMIN");
            adminRole.setDescription("Quản trị viên - Có toàn quyền quản lý hệ thống");
            roleRepository.save(adminRole);
        }
        
        // Tạo role MANAGER nếu chưa tồn tại
        if (!roleRepository.existsByName("MANAGER")) {
            Role managerRole = new Role();
            managerRole.setName("MANAGER");
            managerRole.setDescription("Quản lý - Quản lý sản phẩm, đơn hàng và nhân viên");
            roleRepository.save(managerRole);
        }
        
        // Tạo role STAFF nếu chưa tồn tại
        if (!roleRepository.existsByName("STAFF")) {
            Role staffRole = new Role();
            staffRole.setName("STAFF");
            staffRole.setDescription("Nhân viên - Xử lý đơn hàng và hỗ trợ khách hàng");
            roleRepository.save(staffRole);
        }
        
        // Tạo role USER nếu chưa tồn tại
        if (!roleRepository.existsByName("USER")) {
            Role userRole = new Role();
            userRole.setName("USER");
            userRole.setDescription("Người dùng - Khách hàng thông thường");
            roleRepository.save(userRole);
        }
        
        System.out.println("✓ Hoàn tất khởi tạo dữ liệu roles");
    }

    private void initializeAdminUser() {
        // Tạo tài khoản admin nếu chưa tồn tại
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123")); // Mật khẩu mặc định
            admin.setEmail("admin@techstore.com");
            admin.setFullName("Quản trị viên hệ thống");
            admin.setPhone("0999999999");
            admin.setProvider("local"); // Tài khoản admin đăng ký thông thường
            
            // Gán role ADMIN
            Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("Role ADMIN không tồn tại"));
            
            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);
            admin.setRoles(roles);
            
            userRepository.save(admin);
            System.out.println("✓ Đã tạo tài khoản admin - Username: admin, Password: admin123");
        }
        
        System.out.println("✓ Hoàn tất khởi tạo dữ liệu");
    }
}

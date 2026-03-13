package j2ee_backend.nhom05.config;

import j2ee_backend.nhom05.model.AttributeDefinition;
import j2ee_backend.nhom05.model.AttributeDefinition.DataType;
import j2ee_backend.nhom05.model.AttributeGroup;
import j2ee_backend.nhom05.model.Role;
import j2ee_backend.nhom05.model.User;
import j2ee_backend.nhom05.repository.IAttributeDefinitionRepository;
import j2ee_backend.nhom05.repository.IAttributeGroupRepository;
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

    @Autowired
    private IAttributeGroupRepository attributeGroupRepository;

    @Autowired
    private IAttributeDefinitionRepository attributeDefinitionRepository;
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        initializeRoles();
        // initializeAdminUser();
        //initializeAttributeGroups();
        //initializeAttributeDefinitions();
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

    // -----------------------------------------------------------------------
    // EAV: Khởi tạo nhóm thuộc tính
    // -----------------------------------------------------------------------
    private void initializeAttributeGroups() {
        createGroupIfAbsent("Hiệu năng",    "CPU, RAM, bộ nhớ trong, card đồ họa...", 1);
        createGroupIfAbsent("Màn hình",     "Kích thước, độ phân giải, tần số quét, công nghệ tấm nền...", 2);
        createGroupIfAbsent("Lưu trữ",      "SSD, HDD, eMMC, dung lượng lưu trữ...", 3);
        createGroupIfAbsent("Kết nối",      "WiFi, Bluetooth, USB, HDMI, cổng sạc...", 4);
        createGroupIfAbsent("Pin & Sạc",    "Dung lượng pin, công suất sạc, thời lượng pin...", 5);
        createGroupIfAbsent("Camera",       "Độ phân giải camera sau, trước, số lượng camera...", 6);
        createGroupIfAbsent("Thiết kế",     "Kích thước, trọng lượng, màu sắc, chất liệu...", 7);
        createGroupIfAbsent("Hệ điều hành", "Hệ điều hành, phiên bản...", 8);
        System.out.println("✓ Hoàn tất khởi tạo nhóm thuộc tính EAV");
    }

    private AttributeGroup createGroupIfAbsent(String name, String description, int order) {
        return attributeGroupRepository.findByName(name).orElseGet(() -> {
            AttributeGroup g = new AttributeGroup();
            g.setName(name);
            g.setDescription(description);
            g.setDisplayOrder(order);
            return attributeGroupRepository.save(g);
        });
    }

    // -----------------------------------------------------------------------
    // EAV: Khởi tạo định nghĩa thuộc tính (chuẩn hóa attrKey)
    // -----------------------------------------------------------------------
    private void initializeAttributeDefinitions() {
        AttributeGroup grpPerf = attributeGroupRepository.findByName("Hiệu năng").orElse(null);
        AttributeGroup grpScreen = attributeGroupRepository.findByName("Màn hình").orElse(null);
        AttributeGroup grpStorage = attributeGroupRepository.findByName("Lưu trữ").orElse(null);
        AttributeGroup grpConnect = attributeGroupRepository.findByName("Kết nối").orElse(null);
        AttributeGroup grpBattery = attributeGroupRepository.findByName("Pin & Sạc").orElse(null);
        AttributeGroup grpCamera = attributeGroupRepository.findByName("Camera").orElse(null);
        AttributeGroup grpDesign = attributeGroupRepository.findByName("Thiết kế").orElse(null);
        AttributeGroup grpOS = attributeGroupRepository.findByName("Hệ điều hành").orElse(null);

        // --- Hiệu năng ---
        createAttrIfAbsent("CPU",           "cpu",          DataType.STRING,  null,    false, true,  grpPerf,    1);
        createAttrIfAbsent("RAM",           "ram",          DataType.NUMBER,  "GB",    true,  true,  grpPerf,    2);
        createAttrIfAbsent("Card đồ họa",   "gpu",          DataType.STRING,  null,    false, false, grpPerf,    3);
        createAttrIfAbsent("Tốc độ CPU",    "cpu_speed",    DataType.NUMBER,  "GHz",   true,  false, grpPerf,    4);
        createAttrIfAbsent("Số nhân CPU",   "cpu_cores",    DataType.NUMBER,  "nhân",  true,  false, grpPerf,    5);

        // --- Màn hình ---
        createAttrIfAbsent("Kích thước màn hình", "screen_size",       DataType.NUMBER,  "inch",  true,  true,  grpScreen, 1);
        createAttrIfAbsent("Độ phân giải",        "screen_resolution", DataType.STRING,  null,    false, false, grpScreen, 2);
        createAttrIfAbsent("Tần số quét",          "refresh_rate",      DataType.NUMBER,  "Hz",    true,  false, grpScreen, 3);
        createAttrIfAbsent("Công nghệ tấm nền",   "panel_type",        DataType.STRING,  null,    false, false, grpScreen, 4);
        createAttrIfAbsent("Độ sáng tối đa",      "brightness",        DataType.NUMBER,  "nits",  false, false, grpScreen, 5);

        // --- Lưu trữ ---
        createAttrIfAbsent("SSD",  "ssd",  DataType.NUMBER, "GB",  true, true,  grpStorage, 1);
        createAttrIfAbsent("HDD",  "hdd",  DataType.NUMBER, "GB",  true, false, grpStorage, 2);
        createAttrIfAbsent("eMMC", "emmc", DataType.NUMBER, "GB",  true, false, grpStorage, 3);

        // --- Kết nối ---
        createAttrIfAbsent("WiFi",        "wifi",      DataType.STRING,  null, false, false, grpConnect, 1);
        createAttrIfAbsent("Bluetooth",   "bluetooth", DataType.STRING,  null, false, false, grpConnect, 2);
        createAttrIfAbsent("Cổng USB",    "usb_ports", DataType.STRING,  null, false, false, grpConnect, 3);
        createAttrIfAbsent("Cổng sạc",    "charging_port", DataType.STRING, null, false, false, grpConnect, 4);
        createAttrIfAbsent("Hỗ trợ 5G",  "support_5g",  DataType.BOOLEAN, null, true,  false, grpConnect, 5);
        createAttrIfAbsent("NFC",         "nfc",         DataType.BOOLEAN, null, false, false, grpConnect, 6);

        // --- Pin & Sạc ---
        createAttrIfAbsent("Dung lượng pin",   "battery_capacity", DataType.NUMBER, "mAh", true,  true,  grpBattery, 1);
        createAttrIfAbsent("Công suất sạc",    "charging_watt",    DataType.NUMBER, "W",   false, false, grpBattery, 2);
        createAttrIfAbsent("Sạc không dây",    "wireless_charging",DataType.BOOLEAN,null,  false, false, grpBattery, 3);

        // --- Camera ---
        createAttrIfAbsent("Camera sau",       "rear_camera",  DataType.NUMBER, "MP", false, false, grpCamera, 1);
        createAttrIfAbsent("Camera trước",     "front_camera", DataType.NUMBER, "MP", false, false, grpCamera, 2);
        createAttrIfAbsent("Số camera sau",    "rear_cam_count",DataType.NUMBER,"ống",false, false, grpCamera, 3);

        // --- Thiết kế ---
        createAttrIfAbsent("Màu sắc",   "color",   DataType.LIST,   null, false, false, grpDesign, 1);
        createAttrIfAbsent("Trọng lượng","weight", DataType.NUMBER, "g",  false, false, grpDesign, 2);
        createAttrIfAbsent("Chất liệu", "material",DataType.STRING, null, false, false, grpDesign, 3);

        // --- Hệ điều hành ---
        createAttrIfAbsent("Hệ điều hành",  "os",         DataType.STRING, null, false, false, grpOS, 1);
        createAttrIfAbsent("Phiên bản OS",  "os_version", DataType.STRING, null, false, false, grpOS, 2);

        System.out.println("✓ Hoàn tất khởi tạo định nghĩa thuộc tính EAV");
    }

    private void createAttrIfAbsent(String name, String attrKey, DataType dataType,
                                     String unit, boolean isFilterable, boolean isRequired,
                                     AttributeGroup group, int order) {
        if (!attributeDefinitionRepository.existsByAttrKey(attrKey)) {
            AttributeDefinition def = new AttributeDefinition();
            def.setName(name);
            def.setAttrKey(attrKey);
            def.setDataType(dataType);
            def.setUnit(unit);
            def.setIsFilterable(isFilterable);
            def.setIsRequired(isRequired);
            def.setAttributeGroup(group);
            def.setDisplayOrder(order);
            attributeDefinitionRepository.save(def);
        }
    }
}
       
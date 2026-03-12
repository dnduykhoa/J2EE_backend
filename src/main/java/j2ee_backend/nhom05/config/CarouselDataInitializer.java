package j2ee_backend.nhom05.config;

import j2ee_backend.nhom05.model.CarouselSlide;
import j2ee_backend.nhom05.repository.ICarouselSlideRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class CarouselDataInitializer implements CommandLineRunner {

    @Autowired
    private ICarouselSlideRepository repository;

    @Override
    public void run(String... args) throws Exception {
        if (repository.count() > 0) return; // Đã có dữ liệu thì bỏ qua

        CarouselSlide s1 = new CarouselSlide();
        s1.setImage("/image.png");
        s1.setBadge("Công nghệ hàng đầu");
        s1.setTitle("Thiết bị công nghệ\nchính hãng, giá tốt");
        s1.setSubtitle("Laptop, điện thoại và phụ kiện từ các thương hiệu uy tín — giao hàng nhanh toàn quốc.");
        s1.setButtonText("Mua ngay");
        s1.setButtonLink("/products");
        s1.setDisplayOrder(1);
        s1.setIntervalMs(4000);
        s1.setIsActive(true);
        repository.save(s1);

        CarouselSlide s2 = new CarouselSlide();
        s2.setImage("/image2.png");
        s2.setBadge("Ưu đãi hôm nay");
        s2.setTitle("Giảm giá lớn\ncho mùa hè này");
        s2.setSubtitle("Hàng ngàn sản phẩm công nghệ giảm giá sâu — chỉ trong thời gian có hạn.");
        s2.setButtonText("Xem ưu đãi");
        s2.setButtonLink("/products");
        s2.setDisplayOrder(2);
        s2.setIntervalMs(4000);
        s2.setIsActive(true);
        repository.save(s2);

        CarouselSlide s3 = new CarouselSlide();
        s3.setImage("/image3.png");
        s3.setBadge("Mới ra mắt");
        s3.setTitle("Sản phẩm mới\nvừa về kho");
        s3.setSubtitle("Cập nhật những thiết bị công nghệ mới nhất, hiện đại nhất tại TechStore.");
        s3.setButtonText("Khám phá ngay");
        s3.setButtonLink("/products");
        s3.setDisplayOrder(3);
        s3.setIntervalMs(4000);
        s3.setIsActive(true);
        repository.save(s3);
    }
}

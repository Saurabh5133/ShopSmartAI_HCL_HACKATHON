package com.shopsmart.config;

import com.shopsmart.entity.*;
import com.shopsmart.repository.*;
import com.shopsmart.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/** Seeds an admin user, a category tree and sample products on an empty database. */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final PasswordEncoder encoder;
    private final AuthService authService;
    @Value("${app.admin.email}") private String adminEmail;
    @Value("${app.admin.password}") private String adminPassword;

    @Override
    public void run(String... args) {
        if (!users.existsByEmail(adminEmail)) {
            User a = new User();
            a.setName("Admin");
            a.setEmail(adminEmail);
            a.setPasswordHash(encoder.encode(adminPassword));
            a.setRole(Role.ADMIN);
            users.save(a);
            authService.createBasicMembership(a);
        }
        if (categories.count() > 0 || products.count() > 0) return;

        Category electronics = cat("Electronics", null);
        Category laptops = cat("Laptops", electronics);
        Category phones = cat("Smartphones", electronics);
        Category audio = cat("Audio", electronics);
        Category home = cat("Home & Kitchen", null);

        p(laptops, "Dell Inspiron 15", "Dell", "65000", "4.5", 8, "Intel i7, 16GB RAM, 512GB SSD. Great for Java development and AI learning.");
        p(laptops, "Lenovo IdeaPad Slim 5", "Lenovo", "58000", "4.4", 12, "Ryzen 7, 16GB RAM, 512GB SSD. Lightweight laptop for coding.");
        p(laptops, "HP Pavilion 14", "HP", "52000", "4.2", 5, "Intel i5, 16GB RAM, 512GB SSD. Everyday productivity.");
        p(laptops, "ASUS ROG Strix G16", "ASUS", "115000", "4.6", 3, "RTX 4060 gaming laptop, 16GB RAM, 1TB SSD.");
        p(laptops, "Acer Aspire 3", "Acer", "32000", "3.9", 0, "Entry-level laptop, 8GB RAM. Currently out of stock.");
        p(phones, "Samsung Galaxy S24", "Samsung", "74999", "4.5", 20, "6.2-inch AMOLED, 8GB RAM, 256GB storage.");
        p(phones, "OnePlus 12R", "OnePlus", "39999", "4.4", 15, "Snapdragon 8 Gen 2, 8GB RAM, fast charging.");
        p(audio, "Sony WH-1000XM5", "Sony", "29990", "4.7", 25, "Industry-leading noise cancelling headphones.");
        p(audio, "boAt Airdopes 141", "boAt", "1299", "4.0", 100, "True wireless earbuds, 42h playback.");
        p(home, "Philips Air Fryer HD9200", "Philips", "7999", "4.3", 30, "4.1L air fryer with rapid air technology.");
    }

    private Category cat(String name, Category parent) {
        Category c = new Category();
        c.setName(name);
        c.setParent(parent);
        return categories.save(c);
    }

    private void p(Category c, String name, String brand, String price, String rating, int stock, String desc) {
        Product p = new Product();
        p.setCategory(c);
        p.setName(name);
        p.setBrand(brand);
        p.setPrice(new BigDecimal(price));
        p.setRating(new BigDecimal(rating));
        p.setStockQuantity(stock);
        p.setDescription(desc);
        products.save(p);
    }
}

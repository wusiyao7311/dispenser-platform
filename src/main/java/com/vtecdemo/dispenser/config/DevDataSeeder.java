package com.vtecdemo.dispenser.config;

import com.vtecdemo.dispenser.model.Dispenser;
import com.vtecdemo.dispenser.model.DispenserStatus;
import com.vtecdemo.dispenser.model.Product;
import com.vtecdemo.dispenser.model.StockLevel;
import com.vtecdemo.dispenser.repository.DispenserRepository;
import com.vtecdemo.dispenser.repository.ProductRepository;
import com.vtecdemo.dispenser.repository.StockLevelRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Seeds a handful of dispensers/products so the dashboard has data on first run in "dev". */
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private final DispenserRepository dispenserRepository;
    private final ProductRepository productRepository;
    private final StockLevelRepository stockLevelRepository;

    public DevDataSeeder(DispenserRepository dispenserRepository,
                          ProductRepository productRepository,
                          StockLevelRepository stockLevelRepository) {
        this.dispenserRepository = dispenserRepository;
        this.productRepository = productRepository;
        this.stockLevelRepository = stockLevelRepository;
    }

    @Override
    public void run(String... args) {
        if (dispenserRepository.count() > 0) {
            return;
        }

        Product water = productRepository.save(new Product("SKU-WATER-500", "Still Water 500ml", new BigDecimal("1.20")));
        Product coffee = productRepository.save(new Product("SKU-COFFEE-BLK", "Black Coffee", new BigDecimal("1.80")));
        Product snack = productRepository.save(new Product("SKU-SNACK-BAR", "Cereal Bar", new BigDecimal("1.50")));

        Dispenser espelkamp = dispenserRepository.save(new Dispenser("DSP-ESP-01", "Espelkamp - Break Room A", DispenserStatus.ACTIVE));
        Dispenser hannover = dispenserRepository.save(new Dispenser("DSP-HAN-02", "Hannover - Lobby", DispenserStatus.ACTIVE));
        Dispenser bielefeld = dispenserRepository.save(new Dispenser("DSP-BIE-03", "Bielefeld - Workshop Floor", DispenserStatus.MAINTENANCE));

        stockLevelRepository.save(new StockLevel(espelkamp, water, 40, 50));
        stockLevelRepository.save(new StockLevel(espelkamp, coffee, 5, 40));   // low stock on purpose
        stockLevelRepository.save(new StockLevel(hannover, water, 12, 50));
        stockLevelRepository.save(new StockLevel(hannover, snack, 30, 30));
        stockLevelRepository.save(new StockLevel(bielefeld, snack, 2, 30));    // low stock on purpose
    }
}

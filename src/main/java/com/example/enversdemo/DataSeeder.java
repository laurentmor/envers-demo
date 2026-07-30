package com.example.enversdemo;

import com.example.enversdemo.entity.Product;
import com.example.enversdemo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Seeds one product and mutates it across three separate transactions,
 * so on startup there is already a 3-revision audit trail (ADD, MOD, MOD)
 * ready to inspect via GET /api/products/{id}/history.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        Product product = new Product();
        product.setName("Wireless Mouse");
        product.setDescription("Ergonomic wireless mouse");
        product.setPrice(new BigDecimal("29.99"));
        product.setQuantity(100);
        Long id = saveInNewTransaction(product);

        updatePriceInNewTransaction(id, new BigDecimal("24.99")); // revision 2: price drop
        updateQuantityInNewTransaction(id, 85);                   // revision 3: stock sold
    }

    @Transactional
    public Long saveInNewTransaction(Product product) {
        return productRepository.save(product).getId();
    }

    @Transactional
    public void updatePriceInNewTransaction(Long id, BigDecimal newPrice) {
        Product p = productRepository.findById(id).orElseThrow();
        p.setPrice(newPrice);
    }

    @Transactional
    public void updateQuantityInNewTransaction(Long id, int newQuantity) {
        Product p = productRepository.findById(id).orElseThrow();
        p.setQuantity(newQuantity);
    }
}

package com.example.enversdemo.service;

import com.example.enversdemo.dto.ProductRevisionDto;
import com.example.enversdemo.entity.Product;
import com.example.enversdemo.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ProductAuditIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductAuditService productAuditService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void descriptionChangesAreCapturedInAuditHistory() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Long productId = tx.execute(status -> {
            Product initial = createProduct("Initial description");
            return productRepository.saveAndFlush(initial).getId();
        });

        tx.executeWithoutResult(status -> {
            Product toUpdate = productRepository.findById(productId).orElseThrow();
            toUpdate.setDescription("Updated description");
            productRepository.saveAndFlush(toUpdate);
        });

        List<ProductRevisionDto> history = tx.execute(status -> productAuditService.getHistory(productId));
        Product latestState = history.get(history.size() - 1).state();

        assertEquals("Updated description", latestState.getDescription());
        assertEquals(1, history.get(history.size() - 1).changes().size());
        assertEquals("description", history.get(history.size() - 1).changes().get(0).field());
        assertEquals("Initial description", history.get(history.size() - 1).changes().get(0).fromValue());
        assertEquals("Updated description", history.get(history.size() - 1).changes().get(0).toValue());
    }

    private Product createProduct(String description) {
        Product product = new Product();
        product.setName("Keyboard");
        product.setDescription(description);
        product.setPrice(new BigDecimal("99.99"));
        product.setQuantity(10);
        return product;
    }
}

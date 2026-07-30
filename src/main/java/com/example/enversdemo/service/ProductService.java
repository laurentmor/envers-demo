package com.example.enversdemo.service;

import com.example.enversdemo.entity.Product;
import com.example.enversdemo.exception.ResourceNotFoundException;
import com.example.enversdemo.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final EntityManager entityManager;

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + id + " not found"));
    }

    public Product create(Product product) {
        // ignore any client-supplied id; this is always an insert
        product.setId(null);
        Product saved = productRepository.save(product);
        flushChanges();
        return saved;
    }

    public Product update(Long id, Product changes) {
        Product existing = findById(id);
        existing.setName(changes.getName());
        existing.setDescription(changes.getDescription());
        existing.setPrice(changes.getPrice());
        existing.setQuantity(changes.getQuantity());
        Product saved = productRepository.save(existing);
        flushChanges();
        return saved;
    }

    private void flushChanges() {
        if (entityManager != null) {
            entityManager.flush();
        }
    }

    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product " + id + " not found");
        }
        productRepository.deleteById(id);
    }
}

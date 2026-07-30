package com.example.enversdemo.service;

import com.example.enversdemo.entity.Product;
import com.example.enversdemo.exception.ResourceNotFoundException;
import com.example.enversdemo.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void findAllReturnsProductsFromRepository() {
        Product product = buildProduct(1L, "Keyboard", "Mechanical", new BigDecimal("99.99"), 10);
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<Product> result = productService.findAll();

        assertEquals(List.of(product), result);
        verify(productRepository).findAll();
    }

    @Test
    void findByIdReturnsProductWhenPresent() {
        Product product = buildProduct(1L, "Keyboard", "Mechanical", new BigDecimal("99.99"), 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Product result = productService.findById(1L);

        assertEquals(product, result);
        verify(productRepository).findById(1L);
    }

    @Test
    void findByIdThrowsWhenProductIsMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> productService.findById(99L));

        assertEquals("Product 99 not found", exception.getMessage());
    }

    @Test
    void createIgnoresClientAssignedId() {
        Product input = buildProduct(7L, "Mouse", "Wireless", new BigDecimal("19.99"), 5);
        Product saved = buildProduct(null, "Mouse", "Wireless", new BigDecimal("19.99"), 5);
        when(productRepository.save(input)).thenReturn(saved);

        Product result = productService.create(input);

        assertEquals(saved, result);
        assertEquals(null, result.getId());
        verify(productRepository).save(input);
    }

    @Test
    void updateCopiesFieldsFromChangesAndSavesExistingProduct() {
        Product existing = buildProduct(1L, "Old", "Old desc", new BigDecimal("10.00"), 1);
        Product changes = buildProduct(2L, "New", "New desc", new BigDecimal("20.00"), 2);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);

        Product result = productService.update(1L, changes);

        assertEquals("New", result.getName());
        assertEquals("New desc", result.getDescription());
        assertEquals(new BigDecimal("20.00"), result.getPrice());
        assertEquals(2, result.getQuantity());
        verify(productRepository).findById(1L);
        verify(productRepository).save(existing);
    }

    @Test
    void deleteThrowsWhenProductDoesNotExist() {
        when(productRepository.existsById(42L)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> productService.delete(42L));

        assertEquals("Product 42 not found", exception.getMessage());
    }

    @Test
    void deleteDeletesWhenProductExists() {
        when(productRepository.existsById(7L)).thenReturn(true);

        productService.delete(7L);

        verify(productRepository).deleteById(7L);
    }

    private Product buildProduct(Long id, String name, String description, BigDecimal price, int quantity) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setQuantity(quantity);
        return product;
    }
}

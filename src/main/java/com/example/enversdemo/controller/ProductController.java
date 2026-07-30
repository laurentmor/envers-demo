package com.example.enversdemo.controller;

import com.example.enversdemo.dto.ProductRevisionDto;
import com.example.enversdemo.entity.Product;
import com.example.enversdemo.service.ProductAuditService;
import com.example.enversdemo.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductAuditService productAuditService;

    // ---- plain CRUD ----------------------------------------------------

    @GetMapping
    public List<Product> findAll() {
        return productService.findAll();
    }

    @GetMapping("/{id}")
    public Product findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody Product product) {
        return productService.create(product);
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @Valid @RequestBody Product product) {
        return productService.update(id, product);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }

    // ---- Envers audit history -------------------------------------------

    /** Full change history for a product: one entry per INSERT/UPDATE/DELETE revision. */
    @GetMapping("/{id}/history")
    public List<ProductRevisionDto> history(@PathVariable Long id) {
        return productAuditService.getHistory(id);
    }

    /** Product state as it existed at one specific revision number. */
    @GetMapping("/{id}/history/{revision}")
    public ProductRevisionDto historyAtRevision(@PathVariable Long id, @PathVariable int revision) {
        return productAuditService.getRevision(id, revision);
    }

    /** Just the revisions where price changed, using an Envers AuditQuery. */
    @GetMapping("/{id}/history/price-changes")
    public List<ProductRevisionDto> priceChanges(@PathVariable Long id) {
        return productAuditService.getPriceChanges(id);
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<String> handleValidation(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return ResponseEntity.badRequest().body(message);
    }
}

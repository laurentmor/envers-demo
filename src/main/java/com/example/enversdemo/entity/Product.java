package com.example.enversdemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain entity whose changes we want to version with Hibernate Envers.
 *
 * {@code @Audited} on the class tells Envers to track every INSERT / UPDATE / DELETE
 * against this entity in a shadow "_AUD" table (product_aud), together with a
 * revision number and revision type (ADD / MOD / DEL).
 *
 * {@code @NotAudited} on a single field excludes just that field from history,
 * which is handy for things like internal/derived data you never need to audit.
 */
@Entity
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @PositiveOrZero
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @PositiveOrZero
    @Column(nullable = false)
    private Integer quantity;
    
    @ManyToOne
    @JoinColumn(name = "added_by_id"  )
    private Worker addedBy;

    /** Excluded from Envers history on purpose, to demonstrate @NotAudited. */
    @NotAudited
    private Instant lastViewedAt;
}

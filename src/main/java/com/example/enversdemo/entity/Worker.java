package com.example.enversdemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

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
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Email
    private String email;
    
    private String phone;
    private String position;
    private Instant hiredAt;
    private Instant terminatedAt;

    @JsonIgnore
    private String password;

    @OneToMany(mappedBy = "addedBy")
    @JsonIgnore
    private Set<Product> products;

}

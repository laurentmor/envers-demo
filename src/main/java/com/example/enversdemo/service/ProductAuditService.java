package com.example.enversdemo.service;

import com.example.enversdemo.audit.AuditRevisionInfo;
import com.example.enversdemo.dto.FieldChangeDto;
import com.example.enversdemo.dto.ProductRevisionDto;
import com.example.enversdemo.entity.Product;
import com.example.enversdemo.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reads the audit trail Envers maintains for {@link Product}.
 *
 * Envers stores history in two extra tables it generates automatically:
 *  - REVINFO      : one row per committed transaction that touched an audited entity
 *                    (revision number + timestamp)
 *  - product_aud  : one row per revision that touched a given Product row, holding
 *                    the field values at that revision plus a REVTYPE (0=ADD, 1=MOD, 2=DEL)
 *
 * Everything here goes through the AuditQuery API (reader.createQuery()...), rather
 * than AuditReader.find()/getRevisions() + a separate revision-type lookup, because
 * forRevisionsOfEntity(...) returns the entity state, the revision metadata, AND the
 * RevisionType together in one query - simpler and one round trip per call.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductAuditService {

    private final EntityManager entityManager;

    private AuditReader auditReader() {
        return AuditReaderFactory.get(entityManager);
    }

    /** Full revision history for one product, oldest revision first. */
    @SuppressWarnings("unchecked")
    public List<ProductRevisionDto> getHistory(Long productId) {
        AuditReader reader = auditReader();

        if (!reader.isEntityClassAudited(Product.class)) {
            throw new IllegalStateException("Product is not registered as @Audited");
        }

        List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(Product.class, false, true)
                .add(AuditEntity.id().eq(productId))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();

        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("No audit history found for product " + productId);
        }

        List<ProductRevisionDto> revisions = new ArrayList<>();
        Product previousEntity = null;

        for (Object[] row : rows) {
            revisions.add(toDto(row, previousEntity));
            previousEntity = (Product) row[0];
        }

        return revisions;
    }

    /** Product state exactly as it was at a specific revision number. */
    @SuppressWarnings("unchecked")
    public ProductRevisionDto getRevision(Long productId, int revisionNumber) {
        AuditReader reader = auditReader();

        List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(Product.class, false, true)
                .add(AuditEntity.id().eq(productId))
                .add(AuditEntity.revisionNumber().eq(revisionNumber))
                .getResultList();

        if (rows.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Product " + productId + " has no state at revision " + revisionNumber);
        }

        return toDto(rows.get(0));
    }

    /**
     * Revisions during which price actually changed, using Envers' AuditQuery API
     * (property-level change tracking via the "modified flags" that
     * global_with_modified_flag=true enables for every audited property).
     */
    @SuppressWarnings("unchecked")
    public List<ProductRevisionDto> getPriceChanges(Long productId) {
        AuditReader reader = auditReader();

        List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(Product.class, false, true)
                .add(AuditEntity.id().eq(productId))
                .add(AuditEntity.property("price").hasChanged())
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();

        return rows.stream().map(this::toDto).toList();
    }

    /** Object[] shape from forRevisionsOfEntity(cls, false, ...): {entity, revisionEntity, revisionType}. */
    private ProductRevisionDto toDto(Object[] row) {
        return toDto(row, null);
    }

    private ProductRevisionDto toDto(Object[] row, Product previousEntity) {
        Product entity = (Product) row[0];
        AuditRevisionInfo revEntity = (AuditRevisionInfo) row[1];
        RevisionType type = (RevisionType) row[2];

        List<FieldChangeDto> changes = new ArrayList<>();
        if (type == RevisionType.ADD) {
            addIfChanged(changes, "name", null, entity.getName());
            addIfChanged(changes, "description", null, entity.getDescription());
            addIfChanged(changes, "price", null, entity.getPrice());
            addIfChanged(changes, "quantity", null, entity.getQuantity());
        } else if (type == RevisionType.DEL) {
            addIfChanged(changes, "name", entity.getName(), null);
            addIfChanged(changes, "description", entity.getDescription(), null);
            addIfChanged(changes, "price", entity.getPrice(), null);
            addIfChanged(changes, "quantity", entity.getQuantity(), null);
        } else if (previousEntity != null) {
            addIfChanged(changes, "name", previousEntity.getName(), entity.getName());
            addIfChanged(changes, "description", previousEntity.getDescription(), entity.getDescription());
            addIfChanged(changes, "price", previousEntity.getPrice(), entity.getPrice());
            addIfChanged(changes, "quantity", previousEntity.getQuantity(), entity.getQuantity());
        }

        return new ProductRevisionDto(
                revEntity.getRev(),
                Instant.ofEpochMilli(revEntity.getTimestamp()),
                type,
                revEntity.getUsername(),
                entity,
                changes);
    }

    private void addIfChanged(List<FieldChangeDto> changes, String field, Object fromValue, Object toValue) {
        if (!Objects.equals(fromValue, toValue)) {
            changes.add(new FieldChangeDto(field, formatValue(fromValue), formatValue(toValue)));
        }
    }

    private String formatValue(Object value) {
        return value == null ? "—" : String.valueOf(value);
    }
}

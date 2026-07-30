package com.example.enversdemo.dto;

import com.example.enversdemo.entity.Product;
import org.hibernate.envers.RevisionType;

import java.time.Instant;
import java.util.List;

/**
 * Flattened view of one Envers revision: the entity state at that revision,
 * the revision metadata (number, type, timestamp) that Envers tracks separately,
 * and a human-readable summary of the fields that changed in that revision.
 */
public record ProductRevisionDto(
        Number revisionNumber,
        Instant revisionTimestamp,
        RevisionType revisionType,
        String username,
        Product state,
        List<FieldChangeDto> changes
) {
}

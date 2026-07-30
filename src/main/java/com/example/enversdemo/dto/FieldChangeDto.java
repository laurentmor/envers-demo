package com.example.enversdemo.dto;

/**
 * Describes a single audited field change in a revision.
 */
public record FieldChangeDto(String field, String fromValue, String toValue) {
}

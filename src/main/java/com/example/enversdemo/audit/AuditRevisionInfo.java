package com.example.enversdemo.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

/**
 * Replaces Envers' built-in {@code DefaultRevisionEntity} with one extra column:
 * the username of whoever was logged in when the revision was committed.
 * {@link AuditRevisionListener} is the piece that actually fills it in.
 *
 * Kept independent (not extending DefaultRevisionEntity) rather than subclassed,
 * per Hibernate's documented pattern for custom revision entities - avoids JPA
 * single-table-inheritance surprises on the REVINFO table.
 */
@Entity
@Table(name = "REVINFO")
@RevisionEntity(AuditRevisionListener.class)
@Getter
@Setter
public class AuditRevisionInfo {

    @Id
    @GeneratedValue
    @RevisionNumber
    @Column(name = "REV")
    private int rev;

    @RevisionTimestamp
    @Column(name = "REVTSTMP")
    private long timestamp;

    /** Username of the authenticated principal at commit time, or "system" outside a request. */
    @Column(name = "USERNAME")
    private String username;
}

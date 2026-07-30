package com.example.enversdemo.audit;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Called by Envers exactly once per transaction that touches an {@code @Audited}
 * entity, right before the revision row is written. Whatever this sets on the
 * revision entity gets persisted alongside the REV/REVTSTMP columns - so every
 * table's {@code _AUD} rows can be joined back to "who did this" with zero
 * per-entity or per-endpoint wiring.
 */
public class AuditRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        AuditRevisionInfo revision = (AuditRevisionInfo) revisionEntity;
        revision.setUsername(currentUsername());
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean realUser = auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
        // Falls back to "system" for changes made outside an authenticated HTTP
        // request - e.g. DataSeeder running at startup, before anyone has logged in.
        return realUser ? auth.getName() : "system";
    }
}

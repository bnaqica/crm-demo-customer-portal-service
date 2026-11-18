package com.bnaqica.crmdemocustomerportalservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A portal user, keyed by the token's {@code sub}. Rows are written by the native upserts in the repository. */
@Entity
@Table(name = "portal_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA
public class PortalUser {

    @Id
    private UUID sub;

    @Column(nullable = false)
    private String email;

    /** {@code iat} of the token the stored email came from. */
    @Column(name = "token_issued_at", nullable = false)
    private Instant tokenIssuedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

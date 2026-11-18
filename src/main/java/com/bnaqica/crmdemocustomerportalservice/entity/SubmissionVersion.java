package com.bnaqica.crmdemocustomerportalservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One version of a submission. Never changed once saved: a correction is a new version. */
@Entity
@Table(name = "submission_version")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA
@AllArgsConstructor
public class SubmissionVersion {

    @Id
    private UUID id;

    @Column(name = "submission_id", nullable = false, updatable = false)
    private UUID submissionId;

    @Column(nullable = false, updatable = false)
    private int version;

    /** The submitted data, a JSON object. */
    @Column(nullable = false, updatable = false)
    private String data;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "submission_version_document",
            joinColumns = @JoinColumn(name = "submission_version_id"),
            inverseJoinColumns = @JoinColumn(name = "document_id"))
    private Set<Document> documents;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

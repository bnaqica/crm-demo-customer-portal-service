package com.bnaqica.crmdemocustomerportalservice.service;

import com.bnaqica.crmdemocustomerportalservice.entity.Submission;
import com.bnaqica.crmdemocustomerportalservice.entity.SubmissionVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** CRUD for {@code submission} and its immutable {@code submission_version}s. */
public interface SubmissionService {

    Submission save(Submission submission);

    SubmissionVersion addVersion(SubmissionVersion version);

    Optional<Submission> find(UUID id);

    Optional<Submission> findOwned(UUID id, UUID owner);

    /** As {@link #findOwned}, locked until the caller's transaction ends. */
    Optional<Submission> lockOwned(UUID id, UUID owner);

    List<Submission> listOwned(UUID owner);

    /** Oldest first, with their documents. */
    List<SubmissionVersion> versions(UUID submissionId);
}

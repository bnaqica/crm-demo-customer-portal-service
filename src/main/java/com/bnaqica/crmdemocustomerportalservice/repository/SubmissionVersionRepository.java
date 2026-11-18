package com.bnaqica.crmdemocustomerportalservice.repository;

import com.bnaqica.crmdemocustomerportalservice.entity.SubmissionVersion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionVersionRepository extends JpaRepository<SubmissionVersion, UUID> {

    @EntityGraph(attributePaths = "documents")
    List<SubmissionVersion> findAllBySubmissionIdOrderByVersion(UUID submissionId);
}

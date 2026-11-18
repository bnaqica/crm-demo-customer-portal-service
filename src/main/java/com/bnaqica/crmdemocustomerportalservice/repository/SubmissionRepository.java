package com.bnaqica.crmdemocustomerportalservice.repository;

import com.bnaqica.crmdemocustomerportalservice.entity.Submission;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    Optional<Submission> findByIdAndOwnerSub(UUID id, UUID ownerSub);

    /** As {@link #findByIdAndOwnerSub}, locked until the transaction ends, so corrections run one at a time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Submission s WHERE s.id = :id AND s.ownerSub = :owner")
    Optional<Submission> lockOwned(@Param("id") UUID id, @Param("owner") UUID owner);

    List<Submission> findAllByOwnerSubOrderByCreatedAtDesc(UUID ownerSub);
}

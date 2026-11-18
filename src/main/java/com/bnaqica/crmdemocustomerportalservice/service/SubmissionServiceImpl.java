package com.bnaqica.crmdemocustomerportalservice.service;

import com.bnaqica.crmdemocustomerportalservice.entity.Submission;
import com.bnaqica.crmdemocustomerportalservice.entity.SubmissionVersion;
import com.bnaqica.crmdemocustomerportalservice.repository.SubmissionRepository;
import com.bnaqica.crmdemocustomerportalservice.repository.SubmissionVersionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissions;
    private final SubmissionVersionRepository versions;

    @Override
    @Transactional
    public Submission save(Submission submission) {
        return submissions.save(submission);
    }

    @Override
    @Transactional
    public SubmissionVersion addVersion(SubmissionVersion version) {
        return versions.save(version);
    }

    @Override
    public Optional<Submission> find(UUID id) {
        return submissions.findById(id);
    }

    @Override
    public Optional<Submission> findOwned(UUID id, UUID owner) {
        return submissions.findByIdAndOwnerSub(id, owner);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Submission> lockOwned(UUID id, UUID owner) {
        return submissions.lockOwned(id, owner);
    }

    @Override
    public List<Submission> listOwned(UUID owner) {
        return submissions.findAllByOwnerSubOrderByCreatedAtDesc(owner);
    }

    @Override
    public List<SubmissionVersion> versions(UUID submissionId) {
        return versions.findAllBySubmissionIdOrderByVersion(submissionId);
    }
}

package com.bnaqica.crmdemocustomerportalservice.service;

import com.bnaqica.crmdemocustomerportalservice.entity.PortalUser;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** CRUD for {@code portal_user}. */
public interface PortalUserService {

    /** @return whether the user was created by this call */
    boolean createIfAbsent(UUID sub, String email, Instant tokenIssuedAt);

    /** @return whether the stored email was different, older than {@code tokenIssuedAt}, and has been changed */
    boolean changeEmail(UUID sub, String email, Instant tokenIssuedAt);

    Optional<PortalUser> find(UUID sub);

    /** Locks the user's row until the transaction ends, so per-user checks such as the quota run one at a time. */
    void lock(UUID sub);
}

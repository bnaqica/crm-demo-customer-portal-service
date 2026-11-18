package com.bnaqica.crmdemocustomerportalservice.repository;

import com.bnaqica.crmdemocustomerportalservice.entity.PortalUser;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Writes are single statements, so two concurrent first calls of the same user create one row and only one of them
 * sees it created.
 */
public interface PortalUserRepository extends JpaRepository<PortalUser, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM PortalUser u WHERE u.sub = :sub")
    Optional<PortalUser> lockBySub(@Param("sub") UUID sub);

    /** @return 1 if the row was created, 0 if the user already existed */
    @Modifying
    @Query(
            value = "INSERT INTO {h-schema}portal_user (sub, email, token_issued_at, created_at, updated_at)"
                    + " VALUES (:sub, :email, :issuedAt, :now, :now) ON CONFLICT (sub) DO NOTHING",
            nativeQuery = true)
    int insertIfAbsent(
            @Param("sub") UUID sub,
            @Param("email") String email,
            @Param("issuedAt") Instant issuedAt,
            @Param("now") Instant now);

    /**
     * Stores {@code email} only if it differs and comes from a newer token than the stored one, so a user with an
     * old token still in use on another device doesn't flip the email back. {@code iat} has one-second
     * resolution, so a token from the same second as the stored one counts as newer.
     *
     * @return 1 if the stored email changed, otherwise 0
     */
    @Modifying(clearAutomatically = true)
    @Query(
            value = "UPDATE {h-schema}portal_user SET email = :email, token_issued_at = :issuedAt, updated_at = :now"
                    + " WHERE sub = :sub AND email <> :email AND token_issued_at <= :issuedAt",
            nativeQuery = true)
    int updateEmailIfChanged(
            @Param("sub") UUID sub,
            @Param("email") String email,
            @Param("issuedAt") Instant issuedAt,
            @Param("now") Instant now);
}

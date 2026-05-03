package de.tellerstatttonne.backend.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshTokenEntity t set t.revoked = true where t.userId = :userId")
    void revokeAllForUser(@Param("userId") Long userId);

    @Modifying
    @Query("delete from RefreshTokenEntity t where t.id = :id and t.revoked = false")
    int deleteByIdAndRevokedFalse(@Param("id") Long id);
}

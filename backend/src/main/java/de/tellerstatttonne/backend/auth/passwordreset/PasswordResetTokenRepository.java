package de.tellerstatttonne.backend.auth.passwordreset;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from PasswordResetTokenEntity t where t.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}

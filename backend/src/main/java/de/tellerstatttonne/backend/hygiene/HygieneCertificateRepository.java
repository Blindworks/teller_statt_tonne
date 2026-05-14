package de.tellerstatttonne.backend.hygiene;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HygieneCertificateRepository extends JpaRepository<HygieneCertificateEntity, Long> {

    /** Latest record (any status) for a user; semantic replacement of historic findByUserId. */
    Optional<HygieneCertificateEntity> findFirstByUser_IdOrderByCreatedAtDesc(Long userId);

    /** Latest APPROVED record for a user (current effective certificate). */
    Optional<HygieneCertificateEntity> findFirstByUser_IdAndStatusOrderByExpiryDateDesc(
        Long userId, HygieneCertificateStatus status);

    List<HygieneCertificateEntity> findByStatusOrderByCreatedAtAsc(HygieneCertificateStatus status);

    List<HygieneCertificateEntity> findAllByOrderByCreatedAtAsc();

    long countByStatus(HygieneCertificateStatus status);

    List<HygieneCertificateEntity> findByStatusAndExpiryDateAndWarningSentAtIsNull(
        HygieneCertificateStatus status, LocalDate expiryDate);

    List<HygieneCertificateEntity> findByStatusAndExpiryDateBeforeAndExpiredNoticeSentAtIsNull(
        HygieneCertificateStatus status, LocalDate threshold);

    List<HygieneCertificateEntity> findByUser_IdAndStatus(Long userId, HygieneCertificateStatus status);
}

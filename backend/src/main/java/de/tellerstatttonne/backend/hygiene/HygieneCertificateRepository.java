package de.tellerstatttonne.backend.hygiene;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HygieneCertificateRepository extends JpaRepository<HygieneCertificateEntity, Long> {

    Optional<HygieneCertificateEntity> findByUserId(Long userId);

    List<HygieneCertificateEntity> findByStatusOrderByCreatedAtAsc(HygieneCertificateStatus status);

    List<HygieneCertificateEntity> findAllByOrderByCreatedAtAsc();

    long countByStatus(HygieneCertificateStatus status);
}

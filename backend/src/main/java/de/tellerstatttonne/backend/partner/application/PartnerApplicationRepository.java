package de.tellerstatttonne.backend.partner.application;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerApplicationRepository extends JpaRepository<PartnerApplicationEntity, Long> {

    List<PartnerApplicationEntity> findByPartnerIdOrderByCreatedAtDesc(Long partnerId);

    List<PartnerApplicationEntity> findByPartnerIdAndStatusOrderByCreatedAtDesc(Long partnerId, ApplicationStatus status);

    List<PartnerApplicationEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByPartnerIdAndUserIdAndStatus(Long partnerId, Long userId, ApplicationStatus status);

    long countByStatus(ApplicationStatus status);
}

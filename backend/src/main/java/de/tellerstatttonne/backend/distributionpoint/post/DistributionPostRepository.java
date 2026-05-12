package de.tellerstatttonne.backend.distributionpoint.post;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistributionPostRepository extends JpaRepository<DistributionPostEntity, Long> {
    Optional<DistributionPostEntity> findByPickupRunId(Long pickupRunId);
    List<DistributionPostEntity> findByDistributionPointIdOrderByCreatedAtDesc(Long distributionPointId);
    List<DistributionPostEntity> findByDistributionPointIdAndStatusOrderByCreatedAtDesc(Long distributionPointId, DistributionPostStatus status);
}

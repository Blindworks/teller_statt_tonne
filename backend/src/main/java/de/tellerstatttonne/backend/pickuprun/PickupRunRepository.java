package de.tellerstatttonne.backend.pickuprun;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PickupRunRepository extends JpaRepository<PickupRunEntity, Long> {
    Optional<PickupRunEntity> findByPickupIdAndRetterId(Long pickupId, Long retterId);
}

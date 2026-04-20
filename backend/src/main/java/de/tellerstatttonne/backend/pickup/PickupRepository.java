package de.tellerstatttonne.backend.pickup;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PickupRepository extends JpaRepository<PickupEntity, String> {

    List<PickupEntity> findByDateBetweenOrderByDateAscStartTimeAsc(LocalDate from, LocalDate to);

    List<PickupEntity> findTop10ByOrderByDateDescStartTimeDesc();

    List<PickupEntity> findByStatusAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
        Pickup.Status status, LocalDate from, Pageable pageable);
}

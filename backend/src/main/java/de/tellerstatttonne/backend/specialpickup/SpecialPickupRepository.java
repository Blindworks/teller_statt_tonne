package de.tellerstatttonne.backend.specialpickup;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialPickupRepository extends JpaRepository<SpecialPickupEntity, Long> {

    List<SpecialPickupEntity> findByEndDateGreaterThanEqualOrderByStartDateAsc(LocalDate today);

    List<SpecialPickupEntity> findByEndDateLessThanOrderByStartDateDesc(LocalDate today);
}

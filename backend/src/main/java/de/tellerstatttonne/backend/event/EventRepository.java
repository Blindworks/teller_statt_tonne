package de.tellerstatttonne.backend.event;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<EventEntity, Long> {

    List<EventEntity> findByEndDateGreaterThanEqualOrderByStartDateAsc(LocalDate today);

    List<EventEntity> findByEndDateLessThanOrderByStartDateDesc(LocalDate today);
}

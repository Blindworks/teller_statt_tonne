package de.tellerstatttonne.backend.introduction;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntroductionSlotRepository extends JpaRepository<IntroductionSlotEntity, Long> {

    List<IntroductionSlotEntity> findByDateGreaterThanEqualOrderByDateAscStartTimeAsc(LocalDate date);

    List<IntroductionSlotEntity> findAllByOrderByDateDescStartTimeDesc();
}

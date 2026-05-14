package de.tellerstatttonne.backend.introduction;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntroductionBookingRepository extends JpaRepository<IntroductionBookingEntity, Long> {

    List<IntroductionBookingEntity> findBySlotIdAndCancelledAtIsNull(Long slotId);

    Optional<IntroductionBookingEntity> findByUserIdAndCancelledAtIsNull(Long userId);

    long countBySlotIdAndCancelledAtIsNull(Long slotId);

    List<IntroductionBookingEntity> findBySlotId(Long slotId);
}

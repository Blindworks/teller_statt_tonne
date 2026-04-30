package de.tellerstatttonne.backend.pickup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PickupRepository extends JpaRepository<PickupEntity, String> {

    List<PickupEntity> findByDateBetweenOrderByDateAscStartTimeAsc(LocalDate from, LocalDate to);

    List<PickupEntity> findTop10ByOrderByDateDescStartTimeDesc();

    List<PickupEntity> findByStatusAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
        Pickup.Status status, LocalDate from, Pageable pageable);

    @Query("""
        select a.memberId as memberId,
               max(p.date) as lastPickupDate,
               coalesce(sum(p.savedKg), 0) as totalSavedKg,
               count(p) as pickupCount
        from PickupEntity p
        join p.assignments a
        where p.partner.id = :partnerId
        group by a.memberId
        """)
    List<MemberPickupStats> aggregateByPartner(@Param("partnerId") String partnerId);

    interface MemberPickupStats {
        String getMemberId();
        LocalDate getLastPickupDate();
        BigDecimal getTotalSavedKg();
        Long getPickupCount();
    }
}

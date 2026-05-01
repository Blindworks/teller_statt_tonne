package de.tellerstatttonne.backend.pickup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PickupRepository extends JpaRepository<PickupEntity, Long> {

    List<PickupEntity> findByDateBetweenOrderByDateAscStartTimeAsc(LocalDate from, LocalDate to);

    List<PickupEntity> findTop10ByOrderByDateDescStartTimeDesc();

    List<PickupEntity> findByStatusAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
        Pickup.Status status, LocalDate from, Pageable pageable);

    List<PickupEntity> findByPartnerIdAndStatusAndDateGreaterThanEqual(
        Long partnerId, Pickup.Status status, LocalDate from);

    @Query("""
        select a.userId as memberId,
               max(p.date) as lastPickupDate,
               coalesce(sum(p.savedKg), 0) as totalSavedKg,
               count(p) as pickupCount
        from PickupEntity p
        join p.assignments a
        where p.partner.id = :partnerId
        group by a.userId
        """)
    List<MemberPickupStats> aggregateByPartner(@Param("partnerId") Long partnerId);

    interface MemberPickupStats {
        Long getMemberId();
        LocalDate getLastPickupDate();
        BigDecimal getTotalSavedKg();
        Long getPickupCount();
    }
}

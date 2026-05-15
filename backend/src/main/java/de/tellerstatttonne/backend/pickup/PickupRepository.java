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
        select case when count(p) > 0 then true else false end
        from PickupEntity p
        where p.event.id = :eventId
          and p.date = :date
          and p.startTime < :endTime
          and p.endTime > :startTime
        """)
    boolean existsEventPickupOverlap(
        @Param("eventId") Long eventId,
        @Param("date") LocalDate date,
        @Param("startTime") String startTime,
        @Param("endTime") String endTime);

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

    @Query("""
        select coalesce(sum(p.savedKg), 0) as totalSavedKg,
               count(p) as pickupCount
        from PickupEntity p
        where p.status = de.tellerstatttonne.backend.pickup.Pickup.Status.COMPLETED
        """)
    OverallStats aggregateOverall();

    @Query("""
        select p.partner.id as partnerId,
               p.partner.name as partnerName,
               coalesce(sum(p.savedKg), 0) as savedKg,
               count(p) as pickupCount
        from PickupEntity p
        where p.status = de.tellerstatttonne.backend.pickup.Pickup.Status.COMPLETED
        group by p.partner.id, p.partner.name
        order by coalesce(sum(p.savedKg), 0) desc
        """)
    List<PartnerStats> aggregateTopPartners(Pageable pageable);

    @Query("""
        select a.userId as memberId,
               coalesce(sum(p.savedKg), 0) as savedKg,
               count(p) as pickupCount
        from PickupEntity p
        join p.assignments a
        where p.status = de.tellerstatttonne.backend.pickup.Pickup.Status.COMPLETED
        group by a.userId
        order by coalesce(sum(p.savedKg), 0) desc
        """)
    List<MemberStats> aggregateTopMembers(Pageable pageable);

    interface MemberPickupStats {
        Long getMemberId();
        LocalDate getLastPickupDate();
        BigDecimal getTotalSavedKg();
        Long getPickupCount();
    }

    interface OverallStats {
        BigDecimal getTotalSavedKg();
        Long getPickupCount();
    }

    interface PartnerStats {
        Long getPartnerId();
        String getPartnerName();
        BigDecimal getSavedKg();
        Long getPickupCount();
    }

    interface MemberStats {
        Long getMemberId();
        BigDecimal getSavedKg();
        Long getPickupCount();
    }
}

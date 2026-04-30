package de.tellerstatttonne.backend.member.availability;

import de.tellerstatttonne.backend.partner.Partner;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberAvailabilityRepository
    extends JpaRepository<MemberAvailabilityEntity, String> {

    List<MemberAvailabilityEntity> findByMemberId(String memberId);

    void deleteByMemberId(String memberId);

    /**
     * Counts distinct ACTIVE members that have at least one availability covering the given slot.
     * Coverage means the member's availability fully contains the slot window:
     *   availability.start_time <= slot.start AND availability.end_time >= slot.end
     */
    @Query("""
        SELECT COUNT(DISTINCT a.memberId)
          FROM MemberAvailabilityEntity a
          JOIN de.tellerstatttonne.backend.member.MemberEntity m ON m.id = a.memberId
         WHERE m.status = de.tellerstatttonne.backend.member.Member.Status.ACTIVE
           AND a.weekday = :weekday
           AND a.startTime <= :startTime
           AND a.endTime   >= :endTime
        """)
    long countAvailableMembersForSlot(
        @Param("weekday") Partner.Weekday weekday,
        @Param("startTime") String startTime,
        @Param("endTime") String endTime
    );
}

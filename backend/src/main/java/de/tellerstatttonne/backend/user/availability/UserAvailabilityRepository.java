package de.tellerstatttonne.backend.user.availability;

import de.tellerstatttonne.backend.partner.Partner;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAvailabilityRepository
    extends JpaRepository<UserAvailabilityEntity, Long> {

    List<UserAvailabilityEntity> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    /**
     * Counts distinct ACTIVE users that have at least one availability covering the given slot.
     * Coverage means the user's availability fully contains the slot window:
     *   availability.start_time <= slot.start AND availability.end_time >= slot.end
     */
    @Query("""
        SELECT COUNT(DISTINCT a.userId)
          FROM UserAvailabilityEntity a
          JOIN de.tellerstatttonne.backend.user.UserEntity u ON u.id = a.userId
         WHERE u.status = de.tellerstatttonne.backend.user.UserEntity.Status.ACTIVE
           AND a.weekday = :weekday
           AND a.startTime <= :startTime
           AND a.endTime   >= :endTime
        """)
    long countAvailableUsersForSlot(
        @Param("weekday") Partner.Weekday weekday,
        @Param("startTime") String startTime,
        @Param("endTime") String endTime
    );
}

package de.tellerstatttonne.backend.appointment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<AppointmentEntity, Long> {

    @Query("""
        select distinct a from AppointmentEntity a
        left join a.targetRoles r
        where r.id in :roleIds
          and a.endTime >= :from
        order by a.startTime asc
        """)
    List<AppointmentEntity> findUpcomingForRoles(
        @Param("roleIds") Collection<Long> roleIds,
        @Param("from") Instant from
    );

    @Query("""
        select distinct a from AppointmentEntity a
        left join a.targetRoles r
        where r.id in :roleIds
          and a.endTime < :from
        order by a.startTime desc
        """)
    List<AppointmentEntity> findPastForRoles(
        @Param("roleIds") Collection<Long> roleIds,
        @Param("from") Instant from
    );

    @Query("""
        select a from AppointmentEntity a
        where a.endTime >= :from
        order by a.startTime asc
        """)
    List<AppointmentEntity> findUpcomingAll(@Param("from") Instant from);

    @Query("""
        select a from AppointmentEntity a
        where a.endTime < :from
        order by a.startTime desc
        """)
    List<AppointmentEntity> findPastAll(@Param("from") Instant from);

    @Query("""
        select a from AppointmentEntity a
        where a.isPublic = true
          and a.endTime >= :from
        order by a.startTime asc
        """)
    List<AppointmentEntity> findPublicUpcoming(@Param("from") Instant from);
}

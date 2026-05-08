package de.tellerstatttonne.backend.systemlog;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SystemLogRepository
    extends JpaRepository<SystemLogEntity, Long>, JpaSpecificationExecutor<SystemLogEntity> {

    @Modifying
    @Query("delete from SystemLogEntity s where s.createdAt < :threshold")
    int deleteOlderThan(Instant threshold);
}

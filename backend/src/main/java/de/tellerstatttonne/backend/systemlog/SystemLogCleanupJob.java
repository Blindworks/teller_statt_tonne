package de.tellerstatttonne.backend.systemlog;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SystemLogCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(SystemLogCleanupJob.class);

    private final SystemLogService service;
    private final long retentionDays;

    public SystemLogCleanupJob(
        SystemLogService service,
        @Value("${systemlog.retention-days:90}") long retentionDays
    ) {
        this.service = service;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${systemlog.cleanup-cron:0 0 3 * * *}")
    public void cleanup() {
        if (retentionDays <= 0) {
            log.debug("Systemlog-Cleanup deaktiviert (retention-days <= 0)");
            return;
        }
        Instant threshold = Instant.now().minus(Duration.ofDays(retentionDays));
        int deleted = service.deleteOlderThan(threshold);
        if (deleted > 0) {
            log.info("Systemlog-Cleanup: {} Eintraege aelter als {} Tage geloescht", deleted, retentionDays);
        }
    }
}

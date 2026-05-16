package de.tellerstatttonne.backend.pickup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PickupAutoGenerationScheduler {

    private static final Logger log = LoggerFactory.getLogger(PickupAutoGenerationScheduler.class);

    private final PickupAutoGenerationService service;

    public PickupAutoGenerationScheduler(PickupAutoGenerationService service) {
        this.service = service;
    }

    @Scheduled(cron = "${app.pickup.auto-generation.cron:0 30 3 * * *}")
    public void runDaily() {
        try {
            service.runOnce();
        } catch (RuntimeException e) {
            log.error("Pickup auto-generation scheduler failed: {}", e.getMessage(), e);
        }
    }
}

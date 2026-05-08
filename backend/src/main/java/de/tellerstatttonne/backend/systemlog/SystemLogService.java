package de.tellerstatttonne.backend.systemlog;

import de.tellerstatttonne.backend.systemlog.event.SystemLogEvent;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class SystemLogService {

    private static final Logger log = LoggerFactory.getLogger(SystemLogService.class);
    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int MAX_USER_AGENT_LENGTH = 500;

    private final SystemLogRepository repository;

    public SystemLogService(SystemLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Persistiert einen Event-Eintrag in einer eigenen Transaktion. Funktioniert sowohl bei
     * erfolgreichen Aufrufen als auch wenn die umgebende Transaktion zurückgerollt wird
     * (z. B. fehlgeschlagener Login).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SystemLogEntity record(SystemLogEvent event) {
        SystemLogEntity entity = new SystemLogEntity();
        entity.setEventType(event.eventType());
        entity.setCategory(event.eventType().getCategory());
        entity.setSeverity(event.severity() != null ? event.severity() : event.eventType().getDefaultSeverity());
        entity.setActorUserId(event.actorUserId());
        entity.setActorEmail(truncate(event.actorEmail(), 255));
        entity.setTargetType(event.targetType());
        entity.setTargetId(event.targetId());
        entity.setMessage(truncate(event.message() != null ? event.message() : event.eventType().name(), MAX_MESSAGE_LENGTH));
        entity.setDetails(event.details());

        HttpServletRequest request = currentRequest();
        if (request != null) {
            entity.setIpAddress(extractIp(request));
            entity.setUserAgent(truncate(request.getHeader("User-Agent"), MAX_USER_AGENT_LENGTH));
        }

        try {
            return repository.save(entity);
        } catch (RuntimeException ex) {
            // Niemals einen Fehler im Logging zurück an den Aufrufer durchreichen
            log.error("System-Log Eintrag konnte nicht gespeichert werden ({}): {}",
                event.eventType(), ex.getMessage(), ex);
            return null;
        }
    }

    @Transactional(readOnly = true)
    public Page<SystemLogEntity> find(SystemLogFilter filter, Pageable pageable) {
        return repository.findAll(toSpec(filter), pageable);
    }

    @Transactional
    public int deleteOlderThan(Instant threshold) {
        return repository.deleteOlderThan(threshold);
    }

    private Specification<SystemLogEntity> toSpec(SystemLogFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter == null) {
                return cb.conjunction();
            }
            if (filter.category() != null) {
                predicates.add(cb.equal(root.get("category"), filter.category()));
            }
            if (filter.eventType() != null) {
                predicates.add(cb.equal(root.get("eventType"), filter.eventType()));
            }
            if (filter.severity() != null) {
                predicates.add(cb.equal(root.get("severity"), filter.severity()));
            }
            if (filter.actorUserId() != null) {
                predicates.add(cb.equal(root.get("actorUserId"), filter.actorUserId()));
            }
            if (filter.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), filter.to()));
            }
            if (filter.search() != null && !filter.search().isBlank()) {
                String pattern = "%" + filter.search().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("message")), pattern),
                    cb.like(cb.lower(root.get("actorEmail")), pattern)
                ));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }

    private static String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String first = (comma < 0 ? forwarded : forwarded.substring(0, comma)).trim();
            if (!first.isEmpty()) return first;
        }
        return request.getRemoteAddr();
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }

    /** Hilfsmethode für Aufrufer, die sich nicht um Sub-Sekunden kümmern wollen. */
    public static Instant nowTruncated() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }
}

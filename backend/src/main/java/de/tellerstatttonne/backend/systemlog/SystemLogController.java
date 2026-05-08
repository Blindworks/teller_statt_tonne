package de.tellerstatttonne.backend.systemlog;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/system-log")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class SystemLogController {

    private static final int MAX_PAGE_SIZE = 200;

    private final SystemLogService service;

    public SystemLogController(SystemLogService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<SystemLogPageResponse> list(
        @RequestParam(required = false) SystemLogCategory category,
        @RequestParam(required = false) SystemLogEventType eventType,
        @RequestParam(required = false) SystemLogSeverity severity,
        @RequestParam(required = false) Long actorUserId,
        @RequestParam(required = false) Instant from,
        @RequestParam(required = false) Instant to,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        SystemLogFilter filter = new SystemLogFilter(category, eventType, severity, actorUserId, from, to, search);
        Page<SystemLogEntity> result = service.find(
            filter,
            PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
        );
        return ResponseEntity.ok(new SystemLogPageResponse(
            result.map(SystemLogDto::from).getContent(),
            result.getTotalElements(),
            result.getTotalPages(),
            result.getNumber(),
            result.getSize()
        ));
    }

    @GetMapping("/event-types")
    public Map<String, List<String>> eventTypes() {
        return Map.of(
            "categories", List.of(SystemLogCategory.values()).stream().map(Enum::name).toList(),
            "severities", List.of(SystemLogSeverity.values()).stream().map(Enum::name).toList(),
            "eventTypes", List.of(SystemLogEventType.values()).stream().map(Enum::name).toList()
        );
    }

    public record SystemLogPageResponse(
        List<SystemLogDto> content,
        long totalElements,
        int totalPages,
        int page,
        int size
    ) {}
}

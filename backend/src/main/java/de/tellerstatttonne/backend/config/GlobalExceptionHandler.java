package de.tellerstatttonne.backend.config;

import de.tellerstatttonne.backend.systemlog.SystemLogEventType;
import de.tellerstatttonne.backend.systemlog.event.SystemLogEvent;
import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

/**
 * Fängt unerwartete Exceptions, die nicht von den Controller-lokalen Handlern abgedeckt werden,
 * loggt sie als SYSTEM-Event und liefert eine generische 500-Antwort.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final int MAX_DETAILS = 4000;

    private final ApplicationEventPublisher publisher;

    public GlobalExceptionHandler(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * Erwarteter Lifecycle-Event: SSE/Async-Client hat die Verbindung getrennt. Kein ERROR-Log
     * und kein SystemLog-Eintrag — der Emitter wird vom {@code NotificationStreamService} selbst
     * aufgeräumt, der Response-Body ist irrelevant, weil der Client weg ist.
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public ResponseEntity<Void> handleClientDisconnect(AsyncRequestNotUsableException ex, HttpServletRequest request) {
        log.debug("Async-Client für {} getrennt: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unbehandelte Exception in {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        Long actorId = currentUserId();
        String details = "URI: " + request.getMethod() + " " + request.getRequestURI() + "\n\n" + stackTrace(ex);
        if (details.length() > MAX_DETAILS) {
            details = details.substring(0, MAX_DETAILS);
        }

        publisher.publishEvent(SystemLogEvent.of(SystemLogEventType.UNHANDLED_EXCEPTION)
            .actorUserId(actorId)
            .message(ex.getClass().getSimpleName() + ": " + safe(ex.getMessage()))
            .details(details)
            .build());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error");
    }

    private static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        try {
            return Long.parseLong(auth.getPrincipal().toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String stackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}

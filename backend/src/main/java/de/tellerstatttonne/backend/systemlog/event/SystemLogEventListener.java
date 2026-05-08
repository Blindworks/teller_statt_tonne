package de.tellerstatttonne.backend.systemlog.event;

import de.tellerstatttonne.backend.systemlog.SystemLogService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SystemLogEventListener {

    private final SystemLogService service;

    public SystemLogEventListener(SystemLogService service) {
        this.service = service;
    }

    /**
     * Persistiert nach erfolgreichem Commit der auslösenden Transaktion. So sind referenzierte
     * Entitäten (z. B. neu angelegte User) bereits sichtbar — kein FK-Konflikt im Listener.
     * `fallbackExecution = true` sorgt dafür, dass Aufrufe ohne aktive Transaktion ebenfalls
     * laufen.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCommit(SystemLogEvent event) {
        service.record(event);
    }

    /**
     * Persistiert auch dann, wenn die auslösende Transaktion zurückgerollt wurde (z. B.
     * fehlgeschlagener Login, der eine `BadCredentialsException` aus einer `@Transactional`-Methode
     * wirft). Der Listener selbst öffnet via `SystemLogService` eine eigene `REQUIRES_NEW`-TX.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void onRollback(SystemLogEvent event) {
        service.record(event);
    }
}

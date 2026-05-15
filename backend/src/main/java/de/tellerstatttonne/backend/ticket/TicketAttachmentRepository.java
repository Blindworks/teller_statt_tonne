package de.tellerstatttonne.backend.ticket;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachmentEntity, Long> {

    List<TicketAttachmentEntity> findAllByTicketIdOrderByUploadedAtAsc(Long ticketId);

    void deleteAllByTicketId(Long ticketId);

    @Query("select a.ticketId as ticketId, count(a) as cnt from TicketAttachmentEntity a where a.ticketId in :ids group by a.ticketId")
    List<TicketCountRow> countByTicketIds(List<Long> ids);

    interface TicketCountRow {
        Long getTicketId();
        Long getCnt();
    }
}

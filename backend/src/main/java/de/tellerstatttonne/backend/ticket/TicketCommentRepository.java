package de.tellerstatttonne.backend.ticket;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TicketCommentRepository extends JpaRepository<TicketCommentEntity, Long> {

    List<TicketCommentEntity> findAllByTicketIdOrderByCreatedAtAsc(Long ticketId);

    void deleteAllByTicketId(Long ticketId);

    @Query("select c.ticketId as ticketId, count(c) as cnt from TicketCommentEntity c where c.ticketId in :ids group by c.ticketId")
    List<TicketCountRow> countByTicketIds(List<Long> ids);

    interface TicketCountRow {
        Long getTicketId();
        Long getCnt();
    }
}

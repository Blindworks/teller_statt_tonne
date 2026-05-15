package de.tellerstatttonne.backend.ticket;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<TicketEntity, Long> {

    List<TicketEntity> findAllByOrderByCreatedAtDesc();

    List<TicketEntity> findAllByStatusOrderByCreatedAtDesc(TicketStatus status);

    List<TicketEntity> findAllByCategoryOrderByCreatedAtDesc(TicketCategory category);

    List<TicketEntity> findAllByStatusAndCategoryOrderByCreatedAtDesc(TicketStatus status, TicketCategory category);
}

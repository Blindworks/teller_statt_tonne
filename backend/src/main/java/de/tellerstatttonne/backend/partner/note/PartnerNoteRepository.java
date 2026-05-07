package de.tellerstatttonne.backend.partner.note;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartnerNoteRepository extends JpaRepository<PartnerNoteEntity, Long> {

    List<PartnerNoteEntity> findByPartnerIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long partnerId);

    @Query("""
        SELECT n FROM PartnerNoteEntity n
        WHERE n.partner.id = :partnerId
          AND n.deletedAt IS NULL
          AND (n.visibility = de.tellerstatttonne.backend.partner.note.Visibility.SHARED
               OR n.author.id = :authorId)
        ORDER BY n.createdAt DESC
        """)
    List<PartnerNoteEntity> findVisibleForRetter(
        @Param("partnerId") Long partnerId,
        @Param("authorId") Long authorId
    );
}

package de.tellerstatttonne.backend.partner.note;

import de.tellerstatttonne.backend.partner.PartnerEntity;
import de.tellerstatttonne.backend.partner.PartnerRepository;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PartnerNoteService {

    private static final String ROLE_ADMIN = "ADMINISTRATOR";
    private static final String ROLE_BOTSCHAFTER = "BOTSCHAFTER";

    private final PartnerNoteRepository repository;
    private final PartnerRepository partnerRepository;
    private final UserRepository userRepository;

    public PartnerNoteService(
        PartnerNoteRepository repository,
        PartnerRepository partnerRepository,
        UserRepository userRepository
    ) {
        this.repository = repository;
        this.partnerRepository = partnerRepository;
        this.userRepository = userRepository;
    }

    public PartnerNote create(Long partnerId, CreatePartnerNoteRequest request, Long authorUserId) {
        PartnerEntity partner = partnerRepository.findById(partnerId)
            .orElseThrow(() -> new EntityNotFoundException("partner not found: " + partnerId));
        UserEntity author = userRepository.findById(authorUserId)
            .orElseThrow(() -> new EntityNotFoundException("user not found: " + authorUserId));

        Visibility visibility = request.visibility();
        if (!canPostInternal(author) && visibility == Visibility.INTERNAL) {
            visibility = Visibility.SHARED;
        }

        PartnerNoteEntity entity = new PartnerNoteEntity();
        entity.setPartner(partner);
        entity.setAuthor(author);
        entity.setBody(request.body());
        entity.setVisibility(visibility);
        return PartnerNoteMapper.toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<PartnerNote> listForUser(Long partnerId, Long userId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("user not found: " + userId));
        List<PartnerNoteEntity> notes = canSeeAll(user)
            ? repository.findByPartnerIdAndDeletedAtIsNullOrderByCreatedAtDesc(partnerId)
            : repository.findVisibleForRetter(partnerId, userId);
        return notes.stream().map(PartnerNoteMapper::toDto).toList();
    }

    public boolean softDelete(Long noteId, Long actingUserId) {
        UserEntity user = userRepository.findById(actingUserId)
            .orElseThrow(() -> new EntityNotFoundException("user not found: " + actingUserId));
        if (!canSeeAll(user)) {
            throw new AccessDeniedException("only Admin or Botschafter may delete notes");
        }
        return repository.findById(noteId).map(entity -> {
            if (entity.getDeletedAt() != null) return true;
            entity.setDeletedAt(Instant.now());
            entity.setDeletedBy(user);
            repository.save(entity);
            return true;
        }).orElse(false);
    }

    private static boolean canSeeAll(UserEntity user) {
        return user.hasRole(ROLE_ADMIN) || user.hasRole(ROLE_BOTSCHAFTER);
    }

    private static boolean canPostInternal(UserEntity user) {
        return user.hasRole(ROLE_ADMIN) || user.hasRole(ROLE_BOTSCHAFTER);
    }
}

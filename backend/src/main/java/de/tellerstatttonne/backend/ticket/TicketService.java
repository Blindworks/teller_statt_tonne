package de.tellerstatttonne.backend.ticket;

import de.tellerstatttonne.backend.ticket.dto.Ticket;
import de.tellerstatttonne.backend.ticket.dto.TicketComment;
import de.tellerstatttonne.backend.ticket.dto.TicketCommentRequest;
import de.tellerstatttonne.backend.ticket.dto.TicketCreateRequest;
import de.tellerstatttonne.backend.ticket.dto.TicketStatusRequest;
import de.tellerstatttonne.backend.ticket.dto.TicketSummary;
import de.tellerstatttonne.backend.ticket.dto.TicketUpdateRequest;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class TicketService {

    static final String ROLE_ADMIN = "ADMINISTRATOR";

    private final TicketRepository repository;
    private final TicketAttachmentRepository attachmentRepository;
    private final TicketCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final TicketAttachmentStorage attachmentStorage;

    public TicketService(
        TicketRepository repository,
        TicketAttachmentRepository attachmentRepository,
        TicketCommentRepository commentRepository,
        UserRepository userRepository,
        TicketAttachmentStorage attachmentStorage
    ) {
        this.repository = repository;
        this.attachmentRepository = attachmentRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.attachmentStorage = attachmentStorage;
    }

    @Transactional(readOnly = true)
    public List<TicketSummary> findAll(TicketStatus status, TicketCategory category) {
        List<TicketEntity> entities;
        if (status != null && category != null) {
            entities = repository.findAllByStatusAndCategoryOrderByCreatedAtDesc(status, category);
        } else if (status != null) {
            entities = repository.findAllByStatusOrderByCreatedAtDesc(status);
        } else if (category != null) {
            entities = repository.findAllByCategoryOrderByCreatedAtDesc(category);
        } else {
            entities = repository.findAllByOrderByCreatedAtDesc();
        }
        if (entities.isEmpty()) return List.of();

        List<Long> ticketIds = entities.stream().map(TicketEntity::getId).toList();
        Map<Long, Long> commentCounts = commentRepository.countByTicketIds(ticketIds).stream()
            .collect(Collectors.toMap(TicketCommentRepository.TicketCountRow::getTicketId,
                TicketCommentRepository.TicketCountRow::getCnt));
        Map<Long, Long> attachmentCounts = attachmentRepository.countByTicketIds(ticketIds).stream()
            .collect(Collectors.toMap(TicketAttachmentRepository.TicketCountRow::getTicketId,
                TicketAttachmentRepository.TicketCountRow::getCnt));

        Function<Long, String> nameLookup = userNameLookup(entities.stream()
            .map(TicketEntity::getCreatedById).distinct().toList());

        return entities.stream()
            .map(t -> TicketMapper.toSummary(
                t,
                commentCounts.getOrDefault(t.getId(), 0L).intValue(),
                attachmentCounts.getOrDefault(t.getId(), 0L).intValue(),
                nameLookup
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Ticket> findById(Long id) {
        return repository.findById(id).map(this::toFullDto);
    }

    public Ticket create(TicketCreateRequest request, Long actorUserId) {
        validateTitle(request.title());
        if (request.category() == null) {
            throw new IllegalArgumentException("category ist erforderlich");
        }
        TicketEntity entity = new TicketEntity();
        entity.setTitle(request.title().trim());
        entity.setDescription(blankToNull(request.description()));
        entity.setCategory(request.category());
        entity.setStatus(TicketStatus.OPEN);
        entity.setCreatedById(actorUserId);
        return toFullDto(repository.save(entity));
    }

    public Optional<Ticket> update(Long id, TicketUpdateRequest request, Long actorUserId) {
        return repository.findById(id).map(entity -> {
            UserEntity actor = requireUser(actorUserId);
            ensureCanEditContent(entity, actor);
            validateTitle(request.title());
            if (request.category() == null) {
                throw new IllegalArgumentException("category ist erforderlich");
            }
            entity.setTitle(request.title().trim());
            entity.setDescription(blankToNull(request.description()));
            entity.setCategory(request.category());
            return toFullDto(repository.save(entity));
        });
    }

    public Optional<Ticket> updateStatus(Long id, TicketStatusRequest request, Long actorUserId) {
        if (request.status() == null) {
            throw new IllegalArgumentException("status ist erforderlich");
        }
        return repository.findById(id).map(entity -> {
            UserEntity actor = requireUser(actorUserId);
            if (!actor.hasRole(ROLE_ADMIN)) {
                throw new AccessDeniedException("nur ADMINISTRATOR darf den Status aendern");
            }
            entity.setStatus(request.status());
            return toFullDto(repository.save(entity));
        });
    }

    public boolean delete(Long id, Long actorUserId) {
        return repository.findById(id).map(entity -> {
            UserEntity actor = requireUser(actorUserId);
            ensureCanDelete(entity, actor);
            attachmentRepository.findAllByTicketIdOrderByUploadedAtAsc(id)
                .forEach(a -> attachmentStorage.deleteByUrl(a.getUrl()));
            attachmentRepository.deleteAllByTicketId(id);
            commentRepository.deleteAllByTicketId(id);
            repository.delete(entity);
            return true;
        }).orElse(false);
    }

    public Optional<Ticket> addAttachment(Long ticketId, MultipartFile file, Long actorUserId) {
        return repository.findById(ticketId).map(entity -> {
            UserEntity actor = requireUser(actorUserId);
            ensureCanEditContent(entity, actor);
            String url = attachmentStorage.store(ticketId, file);
            TicketAttachmentEntity attachment = new TicketAttachmentEntity();
            attachment.setTicketId(ticketId);
            attachment.setUrl(url);
            attachment.setOriginalFilename(file.getOriginalFilename());
            attachment.setUploadedById(actorUserId);
            attachmentRepository.save(attachment);
            entity.setStatus(entity.getStatus()); // trigger @PreUpdate
            repository.save(entity);
            return toFullDto(entity);
        });
    }

    public boolean deleteAttachment(Long ticketId, Long attachmentId, Long actorUserId) {
        TicketEntity ticket = repository.findById(ticketId).orElse(null);
        if (ticket == null) return false;
        TicketAttachmentEntity attachment = attachmentRepository.findById(attachmentId).orElse(null);
        if (attachment == null || !attachment.getTicketId().equals(ticketId)) return false;
        UserEntity actor = requireUser(actorUserId);
        ensureCanEditContent(ticket, actor);
        attachmentStorage.deleteByUrl(attachment.getUrl());
        attachmentRepository.delete(attachment);
        return true;
    }

    public Optional<TicketComment> addComment(Long ticketId, TicketCommentRequest request, Long actorUserId) {
        if (request.body() == null || request.body().isBlank()) {
            throw new IllegalArgumentException("body ist erforderlich");
        }
        return repository.findById(ticketId).map(ticket -> {
            requireUser(actorUserId);
            TicketCommentEntity comment = new TicketCommentEntity();
            comment.setTicketId(ticketId);
            comment.setBody(request.body().trim());
            comment.setAuthorId(actorUserId);
            TicketCommentEntity saved = commentRepository.save(comment);
            // Bump updatedAt
            ticket.setStatus(ticket.getStatus());
            repository.save(ticket);
            return TicketMapper.toCommentDto(saved, userNameLookup(List.of(actorUserId)));
        });
    }

    public boolean deleteComment(Long ticketId, Long commentId, Long actorUserId) {
        TicketCommentEntity comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null || !comment.getTicketId().equals(ticketId)) return false;
        UserEntity actor = requireUser(actorUserId);
        if (!comment.getAuthorId().equals(actorUserId) && !actor.hasRole(ROLE_ADMIN)) {
            throw new AccessDeniedException("nur Autor oder ADMINISTRATOR darf Kommentar loeschen");
        }
        commentRepository.delete(comment);
        return true;
    }

    private Ticket toFullDto(TicketEntity entity) {
        List<TicketAttachmentEntity> attachments =
            attachmentRepository.findAllByTicketIdOrderByUploadedAtAsc(entity.getId());
        List<TicketCommentEntity> comments =
            commentRepository.findAllByTicketIdOrderByCreatedAtAsc(entity.getId());
        List<Long> userIds = new java.util.ArrayList<>();
        userIds.add(entity.getCreatedById());
        attachments.forEach(a -> userIds.add(a.getUploadedById()));
        comments.forEach(c -> userIds.add(c.getAuthorId()));
        return TicketMapper.toDto(entity, attachments, comments, userNameLookup(userIds));
    }

    private Function<Long, String> userNameLookup(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return id -> "";
        }
        Map<Long, String> names = userRepository.findAllById(userIds.stream().distinct().toList()).stream()
            .collect(Collectors.toMap(
                UserEntity::getId,
                u -> (u.getFirstName() + " " + u.getLastName()).trim()
            ));
        return id -> id == null ? "" : names.getOrDefault(id, "");
    }

    private UserEntity requireUser(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("user not found: " + id));
    }

    private static void ensureCanEditContent(TicketEntity entity, UserEntity actor) {
        boolean isAdmin = actor.hasRole(ROLE_ADMIN);
        boolean isOwner = entity.getCreatedById().equals(actor.getId());
        if (isAdmin) return;
        if (!isOwner) {
            throw new AccessDeniedException("nur Ersteller oder ADMINISTRATOR darf das Ticket bearbeiten");
        }
        if (entity.getStatus() != TicketStatus.OPEN) {
            throw new AccessDeniedException("Ticket kann nur im Status OPEN bearbeitet werden");
        }
    }

    private static void ensureCanDelete(TicketEntity entity, UserEntity actor) {
        ensureCanEditContent(entity, actor);
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title ist erforderlich");
        }
        if (title.length() > 255) {
            throw new IllegalArgumentException("title darf max. 255 Zeichen lang sein");
        }
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

}

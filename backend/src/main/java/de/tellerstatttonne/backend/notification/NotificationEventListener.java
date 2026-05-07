package de.tellerstatttonne.backend.notification;

import de.tellerstatttonne.backend.notification.event.PickupStatusChangedEvent;
import de.tellerstatttonne.backend.notification.event.PickupUnassignedEvent;
import de.tellerstatttonne.backend.partner.PartnerEntity;
import de.tellerstatttonne.backend.pickup.Pickup;
import de.tellerstatttonne.backend.pickup.PickupEntity;
import de.tellerstatttonne.backend.pickup.PickupRepository;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final PickupRepository pickupRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public NotificationEventListener(PickupRepository pickupRepository,
                                     UserRepository userRepository,
                                     NotificationService notificationService) {
        this.pickupRepository = pickupRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPickupUnassigned(PickupUnassignedEvent event) {
        Optional<PickupEntity> pickupOpt = pickupRepository.findById(event.pickupId());
        if (pickupOpt.isEmpty()) {
            log.warn("Pickup {} not found for unassign notification", event.pickupId());
            return;
        }
        PickupEntity pickup = pickupOpt.get();
        PartnerEntity partner = pickup.getPartner();
        if (partner == null) return;

        String actorName = userRepository.findById(event.actorUserId())
            .map(this::displayName)
            .orElse("Ein Retter");

        String when = pickup.getDate().format(DATE_FMT) + " um " + pickup.getStartTime();
        String title = "Austragung aus Abholung";
        String body = actorName + " hat sich aus der Abholung bei " + partner.getName()
            + " am " + when + " ausgetragen.";

        List<Long> recipients = collectRetterAndBotschafter(partner);
        notificationService.create(
            recipients,
            NotificationType.PICKUP_UNASSIGNED,
            title,
            body,
            pickup.getId(),
            partner.getId(),
            event.actorUserId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPickupStatusChanged(PickupStatusChangedEvent event) {
        if (event.newStatus() != Pickup.Status.CANCELLED && event.newStatus() != Pickup.Status.COMPLETED) {
            return;
        }
        Optional<PickupEntity> pickupOpt = pickupRepository.findById(event.pickupId());
        if (pickupOpt.isEmpty()) return;
        PickupEntity pickup = pickupOpt.get();
        PartnerEntity partner = pickup.getPartner();
        if (partner == null) return;

        String when = pickup.getDate().format(DATE_FMT) + " um " + pickup.getStartTime();
        NotificationType type;
        String title;
        String body;
        if (event.newStatus() == Pickup.Status.CANCELLED) {
            type = NotificationType.PICKUP_CANCELLED;
            title = "Abholung abgesagt";
            body = "Die Abholung bei " + partner.getName() + " am " + when + " wurde abgesagt.";
        } else {
            type = NotificationType.PICKUP_COMPLETED;
            title = "Abholung abgeschlossen";
            body = "Die Abholung bei " + partner.getName() + " am " + when + " wurde abgeschlossen.";
        }

        List<Long> recipients = collectRetterAndBotschafter(partner);
        notificationService.create(
            recipients,
            type,
            title,
            body,
            pickup.getId(),
            partner.getId(),
            event.actorUserId()
        );
    }

    private List<Long> collectRetterAndBotschafter(PartnerEntity partner) {
        return partner.getMembers().stream()
            .filter(u -> u.hasRole("RETTER") || u.hasRole("BOTSCHAFTER"))
            .map(UserEntity::getId)
            .toList();
    }

    private String displayName(UserEntity user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName();
        String last = user.getLastName() == null ? "" : user.getLastName();
        String initial = last.isEmpty() ? "" : (" " + last.charAt(0) + ".");
        String result = (first + initial).trim();
        return result.isEmpty() ? user.getEmail() : result;
    }
}

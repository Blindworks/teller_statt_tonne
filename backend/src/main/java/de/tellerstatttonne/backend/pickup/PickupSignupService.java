package de.tellerstatttonne.backend.pickup;

import de.tellerstatttonne.backend.notification.event.PickupUnassignedEvent;
import de.tellerstatttonne.backend.partner.Partner;
import de.tellerstatttonne.backend.partner.PartnerEntity;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PickupSignupService {

    public enum Result { OK, PICKUP_NOT_FOUND, USER_NOT_FOUND, NOT_MEMBER, CAPACITY_FULL, NOT_ASSIGNED, PICKUP_PAST, UNASSIGN_TOO_LATE, PARTNER_NOT_COOPERATING }

    private static final long UNASSIGN_CUTOFF_HOURS = 2;

    private final PickupRepository pickupRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PickupSignupService(PickupRepository pickupRepository,
                               UserRepository userRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.pickupRepository = pickupRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    public Result signup(Long pickupId, Long userId) {
        Optional<PickupEntity> pickupOpt = pickupRepository.findById(pickupId);
        if (pickupOpt.isEmpty()) return Result.PICKUP_NOT_FOUND;
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return Result.USER_NOT_FOUND;

        PickupEntity pickup = pickupOpt.get();
        UserEntity user = userOpt.get();

        if (pickup.getDate().isBefore(LocalDate.now())) return Result.PICKUP_PAST;

        PartnerEntity partner = pickup.getPartner();
        if (partner != null) {
            if (partner.getStatus() != Partner.Status.KOOPERIERT) {
                return Result.PARTNER_NOT_COOPERATING;
            }
            boolean isMember = partner.getMembers().stream()
                .anyMatch(m -> userId.equals(m.getId()));
            if (!isMember) return Result.NOT_MEMBER;
        }

        boolean alreadyAssigned = pickup.getAssignments().stream()
            .anyMatch(a -> userId.equals(a.getUserId()));
        if (alreadyAssigned) return Result.OK;

        if (pickup.getAssignments().size() >= pickup.getCapacity()) {
            return Result.CAPACITY_FULL;
        }

        PickupEntity.AssignmentEmbeddable assignment = new PickupEntity.AssignmentEmbeddable();
        assignment.setUserId(user.getId());
        pickup.getAssignments().add(assignment);
        pickupRepository.save(pickup);
        return Result.OK;
    }

    public Result unassign(Long pickupId, Long userId) {
        Optional<PickupEntity> pickupOpt = pickupRepository.findById(pickupId);
        if (pickupOpt.isEmpty()) return Result.PICKUP_NOT_FOUND;

        PickupEntity pickup = pickupOpt.get();
        if (pickup.getDate().isBefore(LocalDate.now())) return Result.PICKUP_PAST;
        LocalDateTime cutoff = LocalDateTime.of(pickup.getDate(), LocalTime.parse(pickup.getStartTime()))
            .minusHours(UNASSIGN_CUTOFF_HOURS);
        if (LocalDateTime.now().isAfter(cutoff)) return Result.UNASSIGN_TOO_LATE;
        boolean removed = pickup.getAssignments().removeIf(a -> userId.equals(a.getUserId()));
        if (!removed) return Result.NOT_ASSIGNED;
        pickupRepository.save(pickup);
        eventPublisher.publishEvent(new PickupUnassignedEvent(pickup.getId(), userId));
        return Result.OK;
    }
}

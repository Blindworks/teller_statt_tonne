package de.tellerstatttonne.backend.partner;

import de.tellerstatttonne.backend.pickup.PickupRepository;
import de.tellerstatttonne.backend.pickup.PickupRepository.MemberPickupStats;
import de.tellerstatttonne.backend.systemlog.SystemLogEventType;
import de.tellerstatttonne.backend.systemlog.event.SystemLogEvent;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PartnerMemberService {

    private final PartnerRepository partnerRepository;
    private final UserRepository userRepository;
    private final PickupRepository pickupRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PartnerMemberService(
        PartnerRepository partnerRepository,
        UserRepository userRepository,
        PickupRepository pickupRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.partnerRepository = partnerRepository;
        this.userRepository = userRepository;
        this.pickupRepository = pickupRepository;
        this.eventPublisher = eventPublisher;
    }

    private static Long currentActorId() {
        try {
            return de.tellerstatttonne.backend.auth.CurrentUser.requireId();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public Optional<List<StoreMember>> list(Long partnerId) {
        return partnerRepository.findById(partnerId).map(partner -> {
            Map<Long, MemberPickupStats> statsByUser = new HashMap<>();
            for (MemberPickupStats row : pickupRepository.aggregateByPartner(partnerId)) {
                statsByUser.put(row.getMemberId(), row);
            }
            return partner.getMembers().stream()
                .sorted(Comparator
                    .comparing(UserEntity::getFirstName, Comparator.nullsLast(String::compareToIgnoreCase))
                    .thenComparing(UserEntity::getLastName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(user -> toDto(user, statsByUser.get(user.getId())))
                .toList();
        });
    }

    public AssignResult assign(Long partnerId, Long userId) {
        Optional<PartnerEntity> partnerOpt = partnerRepository.findById(partnerId);
        if (partnerOpt.isEmpty()) {
            return AssignResult.PARTNER_NOT_FOUND;
        }
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return AssignResult.MEMBER_NOT_FOUND;
        }
        PartnerEntity partner = partnerOpt.get();
        Set<UserEntity> members = partner.getMembers();
        UserEntity user = userOpt.get();
        boolean added = members.add(user);
        if (added) {
            partnerRepository.save(partner);
            eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.STORE_MEMBER_ASSIGNED)
                .actorUserId(currentActorId())
                .target("PARTNER", partner.getId())
                .message("Nutzer " + user.getEmail() + " dem Betrieb " + partner.getName() + " zugeordnet")
                .build());
        }
        return AssignResult.OK;
    }

    public AssignResult unassign(Long partnerId, Long userId) {
        Optional<PartnerEntity> partnerOpt = partnerRepository.findById(partnerId);
        if (partnerOpt.isEmpty()) {
            return AssignResult.PARTNER_NOT_FOUND;
        }
        PartnerEntity partner = partnerOpt.get();
        boolean removed = partner.getMembers().removeIf(m -> userId.equals(m.getId()));
        if (removed) {
            partnerRepository.save(partner);
        }
        return AssignResult.OK;
    }

    private StoreMember toDto(UserEntity u, MemberPickupStats stats) {
        return new StoreMember(
            u.getId(),
            u.getFirstName(),
            u.getLastName(),
            u.getRoleNames(),
            u.getEmail(),
            u.getCity(),
            u.getPhotoUrl(),
            u.getOnlineStatus(),
            u.getStatus(),
            stats != null ? stats.getLastPickupDate() : null,
            stats != null && stats.getTotalSavedKg() != null ? stats.getTotalSavedKg() : BigDecimal.ZERO,
            stats != null && stats.getPickupCount() != null ? stats.getPickupCount() : 0L
        );
    }

    public enum AssignResult { OK, PARTNER_NOT_FOUND, MEMBER_NOT_FOUND }
}

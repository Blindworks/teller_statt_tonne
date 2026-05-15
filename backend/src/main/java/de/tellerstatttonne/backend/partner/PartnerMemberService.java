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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        UserEntity user = userOpt.get();
        boolean coordinator = user.hasRole("KOORDINATOR");
        if (coordinator && !callerIsAdminOrTeamleiter()) {
            return AssignResult.FORBIDDEN;
        }
        Set<UserEntity> members = partner.getMembers();
        boolean added = members.add(user);
        if (added) {
            partnerRepository.save(partner);
            SystemLogEventType type = coordinator
                ? SystemLogEventType.STORE_COORDINATOR_ASSIGNED
                : SystemLogEventType.STORE_MEMBER_ASSIGNED;
            String label = coordinator ? "Koordinator " : "Nutzer ";
            eventPublisher.publishEvent(SystemLogEvent.of(type)
                .actorUserId(currentActorId())
                .target("PARTNER", partner.getId())
                .message(label + user.getEmail() + " dem Betrieb " + partner.getName() + " zugeordnet")
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
        Optional<UserEntity> targetOpt = partner.getMembers().stream()
            .filter(m -> userId.equals(m.getId()))
            .findFirst();
        if (targetOpt.isEmpty()) {
            return AssignResult.OK;
        }
        UserEntity target = targetOpt.get();
        boolean coordinator = target.hasRole("KOORDINATOR");
        if (coordinator && !callerIsAdminOrTeamleiter()) {
            return AssignResult.FORBIDDEN;
        }
        partner.getMembers().remove(target);
        partnerRepository.save(partner);
        if (coordinator) {
            eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.STORE_COORDINATOR_UNASSIGNED)
                .actorUserId(currentActorId())
                .target("PARTNER", partner.getId())
                .message("Koordinator " + target.getEmail() + " vom Betrieb " + partner.getName() + " entfernt")
                .build());
        }
        return AssignResult.OK;
    }

    private static boolean callerIsAdminOrTeamleiter() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a ->
            "ROLE_ADMINISTRATOR".equals(a.getAuthority())
                || "ROLE_TEAMLEITER".equals(a.getAuthority()));
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

    public enum AssignResult { OK, PARTNER_NOT_FOUND, MEMBER_NOT_FOUND, FORBIDDEN }
}

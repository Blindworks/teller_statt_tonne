package de.tellerstatttonne.backend.dashboard;

import de.tellerstatttonne.backend.partner.PartnerRepository;
import de.tellerstatttonne.backend.pickup.Pickup;
import de.tellerstatttonne.backend.pickup.PickupService;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final PickupService pickupService;
    private final PartnerRepository partnerRepository;
    private final UserRepository userRepository;

    public DashboardService(PickupService pickupService,
                            PartnerRepository partnerRepository,
                            UserRepository userRepository) {
        this.pickupService = pickupService;
        this.partnerRepository = partnerRepository;
        this.userRepository = userRepository;
    }

    public List<DaySlot> findDaySlots(LocalDate date, Long currentUserId) {
        return findRangeSlots(date, date, currentUserId);
    }

    public List<DaySlot> findRangeSlots(LocalDate from, LocalDate to, Long currentUserId) {
        UserEntity user = userRepository.findById(currentUserId).orElse(null);
        if (user == null) {
            return List.of();
        }
        boolean isAdmin = user.hasRole("ADMINISTRATOR");
        boolean isTeamleiter = user.hasRole("TEAMLEITER");
        boolean isRetter = user.hasRole("RETTER");
        if (!isAdmin && !isTeamleiter && !isRetter) {
            return List.of();
        }
        Set<Long> allowedPartnerIds = isRetter ? memberPartnerIds(user.getId()) : null;

        List<Pickup> pickups = pickupService.findBetween(from, to);
        List<DaySlot> result = new ArrayList<>();

        for (Pickup p : pickups) {
            if (isRetter && !allowedPartnerIds.contains(p.partnerId())) continue;
            boolean assigned = p.assignments() != null && p.assignments().stream()
                .anyMatch(a -> currentUserId.equals(a.memberId()));
            result.add(new DaySlot(
                p.id(), p.partnerId(), p.partnerName(), p.partnerCategory(),
                p.partnerStreet(), p.partnerCity(), p.partnerLogoUrl(),
                p.date(), p.startTime(), p.endTime(),
                p.capacity(), p.assignments() == null ? List.of() : p.assignments(),
                false, assigned
            ));
        }

        result.sort(Comparator
            .comparing(DaySlot::date, Comparator.nullsLast(LocalDate::compareTo))
            .thenComparing(DaySlot::startTime, Comparator.nullsLast(String::compareTo))
            .thenComparing(DaySlot::partnerName, Comparator.nullsLast(String::compareTo)));
        return result;
    }

    private Set<Long> memberPartnerIds(Long userId) {
        return new HashSet<>(partnerRepository.findIdsByMemberId(userId));
    }
}

package de.tellerstatttonne.backend.dashboard;

import de.tellerstatttonne.backend.partner.Partner;
import de.tellerstatttonne.backend.partner.PartnerEntity;
import de.tellerstatttonne.backend.partner.PartnerRepository;
import de.tellerstatttonne.backend.pickup.Pickup;
import de.tellerstatttonne.backend.pickup.PickupService;
import de.tellerstatttonne.backend.user.Role;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import java.time.DayOfWeek;
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
        if (user == null || user.getRole() == Role.NEW_MEMBER) {
            return List.of();
        }

        boolean isRetter = user.getRole() == Role.RETTER;
        Set<Long> allowedPartnerIds = isRetter ? memberPartnerIds(user.getId()) : null;

        List<Pickup> pickups = pickupService.findBetween(from, to);
        List<DaySlot> result = new ArrayList<>();
        Set<String> covered = new HashSet<>();

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
            covered.add(coverageKey(p.date(), p.partnerId(), p.startTime(), p.endTime()));
        }

        if (!isRetter) {
            List<PartnerEntity> activePartners = partnerRepository.findAll().stream()
                .filter(pa -> pa.getStatus() == Partner.Status.ACTIVE)
                .filter(pa -> pa.getPickupSlots() != null)
                .toList();
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                Partner.Weekday weekday = toWeekday(d.getDayOfWeek());
                for (PartnerEntity partner : activePartners) {
                    for (PartnerEntity.PickupSlotEmbeddable slot : partner.getPickupSlots()) {
                        if (!slot.isActive()) continue;
                        if (slot.getWeekday() != weekday) continue;
                        String key = coverageKey(d, partner.getId(), slot.getStartTime(), slot.getEndTime());
                        if (covered.contains(key)) continue;
                        result.add(new DaySlot(
                            null, partner.getId(), partner.getName(), partner.getCategory(),
                            partner.getStreet(), partner.getCity(), partner.getLogoUrl(),
                            d, slot.getStartTime(), slot.getEndTime(),
                            slot.getCapacity(), List.of(),
                            true, false
                        ));
                    }
                }
            }
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

    private static String coverageKey(LocalDate date, Long partnerId, String startTime, String endTime) {
        return date + "|" + partnerId + "|" + startTime + "|" + endTime;
    }

    private static Partner.Weekday toWeekday(DayOfWeek dow) {
        return Partner.Weekday.values()[dow.getValue() - 1];
    }
}

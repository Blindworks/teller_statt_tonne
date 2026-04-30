package de.tellerstatttonne.backend.dashboard;

import de.tellerstatttonne.backend.partner.Partner;
import de.tellerstatttonne.backend.partner.PartnerEntity;
import de.tellerstatttonne.backend.partner.PartnerRepository;
import de.tellerstatttonne.backend.pickup.Pickup;
import de.tellerstatttonne.backend.pickup.PickupService;
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

    public DashboardService(PickupService pickupService, PartnerRepository partnerRepository) {
        this.pickupService = pickupService;
        this.partnerRepository = partnerRepository;
    }

    public List<DaySlot> findDaySlots(LocalDate date) {
        List<Pickup> pickups = pickupService.findBetween(date, date);
        List<DaySlot> result = new ArrayList<>();
        Set<String> covered = new HashSet<>();

        for (Pickup p : pickups) {
            result.add(new DaySlot(
                p.id(), p.partnerId(), p.partnerName(), p.partnerCategory(),
                p.partnerStreet(), p.partnerCity(), p.partnerLogoUrl(),
                p.date(), p.startTime(), p.endTime(),
                p.capacity(), p.assignments() == null ? List.of() : p.assignments(),
                false
            ));
            covered.add(coverageKey(p.partnerId(), p.startTime(), p.endTime()));
        }

        Partner.Weekday weekday = toWeekday(date.getDayOfWeek());
        for (PartnerEntity partner : partnerRepository.findAll()) {
            if (partner.getStatus() != Partner.Status.ACTIVE) continue;
            if (partner.getPickupSlots() == null) continue;
            for (PartnerEntity.PickupSlotEmbeddable slot : partner.getPickupSlots()) {
                if (!slot.isActive()) continue;
                if (slot.getWeekday() != weekday) continue;
                String key = coverageKey(partner.getId(), slot.getStartTime(), slot.getEndTime());
                if (covered.contains(key)) continue;
                result.add(new DaySlot(
                    null, partner.getId(), partner.getName(), partner.getCategory(),
                    partner.getStreet(), partner.getCity(), partner.getLogoUrl(),
                    date, slot.getStartTime(), slot.getEndTime(),
                    slot.getCapacity(), List.of(),
                    true
                ));
            }
        }

        result.sort(Comparator
            .comparing(DaySlot::startTime, Comparator.nullsLast(String::compareTo))
            .thenComparing(DaySlot::partnerName, Comparator.nullsLast(String::compareTo)));
        return result;
    }

    private static String coverageKey(Long partnerId, String startTime, String endTime) {
        return partnerId + "|" + startTime + "|" + endTime;
    }

    private static Partner.Weekday toWeekday(DayOfWeek dow) {
        return Partner.Weekday.values()[dow.getValue() - 1];
    }
}

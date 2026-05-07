package de.tellerstatttonne.backend.stats;

import de.tellerstatttonne.backend.pickup.PickupRepository;
import de.tellerstatttonne.backend.user.User;
import de.tellerstatttonne.backend.user.UserService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StatsService {

    private static final int TOP_N = 10;

    private final PickupRepository pickupRepository;
    private final UserService userService;

    public StatsService(PickupRepository pickupRepository, UserService userService) {
        this.pickupRepository = pickupRepository;
        this.userService = userService;
    }

    public StatsDtos.Overview overview() {
        PickupRepository.OverallStats overall = pickupRepository.aggregateOverall();
        BigDecimal totalKg = overall != null && overall.getTotalSavedKg() != null
            ? overall.getTotalSavedKg()
            : BigDecimal.ZERO;
        long count = overall != null && overall.getPickupCount() != null
            ? overall.getPickupCount()
            : 0L;

        List<StatsDtos.PartnerEntry> partners = pickupRepository
            .aggregateTopPartners(PageRequest.of(0, TOP_N))
            .stream()
            .map(p -> new StatsDtos.PartnerEntry(
                p.getPartnerId(),
                p.getPartnerName(),
                p.getSavedKg() != null ? p.getSavedKg() : BigDecimal.ZERO,
                p.getPickupCount() != null ? p.getPickupCount() : 0L))
            .toList();

        List<StatsDtos.MemberEntry> members = pickupRepository
            .aggregateTopMembers(PageRequest.of(0, TOP_N))
            .stream()
            .map(m -> {
                Optional<User> user = userService.findById(m.getMemberId());
                String name = user.map(u -> (u.firstName() + " " + u.lastName()).trim())
                    .orElse(null);
                return new StatsDtos.MemberEntry(
                    m.getMemberId(),
                    name,
                    m.getSavedKg() != null ? m.getSavedKg() : BigDecimal.ZERO,
                    m.getPickupCount() != null ? m.getPickupCount() : 0L);
            })
            .toList();

        return new StatsDtos.Overview(totalKg, count, partners, members);
    }
}

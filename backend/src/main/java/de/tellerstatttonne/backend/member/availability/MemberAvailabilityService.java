package de.tellerstatttonne.backend.member.availability;

import de.tellerstatttonne.backend.member.MemberRepository;
import de.tellerstatttonne.backend.partner.Partner;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MemberAvailabilityService {

    private final MemberAvailabilityRepository repository;
    private final MemberRepository memberRepository;

    public MemberAvailabilityService(
        MemberAvailabilityRepository repository,
        MemberRepository memberRepository
    ) {
        this.repository = repository;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public List<MemberAvailability> findByMemberId(String memberId) {
        return repository.findByMemberId(memberId).stream()
            .map(MemberAvailabilityService::toDto)
            .toList();
    }

    public List<MemberAvailability> replaceAll(String memberId, List<MemberAvailability> items) {
        if (!memberRepository.existsById(memberId)) {
            throw new IllegalArgumentException("member not found: " + memberId);
        }
        validate(items);
        repository.deleteByMemberId(memberId);
        // Deduplicate within the request to avoid violating the unique constraint.
        Set<String> seen = new HashSet<>();
        List<MemberAvailabilityEntity> entities = items.stream()
            .filter(a -> seen.add(a.weekday() + "|" + a.startTime() + "|" + a.endTime()))
            .map(a -> {
                MemberAvailabilityEntity e = new MemberAvailabilityEntity();
                e.setId(UUID.randomUUID().toString());
                e.setMemberId(memberId);
                e.setWeekday(a.weekday());
                e.setStartTime(a.startTime());
                e.setEndTime(a.endTime());
                return e;
            })
            .toList();
        return repository.saveAll(entities).stream()
            .map(MemberAvailabilityService::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public int countAvailableForSlot(Partner.Weekday weekday, String startTime, String endTime) {
        if (weekday == null || startTime == null || endTime == null) return 0;
        return (int) repository.countAvailableMembersForSlot(weekday, startTime, endTime);
    }

    /**
     * Batch convenience: counts availability per (weekday,start,end) tuple.
     * Falls back to per-slot calls; the dataset is small (≤7 slots per partner).
     */
    @Transactional(readOnly = true)
    public Map<SlotKey, Integer> countAvailableForSlots(List<SlotKey> slots) {
        Map<SlotKey, Integer> result = new HashMap<>();
        for (SlotKey s : slots) {
            result.computeIfAbsent(s, k ->
                countAvailableForSlot(k.weekday(), k.startTime(), k.endTime()));
        }
        return result;
    }

    public record SlotKey(Partner.Weekday weekday, String startTime, String endTime) {}

    private static MemberAvailability toDto(MemberAvailabilityEntity e) {
        return new MemberAvailability(
            e.getId(), e.getMemberId(), e.getWeekday(), e.getStartTime(), e.getEndTime()
        );
    }

    private static void validate(List<MemberAvailability> items) {
        if (items == null) return;
        for (MemberAvailability a : items) {
            if (a.weekday() == null) {
                throw new IllegalArgumentException("weekday is required");
            }
            if (a.startTime() == null || !a.startTime().matches("\\d{2}:\\d{2}")) {
                throw new IllegalArgumentException("startTime must be HH:mm");
            }
            if (a.endTime() == null || !a.endTime().matches("\\d{2}:\\d{2}")) {
                throw new IllegalArgumentException("endTime must be HH:mm");
            }
            if (a.startTime().compareTo(a.endTime()) >= 0) {
                throw new IllegalArgumentException("startTime must be before endTime");
            }
        }
    }
}

package de.tellerstatttonne.backend.user.availability;

import de.tellerstatttonne.backend.partner.Partner;
import de.tellerstatttonne.backend.user.UserRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserAvailabilityService {

    private final UserAvailabilityRepository repository;
    private final UserRepository userRepository;

    public UserAvailabilityService(
        UserAvailabilityRepository repository,
        UserRepository userRepository
    ) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserAvailability> findByUserId(Long userId) {
        return repository.findByUserId(userId).stream()
            .map(UserAvailabilityService::toDto)
            .toList();
    }

    public List<UserAvailability> replaceAll(Long userId, List<UserAvailability> items) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("user not found: " + userId);
        }
        validate(items);
        repository.deleteByUserId(userId);
        Set<String> seen = new HashSet<>();
        List<UserAvailabilityEntity> entities = items.stream()
            .filter(a -> seen.add(a.weekday() + "|" + a.startTime() + "|" + a.endTime()))
            .map(a -> {
                UserAvailabilityEntity e = new UserAvailabilityEntity();
                e.setUserId(userId);
                e.setWeekday(a.weekday());
                e.setStartTime(a.startTime());
                e.setEndTime(a.endTime());
                return e;
            })
            .toList();
        return repository.saveAll(entities).stream()
            .map(UserAvailabilityService::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public int countAvailableForSlot(Partner.Weekday weekday, String startTime, String endTime) {
        if (weekday == null || startTime == null || endTime == null) return 0;
        return (int) repository.countAvailableUsersForSlot(weekday, startTime, endTime);
    }

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

    private static UserAvailability toDto(UserAvailabilityEntity e) {
        return new UserAvailability(
            e.getId(), e.getUserId(), e.getWeekday(), e.getStartTime(), e.getEndTime()
        );
    }

    private static void validate(List<UserAvailability> items) {
        if (items == null) return;
        for (UserAvailability a : items) {
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

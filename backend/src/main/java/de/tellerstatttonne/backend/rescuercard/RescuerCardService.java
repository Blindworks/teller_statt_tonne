package de.tellerstatttonne.backend.rescuercard;

import de.tellerstatttonne.backend.hygiene.HygieneCertificateDto;
import de.tellerstatttonne.backend.hygiene.HygieneCertificateService;
import de.tellerstatttonne.backend.pickup.Pickup;
import de.tellerstatttonne.backend.pickup.PickupEntity;
import de.tellerstatttonne.backend.pickup.PickupRepository;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RescuerCardService {

    private final UserRepository userRepository;
    private final PickupRepository pickupRepository;
    private final HygieneCertificateService hygieneService;
    private Clock clock = Clock.systemDefaultZone();

    public RescuerCardService(
        UserRepository userRepository,
        PickupRepository pickupRepository,
        HygieneCertificateService hygieneService
    ) {
        this.userRepository = userRepository;
        this.pickupRepository = pickupRepository;
        this.hygieneService = hygieneService;
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    public RescuerCardContext buildContext(Long userId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("user not found: " + userId));

        Optional<HygieneCertificateDto> certificate = hygieneService.findByUserId(userId);
        boolean hygieneValid = hygieneService.isCurrentlyValid(userId);
        LocalDate expiry = certificate.map(HygieneCertificateDto::expiryDate).orElse(null);

        RescuerCardContext.CurrentPickup pickup = selectCurrentPickup(userId).orElse(null);

        return new RescuerCardContext(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getPhotoUrl(),
            user.getIntroductionCompletedAt(),
            hygieneValid,
            expiry,
            pickup,
            Instant.now(clock)
        );
    }

    private Optional<RescuerCardContext.CurrentPickup> selectCurrentPickup(Long userId) {
        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);

        List<PickupEntity> todays = pickupRepository
            .findByDateBetweenOrderByDateAscStartTimeAsc(today, today)
            .stream()
            .filter(p -> p.getStatus() != Pickup.Status.CANCELLED)
            .filter(p -> p.getAssignments().stream().anyMatch(a -> userId.equals(a.getUserId())))
            .toList();

        if (todays.isEmpty()) {
            return Optional.empty();
        }

        Optional<PickupEntity> active = todays.stream()
            .filter(p -> withinSlot(p, now))
            .findFirst();
        if (active.isPresent()) {
            return active.map(p -> toDto(p, true));
        }

        return todays.stream()
            .filter(p -> parseTime(p.getStartTime()).isAfter(now))
            .min(Comparator.comparing(p -> parseTime(p.getStartTime())))
            .map(p -> toDto(p, false));
    }

    private static boolean withinSlot(PickupEntity p, LocalTime now) {
        LocalTime start = parseTime(p.getStartTime());
        LocalTime end = parseTime(p.getEndTime());
        return !now.isBefore(start) && now.isBefore(end);
    }

    private static LocalTime parseTime(String s) {
        return LocalTime.parse(s);
    }

    private static RescuerCardContext.CurrentPickup toDto(PickupEntity p, boolean active) {
        return new RescuerCardContext.CurrentPickup(
            p.getId(),
            p.getPartner() != null ? p.getPartner().getId() : null,
            p.getPartner() != null ? p.getPartner().getName() : null,
            p.getDate(),
            p.getStartTime(),
            p.getEndTime(),
            active
        );
    }
}

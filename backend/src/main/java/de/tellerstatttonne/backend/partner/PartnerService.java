package de.tellerstatttonne.backend.partner;

import de.tellerstatttonne.backend.member.availability.MemberAvailabilityService;
import de.tellerstatttonne.backend.member.availability.MemberAvailabilityService.SlotKey;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PartnerService {

    private final PartnerRepository repository;
    private final GeocodingService geocodingService;
    private final MemberAvailabilityService availabilityService;

    public PartnerService(
        PartnerRepository repository,
        GeocodingService geocodingService,
        MemberAvailabilityService availabilityService
    ) {
        this.repository = repository;
        this.geocodingService = geocodingService;
        this.availabilityService = availabilityService;
    }

    @Transactional(readOnly = true)
    public List<Partner> findAll() {
        List<Partner> partners = repository.findAll().stream().map(PartnerMapper::toDto).toList();
        return enrichWithAvailability(partners);
    }

    @Transactional(readOnly = true)
    public Optional<Partner> findById(Long id) {
        return repository.findById(id)
            .map(PartnerMapper::toDto)
            .map(p -> enrichWithAvailability(List.of(p)).get(0));
    }

    private List<Partner> enrichWithAvailability(List<Partner> partners) {
        LinkedHashSet<SlotKey> slotKeys = new LinkedHashSet<>();
        for (Partner p : partners) {
            if (p.pickupSlots() == null) continue;
            for (Partner.PickupSlot s : p.pickupSlots()) {
                if (s.weekday() != null && s.startTime() != null && s.endTime() != null) {
                    slotKeys.add(new SlotKey(s.weekday(), s.startTime(), s.endTime()));
                }
            }
        }
        if (slotKeys.isEmpty()) return partners;
        Map<SlotKey, Integer> counts = availabilityService.countAvailableForSlots(new ArrayList<>(slotKeys));
        return partners.stream().map(p -> withSlotCounts(p, counts)).toList();
    }

    private static Partner withSlotCounts(Partner p, Map<SlotKey, Integer> counts) {
        if (p.pickupSlots() == null || p.pickupSlots().isEmpty()) return p;
        List<Partner.PickupSlot> enriched = p.pickupSlots().stream()
            .map(s -> {
                Integer count = counts.get(new SlotKey(s.weekday(), s.startTime(), s.endTime()));
                return new Partner.PickupSlot(
                    s.weekday(), s.startTime(), s.endTime(), s.active(),
                    s.capacity(), count != null ? count : 0
                );
            })
            .toList();
        return new Partner(
            p.id(), p.name(), p.category(), p.street(), p.postalCode(), p.city(),
            p.logoUrl(), p.contact(), enriched, p.status(), p.latitude(), p.longitude()
        );
    }

    public Partner create(Partner partner) {
        validate(partner);
        PartnerEntity entity = new PartnerEntity();
        PartnerMapper.applyToEntity(entity, partner);
        if (hasCoordinates(partner)) {
            entity.setLatitude(partner.latitude());
            entity.setLongitude(partner.longitude());
        } else {
            applyForwardGeocoding(entity);
        }
        return PartnerMapper.toDto(repository.save(entity));
    }

    public Optional<Partner> update(Long id, Partner partner) {
        return repository.findById(id).map(entity -> {
            validate(partner);
            boolean addressChanged = !addressEquals(entity, partner);
            PartnerMapper.applyToEntity(entity, partner);
            if (hasCoordinates(partner)) {
                entity.setLatitude(partner.latitude());
                entity.setLongitude(partner.longitude());
            } else if (addressChanged || entity.getLatitude() == null || entity.getLongitude() == null) {
                applyForwardGeocoding(entity);
            }
            return PartnerMapper.toDto(repository.save(entity));
        });
    }

    public Optional<Partner> regeocode(Long id) {
        return repository.findById(id).map(entity -> {
            applyForwardGeocoding(entity);
            return PartnerMapper.toDto(repository.save(entity));
        });
    }

    public boolean delete(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    private static boolean hasCoordinates(Partner partner) {
        return partner.latitude() != null && partner.longitude() != null;
    }

    private void applyForwardGeocoding(PartnerEntity entity) {
        Optional<GeocodingService.Coordinates> coords = geocodingService.geocode(
            entity.getStreet(), entity.getPostalCode(), entity.getCity());
        if (coords.isPresent()) {
            entity.setLatitude(coords.get().lat());
            entity.setLongitude(coords.get().lon());
        } else {
            entity.setLatitude(null);
            entity.setLongitude(null);
        }
    }

    private boolean addressEquals(PartnerEntity entity, Partner partner) {
        return Objects.equals(entity.getStreet(), partner.street())
            && Objects.equals(entity.getPostalCode(), partner.postalCode())
            && Objects.equals(entity.getCity(), partner.city());
    }

    private void validate(Partner partner) {
        if (partner.name() == null || partner.name().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (partner.category() == null) {
            throw new IllegalArgumentException("category is required");
        }
    }
}

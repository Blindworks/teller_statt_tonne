package de.tellerstatttonne.backend.partner;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PartnerService {

    private final PartnerRepository repository;
    private final GeocodingService geocodingService;

    public PartnerService(PartnerRepository repository, GeocodingService geocodingService) {
        this.repository = repository;
        this.geocodingService = geocodingService;
    }

    @Transactional(readOnly = true)
    public List<Partner> findAll() {
        return repository.findAll().stream().map(PartnerMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Partner> findById(String id) {
        return repository.findById(id).map(PartnerMapper::toDto);
    }

    public Partner create(Partner partner) {
        validate(partner);
        PartnerEntity entity = new PartnerEntity();
        entity.setId(UUID.randomUUID().toString());
        PartnerMapper.applyToEntity(entity, partner);
        if (hasCoordinates(partner)) {
            entity.setLatitude(partner.latitude());
            entity.setLongitude(partner.longitude());
        } else {
            applyForwardGeocoding(entity);
        }
        return PartnerMapper.toDto(repository.save(entity));
    }

    public Optional<Partner> update(String id, Partner partner) {
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

    public Optional<Partner> regeocode(String id) {
        return repository.findById(id).map(entity -> {
            applyForwardGeocoding(entity);
            return PartnerMapper.toDto(repository.save(entity));
        });
    }

    public boolean delete(String id) {
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

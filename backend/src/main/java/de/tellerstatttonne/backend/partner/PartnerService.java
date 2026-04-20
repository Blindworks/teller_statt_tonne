package de.tellerstatttonne.backend.partner;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PartnerService {

    private final PartnerRepository repository;

    public PartnerService(PartnerRepository repository) {
        this.repository = repository;
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
        return PartnerMapper.toDto(repository.save(entity));
    }

    public Optional<Partner> update(String id, Partner partner) {
        return repository.findById(id).map(entity -> {
            validate(partner);
            PartnerMapper.applyToEntity(entity, partner);
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

    private void validate(Partner partner) {
        if (partner.name() == null || partner.name().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (partner.category() == null) {
            throw new IllegalArgumentException("category is required");
        }
    }
}

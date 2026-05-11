package de.tellerstatttonne.backend.partnercategory;

import de.tellerstatttonne.backend.partner.PartnerRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PartnerCategoryService {

    private final PartnerCategoryRepository repository;
    private final PartnerRepository partnerRepository;

    public PartnerCategoryService(
        PartnerCategoryRepository repository,
        PartnerRepository partnerRepository
    ) {
        this.repository = repository;
        this.partnerRepository = partnerRepository;
    }

    @Transactional(readOnly = true)
    public List<PartnerCategory> findActive() {
        return repository.findAllByActiveTrueOrderByOrderIndexAscIdAsc().stream()
            .map(PartnerCategoryMapper::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PartnerCategory> findAll() {
        return repository.findAllByOrderByOrderIndexAscIdAsc().stream()
            .map(PartnerCategoryMapper::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<PartnerCategory> findById(Long id) {
        return repository.findById(id).map(PartnerCategoryMapper::toDto);
    }

    public PartnerCategory create(PartnerCategory dto) {
        validate(dto, null);
        PartnerCategoryEntity entity = new PartnerCategoryEntity();
        PartnerCategoryMapper.applyToEntity(entity, dto);
        return PartnerCategoryMapper.toDto(repository.save(entity));
    }

    public Optional<PartnerCategory> update(Long id, PartnerCategory dto) {
        return repository.findById(id).map(entity -> {
            validate(dto, id);
            PartnerCategoryMapper.applyToEntity(entity, dto);
            return PartnerCategoryMapper.toDto(repository.save(entity));
        });
    }

    public DeleteResult delete(Long id) {
        Optional<PartnerCategoryEntity> opt = repository.findById(id);
        if (opt.isEmpty()) return DeleteResult.NOT_FOUND;
        if (partnerRepository.countByCategoryId(id) > 0) return DeleteResult.IN_USE;
        repository.delete(opt.get());
        return DeleteResult.DELETED;
    }

    public enum DeleteResult { DELETED, NOT_FOUND, IN_USE }

    private void validate(PartnerCategory dto, Long currentId) {
        if (dto.code() == null || dto.code().isBlank()) {
            throw new IllegalArgumentException("code ist erforderlich");
        }
        if (dto.label() == null || dto.label().isBlank()) {
            throw new IllegalArgumentException("label ist erforderlich");
        }
        if (dto.icon() == null || dto.icon().isBlank()) {
            throw new IllegalArgumentException("icon ist erforderlich");
        }
        String code = dto.code().trim();
        repository.findByCodeIgnoreCase(code).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new IllegalArgumentException("Code bereits vergeben: " + code);
            }
        });
    }
}

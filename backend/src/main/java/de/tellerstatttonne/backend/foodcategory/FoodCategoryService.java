package de.tellerstatttonne.backend.foodcategory;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FoodCategoryService {

    private final FoodCategoryRepository repository;

    public FoodCategoryService(FoodCategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<FoodCategory> findAllActive() {
        return repository.findAllByActiveTrueOrderBySortOrderAscNameAsc().stream()
            .map(FoodCategoryService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<FoodCategory> findAll() {
        return repository.findAllByOrderBySortOrderAscNameAsc().stream()
            .map(FoodCategoryService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<FoodCategory> findById(Long id) {
        return repository.findById(id).map(FoodCategoryService::toDto);
    }

    public FoodCategory create(FoodCategory dto) {
        validate(dto);
        ensureUniqueName(dto.name(), null);
        FoodCategoryEntity e = new FoodCategoryEntity();
        applyToEntity(e, dto);
        if (e.getSortOrder() <= 0) {
            e.setSortOrder(repository.findMaxSortOrder() + 1);
        }
        return toDto(repository.save(e));
    }

    public Optional<FoodCategory> update(Long id, FoodCategory dto) {
        return repository.findById(id).map(e -> {
            validate(dto);
            ensureUniqueName(dto.name(), id);
            applyToEntity(e, dto);
            return toDto(repository.save(e));
        });
    }

    private void ensureUniqueName(String name, Long excludeId) {
        repository.findFirstByNameIgnoreCase(name.trim()).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new IllegalArgumentException(
                    "Eine Kategorie mit dem Namen \"" + existing.getName() + "\" existiert bereits.");
            }
        });
    }

    public boolean delete(Long id) {
        if (!repository.existsById(id)) return false;
        repository.deleteById(id);
        return true;
    }

    static FoodCategory toDto(FoodCategoryEntity e) {
        return new FoodCategory(e.getId(), e.getName(), e.getEmoji(),
            e.getColorHex(), e.getSortOrder(), e.isActive());
    }

    private static void applyToEntity(FoodCategoryEntity e, FoodCategory dto) {
        e.setName(dto.name());
        e.setEmoji(dto.emoji());
        e.setColorHex(dto.colorHex());
        e.setSortOrder(dto.sortOrder());
        e.setActive(dto.active());
    }

    private static void validate(FoodCategory dto) {
        if (dto.name() == null || dto.name().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
    }
}

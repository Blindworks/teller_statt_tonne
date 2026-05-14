package de.tellerstatttonne.backend.settings;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SystemSettingService {

    private final SystemSettingRepository repository;

    public SystemSettingService(SystemSettingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SystemSettingEntity> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public String getString(String key, String defaultValue) {
        return repository.findById(key).map(SystemSettingEntity::getValue).orElse(defaultValue);
    }

    @Transactional(readOnly = true)
    public int getInt(String key, int defaultValue) {
        return repository.findById(key)
            .map(SystemSettingEntity::getValue)
            .map(v -> {
                try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return defaultValue; }
            })
            .orElse(defaultValue);
    }

    public SystemSettingEntity set(String key, String value, Long updatedByUserId) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key required");
        }
        if (value == null) {
            throw new IllegalArgumentException("value required");
        }
        SystemSettingEntity entity = repository.findById(key).orElseGet(() -> {
            SystemSettingEntity created = new SystemSettingEntity();
            created.setKey(key);
            return created;
        });
        entity.setValue(value);
        entity.setUpdatedByUserId(updatedByUserId);
        return repository.save(entity);
    }
}

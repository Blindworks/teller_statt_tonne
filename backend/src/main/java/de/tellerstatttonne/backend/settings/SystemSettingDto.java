package de.tellerstatttonne.backend.settings;

import java.time.Instant;

public record SystemSettingDto(
    String key,
    String value,
    Instant updatedAt,
    Long updatedByUserId
) {
    public static SystemSettingDto from(SystemSettingEntity entity) {
        return new SystemSettingDto(
            entity.getKey(),
            entity.getValue(),
            entity.getUpdatedAt(),
            entity.getUpdatedByUserId()
        );
    }
}

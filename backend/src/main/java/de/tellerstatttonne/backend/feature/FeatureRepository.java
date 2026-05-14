package de.tellerstatttonne.backend.feature;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureRepository extends JpaRepository<FeatureEntity, Long> {

    List<FeatureEntity> findAllByOrderBySortOrderAscIdAsc();

    Optional<FeatureEntity> findByKey(String key);

    boolean existsByKey(String key);
}

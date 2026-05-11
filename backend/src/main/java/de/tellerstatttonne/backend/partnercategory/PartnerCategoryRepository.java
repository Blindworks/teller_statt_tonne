package de.tellerstatttonne.backend.partnercategory;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerCategoryRepository extends JpaRepository<PartnerCategoryEntity, Long> {

    List<PartnerCategoryEntity> findAllByOrderByOrderIndexAscIdAsc();

    List<PartnerCategoryEntity> findAllByActiveTrueOrderByOrderIndexAscIdAsc();

    Optional<PartnerCategoryEntity> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}

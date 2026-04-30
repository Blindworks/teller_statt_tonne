package de.tellerstatttonne.backend.quiz;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultCategoryRepository extends JpaRepository<ResultCategoryEntity, Long> {
    List<ResultCategoryEntity> findAllByOrderByOrderIndexAsc();
}

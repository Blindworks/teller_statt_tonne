package de.tellerstatttonne.backend.foodcategory;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FoodCategoryRepository extends JpaRepository<FoodCategoryEntity, Long> {
    List<FoodCategoryEntity> findAllByActiveTrueOrderBySortOrderAscNameAsc();
    List<FoodCategoryEntity> findAllByOrderBySortOrderAscNameAsc();

    @Query("select coalesce(max(f.sortOrder), 0) from FoodCategoryEntity f")
    int findMaxSortOrder();

    Optional<FoodCategoryEntity> findFirstByNameIgnoreCase(String name);
}

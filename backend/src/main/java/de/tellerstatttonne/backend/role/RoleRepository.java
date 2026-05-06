package de.tellerstatttonne.backend.role;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByName(String name);

    List<RoleEntity> findByNameIn(Set<String> names);

    boolean existsByName(String name);

    long countByEnabledTrue();
}

package de.tellerstatttonne.backend.feature;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleFeatureRepository extends JpaRepository<RoleFeatureEntity, RoleFeatureId> {

    List<RoleFeatureEntity> findAllByIdRoleId(Long roleId);

    @Query("select rf.id.featureId from RoleFeatureEntity rf where rf.id.roleId = :roleId")
    List<Long> findFeatureIdsByRoleId(@Param("roleId") Long roleId);

    @Query("select distinct f.key from RoleFeatureEntity rf, FeatureEntity f "
        + "where f.id = rf.id.featureId and rf.id.roleId in :roleIds")
    List<String> findFeatureKeysByRoleIds(@Param("roleIds") List<Long> roleIds);

    @Modifying
    @Query("delete from RoleFeatureEntity rf where rf.id.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);
}

package de.tellerstatttonne.backend.feature;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "role_feature")
public class RoleFeatureEntity {

    @EmbeddedId
    private RoleFeatureId id;

    public RoleFeatureEntity() {}

    public RoleFeatureEntity(Long roleId, Long featureId) {
        this.id = new RoleFeatureId(roleId, featureId);
    }

    public RoleFeatureId getId() { return id; }
    public void setId(RoleFeatureId id) { this.id = id; }

    public Long getRoleId() { return id == null ? null : id.getRoleId(); }
    public Long getFeatureId() { return id == null ? null : id.getFeatureId(); }
}

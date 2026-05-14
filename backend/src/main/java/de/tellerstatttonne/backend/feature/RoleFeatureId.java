package de.tellerstatttonne.backend.feature;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RoleFeatureId implements Serializable {

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "feature_id")
    private Long featureId;

    public RoleFeatureId() {}

    public RoleFeatureId(Long roleId, Long featureId) {
        this.roleId = roleId;
        this.featureId = featureId;
    }

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public Long getFeatureId() { return featureId; }
    public void setFeatureId(Long featureId) { this.featureId = featureId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoleFeatureId other)) return false;
        return Objects.equals(roleId, other.roleId) && Objects.equals(featureId, other.featureId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId, featureId);
    }
}

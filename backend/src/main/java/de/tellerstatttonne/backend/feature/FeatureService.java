package de.tellerstatttonne.backend.feature;

import de.tellerstatttonne.backend.role.Role;
import de.tellerstatttonne.backend.role.RoleEntity;
import de.tellerstatttonne.backend.role.RoleRepository;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FeatureService {

    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-z][a-z0-9.\\-]{1,127}$");

    private final FeatureRepository featureRepository;
    private final RoleFeatureRepository roleFeatureRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public FeatureService(FeatureRepository featureRepository,
                          RoleFeatureRepository roleFeatureRepository,
                          RoleRepository roleRepository,
                          UserRepository userRepository) {
        this.featureRepository = featureRepository;
        this.roleFeatureRepository = roleFeatureRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Feature> listAll() {
        return featureRepository.findAllByOrderBySortOrderAscIdAsc().stream()
            .map(FeatureMapper::toDto)
            .toList();
    }

    public Feature create(FeatureRequest request) {
        String key = trim(request.key());
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException(
                "Ungültiger Feature-Key. Erlaubt: a-z, 0-9, Punkt, Bindestrich. Beginnt mit Buchstabe.");
        }
        if (featureRepository.existsByKey(key)) {
            throw new IllegalArgumentException("Feature-Key bereits vergeben: " + key);
        }
        FeatureEntity entity = new FeatureEntity();
        entity.setKey(key);
        applyMutableFields(entity, request);
        return FeatureMapper.toDto(featureRepository.save(entity));
    }

    public Feature update(Long id, FeatureRequest request) {
        FeatureEntity entity = featureRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Feature nicht gefunden"));
        applyMutableFields(entity, request);
        return FeatureMapper.toDto(featureRepository.save(entity));
    }

    public void delete(Long id) {
        FeatureEntity entity = featureRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Feature nicht gefunden"));
        featureRepository.delete(entity);
    }

    // --- Role <-> Feature Matrix ---

    @Transactional(readOnly = true)
    public List<Long> featureIdsForRole(Long roleId) {
        return roleFeatureRepository.findFeatureIdsByRoleId(roleId);
    }

    public void setFeaturesForRole(Long roleId, List<Long> featureIds) {
        RoleEntity role = roleRepository.findById(roleId)
            .orElseThrow(() -> new NoSuchElementException("Rolle nicht gefunden"));
        if (Role.ADMIN_ROLE_NAME.equals(role.getName())) {
            throw new AdminRoleLockedException(
                "Die Administrator-Rolle hat immer Zugriff auf alle Features und kann nicht angepasst werden.");
        }
        Set<Long> valid = new HashSet<>(
            featureRepository.findAll().stream().map(FeatureEntity::getId).toList());
        roleFeatureRepository.deleteByRoleId(roleId);
        roleFeatureRepository.flush();
        if (featureIds != null) {
            for (Long fid : featureIds) {
                if (fid != null && valid.contains(fid)) {
                    roleFeatureRepository.save(new RoleFeatureEntity(roleId, fid));
                }
            }
        }
    }

    /**
     * Liefert die Feature-Keys, die der angegebene User sehen darf.
     * Für ADMINISTRATOR → alle existierenden Feature-Keys.
     */
    @Transactional(readOnly = true)
    public Set<String> featureKeysForUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("Benutzer nicht gefunden"));
        if (user.hasRole(Role.ADMIN_ROLE_NAME)) {
            return featureRepository.findAll().stream()
                .map(FeatureEntity::getKey)
                .collect(Collectors.toCollection(HashSet::new));
        }
        List<Long> roleIds = user.getRoles().stream().map(RoleEntity::getId).toList();
        if (roleIds.isEmpty()) return Set.of();
        return new HashSet<>(roleFeatureRepository.findFeatureKeysByRoleIds(roleIds));
    }

    private void applyMutableFields(FeatureEntity entity, FeatureRequest request) {
        String label = trim(request.label());
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label ist erforderlich");
        }
        entity.setLabel(label);
        entity.setDescription(trim(request.description()));
        entity.setCategory(trim(request.category()));
        entity.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    }

    private static String trim(String s) { return s == null ? null : s.trim(); }

    public static class AdminRoleLockedException extends RuntimeException {
        public AdminRoleLockedException(String message) { super(message); }
    }
}

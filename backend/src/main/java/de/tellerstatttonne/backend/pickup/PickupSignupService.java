package de.tellerstatttonne.backend.pickup;

import de.tellerstatttonne.backend.partner.PartnerEntity;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PickupSignupService {

    public enum Result { OK, PICKUP_NOT_FOUND, USER_NOT_FOUND, NOT_MEMBER, CAPACITY_FULL, NOT_ASSIGNED }

    private final PickupRepository pickupRepository;
    private final UserRepository userRepository;

    public PickupSignupService(PickupRepository pickupRepository, UserRepository userRepository) {
        this.pickupRepository = pickupRepository;
        this.userRepository = userRepository;
    }

    public Result signup(Long pickupId, Long userId) {
        Optional<PickupEntity> pickupOpt = pickupRepository.findById(pickupId);
        if (pickupOpt.isEmpty()) return Result.PICKUP_NOT_FOUND;
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return Result.USER_NOT_FOUND;

        PickupEntity pickup = pickupOpt.get();
        UserEntity user = userOpt.get();

        PartnerEntity partner = pickup.getPartner();
        boolean isMember = partner.getMembers().stream()
            .anyMatch(m -> userId.equals(m.getId()));
        if (!isMember) return Result.NOT_MEMBER;

        boolean alreadyAssigned = pickup.getAssignments().stream()
            .anyMatch(a -> userId.equals(a.getUserId()));
        if (alreadyAssigned) return Result.OK;

        if (pickup.getAssignments().size() >= pickup.getCapacity()) {
            return Result.CAPACITY_FULL;
        }

        PickupEntity.AssignmentEmbeddable assignment = new PickupEntity.AssignmentEmbeddable();
        assignment.setUserId(user.getId());
        pickup.getAssignments().add(assignment);
        pickupRepository.save(pickup);
        return Result.OK;
    }

    public Result unassign(Long pickupId, Long userId) {
        Optional<PickupEntity> pickupOpt = pickupRepository.findById(pickupId);
        if (pickupOpt.isEmpty()) return Result.PICKUP_NOT_FOUND;

        PickupEntity pickup = pickupOpt.get();
        boolean removed = pickup.getAssignments().removeIf(a -> userId.equals(a.getUserId()));
        if (!removed) return Result.NOT_ASSIGNED;
        pickupRepository.save(pickup);
        return Result.OK;
    }
}

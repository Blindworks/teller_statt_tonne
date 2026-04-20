package de.tellerstatttonne.backend.pickup;

import de.tellerstatttonne.backend.member.Member;
import de.tellerstatttonne.backend.member.MemberService;
import de.tellerstatttonne.backend.partner.PartnerEntity;
import de.tellerstatttonne.backend.partner.PartnerRepository;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PickupService {

    private static final Pattern TIME = Pattern.compile("^\\d{2}:\\d{2}$");

    private final PickupRepository repository;
    private final PartnerRepository partnerRepository;
    private final MemberService memberService;

    public PickupService(PickupRepository repository,
                         PartnerRepository partnerRepository,
                         MemberService memberService) {
        this.repository = repository;
        this.partnerRepository = partnerRepository;
        this.memberService = memberService;
    }

    @Transactional(readOnly = true)
    public List<Pickup> findBetween(LocalDate from, LocalDate to) {
        List<PickupEntity> entities = repository.findByDateBetweenOrderByDateAscStartTimeAsc(from, to);
        return mapAll(entities);
    }

    @Transactional(readOnly = true)
    public List<Pickup> findRecent() {
        return mapAll(repository.findTop10ByOrderByDateDescStartTimeDesc());
    }

    @Transactional(readOnly = true)
    public List<Pickup> findUpcoming(int limit) {
        int capped = Math.max(1, Math.min(limit, 50));
        return mapAll(repository.findByStatusAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
            Pickup.Status.SCHEDULED, LocalDate.now(), PageRequest.of(0, capped)));
    }

    @Transactional(readOnly = true)
    public Optional<Pickup> findById(String id) {
        return repository.findById(id).map(e -> PickupMapper.toDto(e, resolveMembers(List.of(e))));
    }

    public Pickup create(Pickup pickup) {
        validate(pickup);
        PartnerEntity partner = loadPartner(pickup.partnerId());
        PickupEntity entity = new PickupEntity();
        entity.setId(UUID.randomUUID().toString());
        PickupMapper.applyToEntity(entity, pickup, partner);
        PickupEntity saved = repository.save(entity);
        return PickupMapper.toDto(saved, resolveMembers(List.of(saved)));
    }

    public Optional<Pickup> update(String id, Pickup pickup) {
        return repository.findById(id).map(entity -> {
            validate(pickup);
            PartnerEntity partner = loadPartner(pickup.partnerId());
            PickupMapper.applyToEntity(entity, pickup, partner);
            PickupEntity saved = repository.save(entity);
            return PickupMapper.toDto(saved, resolveMembers(List.of(saved)));
        });
    }

    public boolean delete(String id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    private List<Pickup> mapAll(List<PickupEntity> entities) {
        Map<String, Member> members = resolveMembers(entities);
        return entities.stream().map(e -> PickupMapper.toDto(e, members)).toList();
    }

    private Map<String, Member> resolveMembers(List<PickupEntity> entities) {
        Set<String> ids = new HashSet<>();
        for (PickupEntity e : entities) {
            if (e.getAssignments() == null) continue;
            for (PickupEntity.AssignmentEmbeddable a : e.getAssignments()) {
                if (a.getMemberId() != null) ids.add(a.getMemberId());
            }
        }
        return ids.stream()
            .map(memberService::findById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toMap(Member::id, m -> m));
    }

    private PartnerEntity loadPartner(String partnerId) {
        if (partnerId == null || partnerId.isBlank()) {
            throw new IllegalArgumentException("partnerId is required");
        }
        return partnerRepository.findById(partnerId)
            .orElseThrow(() -> new IllegalArgumentException("partner not found: " + partnerId));
    }

    private void validate(Pickup pickup) {
        if (pickup.date() == null) {
            throw new IllegalArgumentException("date is required");
        }
        if (pickup.startTime() == null || !TIME.matcher(pickup.startTime()).matches()) {
            throw new IllegalArgumentException("startTime must match HH:mm");
        }
        if (pickup.endTime() == null || !TIME.matcher(pickup.endTime()).matches()) {
            throw new IllegalArgumentException("endTime must match HH:mm");
        }
        if (pickup.endTime().compareTo(pickup.startTime()) <= 0) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        if (pickup.capacity() <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        int assigned = pickup.assignments() == null ? 0 : pickup.assignments().size();
        if (assigned > pickup.capacity()) {
            throw new IllegalArgumentException("capacity must be >= assignments count");
        }
    }
}

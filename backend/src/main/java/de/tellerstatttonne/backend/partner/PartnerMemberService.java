package de.tellerstatttonne.backend.partner;

import de.tellerstatttonne.backend.member.MemberEntity;
import de.tellerstatttonne.backend.member.MemberRepository;
import de.tellerstatttonne.backend.pickup.PickupRepository;
import de.tellerstatttonne.backend.pickup.PickupRepository.MemberPickupStats;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PartnerMemberService {

    private final PartnerRepository partnerRepository;
    private final MemberRepository memberRepository;
    private final PickupRepository pickupRepository;

    public PartnerMemberService(
        PartnerRepository partnerRepository,
        MemberRepository memberRepository,
        PickupRepository pickupRepository
    ) {
        this.partnerRepository = partnerRepository;
        this.memberRepository = memberRepository;
        this.pickupRepository = pickupRepository;
    }

    @Transactional(readOnly = true)
    public Optional<List<StoreMember>> list(String partnerId) {
        return partnerRepository.findById(partnerId).map(partner -> {
            Map<String, MemberPickupStats> statsByMember = new HashMap<>();
            for (MemberPickupStats row : pickupRepository.aggregateByPartner(partnerId)) {
                statsByMember.put(row.getMemberId(), row);
            }
            return partner.getMembers().stream()
                .sorted(Comparator
                    .comparing(MemberEntity::getFirstName, Comparator.nullsLast(String::compareToIgnoreCase))
                    .thenComparing(MemberEntity::getLastName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(member -> toDto(member, statsByMember.get(member.getId())))
                .toList();
        });
    }

    public AssignResult assign(String partnerId, String memberId) {
        Optional<PartnerEntity> partnerOpt = partnerRepository.findById(partnerId);
        if (partnerOpt.isEmpty()) {
            return AssignResult.PARTNER_NOT_FOUND;
        }
        Optional<MemberEntity> memberOpt = memberRepository.findById(memberId);
        if (memberOpt.isEmpty()) {
            return AssignResult.MEMBER_NOT_FOUND;
        }
        PartnerEntity partner = partnerOpt.get();
        Set<MemberEntity> members = partner.getMembers();
        boolean added = members.add(memberOpt.get());
        if (added) {
            partnerRepository.save(partner);
        }
        return AssignResult.OK;
    }

    public AssignResult unassign(String partnerId, String memberId) {
        Optional<PartnerEntity> partnerOpt = partnerRepository.findById(partnerId);
        if (partnerOpt.isEmpty()) {
            return AssignResult.PARTNER_NOT_FOUND;
        }
        PartnerEntity partner = partnerOpt.get();
        boolean removed = partner.getMembers().removeIf(m -> memberId.equals(m.getId()));
        if (removed) {
            partnerRepository.save(partner);
        }
        return AssignResult.OK;
    }

    private StoreMember toDto(MemberEntity m, MemberPickupStats stats) {
        return new StoreMember(
            m.getId(),
            m.getFirstName(),
            m.getLastName(),
            m.getRole(),
            m.getEmail(),
            m.getCity(),
            m.getPhotoUrl(),
            m.getOnlineStatus(),
            m.getStatus(),
            stats != null ? stats.getLastPickupDate() : null,
            stats != null && stats.getTotalSavedKg() != null ? stats.getTotalSavedKg() : BigDecimal.ZERO,
            stats != null && stats.getPickupCount() != null ? stats.getPickupCount() : 0L
        );
    }

    public enum AssignResult { OK, PARTNER_NOT_FOUND, MEMBER_NOT_FOUND }
}

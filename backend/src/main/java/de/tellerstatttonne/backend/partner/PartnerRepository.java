package de.tellerstatttonne.backend.partner;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PartnerRepository extends JpaRepository<PartnerEntity, Long> {

    @Query("select p.id as partnerId, size(p.members) as memberCount from PartnerEntity p")
    List<MemberCountRow> countMembersGroupedByPartner();

    @Query("select p.id from PartnerEntity p join p.members m where m.id = :userId")
    List<Long> findIdsByMemberId(Long userId);

    interface MemberCountRow {
        Long getPartnerId();
        Integer getMemberCount();
    }
}

package de.tellerstatttonne.backend.partner;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PartnerRepository extends JpaRepository<PartnerEntity, Long> {

    List<PartnerEntity> findAllByStatusNot(Partner.Status status);

    List<PartnerEntity> findAllByStatus(Partner.Status status);

    @Query("select distinct p from PartnerEntity p join p.members m where p.status <> :excluded and m.id = :userId")
    List<PartnerEntity> findAllByMemberIdAndStatusNot(Long userId, Partner.Status excluded);

    @Query("select p.id as partnerId, size(p.members) as memberCount from PartnerEntity p where p.status <> :excluded")
    List<MemberCountRow> countMembersGroupedByPartnerExcluding(Partner.Status excluded);

    default List<MemberCountRow> countMembersGroupedByPartner() {
        return countMembersGroupedByPartnerExcluding(Partner.Status.EXISTIERT_NICHT_MEHR);
    }

    @Query("select p.id from PartnerEntity p join p.members m where m.id = :userId")
    List<Long> findIdsByMemberId(Long userId);

    long countByCategoryId(Long categoryId);

    interface MemberCountRow {
        Long getPartnerId();
        Integer getMemberCount();
    }
}

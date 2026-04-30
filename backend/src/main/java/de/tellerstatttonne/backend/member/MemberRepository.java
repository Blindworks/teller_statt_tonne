package de.tellerstatttonne.backend.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MemberRepository
    extends JpaRepository<MemberEntity, Long>, JpaSpecificationExecutor<MemberEntity> {
}

package de.tellerstatttonne.backend.member;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MemberService {

    private final MemberRepository repository;

    public MemberService(MemberRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Member> findAll(MemberRole role, boolean activeOnly, String search) {
        Specification<MemberEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (activeOnly) {
                predicates.add(cb.equal(root.get("onlineStatus"), Member.OnlineStatus.ONLINE));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern),
                    cb.like(cb.lower(root.get("lastName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)
                ));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec).stream().map(MemberMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Member> findById(Long id) {
        return repository.findById(id).map(MemberMapper::toDto);
    }

    public Member create(Member member) {
        validate(member);
        MemberEntity entity = new MemberEntity();
        MemberMapper.applyToEntity(entity, member);
        return MemberMapper.toDto(repository.save(entity));
    }

    public Member createForUser(String email) {
        String localPart = email == null ? "" : email.split("@", 2)[0];
        String firstName = localPart.isBlank() ? "Neues" : capitalize(localPart);
        MemberEntity entity = new MemberEntity();
        entity.setFirstName(firstName);
        entity.setLastName("Mitglied");
        entity.setRole(MemberRole.NEW_MEMBER);
        entity.setEmail(email);
        return MemberMapper.toDto(repository.save(entity));
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public Optional<Member> update(Long id, Member member) {
        return repository.findById(id).map(entity -> {
            validate(member);
            MemberMapper.applyToEntity(entity, member);
            return MemberMapper.toDto(repository.save(entity));
        });
    }

    public Optional<Member> updatePhotoUrl(Long id, String photoUrl) {
        return repository.findById(id).map(entity -> {
            entity.setPhotoUrl(photoUrl);
            return MemberMapper.toDto(repository.save(entity));
        });
    }

    @Transactional(readOnly = true)
    public Optional<String> findPhotoUrl(Long id) {
        return repository.findById(id).map(MemberEntity::getPhotoUrl);
    }

    public boolean delete(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    private void validate(Member member) {
        if (member.firstName() == null || member.firstName().isBlank()) {
            throw new IllegalArgumentException("firstName is required");
        }
        if (member.lastName() == null || member.lastName().isBlank()) {
            throw new IllegalArgumentException("lastName is required");
        }
        if (member.role() == null) {
            throw new IllegalArgumentException("role is required");
        }
    }
}

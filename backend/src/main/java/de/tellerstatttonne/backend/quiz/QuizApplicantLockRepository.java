package de.tellerstatttonne.backend.quiz;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizApplicantLockRepository extends JpaRepository<QuizApplicantLockEntity, Long> {
    Optional<QuizApplicantLockEntity> findByEmail(String email);
}

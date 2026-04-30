package de.tellerstatttonne.backend.quiz;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRepository extends JpaRepository<QuizAttemptEntity, Long> {
    List<QuizAttemptEntity> findAllByOrderByCompletedAtDesc();

    long countByApplicantEmailIgnoreCase(String email);

    boolean existsByApplicantEmailIgnoreCaseAndResultColor(String email, QuizColor color);
}

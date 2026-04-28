package de.tellerstatttonne.backend.quiz;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<QuestionEntity, String> {
    List<QuestionEntity> findAllByOrderByOrderIndexAsc();
}

package de.tellerstatttonne.backend.quiz;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class QuestionService {

    private static final List<BigDecimal> ALLOWED_WEIGHTS =
        List.of(new BigDecimal("0.5"), new BigDecimal("1.0"), new BigDecimal("1.5"));

    private final QuestionRepository repository;

    public QuestionService(QuestionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Question> findAll(boolean includeSolutions) {
        return repository.findAllByOrderByOrderIndexAsc().stream()
            .map(e -> QuizMapper.toDto(e, includeSolutions))
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Question> findById(String id) {
        return repository.findById(id).map(e -> QuizMapper.toDto(e, true));
    }

    public Question create(Question question) {
        validate(question);
        QuestionEntity entity = new QuestionEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setOrderIndex((int) repository.count());
        QuizMapper.applyToEntity(entity, question);
        return QuizMapper.toDto(repository.save(entity), true);
    }

    public Optional<Question> update(String id, Question question) {
        return repository.findById(id).map(entity -> {
            validate(question);
            QuizMapper.applyToEntity(entity, question);
            return QuizMapper.toDto(repository.save(entity), true);
        });
    }

    public boolean delete(String id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    private void validate(Question q) {
        if (q.text() == null || q.text().isBlank()) {
            throw new IllegalArgumentException("text is required");
        }
        if (q.weight() == null) {
            throw new IllegalArgumentException("weight is required");
        }
        if (ALLOWED_WEIGHTS.stream().noneMatch(w -> w.compareTo(q.weight()) == 0)) {
            throw new IllegalArgumentException("weight must be 0.5, 1.0 or 1.5");
        }
        if (q.answers() == null || q.answers().isEmpty()) {
            throw new IllegalArgumentException("at least one answer is required");
        }
        for (Answer a : q.answers()) {
            if (a.text() == null || a.text().isBlank()) {
                throw new IllegalArgumentException("answer text is required");
            }
        }
    }
}

package de.tellerstatttonne.backend.quiz;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ResultCategoryService {

    private final ResultCategoryRepository repository;

    public ResultCategoryService(ResultCategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ResultCategory> findAll() {
        return repository.findAllByOrderByOrderIndexAsc().stream()
            .map(QuizMapper::toDto)
            .toList();
    }

    public ResultCategory create(ResultCategory category) {
        validate(category);
        ResultCategoryEntity entity = new ResultCategoryEntity();
        entity.setOrderIndex((int) repository.count());
        QuizMapper.applyToEntity(entity, category);
        return QuizMapper.toDto(repository.save(entity));
    }

    public Optional<ResultCategory> update(Long id, ResultCategory category) {
        return repository.findById(id).map(entity -> {
            validate(category);
            QuizMapper.applyToEntity(entity, category);
            return QuizMapper.toDto(repository.save(entity));
        });
    }

    public boolean delete(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    private void validate(ResultCategory c) {
        if (c.label() == null || c.label().isBlank()) {
            throw new IllegalArgumentException("label is required");
        }
        if (c.color() == null) {
            throw new IllegalArgumentException("color is required");
        }
        if (c.minScore() == null) {
            throw new IllegalArgumentException("minScore is required");
        }
        if (c.maxScore() != null && c.minScore().compareTo(c.maxScore()) > 0) {
            throw new IllegalArgumentException("minScore must be <= maxScore");
        }
    }
}

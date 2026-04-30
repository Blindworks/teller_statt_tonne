package de.tellerstatttonne.backend.quiz;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quiz")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class QuizAdminController {

    private final QuestionService questionService;
    private final ResultCategoryService categoryService;
    private final QuizService quizService;

    public QuizAdminController(
        QuestionService questionService,
        ResultCategoryService categoryService,
        QuizService quizService
    ) {
        this.questionService = questionService;
        this.categoryService = categoryService;
        this.quizService = quizService;
    }

    @GetMapping("/questions")
    public List<Question> listQuestions() {
        return questionService.findAll(true);
    }

    @GetMapping("/questions/{id}")
    public ResponseEntity<Question> getQuestion(@PathVariable Long id) {
        return questionService.findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/questions")
    public ResponseEntity<Question> createQuestion(@RequestBody Question question) {
        return ResponseEntity.ok(questionService.create(question));
    }

    @PutMapping("/questions/{id}")
    public ResponseEntity<Question> updateQuestion(@PathVariable Long id, @RequestBody Question question) {
        return questionService.update(id, question)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        return questionService.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/categories")
    public List<ResultCategory> listCategories() {
        return categoryService.findAll();
    }

    @PostMapping("/categories")
    public ResponseEntity<ResultCategory> createCategory(@RequestBody ResultCategory category) {
        return ResponseEntity.ok(categoryService.create(category));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ResultCategory> updateCategory(@PathVariable Long id, @RequestBody ResultCategory category) {
        return categoryService.update(id, category)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        return categoryService.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/attempts")
    public List<QuizAttempt> listAttempts() {
        return quizService.findAllAttempts();
    }

    @GetMapping("/attempts/{id}")
    public ResponseEntity<QuizAttempt> getAttempt(@PathVariable Long id) {
        return quizService.findAttempt(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/applicants")
    public List<QuizApplicantStatus> listApplicants() {
        return quizService.findAllApplicants();
    }

    @PostMapping("/applicants/{email}/unlock")
    public ResponseEntity<Void> unlockApplicant(@PathVariable String email) {
        quizService.unlock(email);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}

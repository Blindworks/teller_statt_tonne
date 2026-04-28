package de.tellerstatttonne.backend.quiz;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/quiz")
public class QuizPublicController {

    private final QuestionService questionService;
    private final QuizService quizService;

    public QuizPublicController(QuestionService questionService, QuizService quizService) {
        this.questionService = questionService;
        this.quizService = quizService;
    }

    @GetMapping("/questions")
    public List<Question> questions() {
        return questionService.findAll(false);
    }

    @PostMapping("/submit")
    public ResponseEntity<QuizResult> submit(@RequestBody QuizSubmission submission) {
        return ResponseEntity.ok(quizService.submit(submission));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
